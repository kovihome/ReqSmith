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

const val VIEW_LAYOUT_ELEMENT_ATTR_TITLE = "title"
const val VIEW_LAYOUT_ELEMENT_ATTR_DATA = "data"
const val VIEW_LAYOUT_ELEMENT_ATTR_TO = "to"

enum class StandardLayoutElements(val attributes: List<String>) {
    datatable(listOf(VIEW_LAYOUT_ELEMENT_ATTR_TITLE, VIEW_LAYOUT_ELEMENT_ATTR_DATA, "createForm")),
    footer(listOf("linkGroup", "copyrightText", "facebookLink", "twitterLink", "linkedinLink", "youtubeLink", "githubLink")),
    form(listOf(VIEW_LAYOUT_ELEMENT_ATTR_TITLE, VIEW_LAYOUT_ELEMENT_ATTR_DATA)),
    `header`(listOf(VIEW_LAYOUT_ELEMENT_ATTR_TITLE, "logo")),
    linkButton(listOf(VIEW_LAYOUT_ELEMENT_ATTR_TITLE, VIEW_LAYOUT_ELEMENT_ATTR_TO, "icon")),
    linkGroup(listOf(VIEW_LAYOUT_ELEMENT_ATTR_TITLE, VIEW_LAYOUT_ELEMENT_ATTR_TO)),
    menu(listOf("type", VIEW_LAYOUT_ELEMENT_ATTR_TITLE, VIEW_LAYOUT_ELEMENT_ATTR_TO)),
    navigation(listOf(VIEW_LAYOUT_ELEMENT_ATTR_TITLE, "position", "side", "home", "menu", "breadcrumb", "search", "cta", "select")),
    panel(listOf("align")),
    spacer(listOf()),
    text(listOf("label")),
//  accordion,
//  alert,
//  badge,
//  breadcrumbs,
    button(listOf(VIEW_LAYOUT_ELEMENT_ATTR_TITLE, VIEW_LAYOUT_ELEMENT_ATTR_TO, "icon")),
//  button group,
//  card (tile),
//  carousel,
//  check/combo/list box,
//  collapse,
//  container,
//  grid,
//  heading,
//  hero,
    image(listOf("src", "alt", "size")),
//  input(listOf("name", "type", "placeholder", "value", "required", "readonly", "disabled", "autocomplete", "autofocus", "list", "maxlength", "minlength", "pattern", "size", "step", "min", "max")),
//  link(listOf("to", "text", "title", "target")),
//  list, grid list,
//  loader,
//  modal,
//  pagination,
//  paragraph,
//  popup/popover/modal,
//  pre-header,
//  progress,
//  progress bar,
//  region,
//  search,
//  sidebar,
//  slider,
//  spinner,
//  tab (tab bar),
//  table,
//  toast (notification),
//  tooltip,
//  video,
    ;

    fun hasAttribute(a: String) = attributes.contains(a)

    companion object {
        fun contains(s: String) = entries.map { it.name }.contains(s)
    }
}
