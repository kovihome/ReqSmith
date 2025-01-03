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
//                        style = "width: 50px; height: 50px;"
                        viewArts.add(attr["logo"] ?: "")
                    }
                }
                if (attr.contains("title")) {
                    h1 {
                        text(attr["title"] ?: "(Title comes here)")
//                        classes = setOf("m-0", "fs-4")
                    }
                }
            }
        }
    }

    override fun collectBuildScriptElement(buildScriptUpdates: Map<String, MutableList<String>>) {
        TODO("Not yet implemented")
    }

    override fun createPanel(node: IGMView.IGMNode): String {
        val attr = node.attributes.toMap()
        return createHTML(true).div {
            classes = setOf("container", "mt-4")
            if (attr.contains("text")) {
                p {
                    // write multiline text
                    val texts = (attr["text"]?:"").split("\\n")
                    text(texts[0])
                    if (texts.size > 1) {
                        texts.subList(1, texts.size).forEach {
                            br()
                            text(it)
                        }
                    }
                    // --------------------
                }
            }

        }
    }

    override fun createFooter(node: IGMView.IGMNode): String {
//        val attr = node.attributes.toMap()
        val footerSpecAttributes = listOf("copyright", "social", "contact")
        return createHTML(true).footer {
            classes = setOf("bg-dark", "text-white", "py-4", "mt-5", "fixed-bottom")
            div {
                classes = setOf("container")
                div {
                    classes = setOf("row")
                    // footer link groups
                    node.children.filter { !footerSpecAttributes.contains(it.name) }.forEach { linkGroup ->
                        val groupTitle = linkGroup.name.replace("_", " ")
                        div {
                            classes = setOf("col-md-6", "mb-3", "mb-md-0")
                            h5 { text(groupTitle) }
                            ul {
                                classes = setOf("list-unstyled")
                                linkGroup.attributes.forEach { link ->
                                    val linkText = link.first.replace("_", " ")
                                    var linkValue = link.second
                                    if (linkValue.isNullOrBlank()) {
                                        // is link name an another view?
                                        // TODO: check view name for link.first
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
                    // footer info (copyright)
                    val copyrightList = node.attributes.filter { it.first == "copyright" }
                    if (copyrightList.isNotEmpty()) {
                        div {
                            classes = setOf("col-md-6", "text-md-end")
                            copyrightList.forEach {
                                p {
                                    classes = setOf("mb-0")
                                    text(it.second)
                                }
                            }

                            // social media icons
                            val socialMediaLinkGroup = node.children.filter { it.name == "social" }
                            if (socialMediaLinkGroup.isNotEmpty()) {
                                div {
                                    classes = setOf("mt-3")
                                    socialMediaLinkGroup[0].attributes.forEach { link ->
                                        val media = link.first
                                        val linkValue = if (link.second.isNullOrBlank()) "#" else link.second
                                        val mediaClass = "bi-${media}"
                                        a {
                                            href = linkValue
                                            classes = setOf("pe-3")
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

}