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

package dev.reqsmith.composer.generator.plugin.language

import dev.reqsmith.model.igm.IGMClass
import dev.reqsmith.model.igm.IGMEnumeration
import dev.reqsmith.model.igm.IGMView

interface LanguageBuilder {
    val extension: String
    val language: String

    var artPathPrefix: String
    val viewArts : MutableList<String>

    fun addClass(cls: IGMClass, indent: Int = 0): String
    fun addEnumeration(enum: IGMEnumeration, indent: Int = 0): String
    fun addComment(text: String, indent: Int = 0): String
    fun addImport(imported: String, indent: Int = 0): String
    fun createView(view: IGMView): String

    fun typeMapper(type : String?) : String

    fun collectBuildScriptElement(buildScriptUpdates: Map<String, MutableList<String>>)

}