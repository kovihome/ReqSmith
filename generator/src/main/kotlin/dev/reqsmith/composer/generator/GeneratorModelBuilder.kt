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

package dev.reqsmith.composer.generator

import dev.reqsmith.composer.common.configuration.ConfigManager
import dev.reqsmith.composer.common.plugin.PluginManager
import dev.reqsmith.composer.common.plugin.PluginType
import dev.reqsmith.composer.common.templating.Template
import dev.reqsmith.composer.generator.entities.IGMAction
import dev.reqsmith.composer.generator.entities.IGMClass
import dev.reqsmith.composer.generator.entities.InternalGeneratorModel
import dev.reqsmith.composer.generator.plugin.framework.FrameworkBuilder
import dev.reqsmith.composer.parser.entities.*
import dev.reqsmith.composer.parser.enumeration.StandardTypes

class GeneratorModelBuilder(private val reqMSource: ReqMSource, val resourcesFolderName: String) {
    private val appRootPackage = reqMSource.applications[0].qid?.domain ?: "com.sample.app"
    var viewGeneratorName = ""
    var suggestedWebFolderName = ""
    var codeBuilder: FrameworkBuilder? = null
    var viewBuilder: FrameworkBuilder? = null

    fun determineBuilders() {
        // determine code generator
        val app = reqMSource.applications[0]
        var generatorName = app.definition.properties.find { it.key == "generator" }?.value ?: "framework.base"
        codeBuilder = determineFrameworkBuilder(generatorName)

        // determine view builder
        val viewLang = codeBuilder!!.getViewLanguage()
        val plugin = ConfigManager.defaults[viewLang]
        viewBuilder = if (plugin != null) {
            PluginManager.get<FrameworkBuilder>(PluginType.Framework, "$generatorName.$plugin")
        } else {
            codeBuilder
        }

        //
        if (plugin != null) {
            viewGeneratorName = "$viewLang.$plugin"
            suggestedWebFolderName = codeBuilder!!.getViewFolder()
        }
    }

    fun build(): InternalGeneratorModel {
        // create internal generator model
        val igm = InternalGeneratorModel(appRootPackage)

        // determine code and view generator
        determineBuilders()

        // create application
        val app = reqMSource.applications[0]
        createApplication(app, igm, codeBuilder!!)

        // create classes for classes
        reqMSource.classes.forEach {
            when {
                it.enumeration -> createEnumeration(it, igm)
                else -> createClass(it, igm)
            }
        }

        // create classes for entities
        reqMSource.entities.forEach { createEntity(it, igm) }

        // create class methods for actions
        val templateContext = getTemplateContext(reqMSource)
        reqMSource.actions.forEach { createAction(it, igm, templateContext) }

        // create view descriptors
        reqMSource.views.forEach { createView(it, igm, templateContext, viewBuilder!!) }

        // manage additional resources
        codeBuilder!!.processResources(resourcesFolderName)

        return igm
    }

    private fun createView(viewModel: View, igm: InternalGeneratorModel, templateContext: Map<String, String>, builder: FrameworkBuilder) {
        builder.buildView(viewModel, igm, templateContext)
    }

    private fun getTemplateContext(reqMSource: ReqMSource): Map<String, String> {
        val context = mutableMapOf<String, String>()
        context["name"] = reqMSource.applications[0].qid!!.id!!
        reqMSource.applications[0].definition.properties.filter { it.value != null }.associateTo(context) {
            it.key!! to if (it.type == StandardTypes.stringLiteral.name) it.value!!.trim('\'', '\"') else it.value!!
        }
        return context
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
            member.listOf = it.listOf
            member.optionality = it.optionality ?: ""
            member.value = it.value
        }

        // TODO: add actions as class methods

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

        val actionSrc = reqMSource.actions.find { it.qid.toString() == actionName }
        actionSrc?.let {
            it.definition.actionCalls.forEach {call ->
                val stmt = IGMAction.IGMActionStmt(call.actionName)
                call.parameters.forEach { p ->
                    val newparam = if (p.value != null && (p.type == StandardTypes.string.name || p.type == StandardTypes.stringLiteral.name)) {
                            template.translate(templateContext, p.value!!)
                        } else {
                            p.value!!
                        }
//                    if (newparam.startsWith('\'') || newparam.startsWith('\"')) {
//                        val rc = newparam.subSequence(0,1)
//                        newparam = newparam.removeSurrounding(rc, rc)
//                    }
                    stmt.withParam(newparam, p.type!!)
                }
                action.statements.add(stmt)
            }
        }
    }

}