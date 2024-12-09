/*
 * ReqSmith - Build application from requirements
 * Copyright (c) 2023-2024. Kovi <kovihome86@gmail.com> 
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
import dev.reqsmith.composer.parser.entities.Definition
import dev.reqsmith.composer.parser.entities.QualifiedId
import dev.reqsmith.composer.parser.entities.ReqMSource

class ModelValidator {

    fun validateCompleteness(model: ReqMSource): Boolean {
        // Search for typeless properties, and assign default type to them
        model.applications.forEach {  item -> resolveTypelessProperties(item.qid!!, item.definition, "string") }
        model.modules.forEach {  item -> resolveTypelessProperties(item.qid!!, item.definition, "string") }
        model.actors.forEach {  item -> resolveTypelessProperties(item.qid!!, item.definition, ConfigManager.defaults.propertyType) }
        model.classes.forEach {  item -> resolveTypelessProperties(item.qid!!, item.definition, ConfigManager.defaults.propertyType) }
        model.entities.forEach {  item -> resolveTypelessProperties(item.qid!!, item.definition, ConfigManager.defaults.propertyType) }

        // find orfan actions
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
            if (app.qid?.domain.isNullOrBlank()) {
                Log.warning("application ${app.qid} has no domain name; set the default domain name ${ConfigManager.defaults.domainName} to it.")
                app.qid?.domain = ConfigManager.defaults.domainName
            }
            if (app.sourceRef == null || app.sourceRef == QualifiedId.Undefined) {
                Log.warning("application ${app.qid} has no application type; set the default application type ${ConfigManager.defaults.applicationType}")
                app.sourceRef = QualifiedId.fromString(ConfigManager.defaults.applicationType)
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
     * @param reqmsrc The ReqM model of the application
     * @param dependenciesReqMModel The ReqM model of the dependencies
     * @return Error list (empty list = no errors found)
     */
    fun resolveActionOwnership(reqmsrc: ReqMSource, dependenciesReqMModel: ReqMSource): List<String> {
        val errors: MutableList<String> = ArrayList()

        // search event actions in event sources
        reqmsrc.applications.forEach { app ->
            resolveActionOwnershipFromEvents(app.qid!!, app.definition, reqmsrc, dependenciesReqMModel, errors)
        }
        dependenciesReqMModel.modules.forEach { mod ->
            resolveActionOwnershipFromEvents(mod.qid!!, mod.definition, reqmsrc, dependenciesReqMModel, errors)
        }

        // search modules/applications for orfan actions
        searchModuleForOrfanActions(reqmsrc, errors)
        searchModuleForOrfanActions(dependenciesReqMModel, errors)

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

    private fun resolveActionOwnershipFromEvents(qid: QualifiedId, definition: Definition, reqmsrc: ReqMSource, reqmdep: ReqMSource, errors: MutableList<String>) {
        val events = definition.properties.find { it.key == "events" }
        events?.simpleAttributes?.forEach { ev ->
            val actionName = ev.value
            var result = searchActionOwnerInModel(reqmsrc, actionName, qid)
            if (result == null) {
                result = searchActionOwnerInModel(reqmdep, actionName, qid)
            }
            if (result != null && result != "OK") {
                errors.add(result)
            }
        }
    }

    private fun searchActionOwnerInModel(reqmsrc: ReqMSource, actionName: String?, qid: QualifiedId): String? {
        return reqmsrc.getAction(actionName!!)?.let {
            if (it.owner == null) {
                it.owner = qid.toString()
                Log.debug("action ${it.qid} owner is application $qid")
                "OK"
            } else if (it.owner != qid.toString()) {
                "action ${it.qid} has multiple owners: ${it.owner} and $qid"
            } else {
                "action ${it.qid} owner ${it.owner} referenced twice to it"
            }
        } ?: null
    }
}
