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

package dev.reqsmith.composer.common.plugin.buildsys

interface BuildSystem {
    val buildFolder: String
    val sourceFolder: String
    val resourceFolder: String

    /**
     * Update the build scripts
     * @param params Template parameters of the build script template file
     */
    fun updateBuildScript(params: MutableMap<String, String>)

    /**
     * Invoke the build system command to compile/run the application
     * @param projectPath The path of the project root folder
     * @param buildAndRun True - build, then run the application, false - build only
     * @param infoLogging Logging level: true - info log level, false - normal log level
     */
    fun build(projectPath: String, buildAndRun: Boolean, infoLogging: Boolean)

    /**
     * Formats the plugin list in build script format
     * @param plugins List of the build system plugins
     * @return The formatted plugin block
     */
    fun formatPluginBlock(plugins: List<String>): String

    /**
     * Formats the dependencies in build script format
     * @param dependencies List of the build system dependecies
     * @return The formatted dependencies block
     */
    fun formatDependenciesBlock(dependencies: List<String>): String
}