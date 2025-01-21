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
import dev.reqsmith.composer.common.plugin.PluginManager
import dev.reqsmith.composer.common.plugin.PluginType
import dev.reqsmith.composer.common.templating.Template
import dev.reqsmith.composer.generator.CodeGenerator
import dev.reqsmith.composer.generator.GeneratorModelBuilder
import dev.reqsmith.composer.generator.ViewGenerator
import dev.reqsmith.composer.generator.plugin.language.LanguageBuilder
import dev.reqsmith.model.ProjectModel
import java.io.File
import java.io.FileWriter
import java.util.*

private const val RESOURCE_SAVING_PREFIX = "<save>"

class Generator(
    private val project: Project,
    private val projectModel: ProjectModel,
    private val appHome: String,
    private val lang: String
) {

    private val langBuilder = PluginManager.get<LanguageBuilder>(PluginType.Language, lang)
    private val srcPath = project.srcPath(lang)

    fun generate(): Boolean {
        // create internal generator model
        Log.debug("language plugin $lang is using in app.Generator.constructor().")
        Log.title("Build Internal Generator Model")
        Log.info("Generated source path: $srcPath")
        val resourcesFolderName = "${project.buildFolder}/${project.buildSystem.resourceFolder}"
        Project.ensureFolderExists(resourcesFolderName, null)
        val gmb = GeneratorModelBuilder(projectModel, resourcesFolderName, project)
        gmb.build()
        Log.info("=============== InternalGeneratorModel ===============\n${projectModel.igm.print()}")
        Log.info("======================================================\n")
        if (Log.level == Log.LogLevel.DEBUG) {
            val igmFolder = "${project.buildFolder}/igm"
            Project.ensureFolderExists(igmFolder, null)
            val igmPath = "$igmFolder/${projectModel.source.applications[0].qid?.id}.igm"
            Log.debug("Write IGM to $igmPath")
            FileWriter(igmPath).use { it.write(projectModel.igm.print()) }
        }

        // generate the source code
        Log.title("Generate source code")
        val success = CodeGenerator(langBuilder, project, projectModel.igm).generate(getFileHeader())

        // generate views
        var successView = true
        if (projectModel.igm.views.isNotEmpty()) {
            Log.debug("view language plugin ${gmb.viewGeneratorName} is using in app.Generator.generate().")
            val viewLangBuilder = PluginManager.get<LanguageBuilder>(PluginType.Language, gmb.viewGeneratorName).apply {
                igm = projectModel.igm
            }
            val viewResourceFolderName = "$resourcesFolderName/${gmb.codeBuilder!!.getViewFolder()}"
            val artResourceFolderName = "$resourcesFolderName/${gmb.codeBuilder!!.getArtFolder()}"
            val viewGenerator = ViewGenerator(viewLangBuilder, project, projectModel, viewResourceFolderName, artResourceFolderName)
            successView = viewGenerator.generate()

            viewGenerator.copyArts()
        }

        // copy resources
        projectModel.resources.forEach {
            copyResource(it.first, it.second)
        }

        // update build script
        val buildScriptUpdates = mapOf(
            "plugins" to mutableListOf<String>(),
            "dependencies" to mutableListOf()
        )
        langBuilder.collectBuildScriptElement(buildScriptUpdates)
//        if (!gmb.viewBuilder!!::class.isSubclassOf(gmb.codeBuilder!!::class)) {
            gmb.codeBuilder?.collectBuildScriptElement(buildScriptUpdates)
//        }
//        gmb.viewBuilder?.collectBuildScriptElement(buildScriptUpdates)

        generateBuildScripts(buildScriptUpdates)

        return success.and(successView)
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
            val from = File(fileNameFrom)
            if (!from.exists()) {
                Log.error("Resource file $fileNameFrom is not exists.")
                return
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
        val version = projectModel.source.applications[0].definition.properties.find { it.key == "version" }?.value ?: "0.1.0"
        project.updateBuildScript(appHome, lang, projectModel.source.applications[0].qid.toString(), version, buildScriptUpdates)
    }

    private fun getFileHeader(): String {
        // TODO: use different licence model templates
        val thisApplication = projectModel.source.applications[0]
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
