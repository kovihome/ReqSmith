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

import dev.reqsmith.composer.common.plugin.Plugin
import dev.reqsmith.composer.common.plugin.PluginDef
import dev.reqsmith.composer.common.plugin.PluginType
import dev.reqsmith.model.igm.IGMClass
import dev.reqsmith.model.igm.IGMEnumeration
import dev.reqsmith.model.igm.IGMView
import dev.reqsmith.model.igm.InternalGeneratorModel
import kotlinx.html.*
import kotlinx.html.stream.createHTML

open class HtmlBuilder : LanguageBuilder, Plugin {
    override val extension: String = "html"
    override val language: String = "html"
    override var artPathPrefix: String = ""
    override val viewArts: MutableList<String> = ArrayList()

    override lateinit var igm: InternalGeneratorModel

    override fun definition(): PluginDef {
        return PluginDef(language, PluginType.Language)
    }

    override fun createView(view: IGMView): String {
        // build source code in string builder
        return createHTML().html {
            head {
                meta { charset = Charsets.UTF_8.name() }
                meta {
                    name = "viewport"
                    content = "width=device-width, initial-scale=1.0"
                }
                title { text(view.id) }
                view.imports.filter { it.endsWith(".css") }.forEach {
                    link {
                        href = it
                        rel = "stylesheet"
                    }
                }
                view.imports.filter { it.endsWith(".js") }.forEach {
                    script {
                        src = it
                    }
                }
            }
            body {
                unsafe {
                    raw(createNode(view.layout))
                }
            }
        }
    }

    fun createNode(node: IGMView.IGMNode): String {
        return when (node.name) {
            "header" -> createHeader(node)
            "footer" -> createFooter(node)
            "panel" -> createPanel(node)
            "text" -> createText(node)
            "linkGroup" -> createLinkGroup(node)
            "linkButton" -> createLinkButton(node)
            "spacer" -> createSpacer(node)
            "form" -> createForm(node)
            else -> {
                if (node.children.isNotEmpty()) {
                    createHTML().div {
                        id = node.name
                        node.children.forEach {
                            unsafe { raw(createNode(it)) }
                        }
                    }
                } else {
                    createHTML(true).p { text("Unknown node: ${node.name}") }
                }
            }
        }
    }

    open fun createForm(node: IGMView.IGMNode): String {
        return createHTML(true).div {

        }
    }

    open fun createPanel(node: IGMView.IGMNode): String {
        return createHTML(true).div {

        }
    }

    open fun createFooter(node: IGMView.IGMNode): String {
        return createHTML(true).div {

        }
    }

    open fun createHeader(node: IGMView.IGMNode): String {
        return createHTML(true).div { 
            
        }
    }

    open fun createText(node: IGMView.IGMNode): String {
        return createHTML(true).div {

        }
    }

    open fun createLinkGroup(node: IGMView.IGMNode): String {
        return createHTML(true).div {

        }
    }

    open fun createLinkButton(node: IGMView.IGMNode): String {
        return createHTML(true).div {

        }
    }

    open fun createSpacer(node: IGMView.IGMNode): String {
        return createHTML(true).div {

        }
    }

    override fun addClass(cls: IGMClass, indent: Int): String {
        throw NotImplementedError()
    }

    override fun addEnumeration(enum: IGMEnumeration, indent: Int): String {
        throw NotImplementedError()
    }

    override fun addComment(text: String, indent: Int): String {
        throw NotImplementedError()
    }

    override fun addImport(imported: String, indent: Int): String {
        TODO("Not yet implemented")
    }

    override fun typeMapper(type: String?): String {
        TODO("Not yet implemented")
    }

    override fun collectBuildScriptElement(buildScriptUpdates: Map<String, MutableList<String>>) {
        TODO("Not yet implemented")
    }

}