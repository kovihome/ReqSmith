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

import dev.reqsmith.composer.common.WholeProject
import dev.reqsmith.composer.common.plugin.Plugin
import dev.reqsmith.composer.common.plugin.PluginDef
import dev.reqsmith.composer.common.plugin.PluginType
import dev.reqsmith.model.REQM_GENERAL_ATTRIBUTE_EVENTS
import dev.reqsmith.model.igm.IGMClass
import dev.reqsmith.model.reqm.*

open class DefaultFrameworkBuilder : FrameworkBuilder, Plugin {

    override fun definition(): PluginDef {
        return PluginDef("framework.default", PluginType.Framework)
    }

    override fun getViewLanguage(): String = ""

    override fun getViewFolder(): String = ""

    override fun getArtFolder(): String = ""

    override fun collectBuildScriptElement(buildScriptUpdates: Map<String, MutableList<String>>) {
        // nothing to do
    }

    override fun processResources(reqmResourcesFolderName: String, buildResourcesFolderName: String) {
        // nothing to do
    }

    override fun applyFeatureOnEntity(ent: Entity, feature: Feature) {
        TODO("Not yet implemented")
    }

    override fun buildApplication(app: Application) {
        processEvents(app.definition, WholeProject.projectModel.igm.getClass(app.qid.toString()).apply { mainClass = true })
    }

    override fun buildView(view: View, templateContext: MutableMap<String, String>) {
        TODO("Not yet implemented")
    }

    private fun processEvents(definition: Definition, cls: IGMClass) {
        val events = definition.properties.find { it.key == REQM_GENERAL_ATTRIBUTE_EVENTS }
        events?.let {
            it.simpleAttributes.forEach {prop ->
                processEvent(prop, cls)
            }
        }
    }

    protected open fun processEvent(prop: Property, cls: IGMClass) {
        // intentionally empty function: no default events to be processed
    }

}