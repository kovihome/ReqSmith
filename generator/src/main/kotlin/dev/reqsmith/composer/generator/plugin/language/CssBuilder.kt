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

package dev.reqsmith.composer.generator.plugin.language

import dev.reqsmith.composer.common.WholeProject
import dev.reqsmith.composer.common.plugin.Plugin
import dev.reqsmith.composer.common.plugin.PluginDef
import dev.reqsmith.composer.common.plugin.PluginType
import dev.reqsmith.model.enumeration.StandardLayoutElements
import dev.reqsmith.model.enumeration.StandardStyleElements
import dev.reqsmith.model.igm.IGMClass
import dev.reqsmith.model.igm.IGMEnumeration
import dev.reqsmith.model.igm.IGMStyle
import dev.reqsmith.model.igm.IGMView

class CssBuilder : LanguageBuilder, Plugin {
    private val cssAttributeMap = mapOf(
        "textcolor" to "color",
        "font-color" to "color"
    )
    override val extension: String = "css"
    override val language: String = "css"
    override var artPathPrefix: String = ""
    override val viewArts: MutableList<String> = ArrayList()

    override fun definition(): PluginDef {
        return PluginDef(language, PluginType.Language)
    }

    override fun createStyle(style: IGMStyle): String {
        val sb = StringBuilder()

        val cssClassName = style.id.lowercase()
        sb.append(addCssClass(cssClassName, resolveStyleAttributes(style.attributes))).append("\n")

        style.attributes.filter { it.attributes.isNotEmpty() && StandardLayoutElements.contains(it.key) }.forEach {
            sb.append(addCssClass(cssName(cssClassName, it.key), resolveStyleAttributes(it.attributes))).append("\n")
        }

        return sb.toString()
    }

    private fun resolveStyleAttributes(attributes: List<IGMStyle.IGMStyleAttribute>): List<IGMStyle.IGMStyleAttribute> {
        val rootAttributes = attributes.filter { it.attributes.isEmpty() }.toMutableList()
        attributes.filter { it.attributes.isNotEmpty() && StandardStyleElements.contains(it.key) }.forEach { se ->
            se.attributes.forEach { a ->
                rootAttributes.add(IGMStyle.IGMStyleAttribute("${se.key}-${a.key}").apply {
                    value = a.value
                })
            }
        }
        return rootAttributes
    }

    private fun cssName(cssClassName: String, key: String) = "$cssClassName-${key.lowercase()}"

    private fun addCssClass(cssClassName: String, attributes: List<IGMStyle.IGMStyleAttribute>):String {
        WholeProject.generatorData.availableStyleClasses.add(cssClassName)

        val sb = StringBuilder(".${cssClassName} {\n")
        attributes.forEach {
            sb.append(mapAttributeToCssProperty(it))
        }
        sb.append("}\n")
        return sb.toString()
    }

    private fun mapAttributeToCssProperty(attribute: IGMStyle.IGMStyleAttribute): String {
        val cssKey = if (cssAttributeMap.containsKey(attribute.key)) {
            cssAttributeMap[attribute.key]
        } else {
            attribute.key
        }
        return "    $cssKey: ${attribute.value};\n"
    }

    override fun addComment(text: String, indent: Int): String {
        return text.split("\n").joinToString("\n") { "// $it" }
    }

    override fun addClass(cls: IGMClass, indent: Int): String {
        TODO("Not yet implemented")
    }

    override fun addEnumeration(enum: IGMEnumeration, indent: Int): String {
        TODO("Not yet implemented")
    }

    override fun addImport(imported: String, indent: Int): String {
        TODO("Not yet implemented")
    }

    override fun createView(view: IGMView): String {
        TODO("Not yet implemented")
    }

    override fun typeMapper(type: String?): String {
        TODO("Not yet implemented")
    }

    override fun collectBuildScriptElement(buildScriptUpdates: Map<String, MutableList<String>>) {
        TODO("Not yet implemented")
    }
}