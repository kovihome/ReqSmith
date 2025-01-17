/*
 * ReqSmith - Build application from requirements
 * Copyright (c) 2024-2025. Kovi <kovihome86@gmail.com>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package dev.reqsmith.composer.generator.plugin.framework

import dev.reqsmith.composer.common.plugin.Plugin
import dev.reqsmith.composer.common.plugin.PluginDef
import dev.reqsmith.composer.common.plugin.PluginType
import dev.reqsmith.composer.common.templating.Template
import dev.reqsmith.composer.parser.enumeration.Optionality
import dev.reqsmith.model.ProjectModel
import dev.reqsmith.model.igm.IGMAction
import dev.reqsmith.model.igm.InternalGeneratorModel
import dev.reqsmith.model.reqm.Application
import dev.reqsmith.model.reqm.Entity
import dev.reqsmith.model.reqm.Feature
import dev.reqsmith.model.reqm.Property
import java.io.File
import java.util.*

private const val ID_TYPE = "Long"

/**
 * Spring Framework Builder
 *
 * It works as plugin for:
 *
 *   frameworks.web
 *   feature.persistence
 */
open class SpringFrameworkBuilder : WebFrameworkBuilder(), Plugin {
    private var applicationName : String? = null
    val applicationProperties = Properties()

    private val springPlugins = mutableListOf(
        "kotlin(\"plugin.spring\"):2.0.20", // TODO ez függ a nyelvtől
        "id:org.springframework.boot:3.4.0",
        "id:io.spring.dependency-management:1.1.6")
    private val springDependencies = mutableListOf(
        "org.springframework.boot:spring-boot-starter-web",
        "com.fasterxml.jackson.module:jackson-module-kotlin",
        "org.jetbrains.kotlin:kotlin-reflect")
    private val springDataPlugins = mutableListOf(
        "kotlin(\"plugin.jpa\"):2.0.20")
    private val springDataDependencies = mutableListOf(
        "org.springframework.boot:spring-boot-starter-data-jpa") // Spring Data JPA

    override fun definition(): PluginDef {
        return PluginDef("framework.web.spring", PluginType.Framework)
    }

    override fun getViewFolder(): String  = "static"

    override fun buildApplication(app: Application, igm: InternalGeneratorModel) {
        super.buildApplication(app, igm)
        val appClass = igm.getClass(app.qid!!.toString())
        appClass.annotations.add(IGMAction.IGMAnnotation("SpringBootApplication"))
        appClass.addImport("org.springframework.boot.autoconfigure.SpringBootApplication")

        val mainAction = appClass.getAction("main")
        // TODO a generic paraméter megadásának legyen külön módja
        // TODO a *args-t is valami módosítóval lehessen megadni
        val call = IGMAction.IGMActionStmt("call").withParam("runApplication<${app.qid!!.id}>").withParam("*${mainAction.parameters[0].name}")
        mainAction.statements.add(call)
        appClass.addImport("org.springframework.boot.runApplication")

        applicationName = app.qid!!.id
    }

    override fun collectBuildScriptElement(buildScriptUpdates: Map<String, MutableList<String>>) {
        super.collectBuildScriptElement(buildScriptUpdates)
        buildScriptUpdates["plugins"]?.addAll(springPlugins)
        buildScriptUpdates["dependencies"]?.addAll(springDependencies)
    }

    open fun addSpringApplicationProperties() {
        applicationProperties.setProperty("spring.application.name", "$applicationName.")
    }

    override fun processResources(
        reqmResourcesFolderName: String,
        buildResourcesFolderName: String,
        projectModel: ProjectModel
    ) {
        super.processResources(reqmResourcesFolderName, buildResourcesFolderName, projectModel)

        // create or update application.properties
        val propFile = File("$buildResourcesFolderName/application.properties")

        if (propFile.exists()) {
            propFile.bufferedReader().use {
                applicationProperties.load(it)
            }
        }
        if (applicationName.isNullOrBlank()) applicationName = projectModel.source.applications[0].qid!!.id
        addSpringApplicationProperties()
        propFile.bufferedWriter().use { writer ->
            writer.write("# Spring application properties for $applicationName\n\n")
            applicationProperties.forEach { k, v -> writer.write("$k=$v\n") }
        }

        // generate index.html page
        if (projectModel.source.views.none { it.qid.toString() == "index" }) {
            val startView = projectModel.source.applications[0].definition.properties.find { it.key == "startView" }
            val viewName = if (startView != null) startView.value!! else "#"
            val context = mapOf( "WelcomePage" to viewName)
            val indexContent = Template().translateFile(context, "templates/index.html.st")
            projectModel.resources.add(Pair("<save>$indexContent", "$buildResourcesFolderName/${getViewFolder()}/index.html"))
        }

    }

    override fun applyFeatureOnEntity(ent: Entity, igm: InternalGeneratorModel, feature: Feature) {
        if (feature.qid.toString() == "Persistent") {
            if (!springPlugins.contains(springDataPlugins[0])) {
                springPlugins.addAll(springDataPlugins)
                springDependencies.addAll(springDataDependencies)

                // add H2 database as default
                val h2 = H2Configurator()
                springDependencies.addAll(h2.dependencies)
                h2.properties.forEach { applicationProperties.setProperty(it.first, it.second) }
            }
            applySpringDataPeristenceOnEntity(ent, igm, feature)
        } else {
            TODO("Not implemented yet!")
        }
    }

    class H2Configurator {
        val dependencies = listOf("com.h2database:h2")
        val properties = listOf(
            Pair("spring.datasource.url", "jdbc:h2:mem:testdb"),
            Pair("spring.datasource.driver-class-name", "org.h2.Driver"),
            Pair("spring.datasource.username", "sa"),
            Pair("spring.datasource.password", ""),
            Pair("spring.jpa.hibernate.ddl-auto", "update"),
            Pair("spring.h2.console.enabled", "true"),
            Pair("spring.h2.console.path", "/h2-console")
        )
    }

    private fun applySpringDataPeristenceOnEntity(ent: Entity, igm: InternalGeneratorModel, feature: Feature) {
        // get IGM class
        val igmClass = igm.getClass(ent.qid.toString())

        // get featureRef and merge properties
        val featureRef = ent.definition.featureRefs.find { it.qid.toString() == feature.qid.toString() }!!
        val persistentProperties : MutableList<Property> = mutableListOf()
        persistentProperties.addAll(featureRef.properties)
        feature.definition.properties.filter { it.key != "generator" }.forEach { prop ->
            if (persistentProperties.none { it.key == prop.key }) {
                persistentProperties.add(prop)
            }
        }

        // add class notifications
        igmClass.annotations.add(IGMAction.IGMAnnotation(igmClass.addImport("jakarta.persistence.Entity")))

        // add new class members from persistenceProperties
        igmClass.getMember("id").apply {
            type = ID_TYPE
            value = "0"
            optionality = Optionality.Mandatory.name
            annotations.add(IGMAction.IGMAnnotation(igmClass.addImport("jakarta.persistence.Id")))
            annotations.add(IGMAction.IGMAnnotation(igmClass.addImport("jakarta.persistence.GeneratedValue")).apply {
                parameters.add(IGMAction.IGMActionParam("strategy", "${igmClass.addImport("jakarta.persistence.GenerationType")}.IDENTITY"))
            })
        }

        persistentProperties.forEach { prop ->
            igmClass.getMember(prop.key!!).apply {
                optionality = Optionality.Mandatory.name
                type = prop.type ?: "String" // TODO: default type
            }
        }

        // annotate class members
        // no annotation needed yet

        // create repository interface for this entity
        val repoClassName = "${igmClass.id.replace("entities", "repository")}Repository"
        igm.getClass(repoClassName).apply {
            interfaceType = true
            parent = addImport("org.springframework.data.jpa.repository.JpaRepository")
            parentClasses.addAll(listOf(addImport(igmClass.id), ID_TYPE))
        }

    }

}