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
import dev.reqsmith.composer.common.WholeProject
import dev.reqsmith.composer.common.templating.Template
import dev.reqsmith.composer.composer.ModelMerger
import dev.reqsmith.composer.parser.ReqMParser
import dev.reqsmith.composer.repository.api.RepositoryFinder
import dev.reqsmith.composer.validator.ModelValidator
import dev.reqsmith.model.reqm.*
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class Composer(/*private val project: Project, private val projectModel: ProjectModel, */private val appHome: String) {

    fun compose() : Boolean {

        // parse load each reqm files
        Log.info("Parsing ReqM sources from folder ${WholeProject.project.inputFolder}")
        val parser = ReqMParser()
        // TODO: parse entire directory tree (as stdlib)
        val success = parser.parseFolder(WholeProject.project.inputFolder!!, WholeProject.projectModel.source)
        if (!success) {
            // Failure Point #1 - syntactical errors in reqm files
            return false
        }
        Log.info("=============== ReqMSource ===============")
        Log.info("ReqM source folder ${WholeProject.project.inputFolder} contains:")
        if (WholeProject.projectModel.source.applications.isNotEmpty()) Log.info("  ${WholeProject.projectModel.source.applications.size} applications")
        if (WholeProject.projectModel.source.modules.isNotEmpty()) Log.info("  ${WholeProject.projectModel.source.modules.size} modules")
        if (WholeProject.projectModel.source.actors.isNotEmpty()) Log.info("  ${WholeProject.projectModel.source.actors.size} actors")
        if (WholeProject.projectModel.source.classes.isNotEmpty()) Log.info("  ${WholeProject.projectModel.source.classes.size} classes")
        if (WholeProject.projectModel.source.entities.isNotEmpty()) Log.info("  ${WholeProject.projectModel.source.entities.size} entities")
        if (WholeProject.projectModel.source.actions.isNotEmpty()) Log.info("  ${WholeProject.projectModel.source.actions.size} actions")
        if (WholeProject.projectModel.source.views.isNotEmpty()) Log.info("  ${WholeProject.projectModel.source.views.size} views")
        if (WholeProject.projectModel.source.features.isNotEmpty()) Log.info("  ${WholeProject.projectModel.source.features.size} features")
        Log.info("==========================================")

        // consolidate reqm source - inconsistencies, merge multiple element instances
        Log.title("Consolidate ReqM")
        val validator = ModelValidator()
        val isConsistent = validator.resolveInconsistencies()
        if (!isConsistent) {
            // TODO: Failure Point #2 - inconsistencies in reqm model
            return false
        }

        // search repository for reqm source elements
        Log.title("Search for ReqM Repositories")
        val finder = RepositoryFinder()
        val ok : Boolean = finder.connect(appHome)
        if (!ok) {
            Log.error("Repository access problem; see previous errors for details")
            return false
        }

        // compose fully defined reqm from reqm source and repository search result
        Log.title("Merge model with identified references")
        ModelMerger(finder).merge()

        // resolve ownership of actions
        Log.info("Resolve actions' ownership")
        val errorList = validator.resolveActionOwnership()
        if (errorList.isNotEmpty()) {
            errorList.forEach { Log.error(it) }
            return false
        }

        // drop applications from dependencies
        WholeProject.projectModel.dependencies.applications.clear()

        // validate model completeness
        Log.title("Validate model completeness")
        val isComplete = validator.validateCompleteness()
        if (!isComplete) {
            // TODO: Failure Point #3 - merged model is not complete
            return false
        }

        // save composed reqm and dependencies
        val projectName = WholeProject.projectModel.source.applications[0].qid!!.id!!
        val reqmFileHeader = getReqmFileHeader(projectName, "templates/reqm_eff_header.st")
        val writer = dev.reqsmith.composer.parser.ReqMWriter()
        writeReqmFile("effective.reqm", WholeProject.projectModel.source, reqmFileHeader, writer)
        writeReqmFile("dependencies.reqm", WholeProject.projectModel.dependencies, reqmFileHeader, writer)

        // add dependencies to the application reqm model for generation
        addDependenciesToGenerate()

        return true
    }

    private fun writeReqmFile(fileName: String, reqmModel: ReqMSource, reqmFileHeader: String, writer: dev.reqsmith.composer.parser.ReqMWriter) {
        val outputDepFileName = "${WholeProject.project.outputFolder}/$fileName"
        Log.text("Saving dependencies to $outputDepFileName")
        writer.writeReqM(reqmModel, outputDepFileName, reqmFileHeader)
        Log.info("=============== ${if (fileName.startsWith("dep")) "Dependencies" else "ReqMSource"} ===============")
        writer.printReqMSource(reqmModel)
        Log.info("==========================================")
    }

    private fun addDependenciesToGenerate() {
        // copy relevant actions
        WholeProject.projectModel.source.actions.addAll(WholeProject.projectModel.dependencies.actions.filter { it.definition != ActionDefinition.Undefined && it.owner != null })

        // copy features
        WholeProject.projectModel.source.features.addAll(WholeProject.projectModel.dependencies.features)

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
        val outputFileName = "${WholeProject.project.inputFolder}/$projectName.reqm"
        Log.info("\nSaving model to $outputFileName")
        val writer = dev.reqsmith.composer.parser.ReqMWriter()
        writer.writeReqM(reqmsrc, outputFileName, getReqmFileHeader(projectName, "templates/reqm_src_header.st"))
    }

    private fun getReqmFileHeader(appName: String, headerFileName: String): String {
        val context = mapOf(
            "appName" to appName,
            "composerName" to "ReqSmith::forge",
            "version" to "0.3.0-Forms",
            "now" to LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME),
        )
        return Template().translateFile(context, headerFileName)
    }

}