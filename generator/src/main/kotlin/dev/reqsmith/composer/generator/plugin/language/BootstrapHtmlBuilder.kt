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
import dev.reqsmith.model.igm.IGMView
import kotlinx.html.*
import kotlinx.html.stream.createHTML
import kotlin.math.max

class BootstrapHtmlBuilder: HtmlBuilder() {
    override fun definition(): PluginDef {
        return PluginDef("html.bootstrap", PluginType.Language)
    }

    override fun createHeader(node: IGMView.IGMNode): String {
        val attr = node.attributes.toMap()
        return createHTML(true).header {
            classes = setOf("bg-light", "py-3")
            div {
                classes = setOf("container", "d-flex", "align-items-center")
                if (attr.contains("logo")) {
                    img {
                        classes = setOf("me-3")
                        src = "$artPathPrefix/${attr["logo"]}"
                        alt = "logo"
//                        style = "width: 128px; height: 128px;"
                        style = "height: 128px;"
                        viewArts.add(attr["logo"] ?: "")
                    }
                }
                if (attr.contains("title")) {
                    h1 {
                        classes = setOf("m-0", "fs-4")
                        text(attr["title"] ?: "(Title comes here)")
                    }
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

    override fun createLinkGroup(node: IGMView.IGMNode): String {
        val groupTitle = node.attributes.find { it.first == "title" }?.second ?: "Links"
        val colSize = node.attributes.find { it.first == "colSize" }?.second ?: "6"
        return createHTML(true).div {
            classes = setOf("col-md-$colSize", "mb-3", "mb-md-0")
            h5 { text(groupTitle) }
            ul {
                classes = setOf("list-unstyled")
                node.attributes.filter { it.first == "to" }.forEach { link ->
                    val linkText = link.second.replace("_", " ")
                    var linkValue = link.second
                    if (linkValue.isBlank()) {
                        // is link name an another view?
                        // TODO v0.3: check view name for link.first
                        // else it is unknown
                        linkValue = "#"
                    }
                    li {
                        a {
                            href = linkValue
                            classes = setOf("text-white", "text-decoration-none")
                            text(linkText)
                        }
                    }
                }
            }
        }
    }

    override fun createLinkButton(node: IGMView.IGMNode): String {
        return createHTML(true).a {
            classes = setOf("btn", "btn-primary")
            role = "button"
            val link = node.attributes.find { it.first == "to" }?.second
            href = if (link != null) "$link.html" else "#" // TODO: a .html-t nem itt kell hozzáadni
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
            div {
                classes = setOf("container")
                div {
                    classes = setOf("row")

                    // footer link groups
                    val linkGroups = node.children.filter { it.name == "linkGroup" }
                    val ngroups = linkGroups.size
                    val colSize = max(6 / ngroups, 1)
                    linkGroups.forEach { linkGroup ->
                        linkGroup.attributes.add(Pair("colSize", colSize.toString()))
                        unsafe { raw(createLinkGroup(linkGroup)) }
                    }

                    //
                    div {
                        classes = setOf("col-md-${12-colSize*ngroups}", "text-md-end")

                        // footer info (copyright)
                        val copyrightList = node.attributes.filter { it.first == "copyrightText" }
                        copyrightList.forEach {
                            p {
                                classes = setOf("mb-0")
                                val copyText = if (attr.containsKey(it.second)) attr.getOrDefault(it.second, it.second) else it.second
                                text(copyText)
                            }
                        }

                        // social media icons
                        val socialMediaLinks = node.attributes.filter { it.first.endsWith("Link") }
                        socialMediaLinks.let { links ->
                            div {
                                classes = setOf("mt-3")
                                listOf("facebook", "twitter", "linkedin", "youtube", "github").forEach { media ->
                                    val linkValue = links.find { it.first == "${media}Link" }?.second ?: "#"
                                    val mediaClass = "bi-${media}"
                                    a {
                                        href = linkValue
                                        classes = setOf("me-3")
                                        i {
                                            classes = setOf("bi", mediaClass)
                                            style = "font-size:1.5rem;color:white;"
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

}