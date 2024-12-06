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

import dev.reqsmith.composer.common.plugin.Plugin
import dev.reqsmith.composer.common.plugin.PluginDef
import dev.reqsmith.composer.common.plugin.PluginType
import dev.reqsmith.composer.generator.entities.IGMClass
import dev.reqsmith.composer.generator.entities.IGMEnumeration
import dev.reqsmith.composer.generator.entities.IGMView
import kotlinx.html.*
import kotlinx.html.stream.createHTML

class HtmlBuilder: LanguageBuilder, Plugin {
    override val extension: String = "html"
    override val language: String = "html"

    override fun definition(): PluginDef {
        return PluginDef(language, PluginType.Language)
    }

    override fun createView(view: IGMView): String {
        // build source code in string builder
        return createHTML().html {
            body {
                unsafe {
                    raw(createNode(view.layout))
                }
            }
        }
    }

    private fun createNode(node: IGMView.IGMNode): String {
        if (node.children.isNotEmpty()) {
            return createHTML().div {
                id = node.name
                node.children.forEach {
                    unsafe { raw(createNode(it)) }
                }
            }
        } else {
            return when (node.name) {
                "image" -> createHTML(true).img(src = node.text)
                "text" -> createHTML(true).p { text(node.text) }
                else -> createHTML(true).p { text("Unknown node: ${node.name}") }
            }
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

}