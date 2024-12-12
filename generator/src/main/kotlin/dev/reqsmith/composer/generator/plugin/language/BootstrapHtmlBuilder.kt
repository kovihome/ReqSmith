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

package dev.reqsmith.composer.generator.plugin.language

import dev.reqsmith.composer.common.plugin.PluginDef
import dev.reqsmith.composer.common.plugin.PluginType
import dev.reqsmith.composer.generator.entities.IGMView
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
                        src = attr["logo"]!!
                        alt = "logo"
                        style = "width: 50px; height: 50px;"
                    }
                }
                if (attr.contains("title")) {
                    h1 {
                        text(attr["title"]!!)
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
                    text(attr["text"]!!)
                }
            }

        }
    }

    override fun createFooter(node: IGMView.IGMNode): String {
        val attr = node.attributes.toMap()
        return createHTML(true).footer {
            classes = setOf("bg-dark", "text-white", "py-4", "mt-5")
            div {
                classes = setOf("container")
                div {
                    classes = setOf("row")
                    // footer links
                    div {
                        classes = setOf("col-md-6", "mb-3", "mb-md-0")
                        h5 { text("Quick Links") }
                        ul {
                            classes = setOf("list-unstyled")
                            listOf("About", "Privacy Policy", "Contact").forEach {
                                li {
                                    a {
                                        href = "#"
                                        classes = setOf("text-white", "text-decoration-none")
                                        text(it)
                                    }
                                }
                            }
                        }
                    }
                    // footer info
                    div {
                        classes = setOf("col-md-6", "text-md-end")
                        listOf("&copy; 2024 ReqSmith Ltd. All rights reserved.", "Powered by Bootstrap.").forEach {
                            p {
                                classes = setOf("mb-0")
                                text(it)
                            }
                        }
                    }
                }
            }
        }
    }

}