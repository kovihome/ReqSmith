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

package dev.reqsmith.composer.app

import dev.reqsmith.composer.common.Log
import dev.reqsmith.composer.common.Project
import dev.reqsmith.composer.common.templating.Template
import dev.reqsmith.composer.composer.ModelMerger
import dev.reqsmith.composer.parser.ReqMParser
import dev.reqsmith.composer.parser.entities.*
import dev.reqsmith.composer.repository.api.RepositoryFinder
import dev.reqsmith.composer.validator.ModelValidator
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class Composer(private val project: Project, private val appHome: String) {

    fun compose() : ReqMSource? {

        // parse load each reqm files
        Log.text("Parsing ReqM sources from folder ${project.inputFolder}")
        val parser = ReqMParser()
        val reqmsrc = ReqMSource()
        // TODO: parse entire directory tree (as stdlib)
        val success = parser.parseFolder(project.inputFolder!!, reqmsrc)
        if (!success) {
            // Failure Point #1 - syntactical errors in reqm files
            return null
        }
        Log.info("=============== ReqMSource ===============")
        Log.info("ReqM source folder ${project.inputFolder} contains:")
        if (reqmsrc.applications.isNotEmpty()) Log.info("  ${reqmsrc.applications.size} applications")
        if (reqmsrc.modules.isNotEmpty()) Log.info("  ${reqmsrc.modules.size} modules")
        if (reqmsrc.actors.isNotEmpty()) Log.info("  ${reqmsrc.actors.size} actors")
        if (reqmsrc.classes.isNotEmpty()) Log.info("  ${reqmsrc.classes.size} classes")
        if (reqmsrc.entities.isNotEmpty()) Log.info("  ${reqmsrc.entities.size} entities")
        if (reqmsrc.actions.isNotEmpty()) Log.info("  ${reqmsrc.actions.size} actions")
        if (reqmsrc.views.isNotEmpty()) Log.info("  ${reqmsrc.views.size} views")
        // TODO: if (reqmsrc.features.isNotEmpty()) Log.info("  ${reqmsrc.features.size} actions")
        Log.info("==========================================")

        // consolidate reqm source - inconsistencies, merge multiple element instances
        Log.title("Consolidate ReqM")
        val validator = ModelValidator()
        val isConsistent = validator.resolveInconsistencies(reqmsrc)
        if (!isConsistent) {
            // TODO: Failure Point #2 - inconsistencies in reqm model
            return null
        }

        // search repository for reqm source elements
        Log.title("Search for ReqM Repositories")
        val finder = RepositoryFinder()
        val ok : Boolean = finder.connect(appHome)
        if (!ok) {
            Log.error("Repository access problem; see previous errors for details")
            return null
        }

        val merger = ModelMerger(finder)

        // compose fully defined reqm from reqm source and repository search result
        Log.title("Merge model with identified references")
        val dependenciesReqMModel = merger.merge(reqmsrc)

        // resolve ownership of actions
        Log.info("Resolve actions' ownership")
        val errorList = validator.resolveActionOwnership(reqmsrc, dependenciesReqMModel)
        if (errorList.isNotEmpty()) {
            errorList.forEach { Log.error(it) }
            return null
        }

        // validate model completeness
        Log.title("Validate model completeness")
        val isComplete = validator.validateCompleteness(reqmsrc)
        if (!isComplete) {
            // TODO: Failure Point #3 - merged model is not complete
            return null
        }

        // save composed reqm and dependencies
        val projectName = reqmsrc.applications[0].qid!!.id!!
        val reqmFileHeader = getReqmFileHeader(projectName, "templates/reqm_eff_header.st")
        val writer = dev.reqsmith.composer.parser.ReqMWriter()
        writeReqmFile("effective.reqm", reqmsrc, reqmFileHeader, writer)
        writeReqmFile("dependencies.reqm", dependenciesReqMModel, reqmFileHeader, writer)

        // add dependencies to the application reqm model for generation
        addDependenciesToGenerate(reqmsrc, dependenciesReqMModel)

        return reqmsrc
    }

    private fun writeReqmFile(fileName: String, reqmModel: ReqMSource, reqmFileHeader: String, writer: dev.reqsmith.composer.parser.ReqMWriter) {
        val outputDepFileName = "${project.outputFolder}/$fileName"
        Log.title("Saving dependencies to $outputDepFileName")
        writer.writeReqM(reqmModel, outputDepFileName, reqmFileHeader)
        Log.info("=============== ${if (fileName.startsWith("dep")) "Dependencies" else "ReqMSource"} ===============")
        writer.printReqMSource(reqmModel)
        Log.info("==========================================")
    }

    private fun addDependenciesToGenerate(reqmsrc: ReqMSource, dependenciesReqMModel: ReqMSource) {
        // copy relevant actions
        reqmsrc.actions.addAll(dependenciesReqMModel.actions.filter { it.definition != ActionDefinition.Undefined })

        // TODO: copy other relevant elements into model
    }

    fun createApp(projectName: String) {
        val application = Application().apply {
            qid = QualifiedId(projectName)
            sourceRef = QualifiedId.Undefined
            definition = Definition.Undefined
        }
        val reqmsrc = ReqMSource().apply {
            applications.add(application)
        }
        val outputFileName = "${project.inputFolder}/$projectName.reqm"
        Log.info("\nSaving model to $outputFileName")
        val writer = dev.reqsmith.composer.parser.ReqMWriter()
        writer.writeReqM(reqmsrc, outputFileName, getReqmFileHeader(projectName, "templates/reqm_src_header.st"))
    }

    private fun getReqmFileHeader(appName: String, headerFileName: String): String {
        val context = mapOf(
            "appName" to appName,
            "composerName" to "ReqSmith::forge",
            "version" to "0.2.0-Web",
            "now" to LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME),
        )
        return Template().translateFile(context, headerFileName)
    }

}