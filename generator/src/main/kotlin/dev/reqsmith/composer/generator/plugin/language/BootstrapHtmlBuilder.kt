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

import dev.reqsmith.composer.common.plugin.PluginDef
import dev.reqsmith.composer.common.plugin.PluginType
import dev.reqsmith.model.igm.IGMClass
import dev.reqsmith.model.igm.IGMView
import kotlinx.html.*
import kotlinx.html.stream.createHTML
import kotlin.math.max

private const val FORM_COLUMN_SIZE_CLS = "col-md-6"

class BootstrapHtmlBuilder: HtmlBuilder() {
    override fun definition(): PluginDef {
        return PluginDef("html.bootstrap", PluginType.Language)
    }

    override fun createHeader(node: IGMView.IGMNode): String {
        val attr = node.attributes.toMap()
        return createHTML(true).header {
            classes = setOf("bg-light", "py-3")

            div { classes = setOf("container", "d-flex", "align-items-center")
                if (attr.contains("logo")) {
                    img { src = "$artPathPrefix/${attr["logo"]}"; alt = "logo"; classes = setOf("me-3"); style = "height: 128px;"
                        viewArts.add(attr["logo"] ?: "")
                    }
                }
                if (attr.contains("title")) {
                    h1 { classes = setOf("m-0", "fs-4"); text(attr["title"] ?: "(Title comes here)") }
                }
            }
        }
    }

    override fun typeMapper(type: String?): String {
        TODO("Not yet implemented")
    }

    override fun collectBuildScriptElement(buildScriptUpdates: Map<String, MutableList<String>>) {
        TODO("Not yet implemented")
    }

    override fun createText(node: IGMView.IGMNode): String {
        val attr = node.attributes.toMap()
        return createHTML(true).p {
            // write multiline text
            val texts = (attr["default"]?:"").split("\\n")
            text(texts[0])
            if (texts.size > 1) {
                texts.subList(1, texts.size).forEach {
                    br()
                    text(it)
                }
            }
        }
    }

    private fun checkLink(link: String?): String {
        return when {
            link.isNullOrBlank() -> "#"
            link.startsWith("http") -> link
            existingView(link) -> "$link.html"
            else -> "#"
        }
    }

    override fun createLinkGroup(node: IGMView.IGMNode): String {
        val groupTitle = node.attributes.find { it.first == "title" }?.second ?: "Links"
        val colSize = node.attributes.find { it.first == "colSize" }?.second ?: "6"
        return createHTML(true).div {
            classes = setOf("col-md-$colSize", "mb-3", "mb-md-0")

            h5 { text(groupTitle) }
            ul { classes = setOf("list-unstyled")
                node.attributes.filter { it.first == "to" }.forEach { link ->
                    val linkText = link.second.replace("_", " ")
                    li {
                        a { href = checkLink(link.second); classes = setOf("text-white", "text-decoration-none"); text(linkText) }
                    }
                }
            }
        }
    }

    override fun createLinkButton(node: IGMView.IGMNode): String {
        return createHTML(true).a {
            classes = setOf("btn", "btn-primary")
            role = "button"
            href = checkLink(node.attributes.find { it.first == "to" }?.second)
            text(node.attributes.find { it.first == "title" }?.second ?: "LinkButton")
        }
    }

    override fun createSpacer(node: IGMView.IGMNode): String {
        val default = node.attributes.find { it.first == "default" }?.second ?: "line"
        return when (default) {
            "line" -> createHTML(true).hr {
            }
            else -> createHTML(true).div {
                classes = setOf("pb-3")
            }
        }
    }

    override fun createPanel(node: IGMView.IGMNode): String {
        return createHTML(true).div {
            classes = setOf("container", "mt-4")
            node.children.forEach {
                unsafe { raw(createNode(it)) }
            }
        }
    }

    override fun createFooter(node: IGMView.IGMNode): String {
        val attr = node.attributes.toMap()
        return createHTML(true).footer {
            classes = setOf("bg-dark", "text-white", "py-4", "mt-5", "fixed-bottom")

            div { classes = setOf("container")
                div { classes = setOf("row")

                    // footer link groups
                    val linkGroups = node.children.filter { it.name == "linkGroup" }
                    val ngroups = linkGroups.size
                    val colSize = max(6 / ngroups, 1)
                    linkGroups.forEach { linkGroup ->
                        linkGroup.attributes.add(Pair("colSize", colSize.toString()))
                        unsafe { raw(createLinkGroup(linkGroup)) }
                    }

                    //
                    div { classes = setOf("col-md-${12-colSize*ngroups}", "text-md-end")

                        // footer info (copyright)
                        val copyrightList = node.attributes.filter { it.first == "copyrightText" }
                        copyrightList.forEach {
                            p { classes = setOf("mb-0")
                                val copyText = if (attr.containsKey(it.second)) attr.getOrDefault(it.second, it.second) else it.second
                                text(copyText)
                            }
                        }

                        // social media icons
                        val socialMediaLinks = node.attributes.filter { it.first.endsWith("Link") }
                        socialMediaLinks.let { links ->
                            div { classes = setOf("mt-3")
                                listOf("facebook", "twitter", "linkedin", "youtube", "github").forEach { media ->
                                    val linkValue = links.find { it.first == "${media}Link" }?.second ?: "#"
                                    val mediaClass = "bi-${media}"
                                    a { href = linkValue; classes = setOf("me-3")
                                        i { classes = setOf("bi", mediaClass); style = "font-size:1.5rem;color:white;" }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    override fun createForm(node: IGMView.IGMNode): String {
        val attr = node.attributes.toMap()
        val entityName = attr["data"] ?: ""
        val title = attr["title"] ?: "Add new $entityName"

        // collect entity members
        val entity = getEntity(entityName)

        return createHTML(true).div {
            classes = setOf("container-sm", "align-items-center", FORM_COLUMN_SIZE_CLS)

            h5 { classes = setOf("mb-3"); text(title) }
            form {
//              classes = setOf("row", "g-3")
                action = "/service/$entityName/persist"
                method = FormMethod.post

                entity.members.filter { it.key != "id" }.forEach { member ->

                    val memberName = member.key
                    val memberType = member.value.type
                    val enumList = getEnumeration(memberType)
                    // control types: text, select, check, radio, range, date
                    val controlType = when (memberType) {
                        "String" -> "text"
                        "Int", "Long" -> "text"
                        "Boolean" -> "checkbox"
                        "Date" -> "date"
                        else -> {
                            if (enumList.isNotEmpty()) {
                                "select"
                            } else {
                                "text"
                            }
                        }
                    }

                    div { classes = setOf("row", "mb-3")
                        label { htmlFor = memberName; classes = setOf(FORM_COLUMN_SIZE_CLS, "col-form-label"); text("${memberName.replaceFirstChar { it.uppercase() }}:") }
                        when (controlType) {
                            "text" -> {
                                div {
                                    classes = setOf(FORM_COLUMN_SIZE_CLS)
                                    input { id = memberName; type = InputType.text; name = memberName; classes = setOf("form-control")
//                                        value = memberType
                                    }
                                }
                            }
                            "select" -> {
                                div {
                                    classes = setOf(FORM_COLUMN_SIZE_CLS)
                                    select {
                                        id = memberName; name = memberName; classes = setOf("form-select")
                                        option { value = ""; selected = true; text("Select...")  }
                                        enumList.forEach {
                                            option { value = it; text(it) }
                                        }
                                    }
                                }
                            }
                            "checkbox" -> {
                                div {
                                    classes = setOf(FORM_COLUMN_SIZE_CLS)
                                    input {
                                        id = memberName; type = InputType.checkBox; name = memberName; classes = setOf("form-check-input")
                                    }
                                }
                            }
                            "date" -> {
                                div {
                                    classes = setOf(FORM_COLUMN_SIZE_CLS)
                                    input { id = memberName; type = InputType.date; name = memberName; classes = setOf("form-control") }
                                }
                            }
                            else -> {
                                throw IllegalArgumentException("Unknown control type: $controlType")
                            }
                        }
                    }

                }
                button { classes = setOf("btn", "btn-primary"); type = ButtonType.submit; text("Submit") }
                button { classes = setOf("btn", "btn-outline-secondary", "ms-3"); type = ButtonType.reset; text("Reset") }
            }
        }
    }

    private fun getEntity(entityName: String): IGMClass {
        val className = igm.classes.keys.find { it.endsWith(entityName) }
        return if (className != null) igm.getClass(className) else IGMClass(entityName)
    }

    private fun getEnumeration(enumName: String): List<String> {
        val enumerationName = igm.enumerations.keys.find { it.endsWith(enumName) }
        return if (enumerationName != null) igm.getEnumeration(enumerationName).values else listOf()
    }

    private fun existingView(viewName: String): Boolean {
        return igm.views.keys.any { it.endsWith(viewName) }
    }

}