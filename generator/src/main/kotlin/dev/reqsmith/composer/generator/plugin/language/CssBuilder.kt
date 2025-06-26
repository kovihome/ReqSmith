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
    override val extension: String = "css"
    override val language: String = "css"
    override var artPathPrefix: String = ""
    override val viewArts: MutableList<String> = ArrayList()

    override fun definition(): PluginDef {
        return PluginDef(language, PluginType.Language)
    }

    private val cssAttributeMap = mapOf(
        "text-color" to "color",
        "color" to "color"
    )

    override fun createStyle(style: IGMStyle): String {
        val sb = StringBuilder()
        val cssClassName = style.id.lowercase()

        sb.append(addCssClass(cssClassName, resolveStyleAttributes(style.attributes.filter { it.attributes.isEmpty() }))).append("\n")

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
                    valueList.addAll(a.valueList)
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
        attributes.forEach { a -> mapAttributeToCssProperty(a).forEach { sb.append(it) }}
        sb.append("}\n")
        return sb.toString()
    }

    private fun mapAttributeToCssProperty(attribute: IGMStyle.IGMStyleAttribute): List<String> {
        return when (attribute.key) {
            // font attributes
            "font-face" -> { listOf(cssa("font-family", attribute.value!!)) }
            "font-size" -> { listOf(cssa(attribute.key, cssSize(attribute.value))) }
            // text attributes
            "text-color" -> { listOf(cssa("color", cssColor(attribute.value))) }
            "text-format" -> {
                val cssAttributes = mutableListOf<String>()
                val attributeList = attribute.valueList
                if (attribute.value != null) attributeList.add(attribute.value!!)
                addCssAttrToList(attributeList, listOf("italic", "oblique"), "font-style", cssAttributes)
                addCssAttrToList(attributeList, listOf("bold", "bolder", "lighter"), "font-weight", cssAttributes)
                addCssAttrToList(attributeList, listOf("ultra-condensed", "extra-condensed", "condensed", "semi-condensed", "semi-expanded", "expanded", "extra-expanded", "ultra-expanded"), "font-stretch", cssAttributes)
                addCssAttrToList(attributeList, listOf("small-caps", "all-small-caps", "petite-caps", "all-petite-caps", "unicase", "titling-caps"), "font-variant-caps", cssAttributes)
                addCssAttrToList(attributeList, listOf("line-through", "underline"), "text-decoration", cssAttributes)
                cssAttributes
            }
            "text-align" -> { listOf("text-align", attribute.value!!) } // left | right | center | justify
            // border attributes
            "border-color" -> { listOf(cssa(attribute.key, cssColor(attribute.value))) }
            "border-format" -> {
                val cssAttributes = mutableListOf<String>()
                val attributeList = attribute.valueList
                if (attribute.value != null) attributeList.add(attribute.value!!)
                addCssAttrToList(attributeList, listOf("solid", "dotted", "dashed", "double", "groove", "ridge", "inset", "outset"), "border-style", cssAttributes)
                addCssAttrToList(attributeList, listOf("round", "rounder", "roundest"), "border-radius", cssAttributes)
                cssAttributes
            }
            "border-size" -> { listOf(cssa("border-width", cssSize(attribute.value))) }
            "border-margin" -> { listOf(cssa("margin", cssSize(attribute.value))) }
            "border-padding" -> { listOf(cssa("padding", cssSize(attribute.value))) }
            // background attributes
            "background-color" -> { listOf(cssa(attribute.key, cssColor(attribute.value))) }
            "background-image" -> {
                listOf(
                    cssa(attribute.key, cssUrl(attribute.value)),
                    cssa("background-size", "cover"), // contain, <lenght>, <percent>
                    cssa("background-position", "center"), // left top, left center, left bottom, right top, right center, right bottom, center top, center center, center bottom, <xpercent> <ypercent>, <xpos> <ypos>
                    cssa("background-repeat", "no-repeat") // repeat, repeat-x, repeat-y, space, round
                )
            }
            else -> { listOf(cssa(cssAttributeMap.getOrDefault(attribute.key, attribute.key), attribute.value!!)) }
        }
    }

    private fun addCssAttrToList(attributeList: List<String>, availableAttributes: List<String>, cssAttrName: String, cssAttributes: MutableList<String>) {
        val a = attributeList.intersect(availableAttributes)
        if (a.isNotEmpty()) cssAttributes.add(cssa(cssAttrName, a.joinToString(" ")))
    }

    private fun cssa(key: String, value: String) = "    $key: $value;\n" // TODO: indentation

    private fun cssUrl(url: String?): String {
        return "url($url)"
    }

    private fun cssSize(sz: String?): String {
        return if (sz?.toIntOrNull() != null) "${sz}px" else sz!!
    }

    private fun cssColor(value: String?): String {
        return value ?: "black"
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