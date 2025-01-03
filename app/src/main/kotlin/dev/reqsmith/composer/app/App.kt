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

package dev.reqsmith.composer.app


import dev.reqsmith.composer.common.Log
import dev.reqsmith.composer.common.Project
import dev.reqsmith.composer.common.configuration.ConfigManager
import dev.reqsmith.composer.common.plugin.PluginManager
import dev.reqsmith.composer.common.plugin.PluginType
import dev.reqsmith.composer.common.plugin.buildsys.BuildSystem
import kotlinx.cli.ArgParser
import kotlinx.cli.ArgType
import kotlinx.cli.default
import kotlinx.cli.optional
import kotlin.io.path.Path
import kotlin.io.path.absolutePathString
import kotlin.system.measureTimeMillis

class App(private val args: Array<String>) {
    val systemName = "ReqSmith"
    val composerVersion = "0.2.0-Web"
    val composerDesc = "Requirement composer and code generator"

    val path = System.getenv("APP_HOME") ?: "."
    val name = System.getenv("APP_BASE_NAME") ?: "forge"

    private val argParser = ArgParser(name)

    // general options
    private val projectDir by argParser.argument(ArgType.String, description = "project root directory (default: ./").optional()
    private val buildSystemName by argParser.option(fullName = "build", shortName = "b", type = ArgType.String, description = "build system name").default(
        ConfigManager.defaults["buildsys"] ?: "gradle")
    private val language by argParser.option(fullName = "language", shortName = "l", type = ArgType.String, description = "language for generated source code").default(
        ConfigManager.defaults["language"] ?: "kotlin")
    private val info by argParser.option(fullName = "info", type = ArgType.Boolean, description = "info log level")
    private val debug by argParser.option(fullName = "debug", type = ArgType.Boolean, description = "debug log level")

    // forge options
    private val inputDir by argParser.option(fullName = "in", shortName = "i", type = ArgType.String, description = "input directory (relative to project folder)")
    private val outputDir by argParser.option(fullName = "out", shortName = "o", type = ArgType.String, description = "output directory (relative to project folder)")

    // init options
    val initCommand by argParser.option(fullName = "init", type = ArgType.Boolean, description = "project structure initialization command")
    private val projectName by argParser.option(fullName = "project", shortName = "p", type = ArgType.String, description = "new project name")

    fun processArgs() {
        argParser.parse(args)
        Log.level = if (debug == true) Log.LogLevel.DEBUG else if (info == true) Log.LogLevel.INFO else Log.LogLevel.NORMAL
    }

    fun initiate() : Boolean {
        if (projectName.isNullOrBlank()) {
            Log.error("no project name was given for initialization")
            return false
        }

        // select build system
        val buildSystem = try {
            PluginManager.get<BuildSystem>(PluginType.BuildSystem, buildSystemName)
        } catch (e: Exception) {
            Log.error("build system $buildSystemName is not supported.")
            return false
        }

        // set up project environment
        val project = Project(projectDir, buildSystem)
        if (!project.calculateFolders(forInit = true)) {
            project.printErrors()
            return false
        }

        // create default minimal app
        Composer(project, path).createApp(projectName!!)

        return true

    }

    fun compose() : Boolean {
        Log.title("Forge ReqM model")

        // select build system
        val buildSystem = try {
            PluginManager.get<BuildSystem>(PluginType.BuildSystem, buildSystemName)
        } catch (e: Exception) {
            Log.error("build system $buildSystemName is not supported.")
            return false
        }

        // set up project environment
        val project = Project(projectDir, buildSystem).apply {
            inputFolder = inputDir
            outputFolder = outputDir
        }
        if (!project.calculateFolders()) {
            project.printErrors()
            return false
        }

        // list file from input pattern
        val filenames = project.getSourceFiles()
        if (filenames.isEmpty()) {
            project.printErrors()
            return false
        }
        Log.info("Input files:")
        filenames.forEach { Log.info("- $it") }

        // check output directory, create if it does not exist
        Log.info("Output directory: ${project.outputFolder}")

        // compose full requirement model
        val mergedReqMSource = Composer(project, path).compose()
        if (mergedReqMSource == null) {
            Log.error("Something goes wrong with the requirement composer, see previous errors; source code will not be generated. ")
            return false
        }

        // generate source code
        val generator = Generator(project, mergedReqMSource, path, language)
        val success = try {
            generator.generate()
        } catch (e: Exception) {
            Log.error("Generating source code failed: ${e.localizedMessage}")
            false
        }
        if (!success) {
            Log.error("Something goes wrong with the code generator, see previous errors; source code will not or just partially be generated. ")
            return false
        }

        return true
    }

}

fun main(args: Array<String>) {

    var success: Boolean
    val elapsedTime = measureTimeMillis {

        // load default values first
        // TODO
        ConfigManager.load(System.getenv("APP_HOME") ?: ".")

        // create the app
        val app = App(args)
        Log.title("${app.systemName}::${app.name}, version ${app.composerVersion}")
        Log.text("${app.composerDesc}\n")
        Log.info("Application root folder: ${Path(app.path).absolutePathString()}")

        // process command line arguments
        app.processArgs()

        // load all plugins
        PluginManager.loadAll()

        // run
        success = if (app.initCommand == true) {
            app.initiate()
        } else {
            app.compose()
        }

    }

    Log.text("\nFORGE ${if (success) "SUCCESSFUL" else "FAILED"} in ${if (elapsedTime > 1000) elapsedTime/1000 else elapsedTime}${if (elapsedTime > 1000) "s" else "ms"}")
}
