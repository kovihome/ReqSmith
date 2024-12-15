/*
 * ReqSmith - Build application from requirements
 * Copyright (c) 2023-2024. Kovi <kovihome86@gmail.com>
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

package dev.reqsmith.composer.common

import dev.reqsmith.composer.common.plugin.buildsys.BuildSystem
import java.io.File
import java.nio.file.Paths
import kotlin.io.path.Path
import kotlin.io.path.createDirectories
import kotlin.io.path.notExists

/**
 * Manage project structure
 *
 * Default project structure is:
 *
 * project
 * -- src/main/reqm : source reqm folder
 * -- build/src/main/reqm : output reqm and index files
 * -- build/src/main/<lang> : generated source code folder
 */
class Project(var projectFolder: String?, val buildSystem: BuildSystem) {
    var inputFolder: String? = null
    var outputFolder: String? = null
    var artFolder: String = "${buildSystem.resourceFolder}/art"
    var buildFolder: String = ""
    private val reqmFolderName = "reqm"
    private val errors: MutableList<String> = ArrayList()

    /**
     * Check whether the folder exists, or create it
     *
     * @param folder the folder must be existing
     * @return *true* - if the folder exists, *false* - if some errors has occured in creating the folder
     */
    fun ensureFolderExists(folder: String): Boolean {
        val folderPath = Path(folder)
        if (folderPath.notExists()) {
            try {
                folderPath.createDirectories()
            } catch (e: Exception) {
                errors.add("Create directory $outputFolder failed - ${e.localizedMessage}")
                return false
            }
        }
        return true
    }

    fun calculateFolders(forInit : Boolean = false): Boolean {

        // if project folder was not given, set cwd to it
        if (projectFolder == null) {
            projectFolder = Paths.get("").toAbsolutePath().toString()
        }
        Log.debug("Project folder = $projectFolder")

        // check input folder/file
        if (inputFolder == null) {
            inputFolder = "${buildSystem.sourceFolder}/$reqmFolderName"
        }
        Log.debug("Input folder = $inputFolder")
        if (forInit) {
            ensureFolderExists(inputFolder!!)
        } else {
            val infolder = File(inputFolder!!)
            if (!infolder.exists()) {
                errors.add("No such file/directory as $inputFolder")
                return false
            }
        }

        Log.debug("Art folder = $artFolder")

        // determine build folder
//        rootFolder = when {
//            inputFolder!!.contains("src/") or inputFolder!!.endsWith("src") -> inputFolder!!.substringBefore("src")
//            else -> Path(inputFolder!!).parent.absolutePathString()
//        }
        buildFolder = "$projectFolder/${buildSystem.buildFolder}"
        if (!forInit && !ensureFolderExists(buildFolder)) {
            return false
        }
        Log.debug("Build folder = $buildFolder")

        // check output directory, create if it does not exist
        if (outputFolder == null) {
            outputFolder = "$buildFolder/${buildSystem.sourceFolder}/$reqmFolderName"
        }
        if (!forInit && !ensureFolderExists(outputFolder!!)) {
            return false
        }
        Log.debug("Output folder = $outputFolder")

        return true
    }

    fun getSourceFiles(): List<String> {
        val filenames : MutableList<String> = ArrayList()
        val infolder = File(inputFolder!!)
        if (infolder.isDirectory) {
            val files = infolder.list()
            if (files == null || files.isEmpty()) {
                return filenames
            }
            filenames.addAll(files.filter { it.endsWith(".reqm") })
        } else {
            filenames.add(inputFolder!!)
        }
        return filenames
    }

    fun printErrors() {
        errors.forEach { Log.error(it) }
    }

    fun srcPath(language: String): String = "$buildFolder/${buildSystem.sourceFolder}/$language"

    fun updateBuildScript(
        appHome: String,
        language: String,
        mainClass: String,
        appVersion: String,
        buildScriptUpdates: Map<String, List<String>>
    ) {
        val plugins = buildScriptUpdates["plugins"]!!
        val pluginsBlock = plugins.joinToString("\n") {
            if (it.startsWith("id:"))
                "    id(\"${it.substring(3).substringBefore(":")}\") version \"${it.substringAfterLast(":")}\""
            else
                "    ${it.substringBefore(":")} version \"${it.substringAfterLast(":")}\""
        }
        val dependecies = buildScriptUpdates["dependencies"]!!
        val dependenciesBlock = dependecies.joinToString("\n") { "    implementation(\"$it\")" }

        val params = mutableMapOf (
            "composerCommand" to "${appHome.replace('\\', '/')}/bin/composer".replace("//", "/"),
            "projectName" to mainClass.substringAfterLast('.').lowercase(),
            "version" to appVersion,
            "mainClass" to mainClass,
            "projectRootFolder" to projectFolder!!,
            "language" to language,
            "reqmSourceDir" to inputFolder!!,
            "reqmOutputDir" to outputFolder!!,
            "plugins" to pluginsBlock,
            "dependencies" to dependenciesBlock
        )
        buildSystem.updateBuildScript(params)
    }

}