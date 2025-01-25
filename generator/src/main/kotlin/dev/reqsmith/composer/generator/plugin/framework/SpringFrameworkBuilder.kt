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

import dev.reqsmith.composer.common.Project
import dev.reqsmith.composer.common.configuration.ConfigManager
import dev.reqsmith.composer.common.plugin.Plugin
import dev.reqsmith.composer.common.plugin.PluginDef
import dev.reqsmith.composer.common.plugin.PluginType
import dev.reqsmith.composer.common.templating.Template
import dev.reqsmith.composer.generator.TemplateContextCollector
import dev.reqsmith.composer.parser.enumeration.Optionality
import dev.reqsmith.model.ProjectModel
import dev.reqsmith.model.enumeration.StandardTypes
import dev.reqsmith.model.igm.IGMAction
import dev.reqsmith.model.igm.IGMClass
import dev.reqsmith.model.igm.IGMView
import dev.reqsmith.model.igm.InternalGeneratorModel
import dev.reqsmith.model.reqm.*
import java.io.File
import java.io.FileNotFoundException
import java.util.*

private const val ID_TYPE = "Long"

private const val CRUD_ACTION_PERSIST = "persist"

private const val CRUD_ACTION_DELETE = "delete"

private const val CRUD_ACTION_GET = "get"

private const val CRUD_ACTION_LISTALL = "listAll"

/**
 * Spring Framework Builder
 *
 * It works as plugin for:
 *
 *   frameworks.web
 *   feature.persistence
 *   feature.template
 */
open class SpringFrameworkBuilder : WebFrameworkBuilder(), Plugin {
    private var applicationName : String? = null
    private val applicationProperties = Properties()

    private val springPlugins = mutableListOf(
        "kotlin(\"plugin.spring\"):2.0.20", // TODO ez függ a nyelvtől
        "id:org.springframework.boot:3.4.0",
        "id:io.spring.dependency-management:1.1.6")
    private val springDependencies = mutableListOf(
        "org.springframework.boot:spring-boot-starter-web",
        "com.fasterxml.jackson.module:jackson-module-kotlin",
        "org.jetbrains.kotlin:kotlin-reflect")
    private val springDataPlugins = listOf(
        "kotlin(\"plugin.jpa\"):2.0.20")
    private val springDataDependencies = listOf(
        "org.springframework.boot:spring-boot-starter-data-jpa") // Spring Data JPA
    private val thymeleafSpringDependencies = listOf(
        "org.springframework.boot:spring-boot-starter-thymeleaf")

    private var hasTemplateViews = false

    override fun definition(): PluginDef {
        return PluginDef("framework.web.spring", PluginType.Framework)
    }

    override fun getViewFolder(): String  = if (hasTemplateViews) "template" else "static"

    override fun getArtFolder(): String = "static"

    override fun buildView(view: View, igm: InternalGeneratorModel, templateContext: MutableMap<String, String>) {
        super.buildView(view, igm, templateContext)

        // check view is it is a template type (
        val isTemplate = view.definition.featureRefs.any { it.qid.toString() == "Template" }
        val igmView = igm.views.getOrDefault(view.qid.toString(), null)
        val dataAttributes : MutableList<Pair<String, String>> = mutableListOf()
        if (igmView != null) {
            findDataAttributeInNode(igmView.layout, dataAttributes)
        }

        if (isTemplate || dataAttributes.isNotEmpty()) {
            hasTemplateViews = true
            // create a controller class for this view
            val domainName = if (!view.qid?.domain.isNullOrBlank()) view.qid?.domain else /* TODO: application domain */ ConfigManager.defaults["domainName"]
            val className = view.qid!!.id!!
            val serviceClasses = dataAttributes.map { "$domainName.service.${it.second}Service" }
            val formClass = dataAttributes.any { it.first == "form" }
            
            getSpringController("$domainName.controller.${className}Controller", igm, serviceClasses).apply {

                getAction(className.lowercase()).apply {
                    this.annotations.add(IGMAction.IGMAnnotation(addImport("org.springframework.web.bind.annotation.GetMapping")).apply {
                        parameters.add(IGMAction.IGMAnnotationParam("", "/${className}.${getViewLanguage()}"))
                    })
                    if (formClass) {
                        parameters.add(IGMAction.IGMActionParam("id", ID_TYPE).apply {
                            annotations.add(IGMAction.IGMAnnotation(addImport("org.springframework.web.bind.annotation.RequestParam")))
                        })
                    }
                    parameters.add(IGMAction.IGMActionParam("model", addImport("org.springframework.ui.Model")))
                    returns(StandardTypes.string.name)

                    // read data
                    dataAttributes.forEach { (nodeType, dataClass) ->
                        var dataVar = dataClass.lowercase()
                        if (nodeType == "datatable") {
                            dataVar = "${dataVar}s"
                            addStmt("set", dataVar, "$domainName.service.${dataClass}Service.$CRUD_ACTION_LISTALL")
                        } else {
                            addStmt("set", dataVar, "$domainName.service.${dataClass}Service.$CRUD_ACTION_GET", "id")  // TODO get id parameter
                        }
                        addStmt("call", "model.addAttribute", "'$dataVar'", dataVar)
                    }

                    val viewTemplateContext = TemplateContextCollector().getItemTemplateContext(view.qid, view.definition.properties, "view").apply {
                        putAll(templateContext)
                    }
                    viewTemplateContext.forEach {
                        addStmt("call", "model.addAttribute", "'${it.key}'", "'${it.value}'")
                    }
                    addStmt("return", "'$className'")
                }
            }

        }

    }

    private fun findDataAttributeInNode(node: IGMView.IGMNode, dataAttributes: MutableList<Pair<String, String>>) {
        val dataAttr = node.attributes.find { it.first == "data" }
        if (dataAttr != null) {
            dataAttributes.add(Pair(node.name, dataAttr.second))
        }
        node.children.forEach { findDataAttributeInNode(it, dataAttributes) }
    }

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
        if (hasTemplateViews) {
            buildScriptUpdates["dependencies"]?.addAll(thymeleafSpringDependencies)
        }
    }

    open fun addSpringApplicationProperties() {
        applicationProperties.setProperty("spring.application.name", "$applicationName.")
        if (hasTemplateViews) {
            applicationProperties.setProperty("spring.thymeleaf.prefix", "classpath:/${getViewFolder()}/")
            applicationProperties.setProperty("spring.thymeleaf.suffix", ".${getViewLanguage()}")
        }
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

        // copy error.html page
        val uri = javaClass.getResource("/templates/error.html.st") ?: throw FileNotFoundException("/templates/error.html.st")
        projectModel.resources.add(Pair("<save>${uri.readText()}", "$buildResourcesFolderName/${getViewFolder()}/error.html"))

        // file template files
        var hasTemplateFiles = false
        projectModel.source.views.forEach { view ->
            view.definition.featureRefs.find { it.qid.toString() == "Template" }?.let { fr ->
                fr.properties.find { it.key == "file" }?.value?.let { fileName ->
                    val fp = fileName.removeSurrounding("'").removeSurrounding("\"")
                    val fn = fp.substringAfterLast('/').substringAfterLast("\\")
                    val destFileName = "$buildResourcesFolderName/${getViewFolder()}/$fn"
                    projectModel.resources.add(Pair("$reqmResourcesFolderName/$fp", destFileName))
                    hasTemplateFiles = true
                }
            }
        }

        // copy arts
        if (hasTemplateFiles) {
            val copyFrom = "$reqmResourcesFolderName/art"
            val copyTo =
                "$buildResourcesFolderName/${getArtFolder()}/art"   // TODO: get art folder name from project
            Project.ensureFolderExists(copyTo, null)

            File(copyFrom).listFiles()?.filter { it.isFile }?.forEach { file ->
                projectModel.resources.add(Pair("$copyFrom/${file.name}", "$copyTo/${file.name}"))
            }
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
            applySpringDataPersistenceOnEntity(ent, igm, feature)
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

    private fun applySpringDataPersistenceOnEntity(ent: Entity, igm: InternalGeneratorModel, feature: Feature) {
        val entityClassName = ent.qid!!.id!!

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

        // set all entity members to optional, and add annotation to date fields
        igmClass.members.forEach {
            it.optionality = Optionality.Optional.name
            if (it.type == "Date") {
                it.annotations.add(IGMAction.IGMAnnotation(igmClass.addImport("org.springframework.format.annotation.DateTimeFormat")).apply {
                    parameters.add(IGMAction.IGMAnnotationParam("pattern", "yyyy-MM-dd"))
                })
            }
        }

        // add new class members from persistenceProperties
        igmClass.getMember("id").apply {
            type = ID_TYPE
            value = "0"
            optionality = Optionality.Mandatory.name
            annotations.add(IGMAction.IGMAnnotation(igmClass.addImport("jakarta.persistence.Id")))
            annotations.add(IGMAction.IGMAnnotation(igmClass.addImport("jakarta.persistence.GeneratedValue")).apply {
                parameters.add(IGMAction.IGMAnnotationParam("strategy", "${igmClass.addImport("jakarta.persistence.GenerationType")}.IDENTITY", StandardTypes.string.name))
            })
        }

        // add member from persistent feature
        persistentProperties.forEach { prop ->
            igmClass.getMember(prop.key!!).apply {
                optionality = Optionality.Optional.name
                type = prop.type ?: ConfigManager.defaults.getOrDefault("propertyType", "String")
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

        // create service class for this entity
        // controller name: <base-package>/controller/<entity>Controller
        val serviceClassName = "${igmClass.id.replace("entities", "service")}Service"
        val repo = repoClassName.substringAfterLast('.').replaceFirstChar { it.lowercase() }
        igm.getClass(serviceClassName).apply {
            annotations.add(IGMAction.IGMAnnotation(addImport("org.springframework.stereotype.Service")))
            ctorParams.add(IGMAction.IGMActionParam(repo, addImport(repoClassName)))

            addImport(repoClassName)

            // create action for create entity
            getAction(CRUD_ACTION_PERSIST).apply {
                parameters.add(IGMAction.IGMActionParam("data", addImport(igmClass.id)))
                addStmt("call", "${repoClassName}.save", "data")
            }

            getAction(CRUD_ACTION_DELETE).apply {
                parameters.add(IGMAction.IGMActionParam("id", ID_TYPE))
                addStmt("call", "${repoClassName}.deleteById", "id")
            }

            getAction(CRUD_ACTION_GET).apply {
                parameters.add(IGMAction.IGMActionParam("id", ID_TYPE))
                returns(entityClassName)
                val varname = entityClassName.replaceFirstChar { it.lowercase() }
                addStmt("set", varname, "${repoClassName}.findById", "id")
                addStmt("return", "$varname.orElse($entityClassName())") // TODO: is kotlin or spring data specific?
            }

            getAction(CRUD_ACTION_LISTALL).apply {
                returns(entityClassName, true)
                val varname = "${entityClassName.replaceFirstChar { it.lowercase() }}s"
                addStmt("set", varname, "${repoClassName}.findAll")
                addStmt("return", varname)
            }

        }

        // create controller class for this entity
        // controller name: <base-package>/controller/<entity>Controller
        val controllerClassName = "${igmClass.id.replace("entities", "controller")}Controller"
        getSpringController(controllerClassName, igm, listOf(serviceClassName)).apply {

            // create action for create entity
            var actionUrl = "/data/${igmClass.id.substringAfterLast('.').lowercase()}/${CRUD_ACTION_PERSIST}"
            getAction(CRUD_ACTION_PERSIST).apply {
                annotations.add(IGMAction.IGMAnnotation(addImport("org.springframework.web.bind.annotation.PostMapping")).apply {
                    parameters.add(IGMAction.IGMAnnotationParam("", actionUrl))
                })
                parameters.add(IGMAction.IGMActionParam("formData", addImport(igmClass.id)).apply {
                    this.annotations.add(IGMAction.IGMAnnotation(addImport("org.springframework.web.bind.annotation.ModelAttribute")))
                })
                parameters.add(IGMAction.IGMActionParam("model", addImport("org.springframework.ui.Model")))
                returns(StandardTypes.string.name)

                addStmt("print", "'controller $actionUrl invoked'")
                addStmt("call", "$serviceClassName.$CRUD_ACTION_PERSIST", "formData")
                addStmt("return", "'redirect:/'")
            }

            // create action for create delete
            actionUrl = "/data/${igmClass.id.substringAfterLast('.').lowercase()}/$CRUD_ACTION_DELETE"
            getAction(CRUD_ACTION_DELETE).apply {
                annotations.add(IGMAction.IGMAnnotation(addImport("org.springframework.web.bind.annotation.GetMapping")).apply {
                    parameters.add(IGMAction.IGMAnnotationParam("", actionUrl))
                })
                parameters.add(IGMAction.IGMActionParam("id", ID_TYPE).apply {
                    this.annotations.add(IGMAction.IGMAnnotation(addImport("org.springframework.web.bind.annotation.RequestParam")))
                })
                parameters.add(IGMAction.IGMActionParam("model", addImport("org.springframework.ui.Model")))

                returns(StandardTypes.string.name)

                addStmt("print", "'controller $actionUrl invoked'")
                addStmt("call", "$serviceClassName.$CRUD_ACTION_DELETE", "id")
                addStmt("return", "'redirect:/'")
            }



        }

    }

    private fun getSpringController(controllerClassName: String, igm: InternalGeneratorModel, serviceClassNames: List<String> = listOf()): IGMClass {
        return igm.getClass(controllerClassName).apply {
            annotations.add(IGMAction.IGMAnnotation(addImport("org.springframework.stereotype.Controller")))
            serviceClassNames.forEach { serviceClassName ->
                val service = serviceClassName.substringAfterLast('.').replaceFirstChar { it.lowercase() }
                ctorParams.add(IGMAction.IGMActionParam(service, addImport(serviceClassName)))
            }
        }

    }

}