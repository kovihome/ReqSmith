/*
 * ReqSmith - Build application from requirements
 * Copyright (c) 2023. Kovi <kovihome86@gmail.com>
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

//    fun updateBuildScript(projectRootFolder: String, composerBinFolder: String, language: String)
    fun updateBuildScript(params: MutableMap<String, String>)

    companion object {
        fun get(buildSystemName: String) =
            when (buildSystemName) {
                "gradle" -> GradleBuildSystem()
//                "maven" -> MavenBuildSystem()
                else -> throw Exception("Unknown build system name $buildSystemName.")
            }
    }
}