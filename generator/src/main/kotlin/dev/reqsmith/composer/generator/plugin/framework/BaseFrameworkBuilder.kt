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

import dev.reqsmith.composer.common.plugin.PluginDef
import dev.reqsmith.composer.common.plugin.PluginType
import dev.reqsmith.model.igm.IGMAction
import dev.reqsmith.model.igm.IGMClass
import dev.reqsmith.model.igm.InternalGeneratorModel
import dev.reqsmith.model.reqm.Property
import dev.reqsmith.model.reqm.View

open class BaseFrameworkBuilder : DefaultFrameworkBuilder() {

    override fun definition(): PluginDef {
        return PluginDef("framework.base", PluginType.Framework)
    }

    override fun processEvent(prop: Property, cls: IGMClass, igm: InternalGeneratorModel) {
        when (prop.key) {
            "applicationStart" -> {
                // create main action
                val main = cls.getAction("main").apply {
                    isMain = true
                }
                val param = IGMAction.IGMActionParam("args", "String", true)
                main.parameters.add(param)
                val call = IGMAction.IGMActionStmt("call").withParam("${cls.id}.${prop.value}")
                main.statements.add(call)
            }
            else -> super.processEvent(prop, cls, igm)
        }

    }

    override fun buildView(view: View, igm: InternalGeneratorModel, templateContext: MutableMap<String, String>) {
        TODO("Not yet implemented")
    }

}
