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

package dev.reqsmith.composer.generator.plugin.framework

import dev.reqsmith.composer.common.exceptions.IGMGenerationException
import dev.reqsmith.composer.common.plugin.PluginDef
import dev.reqsmith.composer.common.plugin.PluginType
import dev.reqsmith.composer.common.templating.Template
import dev.reqsmith.model.igm.IGMView
import dev.reqsmith.model.igm.InternalGeneratorModel
import dev.reqsmith.model.reqm.Property
import dev.reqsmith.model.reqm.View
import dev.reqsmith.model.enumeration.StandardTypes

open class WebFrameworkBuilder : BaseFrameworkBuilder() {

    override fun definition(): PluginDef {
        return PluginDef("framework.web", PluginType.Framework)
    }

    override fun getViewLanguage(): String = "html"

    override fun getViewFolder(): String  = "html"

    override fun getArtFolder() : String = "html"

    override fun buildView(view: View, igm: InternalGeneratorModel, templateContext: MutableMap<String, String>) {
        val template = view.definition.featureRefs.find { it.qid.toString() == "Template"}
        val layout = view.definition.properties.find { it.key == "layout" }
        if (template != null && layout != null) {
            throw IGMGenerationException("Both layout and @Template cannot be defined for a view.")
        }

        if (layout != null) {
            val igmView = igm.getView(view.qid.toString())
            igmView.layout = propertyToNode(layout, templateContext)
        } else if (template != null) {
            igm.addResource("template", template.properties.find { it.key == "file" }?.value!!) // TODO: get template folder name
        }
    }

    private fun propertyToNode(prop: Property, templateContext: Map<String, String>): IGMView.IGMNode {
        val node = IGMView.IGMNode().apply {
            name = prop.key!!
        }
        val template = Template()
        if (prop.type == StandardTypes.propertyList.name) {
            // collect attributes of this node
            val attributeList = layoutElementAttributes[prop.key] ?: listOf()
            prop.simpleAttributes.forEach { a ->
                if (a.type != StandardTypes.propertyList.name && attributeList.contains(a.key)) {
                    // real attribute
                    var value = a.value?.removeSurrounding("'")?.removeSurrounding("\"") ?: ""
                    if (a.key in listOf("text", "title")) {
                        value = template.translate(templateContext, value)
                    }
                    node.attributes.add(Pair(a.key!!, value))
                } else {
                    // child node
                    if (a.type != StandardTypes.propertyList.name) {
                        // simple child
                        node.children.add(IGMView.IGMNode().apply {
                            name = a.key!!
                            if (a.value?.isNotBlank() == true) {
                                var value = a.value?.removeSurrounding("'")?.removeSurrounding("\"") ?: ""
                                if (a.key in listOf("text")) {
                                    value = template.translate(templateContext, value)
                                }
                                val defaultAttributeName = layoutElementAttributes[a.key!!]?.getOrElse(0) { "default" } ?: "default"
                                attributes.add(Pair(defaultAttributeName, value))
                            }
                        })
                    } else {
                        // compound child
                        node.children.add(propertyToNode(a, templateContext))
                    }
                }
            }
        } else if (prop.value?.isNotBlank() == true) {
            node.text = template.translate(templateContext, prop.value!!)
        }
        return node
    }

    private val layoutElementAttributes = mapOf(
        "footer" to listOf("linkGroup", "copyrightText", "facebookLink", "twitterLink", "linkedinLink", "youtubeLink", "githubLink"),
        "header" to listOf("title", "logo"),
        "linkButton" to listOf("title", "to"),
        "linkGroup" to listOf("title", "to"),
        "panel" to listOf(),
        "spacer" to listOf(),
        "text" to listOf(),
        "form" to listOf("title", "data"),
        "datatable" to listOf("title", "data", "createForm"),
//        "image" to listOf("src", "alt", "width", "height"),
//        "input" to listOf("name", "type", "placeholder", "value", "required", "readonly", "disabled", "autocomplete", "autofocus", "list", "maxlength", "minlength", "pattern", "size", "step", "min", "max"),
//        "label" to listOf("for"),
//        "link" to listOf("to", "text", "title", "target"),
    )


}