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

import dev.reqsmith.composer.common.Log
import dev.reqsmith.composer.common.plugin.Plugin
import dev.reqsmith.composer.common.plugin.PluginDef
import dev.reqsmith.composer.common.plugin.PluginType
import dev.reqsmith.composer.common.templating.Template
import java.io.File
import java.io.FileWriter

class GradleBuildSystem : BuildSystem, Plugin {
    override val sourceFolder: String = "src/main"
    override val resourceFolder: String = "src/main/resources"
    override val buildFolder: String = "build"

    private val taskName = "composeReqm"

    override fun definition(): PluginDef {
        return PluginDef("gradle", PluginType.BuildSystem)
    }

    override fun updateBuildScript(params: MutableMap<String, String>) {
        // create compose task script
        val cmd = params["composerCommand"] ?: "forge"
        params["commandAndArgs"] = "\"${cmd}\""
        params["taskName"] = taskName

//        val taskScript = Template().translateFile(params, "templates/compose-task.gradle.st")
        val projectRootFolder = params["projectRootFolder"]

        // check gradle build script existence
        val script = File("${projectRootFolder}/build.gradle.kts")

        // create build script
        val s = Template().translateFile(params, "templates/build.gradle.st")
        FileWriter(script, false).use {
            it.write(s)
//          it.write(taskScript)
        }

        // check gradle settings script existence
        val settings = File("${projectRootFolder}/settings.gradle.kts")
        if (!settings.exists()) {
            // create new build script
            val s = Template().translateFile(params, "templates/settings.gradle.st")
            FileWriter(settings, false).use {
                it.write(s)
            }
        }
    }

    override fun build(projectPath: String, buildAndRun: Boolean, infoLogging: Boolean) {
        val gradleCommand = "gradle build ${if (buildAndRun) "run " else ""}-p $projectPath ${if (infoLogging) "--info" else ""}"
        Log.debug("Gradle command: $gradleCommand")
        // TODO: Windows specific command execution
        val process = ProcessBuilder("cmd /C $gradleCommand".split(" ")).redirectOutput(ProcessBuilder.Redirect.INHERIT).start()
        process.waitFor()
        Log.debug("Gradle exit code: ${process.exitValue()}")
    }

}
