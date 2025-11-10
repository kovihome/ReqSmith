/*
 * ReqSmith - Build application from requirements
 * Copyright (c) 2024-2025. Kovi <kovihome86@gmail.com>
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

package dev.reqsmith.composer.generator

import dev.reqsmith.composer.common.ART_FOLDER_NAME
import dev.reqsmith.composer.common.Log
import dev.reqsmith.composer.common.Project
import dev.reqsmith.composer.common.WholeProject
import dev.reqsmith.composer.common.formatter.NameFormatter
import dev.reqsmith.composer.common.plugin.language.LanguageBuilder
import dev.reqsmith.model.igm.IGMView
import java.io.File
import java.io.FileWriter
import java.nio.charset.StandardCharsets

class ViewGenerator(private val langBuilder: LanguageBuilder, private val viewResourceFolderName: String, private val artResourceFolderName: String) {

    private fun String.toPath():String = this.replace('.', '/')

    fun generate(): Boolean {

        // generate view files
        WholeProject.projectModel.igm.views.forEach { buildView(it.value) }

        return true
    }

    private fun buildView(view: IGMView): Boolean {
        // build source code in string builder
        val sb = StringBuilder()

        // create the view from the IGM model
        sb.append(langBuilder.createView(view))

        // is this view static or dynamic?
        val controllerName = "${view.id}Controller"
        val isDynamic = WholeProject.projectModel.igm.classes.any { it.key.endsWith(controllerName) }

        // create file path
        val entPath = view.id.toPath()
        val entFilePath = "${if (isDynamic) viewResourceFolderName else artResourceFolderName}/$entPath.${langBuilder.extension}"
        Log.info("Generating view $entFilePath")
        val success = Project.ensureFolderExists(File(entFilePath).parent, null)
        if (!success) {
            return false
        }

        FileWriter(entFilePath, StandardCharsets.UTF_8).use { it.write(sb.toString()) }
        return true
    }

    fun copyArts(): Boolean {
        val copyFrom = "${WholeProject.project.projectFolder}/${WholeProject.project.artFolder}"
        val copyTo = "$artResourceFolderName/${ART_FOLDER_NAME}"
        langBuilder.viewArts.forEach {
            val resourceFileName = NameFormatter.deliterateText(it)
            WholeProject.projectModel.resources.add(Pair("$copyFrom/$resourceFileName", "$copyTo/$resourceFileName"))
        }
        return true
    }

}
