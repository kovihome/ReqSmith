/*
 * ReqSmith - Build application from requirements
 * Copyright (c) 2023-2025. Kovi <kovihome86@gmail.com>
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

package dev.reqsmith.composer.validator

import dev.reqsmith.composer.common.Log
import dev.reqsmith.composer.common.WholeProject
import dev.reqsmith.composer.common.configuration.ConfigManager
import dev.reqsmith.model.REQM_GENERAL_ATTRIBUTE_EVENTS
import dev.reqsmith.model.VIEW_ATTRIBUTE_LAYOUT
import dev.reqsmith.model.enumeration.StandardLayoutElements
import dev.reqsmith.model.enumeration.StandardStyleAttributes
import dev.reqsmith.model.enumeration.StandardStyleElements
import dev.reqsmith.model.enumeration.StandardTypes
import dev.reqsmith.model.enumeration.VIEW_LAYOUT_ELEMENT_ATTR_DATA
import dev.reqsmith.model.reqm.*

class ModelValidator {

    fun validateCompleteness(): Boolean {
        val model = WholeProject.projectModel.source

        // Search for typeless properties, and assign a default type to them
        val defPropertyType = ConfigManager.defaults["propertyType"] ?: "String"
        model.applications.forEach {  item -> resolveTypelessProperties(item.qid!!, item.definition, defPropertyType) }
        model.modules.forEach {  item -> resolveTypelessProperties(item.qid!!, item.definition, defPropertyType) }
        model.actors.forEach {  item -> resolveTypelessProperties(item.qid!!, item.definition, defPropertyType) }
        model.classes.forEach {  item -> resolveTypelessProperties(item.qid!!, item.definition, defPropertyType) }
        model.entities.forEach {  item -> resolveTypelessProperties(item.qid!!, item.definition, defPropertyType) }

        // find orphan actions
        model.actions.filter { it.owner == null }.forEach { Log.warning("Action ${it.qid} has no owner (application, module)") }

        // validate view layout elements: search for missing view links, check style elements, event actions
        val missingLinks = mutableListOf<String>()
        model.views.forEach { view ->
            view.definition.properties.find { it.key == VIEW_ATTRIBUTE_LAYOUT }?.let { layout ->
                validateViewLayoutElement(layout, missingLinks)
            }
        }
        missingLinks.forEach { link ->
            val newView = View().apply {
                qid = QualifiedId(link)
                parent = QualifiedId("static")
                definition = Definition().apply {
                    properties.add(Property().apply {
                        key = VIEW_ATTRIBUTE_LAYOUT
                        type = StandardTypes.propertyList.name
                        simpleAttributes.add(Property().apply {
                            key = "panel"
                            type = StandardTypes.propertyList.name
                            simpleAttributes.add(Property().apply {
                                key = "text"
                                value =
                                    "This view is generated by ReqSmith\\n\\nIn the future developer must customize it or delete the link pointing to this view."
                            })
                        })
                    })
                }
            }
            model.views.add(newView)
        }

        // validate style elements
        WholeProject.projectModel.source.styles.forEach {
            validateStyleElements(it.definition.properties)
        }

        // TODO search for missing reference in srcRef (applications and modules? excluded), property types, features
        // dependencies needed for this

        return true
    }

    private fun validateStyleElements(properties: List<Property>) {
        properties.forEach {
            val propertyName = it.key!!
            if (!StandardStyleElements.contains(propertyName) && !StandardStyleAttributes.contains(propertyName) && !StandardLayoutElements.contains(propertyName)) {
                Log.warning("Style property $propertyName is not style element, style attribute or layout element. (${it.coords()})")
            }
        }
    }

    private fun validateViewLayoutElement(property: Property, missingLinks: MutableList<String>) {
        // resolve missing links
        if (property.key == "to") {
            val link = property.value
            if (link != null && !link.startsWith("http:") && !link.startsWith("https:")
                && WholeProject.projectModel.source.views.none { it.qid.toString() == link } && !missingLinks.contains(link)) {
                Log.warning("Missing link ${link}; create new view for this link. (${property.coords()})")
                missingLinks.add(link)
            }
            return
        }

        // check layout element style
        if (property.key == "styles") {
            property.simpleAttributes.forEach { attr ->
                if (StandardStyleElements.contains(attr.key!!)) {
                    if (attr.simpleAttributes.isNotEmpty()) {
                        attr.simpleAttributes.forEach { sattr ->
                            if (!StandardStyleAttributes.contains(sattr.key!!)) {
                                Log.warning("Layout element's style property ${attr.key} is not a valid style attribute. (${attr.coords()})")
                            }
                        }
                    }
                } else if (!StandardStyleAttributes.contains(attr.key!!)) {
                    if (attr.simpleAttributes.isNotEmpty())
                        Log.warning("Layout element's style property ${attr.key} is not a valid style element. (${attr.coords()})")
                    else
                        Log.warning("Layout element's style property ${attr.key} is not a valid style attribute. (${attr.coords()})")
                }
            }
            return
        }

        // check view event action existence
        val events = property.simpleAttributes.find { it.key == REQM_GENERAL_ATTRIBUTE_EVENTS }
        val data = property.simpleAttributes.find { it.key == VIEW_LAYOUT_ELEMENT_ATTR_DATA }
        if (events != null && data != null) {
            val entity = WholeProject.projectModel.source.entities.find { it.qid?.id == data.value }
            if (entity != null) {
                val entityActions = entity.definition.properties.find { it.key == "actions" }
                if (entityActions != null) {
                    events.simpleAttributes.forEach { event ->
                        if (entityActions.valueList.none { it == event.value }) {
                            Log.warning("View event '${event.key}' does not match with action '${event.value}' of entity '${data.value}' (${event.coords()})")
                        }
                    }
                } else {
                    Log.warning("Data layout attribute '${data.value}' match with an entity, but the entity has no actions (${data.coords()})")
                }
            } else {
                Log.warning("Data layout attribute '${data.value}' is not match any entity (${data.coords()})")
            }
        }

        // recurse
        property.simpleAttributes.forEach {
            validateViewLayoutElement(it, missingLinks)
        }
    }

    private fun resolveTypelessProperties(qid: QualifiedId, definition: Definition, default: String) {
        definition.properties.forEach {
            if (it.type == null) {
                Log.warning("Property ${qid}.${it.key} has no type; default type will be assigned to it")
                it.type = default
            }
        }
    }

    /**
     *
     */
    fun resolveInconsistencies(): Boolean {
        val model = WholeProject.projectModel.source

        // check the application item
        if (model.applications.isNotEmpty()) {
            val app = model.applications[0]
            if (app.qid?.id?.get(0)?.isLowerCase() == true) {
                Log.warning("application name ${app.qid?.id} starts with lower case letter; converted to upper case letter")
                app.qid!!.id = app.qid!!.id!!.replaceFirstChar { it.uppercaseChar() }
            }
            if (app.qid?.domain.isNullOrBlank()) {
                Log.warning("application ${app.qid} has no domain name; set the default domain name ${ConfigManager.defaults["domainName"]} to it.")
                app.qid?.domain = ConfigManager.defaults["domainName"]
            }
            if (app.sourceRef == null || app.sourceRef == QualifiedId.Undefined) {
                Log.warning("application ${app.qid} has no application type; set the default application type ${ConfigManager.defaults["applicationType"]}")
                app.sourceRef = QualifiedId.fromString(ConfigManager.defaults["applicationType"] ?: "QualifiedId.Undefined")
            }
        }

        // TODO merge multiple defined other items (entities, classes, views, actors, styles)

        return true
    }

    /**
     * Resolve implicit action ownership
     *
     * Scan event sources for event actions
     * The remainder actions try to associate with its module
     *
     * @return Error list (empty list = no errors found)
     */
    fun resolveActionOwnership(): List<String> {
        val errors: MutableList<String> = ArrayList()

        // search event actions in event sources
        WholeProject.projectModel.source.applications.forEach { app ->
            resolveActionOwnershipFromAppEvents(app, errors)
        }
        WholeProject.projectModel.dependencies.modules.forEach { mod ->
            resolveActionOwnershipFromModEvents(mod, errors)
        }

        // search modules/applications for orphan actions
        searchModuleForOrphanActions(WholeProject.projectModel.source, errors)
        searchModuleForOrphanActions(WholeProject.projectModel.dependencies, errors)

        return errors
    }

    private fun searchModuleForOrphanActions(reqmsrc: ReqMSource, errors: MutableList<String>) {
        reqmsrc.actions.filter { it.owner == null }.forEach { acn ->
            val mods = reqmsrc.modules.filter { it.sourceFileName == acn.sourceFileName }
            if (mods.isNotEmpty()) {
                if (mods.size == 1) {
                    acn.owner = mods[0].qid.toString()
                    Log.debug("action ${acn.qid} owner is module ${acn.owner}")
                } else {
                    errors.add("action ${acn.qid} has multiple owner modules: ${mods.joinToString(", ") { it.qid.toString() }}")
                }
            }
        }
    }

    private fun resolveActionOwnershipFromAppEvents(app: Application, errors: MutableList<String>) {
        val events = app.definition.properties.find { it.key == REQM_GENERAL_ATTRIBUTE_EVENTS }
        events?.simpleAttributes?.forEach { ev ->
            val actionName = ev.value
            var result = searchActionOwnerInModel(WholeProject.projectModel.source, actionName, app.qid!!, null)
            var srcRef: QualifiedId? = app.sourceRef
            while (result == null && srcRef != null) {
                val parent = WholeProject.projectModel.dependencies.applications.find { it.qid == srcRef }
                result = if (parent != null) {
                    searchActionOwnerInModel(WholeProject.projectModel.dependencies, actionName, app.qid!!, parent.sourceFileName)
                } else {
                    "action $actionName was not found."
                }
                srcRef = parent?.sourceRef
            }
            if (result != null && result != "OK") {
                errors.add(result)
            }
        }
    }

    private fun resolveActionOwnershipFromModEvents(mod: Modul, errors: MutableList<String>) {
        val events = mod.definition.properties.find { it.key == REQM_GENERAL_ATTRIBUTE_EVENTS }
        events?.simpleAttributes?.forEach { ev ->
            val actionName = ev.value
            var result = searchActionOwnerInModel(WholeProject.projectModel.source, actionName, mod.qid!!, null)
            var srcRef: QualifiedId? = mod.sourceRef
            while (result == null && srcRef != null) {
                val parent = WholeProject.projectModel.dependencies.modules.find { it.qid == srcRef }
                result = if (parent != null) {
                    searchActionOwnerInModel(WholeProject.projectModel.dependencies, actionName, mod.qid!!, parent.sourceFileName)
                } else {
                    "action $actionName was not found."
                }
                srcRef = parent?.sourceRef
            }
            if (result != null && result != "OK") {
                errors.add(result)
            }
        }
    }

    private fun searchActionOwnerInModel(reqmsrc: ReqMSource, actionName: String?, qid: QualifiedId, sourceFileName: String?): String? {
        return reqmsrc.getAction(actionName!!, sourceFileName)?.let {
            when {
                it.owner == null -> {
                    it.owner = qid.toString()
                    Log.debug("action ${it.qid} owner is application $qid")
                    "OK"
                }
                it.owner != qid.toString() -> {
                    "action ${it.qid} has multiple owners: ${it.owner} and $qid"
                }
                else -> {
                    "action ${it.qid} owner ${it.owner} referenced twice to it"
                }
            }
        }
    }

}
