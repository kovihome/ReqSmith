/*
 * ReqSmith - Build application from requirements
 * Copyright (c) 2024. Kovi <kovihome86@gmail.com> 
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

import dev.reqsmith.composer.common.Log
import dev.reqsmith.composer.common.Project
import dev.reqsmith.composer.generator.entities.IGMView
import dev.reqsmith.composer.generator.entities.InternalGeneratorModel
import dev.reqsmith.composer.generator.plugin.language.LanguageBuilder
import java.io.File
import java.io.FileWriter
import java.nio.charset.StandardCharsets

class ViewGenerator(private val langBuilder: LanguageBuilder, private val project: Project, private val viewResourceFolderName: String) {

    private fun String.toPath():String = this.replace('.', '/')

    fun generate(igm: InternalGeneratorModel): Boolean {
        langBuilder.artPathPrefix = "art"

        // generate view files
        igm.views.forEach { buildView(it.value) }

        return true
    }

    private fun buildView(view: IGMView): Boolean {
        // build source code in string builder
        val sb = StringBuilder()

        // create the view from the IGM model
        sb.append(langBuilder.createView(view))

        // create file path
        val entPath = view.id.toPath()
        val entFilePath = "$viewResourceFolderName/$entPath.${langBuilder.extension}"
        Log.info("Generating view $entFilePath")
        val success = project.ensureFolderExists(File(entFilePath).parent)
        if (!success) {
            return false
        }

        FileWriter(entFilePath, StandardCharsets.UTF_8).use { it.write(sb.toString()) }
        return true
    }

    fun copyArts(): Boolean {
        val copyFrom = "${project.projectFolder}/${project.artFolder}"
        val copyTo = "$viewResourceFolderName/art"
        project.ensureFolderExists(copyTo)

        langBuilder.viewArts.forEach {
            val resourceFileName = it.removeSurrounding("'").removeSurrounding("\"")
            val src = "$copyFrom/$resourceFileName"
            val dest = "$copyTo/$resourceFileName"
            val infile = File(src)
            if (!infile.exists()) {
                Log.warning("Art file $resourceFileName is not exist in the art folder $copyFrom")
            } else {
                infile.copyTo(File(dest), true)
                Log.info("Copy art file $copyTo/$resourceFileName")
            }
        }
        return true
    }

}
