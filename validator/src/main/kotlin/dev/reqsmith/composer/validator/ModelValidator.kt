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
import dev.reqsmith.composer.common.configuration.ConfigManager
import dev.reqsmith.model.ProjectModel
import dev.reqsmith.model.reqm.*

class ModelValidator {

    fun validateCompleteness(model: ReqMSource): Boolean {
        // Search for typeless properties, and assign default type to them
        val defPropertyType = ConfigManager.defaults["propertyType"] ?: "String"
        model.applications.forEach {  item -> resolveTypelessProperties(item.qid!!, item.definition, defPropertyType) }
        model.modules.forEach {  item -> resolveTypelessProperties(item.qid!!, item.definition, defPropertyType) }
        model.actors.forEach {  item -> resolveTypelessProperties(item.qid!!, item.definition, defPropertyType) }
        model.classes.forEach {  item -> resolveTypelessProperties(item.qid!!, item.definition, defPropertyType) }
        model.entities.forEach {  item -> resolveTypelessProperties(item.qid!!, item.definition, defPropertyType) }

        // find orphan actions
        model.actions.filter { it.owner == null }.forEach { Log.warning("Action ${it.qid} has no owner (application, module)") }

        // TODO search for missing reference in srcRef (applications and modules? excluded), property types, features
        // dependencies needed for this

        return true
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
    fun resolveInconsistencies(reqmsrc: ReqMSource): Boolean {

        // TODO if multiple application items exist, merge them
        // error conditions:
        // - multiple srcRefs exist
        // - attributes and features with different values, types

        // check the application item
        if (reqmsrc.applications.isNotEmpty()) {
            val app = reqmsrc.applications[0]
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

        // TODO merge multiple defined other items (entities, classes, views, actors)

        return true
    }

    /**
     * Resolve implicit action ownership
     *
     * Scan event sources for event actions
     * The remainder actions try to associate to its module
     *
     * @param projectModel The Project Model of the application
     * @return Error list (empty list = no errors found)
     */
    fun resolveActionOwnership(projectModel: ProjectModel): List<String> {
        val errors: MutableList<String> = ArrayList()

        // search event actions in event sources
        projectModel.source.applications.forEach { app ->
            resolveActionOwnershipFromAppEvents(app, projectModel, errors)
        }
        projectModel.dependencies.modules.forEach { mod ->
            resolveActionOwnershipFromModEvents(mod, projectModel, errors)
        }

        // search modules/applications for orfan actions
        searchModuleForOrfanActions(projectModel.source, errors)
        searchModuleForOrfanActions(projectModel.dependencies, errors)

        return errors
    }

    private fun searchModuleForOrfanActions(reqmsrc: ReqMSource, errors: MutableList<String>) {
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

    private fun resolveActionOwnershipFromAppEvents(app: Application, projectModel: ProjectModel, errors: MutableList<String>) {
        val events = app.definition.properties.find { it.key == "events" }
        events?.simpleAttributes?.forEach { ev ->
            val actionName = ev.value
            var result = searchActionOwnerInModel(projectModel.source, actionName, app.qid!!, null)
            var srcRef: QualifiedId? = app.sourceRef
            while (result == null && srcRef != null) {
                val parent = projectModel.dependencies.applications.find { it.qid == srcRef }
                result = if (parent != null) {
                    searchActionOwnerInModel(projectModel.dependencies, actionName, app.qid!!, parent.sourceFileName)
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

    private fun resolveActionOwnershipFromModEvents(mod: Modul, projectModel: ProjectModel, errors: MutableList<String>) {
        val events = mod.definition.properties.find { it.key == "events" }
        events?.simpleAttributes?.forEach { ev ->
            val actionName = ev.value
            var result = searchActionOwnerInModel(projectModel.source, actionName, mod.qid!!, null)
            var srcRef: QualifiedId? = mod.sourceRef
            while (result == null && srcRef != null) {
                val parent = projectModel.dependencies.modules.find { it.qid == srcRef }
                result = if (parent != null) {
                    searchActionOwnerInModel(projectModel.dependencies, actionName, mod.qid!!, parent.sourceFileName)
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

    private fun searchActionOwnerInModel(
        reqmsrc: ReqMSource,
        actionName: String?,
        qid: QualifiedId,
        sourceFileName: String?
    ): String? {
        return reqmsrc.getAction(actionName!!, sourceFileName)?.let {
            if (it.owner == null) {
                it.owner = qid.toString()
                Log.debug("action ${it.qid} owner is application $qid")
                "OK"
            } else if (it.owner != qid.toString()) {
                "action ${it.qid} has multiple owners: ${it.owner} and $qid"
            } else {
                "action ${it.qid} owner ${it.owner} referenced twice to it"
            }
        }
    }
}
