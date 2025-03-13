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

package dev.reqsmith.model.enumeration

enum class StandardLayoutElements(val attributes: List<String>) {
    datatable(listOf("title", "data", "createForm")),
    footer(listOf("linkGroup", "copyrightText", "facebookLink", "twitterLink", "linkedinLink", "youtubeLink", "githubLink")),
    form(listOf("title", "data")),
    `header`(listOf("title", "logo")),
    linkButton(listOf("title", "to")),
    linkGroup(listOf("title", "to")),
    panel(listOf()),
    spacer(listOf()),
    text(listOf()),
//  image(listOf("src", "alt", "width", "height")),
//  input(listOf("name", "type", "placeholder", "value", "required", "readonly", "disabled", "autocomplete", "autofocus", "list", "maxlength", "minlength", "pattern", "size", "step", "min", "max")),
//  label(listOf("for")),
//  link(listOf("to", "text", "title", "target")),
    ;

    companion object {
        fun contains(s: String) : Boolean = entries.map { it.name }.contains(s)
    }
}
