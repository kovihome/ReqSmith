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

package dev.reqsmith.composer.common.plugin

import dev.reqsmith.composer.common.Log
import dev.reqsmith.composer.common.configuration.ConfigManager
import java.util.*

object PluginManager {
    val plugins: MutableMap<PluginType, MutableMap<String, Plugin>> = hashMapOf(
        PluginType.BuildSystem to mutableMapOf(),
        PluginType.Language to mutableMapOf(),
        PluginType.Framework to mutableMapOf(),
        PluginType.Template to mutableMapOf(),
        PluginType.Resource to mutableMapOf()
    )

    /**
     * Loads all available plugins using the `ServiceLoader` mechanism and registers them.
     *
     * This function iterates through all discovered implementations of the `Plugin` interface,
     * and registers the plugin.
     *
     * It uses the following steps:
     * 1. Loads all implementations of `Plugin` using `ServiceLoader`.
     * 2. For each plugin, retrieves its definition and registers it using its type and name.
     */
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

    /**
     * Retrieves a plugin of the specified type and name, casting it to the expected type.
     *
     * @param T The expected type of the plugin to be retrieved.
     * @param type The type of the plugin, as defined in the PluginType enum.
     * @param name The name of the plugin to retrieve.
     * @return The plugin of the specified type and name, cast to the expected type.
     * @throws Exception If no plugin is found for the given type and name, or if the plugin cannot be cast to the expected type.
     */
    inline fun <reified T> get(type: PluginType, name: String): T {
        val plugin = plugins[type]?.get(name)
        if (plugin != null && plugin is T) {
            return plugin
        } else {
            throw Exception("No appropriate plugin has found for type ${type.name} and $name")
        }
    }

    /**
     * Retrieves the most appropriate plugin instance based on the provided criteria.
     *
     * The method first checks for a user-declared plugin. If no such plugin is provided or found,
     * it falls back to searching for a default language-specific plugin, a default plugin, and
     * finally a base plugin matching the given type and name.
     *
     * @param language The target language used to search for a language-specific plugin.
     * @param type The type of the plugin, determined by the PluginType enumeration.
     * @param name The base name of the plugin to search for.
     * @param userPluginName Optional user-specified plugin name to prioritize during the search.
     * @return The best matching plugin instance of type T.
     * @throws Exception If no appropriate plugin is found for the specified type and name.
     */
    inline fun <reified T> getBest(language: String, type: PluginType, name: String, userPluginName: String = ""): T {
        var plugin: T? = null
        if (userPluginName.isNotEmpty()) {
            // search for user declared plugin
            plugin = plugins[type]?.get(userPluginName) as? T
        }
        if (plugin == null && language.isNotBlank()) {
            // search for default language specific plugin
            val defaultPluginName = ConfigManager.defaults.getOrDefault("$name.$language", "")
            if (defaultPluginName.isNotEmpty()) {
                plugin = plugins[type]?.get(defaultPluginName) as? T
            }
        }
        if (plugin == null) {
            // search for default plugin
            val defaultPluginName = ConfigManager.defaults.getOrDefault(name, "")
            if (defaultPluginName.isNotEmpty()) {
                plugin = plugins[type]?.get(defaultPluginName) as? T
            }
        }
        if (plugin == null) {
            // search for base plugin
            plugin = plugins[type]?.get(name) as? T
        }
        if (plugin == null) {
            throw Exception("No appropriate plugin has found for type ${type.name} and $name")
        } else {
            return plugin
        }
    }
}