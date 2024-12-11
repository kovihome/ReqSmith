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

package dev.reqsmith.composer.generator.plugin.framework

import dev.reqsmith.composer.common.plugin.PluginManager
import dev.reqsmith.composer.common.plugin.PluginType

object FrameworkBuilderManager {

    private val modules : MutableMap<String, String> = HashMap()
    private val generators : MutableMap<String, FrameworkBuilder> = HashMap()

    fun addModule(moduleId: String, generatorId:String): Boolean {
        if (modules.containsKey(moduleId)) {
            if (modules[moduleId] != generatorId) {
                throw Exception("More than one generator id connected to module '${moduleId}': [${modules[moduleId]}, generatorId] ")
            }
            return false
        }
        modules[moduleId] = generatorId
        return true
    }

    fun getBuilder(moduleId: String) : FrameworkBuilder {
        val generatorId = modules[moduleId] ?: "framework.web.spring"// "framework.default"
        return generators.getOrPut(generatorId) { PluginManager.get<FrameworkBuilder>(PluginType.Framework, generatorId) }
    }

    override fun toString(): String {
        return this.modules.toList().joinToString("\n") { "  ${it.first} -> ${it.second}" }
    }
}