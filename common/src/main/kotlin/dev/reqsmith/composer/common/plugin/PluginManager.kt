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

package dev.reqsmith.composer.common.plugin

import dev.reqsmith.composer.common.Log
import java.util.*

object PluginManager {
    val plugins: MutableMap<PluginType, MutableMap<String, Plugin>> = hashMapOf(
        PluginType.BuildSystem to HashMap(),
        PluginType.Language to HashMap(),
        PluginType.Framework to HashMap(),
    )

    fun loadAll() {
        val plugins = ServiceLoader.load(Plugin::class.java)
        Log.debug("Plugins loaded:")
        plugins.forEach {
            registrate(it.definition(), it)
            Log.debug("${it.definition().name} [ ${it.definition().type.name}, class ${it.javaClass.canonicalName} ]")
        }
    }

    private fun registrate(definition: PluginDef, it: Plugin?) {
        plugins[definition.type]?.set(definition.name, it!!)
    }

    inline fun <reified T> get(type: PluginType, name: String): T {
        val plugin = plugins[type]?.get(name)
        if (plugin != null && plugin is T) {
            return plugin
        } else {
            throw Exception("No appropriate plugin has found for type ${type.name} and $name")
        }
    }
}