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

import dev.reqsmith.composer.common.ART_FOLDER_NAME
import dev.reqsmith.composer.common.Project
import dev.reqsmith.composer.common.WholeProject
import dev.reqsmith.composer.common.configuration.ConfigManager
import dev.reqsmith.composer.common.formatter.NameFormatter
import dev.reqsmith.composer.common.plugin.Plugin
import dev.reqsmith.composer.common.plugin.PluginDef
import dev.reqsmith.composer.common.plugin.PluginType
import dev.reqsmith.composer.common.templating.Template
import dev.reqsmith.composer.generator.TemplateContextCollector
import dev.reqsmith.composer.parser.enumeration.Optionality
import dev.reqsmith.model.*
import dev.reqsmith.model.enumeration.StandardEvents
import dev.reqsmith.model.enumeration.StandardLayoutElements
import dev.reqsmith.model.enumeration.StandardTypes
import dev.reqsmith.model.igm.*
import dev.reqsmith.model.reqm.*
import java.io.File
import java.io.FileNotFoundException
import java.util.*

private const val ID_FIELD_NAME = "id"

private const val ID_FIELD_TYPE = "Long"

private const val CRUD_ACTION_PERSIST = "persist"

private const val CRUD_ACTION_DELETE = "delete"

private const val CRUD_ACTION_GET = "get"

private const val CRUD_ACTION_LISTALL = "listAll"

private const val SPRING_CLASS_MODEL = "org.springframework.ui.Model"

/**
 * Spring Framework Builder
 *
 * It works as a plugin for: frameworks.web, feature.persistence, feature.template
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

    private fun findIgmClass(className: String): IGMClass {
        val dataClassFullName = WholeProject.projectModel.igm.classes.keys.find { it.substringAfterLast('.') == className }
        return WholeProject.projectModel.igm.classes.getOrDefault(dataClassFullName, IGMClass(className))
    }

    // TODO: move it another class
    private fun findInLayoutNodes(node: Property, expression: (Property) -> Boolean): Property? {
        if (expression(node)) return node
        node.simpleAttributes.forEach { subNode -> findInLayoutNodes(subNode, expression)?.let { return it } }
        return null
    }

    // TODO: move it another class
    private fun traverseLayoutNodes(node: Property, action: (Property) -> Unit) {
        action(node)
        node.simpleAttributes.forEach { subNode -> traverseLayoutNodes(subNode, action) }
    }

    // TODO: move it another class
    private fun findViewLayoutElement(view: View, elementName: String): Property? {
        val layout = view.definition.properties.find { it.key == VIEW_ATTRIBUTE_LAYOUT }
        if (layout != null) {
            return findInLayoutNodes(layout) { it.key == elementName }
        }
        return null
    }

    // TODO: move it another class
    private fun findViewEvent(view: View, eventType: String): String? {
        view.definition.properties.find { it.key == VIEW_ATTRIBUTE_LAYOUT }?.let { layout ->
            findInLayoutNodes(layout) { it.key == REQM_GENERAL_ATTRIBUTE_EVENTS}?.let { events ->
                events.simpleAttributes.find { it.key == eventType }?.let {
                    return it.value
                }
            }
        }
        return null
    }

    override fun buildView(view: View, templateContext: MutableMap<String, String>) {
        super.buildView(view, templateContext)

        // check view is it is a template type (
        val isTemplate = view.definition.featureRefs.any { it.qid.toString() == FEATURE_TEMPLATE }
        val isResource  = view.definition.featureRefs.any { it.qid.toString() == FEATURE_RESOURCE }
        val igmView = WholeProject.projectModel.igm.views.getOrDefault(view.qid.toString(), null)
        val dataAttributes : MutableList<Pair<String, String>> = mutableListOf()
        if (igmView != null) {
            findDataAttributeInNode(igmView.layout, dataAttributes)
        }

        if (isResource || isTemplate || dataAttributes.isNotEmpty()) {
            hasTemplateViews = true
            // create a controller class for this view
            val domainName = if (!view.qid?.domain.isNullOrBlank()) view.qid?.domain else WholeProject.projectModel.igm.rootPackage
            val className = view.qid!!.id!!
            val serviceClasses = dataAttributes.map { "$domainName.service.${it.second}Service" }
            val formClass = dataAttributes.any { it.first == "form" }

            // create controller class
            getSpringController("$domainName.controller.${className}Controller", serviceClasses).apply {

                // create init event handler action
                getAction(className.lowercase()).apply {
                    this.annotations.add(IGMAction.IGMAnnotation(addImport("org.springframework.web.bind.annotation.GetMapping")).apply {
                        parameters.add(IGMAction.IGMAnnotationParam("", "/${className}.${getViewLanguage()}"))
                    })
                    if (formClass) {
                        parameters.add(IGMAction.IGMActionParam(ID_FIELD_NAME, ID_FIELD_TYPE).apply {
                            annotations.add(IGMAction.IGMAnnotation(addImport("org.springframework.web.bind.annotation.RequestParam")))
                        })
                    }
                    parameters.add(IGMAction.IGMActionParam("model", addImport(SPRING_CLASS_MODEL)))
                    returns(StandardTypes.string.name)

                    // read data
                    dataAttributes.forEach { (nodeType, dataClass) ->
                        val actionName = findViewEvent(view, StandardEvents.init.name) ?: if (nodeType == StandardLayoutElements.datatable.name) CRUD_ACTION_LISTALL else CRUD_ACTION_GET
                        val action = "$domainName.service.${dataClass}Service.$actionName"
                        var dataVar = dataClass.lowercase()
                        if (nodeType == StandardLayoutElements.datatable.name) {
                            dataVar = "${dataVar}s"
                            addStmt(IGMStatement.set, dataVar, action)
                        } else {
                            addStmt(IGMStatement.set, dataVar, action, ID_FIELD_NAME)
                        }
                        addStmt(IGMStatement.call, "model.addAttribute", "'$dataVar'", dataVar)
                    }

                    val viewTemplateContext = TemplateContextCollector().getItemTemplateContext(view.qid, view.definition.properties, "view").apply {
                        putAll(templateContext)
                    }
                    viewTemplateContext.forEach {
                        addStmt(IGMStatement.call, "model.addAttribute", "'${it.key}'", "'${it.value}'")
                    }
                    addStmt(IGMStatement.`return`, "'$className'")
                }

                // create action to create entity
                val formAttribute = dataAttributes.find { it.first == StandardLayoutElements.form.name }
                if (formAttribute != null) {
                    val dataClass = findIgmClass(formAttribute.second)
                    val serviceClassName = WholeProject.sourceArchitecture.serviceName(dataClass.id)
                    val actionName = findViewEvent(view, StandardEvents.submitForm.name) ?: CRUD_ACTION_PERSIST
                    val persistServiceAction = "$serviceClassName.$actionName"
                    val actionUrl = "/data/${dataClass.id.substringAfterLast('.').lowercase()}/${StandardEvents.submitForm.name}"
                    var returnViewName = view.definition.properties.find { it.key == "returnView" }?.value
                    returnViewName = if (returnViewName != null) "/$returnViewName.${getViewLanguage()}" else "/"

                    getAction(StandardEvents.submitForm.name).apply {
                        annotations.add(
                            IGMAction.IGMAnnotation(addImport("org.springframework.web.bind.annotation.PostMapping"))
                                .apply {
                                    parameters.add(IGMAction.IGMAnnotationParam("", actionUrl))
                                })
                        parameters.add(IGMAction.IGMActionParam("formData", addImport(dataClass.id)).apply {
                            this.annotations.add(IGMAction.IGMAnnotation(addImport("org.springframework.web.bind.annotation.ModelAttribute")))
                        })
                        parameters.add(IGMAction.IGMActionParam("model", addImport(SPRING_CLASS_MODEL)))
                        returns(StandardTypes.string.name)

                        addStmt(IGMStatement.print, "'controller $actionUrl invoked'")
                        addStmt(IGMStatement.call, persistServiceAction, "formData")
                        addStmt(IGMStatement.`return`, "'redirect:$returnViewName'")
                    }
                }

                // delete action for delete datatable item
                val datatableAttribute = dataAttributes.find { it.first == StandardLayoutElements.datatable.name }
                if (datatableAttribute != null) {
                    val dataClass = findIgmClass(datatableAttribute.second)
                    val serviceClassName = WholeProject.sourceArchitecture.serviceName(dataClass.id)
                    val actionName = findViewEvent(view, StandardEvents.deleteItem.name) ?: CRUD_ACTION_DELETE
                    val deleteServiceAction = "$serviceClassName.$actionName"
                    val actionUrl = "/data/${dataClass.id.substringAfterLast('.').lowercase()}/${StandardEvents.deleteItem.name}"
                    getAction(StandardEvents.deleteItem.name).apply {
                        annotations.add(
                            IGMAction.IGMAnnotation(addImport("org.springframework.web.bind.annotation.GetMapping"))
                                .apply {
                                    parameters.add(IGMAction.IGMAnnotationParam("", actionUrl))
                                })
                        parameters.add(IGMAction.IGMActionParam(ID_FIELD_NAME, ID_FIELD_TYPE).apply {
                            this.annotations.add(IGMAction.IGMAnnotation(addImport("org.springframework.web.bind.annotation.RequestParam")))
                        })
                        parameters.add(IGMAction.IGMActionParam("model", addImport(SPRING_CLASS_MODEL)))

                        returns(StandardTypes.string.name)

                        addStmt(IGMStatement.print, "'controller $actionUrl invoked'")
                        addStmt(IGMStatement.call, deleteServiceAction, ID_FIELD_NAME)
                        addStmt(IGMStatement.`return`, "'redirect:/'")
                    }
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

    override fun buildApplication(app: Application) {
        super.buildApplication(app)
        val appClass = WholeProject.projectModel.igm.getClass(app.qid!!.toString())
        appClass.annotations.add(IGMAction.IGMAnnotation("SpringBootApplication"))
        appClass.addImport("org.springframework.boot.autoconfigure.SpringBootApplication")

        val mainAction = appClass.getAction("main")
        // TODO a generic paraméter megadásának legyen külön módja
        // TODO a *args-t is valami módosítóval lehessen megadni
        val call = IGMAction.IGMActionStmt(IGMStatement.call).withParam("runApplication<${app.qid!!.id}>").withParam("*${mainAction.parameters[0].name}")
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

    override fun processResources(reqmResourcesFolderName: String, buildResourcesFolderName: String) {
        super.processResources(reqmResourcesFolderName, buildResourcesFolderName)

        // create or update application.properties
        val propFile = File("$buildResourcesFolderName/application.properties")

        if (propFile.exists()) {
            propFile.bufferedReader().use {
                applicationProperties.load(it)
            }
        }
        if (applicationName.isNullOrBlank()) applicationName = WholeProject.projectModel.source.applications[0].qid!!.id
        addSpringApplicationProperties()
        propFile.bufferedWriter().use { writer ->
            writer.write("# Spring application properties for $applicationName\n\n")
            applicationProperties.forEach { (k, v) -> writer.write("$k=$v\n") }
        }

        // generate index.html page
        if (WholeProject.projectModel.source.views.none { it.qid.toString() == "index" }) {
            val startView = WholeProject.projectModel.source.applications[0].definition.properties.find { it.key == "startView" }
            val viewName = if (startView != null) startView.value!! else "#"
            val context = mapOf( "WelcomePage" to viewName)
            val indexContent = Template().translateFile(context, "templates/index.html.st")
            WholeProject.projectModel.resources.add(Pair("<save>$indexContent", "$buildResourcesFolderName/${getViewFolder()}/index.html"))
        }

        // copy error.html page
        val uri = javaClass.getResource("/templates/error.html.st") ?: throw FileNotFoundException("/templates/error.html.st")
        WholeProject.projectModel.resources.add(Pair("<save>${uri.readText()}", "$buildResourcesFolderName/${getViewFolder()}/error.html"))

        // file resource files
        var hasTemplateFiles = false
        WholeProject.projectModel.source.views.forEach { view ->
            view.definition.featureRefs.find { it.qid.toString() == FEATURE_RESOURCE }?.let { fr ->
                fr.properties.find { it.key == FEATURE_RESOURCE_ATTRIBUTE_FILE }?.value?.let { fileName ->
                    val fp = NameFormatter.deliterateText(fileName)
                    val fn = fp.substringAfterLast('/').substringAfterLast("\\")
                    val destFileName = "$buildResourcesFolderName/${getViewFolder()}/$fn"
                    WholeProject.projectModel.resources.add(Pair("$reqmResourcesFolderName/$fp", destFileName))
                    hasTemplateFiles = true
                }
            }
        }

        // copy arts
        if (hasTemplateFiles) {
            val copyFrom = "$reqmResourcesFolderName/${ART_FOLDER_NAME}"
            val copyTo =
                "$buildResourcesFolderName/${getArtFolder()}/${ART_FOLDER_NAME}"
            Project.ensureFolderExists(copyTo, null)

            File(copyFrom).listFiles()?.filter { it.isFile }?.forEach { file ->
                WholeProject.projectModel.resources.add(Pair("$copyFrom/${file.name}", "$copyTo/${file.name}"))
            }
        }

    }

    override fun applyFeatureOnEntity(ent: Entity, feature: Feature) {
        if (feature.qid.toString() == "Persistent") {
            // add spring data plugins and dependencies to the build system
            if (!springPlugins.contains(springDataPlugins[0])) {
                springPlugins.addAll(springDataPlugins)
                springDependencies.addAll(springDataDependencies)

                // add H2 database as default
                val h2 = H2Configurator()
                springDependencies.addAll(h2.dependencies)
                h2.properties.forEach { applicationProperties.setProperty(it.first, it.second) }
            }
            applySpringDataPersistenceOnEntity(ent, feature)
        } else {
            TODO("Not implemented yet!")
        }
    }

    class H2Configurator {
        val dependencies = listOf("rt:com.h2database:h2") // runtimeOnly
        val properties = listOf(
            Pair("spring.datasource.url", "jdbc:h2:mem:testdb;DB_CLOSE_ON_EXIT=FALSE;AUTO_RECONNECT=TRUE"),
            Pair("spring.datasource.driver-class-name", "org.h2.Driver"),
            Pair("spring.datasource.username", "sa"),
            Pair("spring.datasource.password", ""),
            Pair("spring.jpa.hibernate.ddl-auto", "update"),
            Pair("spring.h2.console.enabled", "true"),
            Pair("spring.h2.console.path", "/h2-console")
        )
    }

    private fun applySpringDataPersistenceOnEntity(ent: Entity, feature: Feature) {
        val entityClassName = ent.qid!!.id!!

        // get IGM class
        val igmClass = WholeProject.projectModel.igm.getClass(ent.qid.toString())

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

        // add member from persistent feature
        persistentProperties.forEach { prop ->
            if (!listOf(StandardTypes.valueList.name, StandardTypes.propertyList.name).contains(prop.type)) {
                igmClass.getMember(prop.key!!).apply {
                    optionality = Optionality.Optional.name
                    type = prop.type ?: ConfigManager.defaults.getOrDefault("propertyType", "String")
                }
            }
        }

        // add new class members from persistenceProperties
        igmClass.getMember(ID_FIELD_NAME).apply {
            type = ID_FIELD_TYPE
            value = "0"
            optionality = Optionality.Mandatory.name
            annotations.add(IGMAction.IGMAnnotation(igmClass.addImport("jakarta.persistence.Id")))
            annotations.add(IGMAction.IGMAnnotation(igmClass.addImport("jakarta.persistence.GeneratedValue")).apply {
                parameters.add(IGMAction.IGMAnnotationParam("strategy", "${igmClass.addImport("jakarta.persistence.GenerationType")}.IDENTITY", StandardTypes.string.name))
            })
        }

        // annotate class members
        // no annotation needed yet

        // create repository interface for this entity
        val repoClassName = WholeProject.sourceArchitecture.repositoryName(igmClass.id)
        WholeProject.projectModel.igm.getClass(repoClassName).apply {
            interfaceType = true
            parent = addImport("org.springframework.data.jpa.repository.JpaRepository")
            parentClasses.addAll(listOf(addImport(igmClass.id), ID_FIELD_TYPE))
        }

        // create service class for this entity
        // controller name: <base-package>/controller/<entity>Controller
        val serviceClassName = WholeProject.sourceArchitecture.serviceName(igmClass.id)
        val repo = repoClassName.substringAfterLast('.').replaceFirstChar { it.lowercase() }
        WholeProject.projectModel.igm.getClass(serviceClassName).apply {
            annotations.add(IGMAction.IGMAnnotation(addImport("org.springframework.stereotype.Service")))
            ctorParams.add(IGMAction.IGMActionParam(repo, addImport(repoClassName)))

            addImport(repoClassName)

            // create action for create entity
            getAction(CRUD_ACTION_PERSIST).apply {
                parameters.add(IGMAction.IGMActionParam("data", addImport(igmClass.id)))
                addStmt(IGMStatement.call, "${repoClassName}.save", "data")
            }

            getAction(CRUD_ACTION_DELETE).apply {
                parameters.add(IGMAction.IGMActionParam(ID_FIELD_NAME, ID_FIELD_TYPE))
                addStmt(IGMStatement.call, "${repoClassName}.deleteById", ID_FIELD_NAME)
            }

            getAction(CRUD_ACTION_GET).apply {
                parameters.add(IGMAction.IGMActionParam(ID_FIELD_NAME, ID_FIELD_TYPE))
                returns(entityClassName)
                val varname = entityClassName.replaceFirstChar { it.lowercase() }
                addStmt(IGMStatement.set, varname, "${repoClassName}.findById", ID_FIELD_NAME)
                addStmt(IGMStatement.`return`, "$varname.orElse($entityClassName())") // TODO: is kotlin or spring data specific?
            }

            getAction(CRUD_ACTION_LISTALL).apply {
                returns(entityClassName, true)
                val varname = "${entityClassName.replaceFirstChar { it.lowercase() }}s"
                addStmt(IGMStatement.set, varname, "${repoClassName}.findAll")
                addStmt(IGMStatement.`return`, varname)
            }
        }

    }

    private fun getSpringController(controllerClassName: String, serviceClassNames: List<String> = listOf()): IGMClass {
        return WholeProject.projectModel.igm.getClass(controllerClassName).apply {
            annotations.add(IGMAction.IGMAnnotation(addImport("org.springframework.stereotype.Controller")))
            serviceClassNames.forEach { serviceClassName ->
                val service = serviceClassName.substringAfterLast('.').replaceFirstChar { it.lowercase() }
                ctorParams.add(IGMAction.IGMActionParam(service, addImport(serviceClassName)))
            }
        }

    }

}