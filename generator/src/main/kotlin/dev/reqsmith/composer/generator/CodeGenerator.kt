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

import dev.reqsmith.composer.common.Log
import dev.reqsmith.composer.common.Project
import dev.reqsmith.model.igm.IGMClass
import dev.reqsmith.model.igm.IGMEnumeration
import dev.reqsmith.model.igm.InternalGeneratorModel
import dev.reqsmith.composer.generator.plugin.language.LanguageBuilder
import dev.reqsmith.model.enumeration.StandardTypes
import java.io.File
import java.io.FileWriter
import java.nio.charset.StandardCharsets

class CodeGenerator(private val langBuilder: LanguageBuilder, private val project: Project) {

    private val srcPath = project.srcPath(langBuilder.language)

    private fun String.toPath():String = this.replace('.', '/')

    fun generate(igm: InternalGeneratorModel, fileHeader: String): Boolean {

        igm.classes.forEach {  buildClass(it.value, fileHeader) }

        igm.enumerations.forEach { buildEnumeration(it.value, fileHeader) }

        return true
    }

    private fun buildEnumeration(enum: IGMEnumeration, fileHeader: String): Boolean {
        // build source code in string builder
        val sb = StringBuilder()

        // add general file header
        sb.append(langBuilder.addComment(fileHeader)).append("\n\n")

        // add package
        val domain = enum.enumId.substringBeforeLast('.')
        if (domain.isNotBlank()) {
            sb.append("package ${domain}\n\n")
        }

        // add imports from parent and definition
        // TODO: map type references and parent to real classes
//        sb.append(getImports(enum))

        // create a class
        sb.append(langBuilder.addEnumeration(enum))

        // create file path
        val entPath = enum.enumId.toPath()
        val entFilePath = "$srcPath/$entPath.${langBuilder.extension}"
        Log.info("Generating enumeration $entFilePath")
        val success = Project.ensureFolderExists(File(entFilePath).parent, null)
        if (!success) {
            return false
        }

        FileWriter(entFilePath, StandardCharsets.UTF_8).use { it.write(sb.toString()) }
        return true

    }

    private fun buildClass(cls: IGMClass, fileHeader: String): Boolean {
        // build source code in string builder
        val sb = StringBuilder()

        // add general file header
        sb.append(langBuilder.addComment(fileHeader)).append("\n\n")

        // add package
        val domain = cls.id.substringBeforeLast('.')
        if (domain.isNotBlank()) {
            sb.append("package ${domain}\n\n")
        }

        // add imports from parent and definition
        // TODO: map type references and parent to real classes
        sb.append(getImports(cls))

        // create a class
        sb.append(langBuilder.addClass(cls))

        // create file path
        val entPath = cls.id.toPath()
        val entFilePath = "$srcPath/$entPath.${langBuilder.extension}"
        Log.info("Generating entity $entFilePath")
        val success = Project.ensureFolderExists(File(entFilePath).parent, null)
        if (!success) {
            return false
        }

        FileWriter(entFilePath, StandardCharsets.UTF_8).use { it.write(sb.toString()) }
        return true
    }

    private fun getImports(cls: IGMClass): String {
        val imports: MutableSet<String> = HashSet()
        if (cls.parent.isNotBlank()) {
            imports.add(cls.parent)
        }
        cls.members.forEach {
            if (!StandardTypes.has(it.value.type)) {
                imports.add(it.value.type)
            }
        }
        cls.imports.forEach { imports.add(it) }

        // TODO: sort imports

        val sb = StringBuilder()
        imports.forEach { sb.append(langBuilder.addImport(it)).append("\n") }
        sb.append("\n")
        return sb.toString()
    }



}