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

import dev.reqsmith.composer.common.Log
import dev.reqsmith.composer.common.WholeProject
import dev.reqsmith.composer.common.configuration.ConfigManager
import dev.reqsmith.composer.common.formatter.NameFormatter
import dev.reqsmith.composer.common.plugin.PluginManager
import dev.reqsmith.composer.common.plugin.PluginType
import dev.reqsmith.composer.common.templating.Template
import dev.reqsmith.composer.generator.plugin.framework.FrameworkBuilder
import dev.reqsmith.model.VIEW_SUBTYPE_TEMPLATE
import dev.reqsmith.model.VIEW_SUBTYPE_WIDGET
import dev.reqsmith.model.enumeration.StandardTypes
import dev.reqsmith.model.igm.IGMAction
import dev.reqsmith.model.igm.IGMClass
import dev.reqsmith.model.igm.IGMStatement
import dev.reqsmith.model.igm.IGMStyle
import dev.reqsmith.model.reqm.*

class GeneratorModelBuilder(private val resourcesFolderName: String) {
    private val appRootPackage = WholeProject.projectModel.source.applications[0].qid?.domain ?: "com.sample.app"
    var styleGeneratorName = ""
    var viewGeneratorName = ""
    var codeBuilder: FrameworkBuilder? = null

    private fun determineBuilders() {
        // determine code generator
        val app = WholeProject.projectModel.source.applications[0]
        val generatorName = app.definition.properties.find { it.key == "generator" }?.value ?: "framework.base"
        codeBuilder = determineFrameworkBuilder(generatorName)

        // determine view builder
        val viewLang = codeBuilder!!.getViewLanguage()
        viewGeneratorName = "$viewLang.${ConfigManager.defaults[viewLang]}"

        // determine style language
        val styleLang = codeBuilder!!.getStyleLanguage()
//        styleGeneratorName = "$styleLang.${ConfigManager.defaults[styleLang]}"
        styleGeneratorName = styleLang
    }

    fun build() {
        // set root package for internal generator model
        WholeProject.projectModel.igm.rootPackage = appRootPackage

        // determine code and view generator
        determineBuilders()

        // create application
        val app = WholeProject.projectModel.source.applications[0]
        createApplication(app, codeBuilder!!)

        // create classes for classes
        WholeProject.projectModel.source.classes.forEach {
            when {
                it.enumeration -> createEnumeration(it)
                else -> createClass(it)
            }
        }

        // create classes for entities
        WholeProject.projectModel.source.entities.forEach { createEntity(it) }

        // create class methods for actions
        val templateContext = TemplateContextCollector().getItemTemplateContext(WholeProject.projectModel.source.applications[0].qid, WholeProject.projectModel.source.applications[0].definition.properties, "app")
        WholeProject.projectModel.source.actions.forEach { createAction(it, templateContext) }

        // create view descriptors
        WholeProject.projectModel.source.views.filter { view -> listOf(VIEW_SUBTYPE_TEMPLATE, VIEW_SUBTYPE_WIDGET).none { it == view.parent.id } }.forEach { createView(it, templateContext, codeBuilder!!) }
        
        // create style descriptors
        WholeProject.projectModel.source.styles.forEach { createStyle(it) }

        // manage additional resources
        val reqmResourceFolder = "${WholeProject.project.projectFolder}/${WholeProject.project.buildSystem.resourceFolder}"
        codeBuilder?.processResources(reqmResourceFolder, resourcesFolderName)

    }

    private fun createStyle(style: Style) {
        // create entity class
        val s = WholeProject.projectModel.igm.getStyle(style.qid.toString())
        style.definition.properties.forEach {
            s.attributes.add(createStyleAttribute(it))
        }

    }

    private fun createStyleAttribute(sprop: Property): IGMStyle.IGMStyleAttribute {
        return IGMStyle.IGMStyleAttribute(sprop.key!!).apply {
            if (sprop.simpleAttributes.isNotEmpty()) {
                sprop.simpleAttributes.forEach {
                    attributes.add(createStyleAttribute(it))
                }
            } else if (!sprop.value.isNullOrBlank()) {
                if (sprop.valueList.isNotEmpty()) {
                    valueList.addAll(sprop.valueList)
                } else {
                    value = sprop.value
                }
            }
        }
    }

    private fun createView(viewModel: View, templateContext: MutableMap<String, String>, builder: FrameworkBuilder) {
        builder.buildView(viewModel, templateContext)
    }

    private fun createAction(act: Action, templateContext: Map<String, String>) {
        val ownerClass = act.owner
        val cls = WholeProject.projectModel.igm.getClass(ownerClass!!)
        act.qid?.id?.let {
            addActionToClass(it, cls, templateContext)
        }
    }

    private fun createEntity(ent: Entity) {
        // ensure package exists
        if (ent.qid?.domain == null) {
            ent.qid?.domain = "$appRootPackage.entities"
        }

        // create entity class
        val c = WholeProject.projectModel.igm.getClass(ent.qid.toString())

        if (ent.parent != QualifiedId.Undefined) {
            c.parent = ent.parent.toString()
        }

        // add class members
        ent.definition.properties.forEach { property ->
            // valueList and propertyList are not real members, exclude them
            if (!listOf(StandardTypes.valueList.name, StandardTypes.propertyList.name).contains(property.type)) {
                val member = c.getMember(property.key!!)
                member.type = property.type!!
                WholeProject.projectModel.source.classes.find { it.enumeration && it.qid?.id == member.type }?.let { cl ->
                    member.enumerationType = true
                    val def = cl.definition.properties.getOrNull(0)
                    member.value = def?.key
                }
                member.listOf = property.listOf
                member.optionality = property.optionality ?: ""
                if (!member.enumerationType) member.value = property.value
            }
        }

        // create empty entity service class
        val sc = WholeProject.projectModel.igm.getClass("${c.id.replace("entities", "service")}Service")

        // TODO v0.3: add actions as class methods
        ent.definition.properties.find { it.key == "actions" }?.let { actions ->
            actions.valueList.forEach { action ->
                sc.getAction(action).apply {
                    // TODO:
                }
            }
        }

        // apply features on entities
        ent.definition.featureRefs.forEach { featureRef ->

            val feature = WholeProject.projectModel.source.features.find { it.qid.toString() ==  featureRef.qid.toString()}
            if (feature != null) {
                val generatorId = NameFormatter.deliterateText(feature.definition.properties.find { it.key == "generator" }?.value ?: "")
                if (generatorId.isNotBlank()) {
                    val featurePlugin = PluginManager.getBest<FrameworkBuilder>("", PluginType.Framework, generatorId)
                    featurePlugin.applyFeatureOnEntity(ent, feature)
                } else {
                    Log.error("Generator property not found in feature ${feature.qid} (${feature.coords()})")
                }

            } else {
                Log.error("Feature ${featureRef.qid} is not found; referenced by entity ${ent.qid} (${featureRef.coords()})")
            }

        }

    }

    private fun createEnumeration(cls: Classs) {
        if (!cls.atomic) {
            // ensure package exists
            if (cls.qid?.domain == null) {
                cls.qid?.domain = "$appRootPackage.enumeration"
            }

            val e = WholeProject.projectModel.igm.getEnumeration(cls.qid.toString())

            if (cls.parent != QualifiedId.Undefined) {
                e.parent = cls.parent.toString()
            }

            e.values = cls.definition.properties.map { it.key!! }

        }

    }

    private fun createClass(cls: Classs) {
        if (!cls.atomic) {
            // ensure package exists
            if (cls.qid?.domain == null) {
                cls.qid?.domain = "$appRootPackage.entities"
            }

            val c = WholeProject.projectModel.igm.getClass(cls.qid.toString())

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

    private fun createApplication(app: Application, builder: FrameworkBuilder): Boolean {
        builder.buildApplication(app)
        WholeProject.projectModel.igm.getClass(app.qid.toString()).apply {
            mainClass = true
        }
        return true
    }

    private fun determineFrameworkBuilder(generatorName: String): FrameworkBuilder {
        var impl = ConfigManager.defaults[generatorName]
        impl = if (!impl.isNullOrBlank()) "$generatorName.$impl" else generatorName
        Log.debug("framework plugin $impl is using in generator.GeneratorModelBuilder.determineFrameworkBuilder().")
        return PluginManager.get<FrameworkBuilder>(PluginType.Framework, impl)
    }

    private fun addActionToClass(actionName: String, cls: IGMClass, templateContext: Map<String, String>) {
        val template = Template()

        val action = cls.getAction(actionName)

        val actionSrc = WholeProject.projectModel.source.actions.find { it.qid.toString() == actionName }
        actionSrc?.let {
            it.definition.actionCalls.forEach {call ->
                val statementName = IGMStatement.valueOf(call.actionName)
                val stmt = IGMAction.IGMActionStmt(statementName)
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