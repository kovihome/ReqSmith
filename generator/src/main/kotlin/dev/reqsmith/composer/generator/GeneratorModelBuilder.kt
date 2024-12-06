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

import dev.reqsmith.composer.common.templating.Template
import dev.reqsmith.composer.generator.entities.IGMAction
import dev.reqsmith.composer.generator.entities.IGMClass
import dev.reqsmith.composer.generator.entities.IGMView
import dev.reqsmith.composer.generator.entities.InternalGeneratorModel
import dev.reqsmith.composer.generator.plugin.framework.FrameworkBuilder
import dev.reqsmith.composer.generator.plugin.framework.FrameworkBuilderManager
import dev.reqsmith.composer.parser.entities.*
import dev.reqsmith.composer.parser.enumeration.StandardTypes

class GeneratorModelBuilder(private val reqMSource: ReqMSource) {
    private val appRootPackage = reqMSource.applications[0].qid?.domain ?: "com.sample.app"

    fun build(): InternalGeneratorModel {
        // create internal generator model
        val igm = InternalGeneratorModel(appRootPackage)

        // create application
        val app = reqMSource.applications[0]
        createApplication(app, igm)

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
        reqMSource.views.forEach { createView(it, igm, templateContext) }

        return igm
    }

    private fun createView(viewModel: View, igm: InternalGeneratorModel, templateContext: Map<String, String>) {
        val view = igm.getView(viewModel.qid.toString())
        val layout = viewModel.definition.properties.find { it.key == "layout" }
        view.layout = layout?.let { propertyToNode(it, templateContext) }!!
    }

    private fun propertyToNode(prop: Property, templateContext: Map<String, String>): IGMView.IGMNode {
        val node = IGMView.IGMNode()
        val template = Template()
        node.name = prop.key!!
        if (prop.type == StandardTypes.propertyList.name) {
            prop.simpleAttributes?.forEach { node.children.add(propertyToNode(it, templateContext)) }
        } else if (prop.value?.isNotBlank() == true) {
            node.text = template.translate(templateContext, prop.value!!)
        }
        return node
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

    private fun createApplication(app: Application, igm: InternalGeneratorModel): Boolean {

        val builder = getGenerator(app.qid.toString())
        builder.buildApplication(app, igm)

        val cls = igm.getClass(app.qid.toString())
        cls.mainClass = true

        return true
    }

    private fun getGenerator(moduleId: String): FrameworkBuilder {
        return FrameworkBuilderManager.getBuilder(moduleId)
    }

    private fun addActionToClass(actionName: String, cls: IGMClass, templateContext: Map<String, String>) {
        val template = Template()

        val action = cls.getAction(actionName)

        val actionSrc = reqMSource.actions.find { it.qid.toString() == actionName }
        actionSrc?.let {
            it.definition.actionCalls.forEach {call ->
                val stmt = IGMAction.IGMActionStmt(call.actionName)
                call.parameters.forEach { p ->
                    var newparam = if (p.value != null && (p.type == StandardTypes.string.name || p.type == StandardTypes.stringLiteral.name)) {
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