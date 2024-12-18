/*
 * ReqSmith - Build application from requirements
 * Copyright (c) 2024. Kovi <kovihome86@gmail.com>
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

import dev.reqsmith.composer.common.Log
import dev.reqsmith.composer.common.plugin.Plugin
import dev.reqsmith.composer.common.plugin.PluginDef
import dev.reqsmith.composer.common.plugin.PluginType
import dev.reqsmith.composer.common.templating.Template
import dev.reqsmith.composer.generator.entities.IGMAction
import dev.reqsmith.composer.generator.entities.InternalGeneratorModel
import dev.reqsmith.composer.parser.entities.Application
import dev.reqsmith.composer.parser.entities.ReqMSource
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.FileWriter
import java.util.Properties

open class SpringFrameworkBuilder : WebFrameworkBuilder(), Plugin {
    private var applicationName : String? = null

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
        buildScriptUpdates["plugins"]?.addAll(listOf(
                "kotlin(\"plugin.spring\"):2.0.20", // TODO ez függ a nyelvtől
                "id:org.springframework.boot:3.4.0",
                "id:io.spring.dependency-management:1.1.6"
        ))
        buildScriptUpdates["dependencies"]?.addAll(listOf(
                "org.springframework.boot:spring-boot-starter-web",
                "com.fasterxml.jackson.module:jackson-module-kotlin",
                "org.jetbrains.kotlin:kotlin-reflect"
        ))
    }

    open fun addSpringApplicationProperties(props: Properties) {
        props.setProperty("spring.application.name", "$applicationName.")
    }

    override fun processResources(reqmResourcesFolderName: String, buildResourcesFolderName: String, reqm: ReqMSource) {
        super.processResources(reqmResourcesFolderName, buildResourcesFolderName, reqm)

        // create or update application.properties
        val props = Properties()
        val propFileName = "$buildResourcesFolderName/application.properties"
        if (File(propFileName).exists()) {
            FileInputStream(propFileName).use {
                props.load(it)
            }
        }
        if (applicationName.isNullOrBlank()) applicationName = reqm.applications[0].qid!!.id
        addSpringApplicationProperties(props)
        FileOutputStream(propFileName, false).use {
            props.store(it, "Spring application properties for $applicationName")
        }

        // generate index.html page
        if (reqm.views.none { it.qid.toString() == "index" }) {
            val startView = reqm.applications[0].definition.properties.find { it.key == "startView" }
            val viewName = if (startView != null) startView.value!! else "#"
            val context = mapOf( "WelcomePage" to viewName)
            val indexContent = Template().translateFile(context, "templates/index.html.st")
            FileWriter("$buildResourcesFolderName/index.html", false).use { it.write(indexContent) }
            Log.info("Generating view $buildResourcesFolderName/index.html")
        }

    }

}