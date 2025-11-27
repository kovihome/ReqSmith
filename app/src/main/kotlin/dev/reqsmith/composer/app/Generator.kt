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

import dev.reqsmith.composer.common.Folders
import dev.reqsmith.composer.common.Log
import dev.reqsmith.composer.common.Project
import dev.reqsmith.composer.common.WholeProject
import dev.reqsmith.composer.common.plugin.PluginManager
import dev.reqsmith.composer.common.plugin.PluginType
import dev.reqsmith.composer.common.templating.Template
import dev.reqsmith.composer.generator.CodeGenerator
import dev.reqsmith.composer.generator.GeneratorModelBuilder
import dev.reqsmith.composer.generator.StyleGenerator
import dev.reqsmith.composer.generator.ViewGenerator
import dev.reqsmith.composer.common.plugin.language.LanguageBuilder
import java.io.File
import java.io.FileWriter
import java.util.*

private const val RESOURCE_SAVING_PREFIX = "<save>"

class Generator(private val lang: String) {

    private val langBuilder = PluginManager.get<LanguageBuilder>(PluginType.Language, lang)
    private val srcPath = WholeProject.project.srcPath(lang)

    fun generate(): Boolean {
        // create the internal generator model
        Log.debug("language plugin $lang is using in app.Generator.constructor().")
        Log.title("Build Internal Generator Model")
        Log.info("Generated source path: $srcPath")
        val resourcesFolderName = "${WholeProject.project.buildFolder}/${WholeProject.project.buildSystem.resourceFolder}"
        Project.ensureFolderExists(resourcesFolderName, null)
        val generatorModelBuilder = GeneratorModelBuilder(resourcesFolderName)
        generatorModelBuilder.build()
        if (Log.level == Log.LogLevel.DEBUG) {
            val igmFolder = Folders.BuildForgeIgm.absPath() // "${WholeProject.project.buildFolder}/${WholeProject.project.INTERNAL_FORGE_FOLDER_NAME}/igm"
            Project.ensureFolderExists(igmFolder, null)
            val igmPath = "$igmFolder/${WholeProject.projectModel.source.applications[0].qid?.id}.igm"
            Log.debug("Write IGM to $igmPath")
            FileWriter(igmPath).use { it.write(WholeProject.projectModel.igm.print()) }
        }

        // generate the source code
        Log.title("Generate source code")
        val success = CodeGenerator(langBuilder).generate(getFileHeader())

        // generate styles
        var successStyles = true
        if (WholeProject.projectModel.igm.styles.isNotEmpty()) {
            Log.debug("style language plugin ${generatorModelBuilder.styleGeneratorName} is using in app.Generator.generate().")
            val styleLangBuilder = PluginManager.get<LanguageBuilder>(PluginType.Language, generatorModelBuilder.styleGeneratorName)
            val artResourceFolderName = "$resourcesFolderName/${generatorModelBuilder.codeBuilder!!.getArtFolder()}"
            val styleGenerator = StyleGenerator(styleLangBuilder, artResourceFolderName)
            successStyles = styleGenerator.generate()

        }

        // generate views
        var successView = true
        if (WholeProject.projectModel.igm.views.isNotEmpty()) {
            Log.debug("view language plugin ${generatorModelBuilder.viewGeneratorName} is using in app.Generator.generate().")
            val viewLangBuilder = PluginManager.get<LanguageBuilder>(PluginType.Language, generatorModelBuilder.viewGeneratorName)
            val viewResourceFolderName = "$resourcesFolderName/${generatorModelBuilder.codeBuilder!!.getViewFolder()}"
            val artResourceFolderName = "$resourcesFolderName/${generatorModelBuilder.codeBuilder!!.getArtFolder()}"
            val viewGenerator = ViewGenerator(viewLangBuilder, viewResourceFolderName, artResourceFolderName)
            successView = viewGenerator.generate()

            viewGenerator.copyArts()
        }

        // copy resources
        WholeProject.projectModel.resources.forEach {
            copyResource(it.first, it.second)
        }

        // update build script
        val buildScriptUpdates = mapOf(
            "plugins" to mutableListOf<String>(),
            "dependencies" to mutableListOf()
        )
        langBuilder.collectBuildScriptElement(buildScriptUpdates)
        generatorModelBuilder.codeBuilder?.collectBuildScriptElement(buildScriptUpdates)

        generateBuildScripts(buildScriptUpdates)

        return success.and(successView).and(successStyles)
    }

    private fun copyResource(fileNameFrom: String, fileNameTo: String) {
        val to = File(fileNameTo)
        if (!Project.ensureFolderExists(to.parent, null)) {
            Log.error("Copy resource file $fileNameTo was failed; folder ${to.parent} is not exists and cannot be created.")
            return
        }
        if (fileNameFrom.startsWith(RESOURCE_SAVING_PREFIX)) {
            val fileContent = fileNameFrom.substring(RESOURCE_SAVING_PREFIX.length)
            try {
                Log.info("Generating resource $fileNameTo")
                FileWriter(to, false).use { it.write(fileContent) }
            } catch (e: Exception) {
                Log.error("Generating resource file $fileNameTo was failed; ${e.localizedMessage}")
            }
        } else {
            var from = File(fileNameFrom)
            if (!from.exists()) {
                from = File(Folders.BuildForgeResourcesArt.absFilePath(from.name))
                if (!from.exists()) {
                    Log.error("Resource file $fileNameFrom is not exists.")
                    return
                }
            }
            try {
                Log.info("Copy resource $fileNameTo")
                from.copyTo(to, overwrite = true)
            } catch (e: Exception) {
                Log.error("Copy resource file $fileNameTo was failed; ${e.localizedMessage}")
            }
        }

    }

    /**
     * Generate/update build script
     */
    private fun generateBuildScripts(buildScriptUpdates: Map<String, List<String>>) {
        val version = WholeProject.projectModel.source.applications[0].definition.properties.find { it.key == "version" }?.value ?: "0.1.0"
        WholeProject.project.updateBuildScript(lang, WholeProject.projectModel.source.applications[0].qid.toString(), version, buildScriptUpdates)
    }

    private fun getFileHeader(): String {
        // TODO: use different licence model templates
        val thisApplication = WholeProject.projectModel.source.applications[0]
        val appName = thisApplication.qid?.id ?: "NamelessApplication"
        val description =
            thisApplication.definition.properties.find { it.key == "description" }?.value ?: "No description"
        val author = thisApplication.definition.properties.find { it.key == "author" }?.value ?: "Author"
        val authorMail =
            thisApplication.definition.properties.find { it.key == "author-mail" }?.value ?: "author@reqsmith.dev"
        val context = mapOf(
            "appName" to appName,
            "description" to description,
            "year" to Calendar.getInstance()[Calendar.YEAR].toString(),
            "author" to author,
            "mail" to authorMail
        )
        return Template().translateFile(context, "templates/gnu_file_header.st")
    }

}
