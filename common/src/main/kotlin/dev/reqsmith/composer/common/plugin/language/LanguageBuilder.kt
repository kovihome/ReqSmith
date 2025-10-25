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

package dev.reqsmith.composer.common.plugin.language

import dev.reqsmith.model.igm.IGMClass
import dev.reqsmith.model.igm.IGMEnumeration
import dev.reqsmith.model.igm.IGMStyle
import dev.reqsmith.model.igm.IGMView

interface LanguageBuilder {
    /**
     * File name extension of the generated artifact
     */
    val extension: String

    /**
     * Language ID
     */
    val language: String

    var artPathPrefix: String
    val viewArts : MutableList<String>

    /**
     * Create a class artifact
     *
     * (Source code generator plugins must implement this method)
     * @param cls Class generator model
     * @param indent Indentation of the source
     * @return The source code artifact
     */
    fun addClass(cls: IGMClass, indent: Int = 0): String

    /**
     * Create an enumeration class artifact
     *
     * (Source code generator plugins must implement this method)
     * @param enum Enumeration class generator model
     * @param indent Indentation of the source
     * @return The source code artifact
     */
    fun addEnumeration(enum: IGMEnumeration, indent: Int = 0): String

    /**
     * Create a comment block for the artifact
     * @param text Comment text, multiple comment lines separated by \\n are accepted
     * @param indent Indentation of the source
     * @return Formatted comment block
     */
    fun addComment(text: String, indent: Int = 0): String

    /**
     * Create an import statement
     * @param imported Name of the imported artifact
     * @param indent Indentation of the source
     * @return The import statement
     */
    fun addImport(imported: String, indent: Int = 0): String

    /**
     * Create a view artifact from the generator model.
     *
     * (View generator plugins must implement this method)
     * @param view View generator model
     * @return The view artifact
     */
    fun createView(view: IGMView): String

    /**
     * Create a style artifact from the generator model.
     *
     * (Style generator plugins must implement this method)
     * @param style Style generator model
     * @return The style artifact
     */
    fun createStyle(style: IGMStyle): String

    fun typeMapper(type : String?) : String

    fun collectBuildScriptElement(buildScriptUpdates: Map<String, MutableList<String>>)

}