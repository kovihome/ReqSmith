/*
 * ReqSmith - Build application from requirements
 * Copyright (c) 2025. Kovi <kovihome86@gmail.com> 
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
import dev.reqsmith.composer.common.WholeProject
import dev.reqsmith.composer.generator.plugin.language.LanguageBuilder
import dev.reqsmith.model.igm.IGMStyle
import java.io.File
import java.io.FileWriter
import java.nio.charset.StandardCharsets

class StyleGenerator(private val langBuilder: LanguageBuilder, private val resourceFolderName: String) {

    private fun String.toPath():String = this.replace('.', '/')

    fun generate(): Boolean {
        var hasErrors = false
        
        // generate view files
        WholeProject.projectModel.igm.styles.forEach { hasErrors = buildStyle(it.value) || hasErrors }

        return !hasErrors
    }

    private fun buildStyle(style: IGMStyle): Boolean {
        // build source code in string builder
        val sb = StringBuilder()

        // create the view from the IGM model
        sb.append(langBuilder.createStyle(style))

        // create the file path
        val entPath = style.id.toPath()
        val entFilePath = "$resourceFolderName/${langBuilder.language}/$entPath.${langBuilder.extension}"
        Log.info("Generating style $entFilePath")
        val success = Project.ensureFolderExists(File(entFilePath).parent, null)
        if (!success) {
            return false
        }

        FileWriter(entFilePath, StandardCharsets.UTF_8).use { it.write(sb.toString()) }
        return true

    }


}
