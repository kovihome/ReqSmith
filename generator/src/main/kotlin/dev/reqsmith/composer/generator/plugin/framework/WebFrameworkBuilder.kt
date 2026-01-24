/*
 * ReqSmith - Build application from requirements
 * Copyright (c) 2024-2026. Kovi <kovihome86@gmail.com>
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

import dev.reqsmith.composer.common.WholeProject
import dev.reqsmith.composer.common.exceptions.IGMGenerationException
import dev.reqsmith.composer.common.formatter.NameFormatter
import dev.reqsmith.composer.common.plugin.PluginDef
import dev.reqsmith.composer.common.plugin.PluginType
import dev.reqsmith.composer.common.templating.Template
import dev.reqsmith.model.FEATURE_RESOURCE
import dev.reqsmith.model.FEATURE_RESOURCE_ATTRIBUTE_FILE
import dev.reqsmith.model.FEATURE_STYLE
import dev.reqsmith.model.REQM_GENERAL_ATTRIBUTE_EVENTS
import dev.reqsmith.model.VIEW_ATTRIBUTE_LAYOUT
import dev.reqsmith.model.VIEW_LAYOUT_ELEMENT_STYLE
import dev.reqsmith.model.enumeration.StandardLayoutElements
import dev.reqsmith.model.enumeration.StandardTypes
import dev.reqsmith.model.igm.IGMStyle
import dev.reqsmith.model.igm.IGMView
import dev.reqsmith.model.reqm.Property
import dev.reqsmith.model.reqm.View

open class WebFrameworkBuilder : BaseFrameworkBuilder() {

    override fun definition(): PluginDef {
        return PluginDef("framework.web", PluginType.Framework)
    }

    override fun getViewLanguage() = "html"

    override fun getStyleLanguage() = "css"

    override fun getViewFolder()  = "html"

    override fun getArtFolder() = "html"

    override fun buildView(view: View, templateContext: MutableMap<String, String>) {
        val resource = view.definition.featureRefs.find { it.qid.toString() == FEATURE_RESOURCE && it.properties.any { p -> p.key == "file" }}
        val styleRef = view.definition.featureRefs.find { it.qid.toString() == FEATURE_STYLE}?.properties?.get(0)?.value
        val layout = view.definition.properties.find { it.key == VIEW_ATTRIBUTE_LAYOUT }
        if (resource != null && layout != null) {
            throw IGMGenerationException("Both layout and @Resource file cannot be defined for a view.")
        }

        if (layout != null) {
            val igmView = WholeProject.projectModel.igm.getView(view.qid.toString())
            igmView.styleRef = styleRef
            igmView.layout = propertyToNode(layout, igmView, templateContext)
        } else if (resource != null) {
            WholeProject.projectModel.igm.addResource("template", resource.properties.find { it.key == FEATURE_RESOURCE_ATTRIBUTE_FILE }?.value!!) // TODO: get template folder name
        }
    }

    private fun propertyToNode(prop: Property, igmView: IGMView, templateContext: Map<String, String>): IGMView.IGMNode {
        val node = IGMView.IGMNode().apply {
            name = prop.key!!
        }
        val template = Template()
        val localTemplateContext = HashMap(prop.simpleAttributes.filter { it.type != StandardTypes.propertyList.name && it.value != null }
            .associate { it.key!! to it.value!! }).apply { putAll(templateContext) }

        if (prop.type == StandardTypes.propertyList.name) {
            // collect attributes of this node
            val attributeList = if (StandardLayoutElements.contains(prop.key!!)) StandardLayoutElements.valueOf(prop.key!!).attributes else listOf()
            prop.simpleAttributes.forEach { a ->
                when {
                    a.key == REQM_GENERAL_ATTRIBUTE_EVENTS -> {
                        val eventNode = IGMView.IGMNode().apply {
                            name = REQM_GENERAL_ATTRIBUTE_EVENTS
                        }
                        a.simpleAttributes.forEach { event ->
                            var value = event.value ?: ""
                            if ((value.startsWith('\'') && value.endsWith('\'')) || (value.startsWith('"') && value.endsWith('"'))) {
                                value = template.translate(localTemplateContext, NameFormatter.deliterateText(value))
                            }
                            eventNode.attributes.add(Pair(event.key!!, value))
                        }
                        node.children.add(eventNode)

                    }
                    a.key == VIEW_LAYOUT_ELEMENT_STYLE -> {
                        if (a.simpleAttributes.isNotEmpty()) {
                            // inline style definition
                            val inlineStyleId = igmView.id
                            val inlineStyle = WholeProject.projectModel.igm.getStyle(inlineStyleId).apply { inline = true }
                            val leNode = IGMStyle.IGMStyleAttribute(node.name)
                            inlineStyle.attributes.add(leNode)
                            a.simpleAttributes.forEach { attr ->
                                leNode.attributes.add(createStyleAttribute(attr))
                            }
                            node.styleRef = inlineStyleId

                        } else if (!a.value.isNullOrBlank()) {
                            // style reference
                            node.styleRef = a.value
                        }

                    }
                    a.type != StandardTypes.propertyList.name && attributeList.contains(a.key) -> {
                        // real attribute
                        val value = NameFormatter.deliterateText(a.value ?: "")
                        node.attributes.add(Pair(a.key!!, template.translate(localTemplateContext, value)))

                    }
                    else -> {
                        // child node
                        if (a.type != StandardTypes.propertyList.name) {
                            // simple child
                            node.children.add(IGMView.IGMNode().apply {
                                name = a.key!!
                                if (a.value?.isNotBlank() == true) {
                                    val literal = a.value?.startsWith("\"") == true || a.value?.startsWith("'") == true
                                    var value = NameFormatter.deliterateText(a.value ?: "")
                                    if (literal) {
                                        value = template.translate(localTemplateContext, value)
                                    }
                                    val elementAttributeList = if (StandardLayoutElements.contains(a.key!!)) StandardLayoutElements.valueOf(a.key!!).attributes else listOf()
                                    val defaultAttributeName = elementAttributeList.getOrElse(0) { "default" }
                                    attributes.add(Pair(defaultAttributeName, value))
                                }
                            })
                        } else {
                            // compound child
                            node.children.add(propertyToNode(a, igmView, localTemplateContext))
                        }
                    }
                }
            }
        } else if (prop.value?.isNotBlank() == true) {
            node.text = template.translate(templateContext, prop.value!!)
        }

        // search for node level style reference
        if (!igmView.styleRef.isNullOrBlank()) {
            WholeProject.projectModel.igm.styles.keys.find { it == igmView.styleRef }?.let { styleName ->
                WholeProject.projectModel.igm.styles[styleName]?.let { style ->
                    if (style.attributes.any { it.key == node.name }) {
                        node.styleRef = "${igmView.styleRef}_${node.name}"
                    }
                }
            }
        }
        return node
    }

    private fun createStyleAttribute(sprop: Property): IGMStyle.IGMStyleAttribute {
        return IGMStyle.IGMStyleAttribute(sprop.key!!).apply {
            when {
                sprop.simpleAttributes.isNotEmpty() -> {
                    sprop.simpleAttributes.forEach {
                        attributes.add(createStyleAttribute(it))
                    }
                }
                sprop.valueList.isNotEmpty() -> {
                    valueList.addAll(sprop.valueList)
                }
                !sprop.value.isNullOrBlank() -> {
                    value = sprop.value
                }
            }
        }
    }


}