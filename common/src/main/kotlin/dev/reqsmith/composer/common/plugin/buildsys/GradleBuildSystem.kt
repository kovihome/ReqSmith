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

import dev.reqsmith.composer.common.templating.Template
import java.io.File
import java.io.FileReader
import java.io.FileWriter

class GradleBuildSystem : BuildSystem {
    override val sourceFolder: String = "src/main"
    override val buildFolder: String = "build"

    private val taskName = "composeReqm"

    override fun updateBuildScript(params: MutableMap<String, String>) {
        // create compose task script
        val cmd = params["composerCommand"] ?: "composer"
        params["commandAndArgs"] = "\"${cmd}\""
        params["taskName"] = taskName

//        val taskScript = Template().translateFile(params, "templates/compose-task.gradle.st")
        val projectRootFolder = params["projectRootFolder"]

        // check gradle build script existence
        val script = File("${projectRootFolder}/build.gradle.kts")
        if (!script.exists()) {
            // create new build script
            val s = Template().translateFile(params, "templates/build.gradle.st")
            FileWriter(script).use {
                it.write(s)
//                it.write(taskScript)
            }
        } else {
            FileReader(script).useLines { scl ->
                // TODO: update dependencies, plugins, etc. if changed
//                if (scl.none { it.contains("tasks.register(\"${taskName}\")") }) {
//                    // build script exists, but it does not contain reqm composer task
//                    FileWriter(script, true).use {
//                        it.append(taskScript)
//                    }
//                }
            }
        }

        // check gradle settings script existence
        val settings = File("${projectRootFolder}/settings.gradle.kts")
        if (!settings.exists()) {
            // create new build script
            val s = Template().translateFile(params, "templates/settings.gradle.st")
            FileWriter(settings).use {
                it.write(s)
            }
        }
    }

}
