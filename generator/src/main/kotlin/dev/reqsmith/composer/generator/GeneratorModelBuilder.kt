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

package dev.reqsmith.composer.generator

import dev.reqsmith.composer.common.Project
import dev.reqsmith.composer.common.configuration.ConfigManager
import dev.reqsmith.composer.common.plugin.PluginManager
import dev.reqsmith.composer.common.plugin.PluginType
import dev.reqsmith.composer.common.templating.Template
import dev.reqsmith.model.igm.IGMAction
import dev.reqsmith.model.igm.IGMClass
import dev.reqsmith.model.igm.InternalGeneratorModel
import dev.reqsmith.composer.generator.plugin.framework.FrameworkBuilder
import dev.reqsmith.model.ProjectModel
import dev.reqsmith.model.enumeration.StandardTypes
import dev.reqsmith.model.reqm.*
import kotlin.reflect.full.isSubclassOf

class GeneratorModelBuilder(private val projectModel: ProjectModel, private val resourcesFolderName: String, private val project: Project) {
    private val appRootPackage = projectModel.source.applications[0].qid?.domain ?: "com.sample.app"
    var viewGeneratorName = ""
    var suggestedWebFolderName = ""
    var codeBuilder: FrameworkBuilder? = null
    var viewBuilder: FrameworkBuilder? = null

    private fun determineBuilders() {
        // determine code generator
        val app = projectModel.source.applications[0]
        val generatorName = app.definition.properties.find { it.key == "generator" }?.value ?: "framework.base"
        codeBuilder = determineFrameworkBuilder(generatorName)

        // determine view builder
        val viewLang = codeBuilder!!.getViewLanguage()
        val templGen = searchViewFeatureGenerator(projectModel.source)
        var generatorId = ConfigManager.defaults[templGen]
        var plugin: String? = null
        if (generatorId == null) {
            plugin = ConfigManager.defaults[viewLang]
            generatorId = if (plugin != null) "$generatorName.$plugin" else null
        }
        viewBuilder = if (generatorId != null) {
            PluginManager.get<FrameworkBuilder>(PluginType.Framework, generatorId)
        } else {
            codeBuilder
        }

        //
        if (plugin != null) {
            viewGeneratorName = "$viewLang.$plugin"
            suggestedWebFolderName = codeBuilder!!.getViewFolder()
        } else if (generatorId != null) {
            suggestedWebFolderName = viewBuilder!!.getViewFolder()
        }
    }

    private fun searchViewFeatureGenerator(reqMSource: ReqMSource): String? {
        val generators: MutableList<String> = ArrayList()
        reqMSource.views.forEach { view ->
            view.definition.featureRefs.filter { it.qid.toString() == "Template" }.forEach { fr ->
                val feature = reqMSource.features.find { it.qid.toString() == fr.qid.toString() }
                feature?.let {
                    feature.definition.properties.forEach {
                        if (it.key == "generator") {
                            generators.add(it.value!!.removeSurrounding("'").removeSurrounding("\""))
                        }
                    }
                }
            }
        }
        return if (generators.isNotEmpty()) generators[0] else null
    }

    fun build() {
        // set root package for internal generator model
        projectModel.igm.rootPackage = appRootPackage

        // determine code and view generator
        determineBuilders()

        // create application
        val app = projectModel.source.applications[0]
        createApplication(app, projectModel.igm, codeBuilder!!)

        // create classes for classes
        projectModel.source.classes.forEach {
            when {
                it.enumeration -> createEnumeration(it, projectModel.igm)
                else -> createClass(it, projectModel.igm)
            }
        }

        // create classes for entities
        projectModel.source.entities.forEach { createEntity(it, projectModel.igm) }

        // create class methods for actions
        val templateContext = TemplateContextCollector().getItemTemplateContext(projectModel.source.applications[0].qid, projectModel.source.applications[0].definition.properties, "app")
        projectModel.source.actions.forEach { createAction(it, projectModel.igm, templateContext) }

        // create view descriptors
        projectModel.source.views.forEach { createView(it, projectModel.igm, templateContext, viewBuilder!!) }

        // manage additional resources
        val reqmResourceFolder = "${project.projectFolder}/${project.buildSystem.resourceFolder}"
        if (!viewBuilder!!::class.isSubclassOf(codeBuilder!!::class)) {
            codeBuilder?.processResources(reqmResourceFolder, resourcesFolderName, projectModel)
        }
        viewBuilder?.processResources(reqmResourceFolder, resourcesFolderName, projectModel)

    }

    private fun createView(viewModel: View, igm: InternalGeneratorModel, templateContext: MutableMap<String, String>, builder: FrameworkBuilder) {
        builder.buildView(viewModel, igm, templateContext)
    }

    private fun createAction(act: Action, igm: InternalGeneratorModel, templateContext: Map<String, String>) {
        val ownerClass = act.owner
        val cls = igm.getClass(ownerClass!!)
        act.qid?.id?.let {
            addActionToClass(it, cls, templateContext)
        }
    }

    private fun createEntity(ent: Entity, igm: InternalGeneratorModel) {
        // ensure package exists
        if (ent.qid?.domain == null) {
            ent.qid?.domain = "$appRootPackage.entities"
        }

        val c = igm.getClass(ent.qid.toString())

        if (ent.parent != QualifiedId.Undefined) {
            c.parent = ent.parent.toString()
        }

        // class members
        ent.definition.properties.forEach {
            val member = c.getMember(it.key!!)
            member.type = it.type!!
            projectModel.source.classes.find { it.enumeration && it.qid?.id == member.type }?.let { cl ->
                member.enumerationType = true
                val def = cl.definition.properties.getOrNull(0)
                member.value = def?.key
            }
            member.listOf = it.listOf
            member.optionality = it.optionality ?: ""
            if (!member.enumerationType) member.value = it.value
        }

        // TODO v0.3: add actions as class methods

//            cls.actions.forEach {
//                addClassMethod(it.qid?.id, emptyList(), emptyList())
//            }

    }

    private fun createEnumeration(cls: Classs, igm: InternalGeneratorModel) {
        if (!cls.atomic) {
            // ensure package exists
            if (cls.qid?.domain == null) {
                cls.qid?.domain = "$appRootPackage.enumeration"
            }

            val e = igm.getEnumeration(cls.qid.toString())

            if (cls.parent != QualifiedId.Undefined) {
                e.parent = cls.parent.toString()
            }

            e.values = cls.definition.properties.map { it.key!! }

        }

    }

    private fun createClass(cls: Classs, igm: InternalGeneratorModel) {
        if (!cls.atomic) {
            // ensure package exists
            if (cls.qid?.domain == null) {
                cls.qid?.domain = "$appRootPackage.entities"
            }

            val c = igm.getClass(cls.qid.toString())

            if (cls.parent != QualifiedId.Undefined) {
                c.parent = cls.parent.toString()
            }

            // class members
            cls.definition.properties.forEach {
                val member = c.getMember(it.key!!)
                member.type = it.type!!
                member.listOf = it.listOf
                member.optionality = it.optionality ?: ""
                member.value = it.value
            }

//            cls.actions.forEach {
//                addClassMethod(it.qid?.id, emptyList(), emptyList())
//            }
        }

    }

    private fun createApplication(app: Application, igm: InternalGeneratorModel, builder: FrameworkBuilder): Boolean {
        builder.buildApplication(app, igm)
        igm.getClass(app.qid.toString()).apply {
            mainClass = true
        }
        return true
    }

    private fun determineFrameworkBuilder(generatorName: String): FrameworkBuilder {
        var impl = ConfigManager.defaults[generatorName]
        impl = if (!impl.isNullOrBlank()) "$generatorName.$impl" else generatorName
        return PluginManager.get<FrameworkBuilder>(PluginType.Framework, impl)
    }

    private fun addActionToClass(actionName: String, cls: IGMClass, templateContext: Map<String, String>) {
        val template = Template()

        val action = cls.getAction(actionName)

        val actionSrc = projectModel.source.actions.find { it.qid.toString() == actionName }
        actionSrc?.let {
            it.definition.actionCalls.forEach {call ->
                val stmt = IGMAction.IGMActionStmt(call.actionName)
                call.parameters.forEach { p ->
                    val newparam = if (p.value != null && (p.type == StandardTypes.string.name || p.type == StandardTypes.stringLiteral.name)) {
                            template.translate(templateContext, p.value!!)
                        } else {
                            p.value!!
                        }
                    stmt.withParam(newparam, p.type!!)
                }
                action.statements.add(stmt)
            }
        }
    }

}