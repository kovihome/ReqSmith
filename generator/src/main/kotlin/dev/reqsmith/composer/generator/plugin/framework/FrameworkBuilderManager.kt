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

import dev.reqsmith.composer.parser.entities.ReqMSource

open class FrameworkBuilderManager {

    private val modules : MutableMap<String, String> = HashMap()
    private val generators : MutableMap<String, FrameworkBuilder> = HashMap()

    object INSTANCE : FrameworkBuilderManager()

    fun get(frameworkGenerator: String, reqmSource: ReqMSource) : FrameworkBuilder =
        when (frameworkGenerator) {
            "framework.default" -> DefaultFrameworkBuilder(reqmSource)
            "framework.base" -> BaseFrameworkBuilder(reqmSource)
            else -> throw Exception("Unknown framework build plugin $frameworkGenerator.")
        }

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

    fun getBuilder(moduleId: String, reqmSource: ReqMSource) : FrameworkBuilder {
        val generatorId = modules[moduleId] ?: "framework.default"
        return generators.getOrPut(generatorId) { get(generatorId, reqmSource) }
    }

    override fun toString(): String {
        return this.modules.toList().joinToString("\n") { "  ${it.first} -> ${it.second}" }
    }
}