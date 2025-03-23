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

import dev.reqsmith.composer.common.WholeProject
import dev.reqsmith.composer.common.exceptions.IGMGenerationException
import dev.reqsmith.composer.common.plugin.PluginDef
import dev.reqsmith.composer.common.plugin.PluginType
import dev.reqsmith.composer.common.templating.Template
import dev.reqsmith.model.FEATURE_RESOURCE
import dev.reqsmith.model.FEATURE_RESOURCE_ATTRIBUTE_FILE
import dev.reqsmith.model.REQM_GENERAL_ATTRIBUTE_EVENTS
import dev.reqsmith.model.VIEW_ATTRIBUTE_LAYOUT
import dev.reqsmith.model.enumeration.StandardLayoutElements
import dev.reqsmith.model.enumeration.StandardTypes
import dev.reqsmith.model.igm.IGMView
import dev.reqsmith.model.reqm.Property
import dev.reqsmith.model.reqm.View

open class WebFrameworkBuilder : BaseFrameworkBuilder() {

    override fun definition(): PluginDef {
        return PluginDef("framework.web", PluginType.Framework)
    }

    override fun getViewLanguage(): String = "html"

    override fun getViewFolder(): String  = "html"

    override fun getArtFolder() : String = "html"

    override fun buildView(view: View, templateContext: MutableMap<String, String>) {
        val resource = view.definition.featureRefs.find { it.qid.toString() == FEATURE_RESOURCE && it.properties.any { p -> p.key == "file" }}
        val layout = view.definition.properties.find { it.key == VIEW_ATTRIBUTE_LAYOUT }
        if (resource != null && layout != null) {
            throw IGMGenerationException("Both layout and @Resource file cannot be defined for a view.")
        }

        if (layout != null) {
            val igmView = WholeProject.projectModel.igm.getView(view.qid.toString())
            igmView.layout = propertyToNode(layout, templateContext)
        } else if (resource != null) {
            WholeProject.projectModel.igm.addResource("template", resource.properties.find { it.key == FEATURE_RESOURCE_ATTRIBUTE_FILE }?.value!!) // TODO: get template folder name
        }
    }

    private fun propertyToNode(prop: Property, templateContext: Map<String, String>): IGMView.IGMNode {
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
                if (listOf(REQM_GENERAL_ATTRIBUTE_EVENTS).contains(a.key)) {
                    val eventNode = IGMView.IGMNode().apply {
                        name = REQM_GENERAL_ATTRIBUTE_EVENTS
                    }
                    a.simpleAttributes.forEach { event ->
                        var value = event.value ?: ""
                        if ((value.startsWith('\'') && value.endsWith('\'')) || (value.startsWith('"') && value.endsWith('"'))) {
                            value = value.removeSurrounding("'").removeSurrounding("\"")
                            value = template.translate(localTemplateContext, value)
                        }
                        eventNode.attributes.add(Pair(event.key!!, value))
                    }
                    node.children.add(eventNode)

                } else if (a.type != StandardTypes.propertyList.name && attributeList.contains(a.key)) {
                    // real attribute
                    val value = a.value?.removeSurrounding("'")?.removeSurrounding("\"") ?: ""
                    node.attributes.add(Pair(a.key!!, template.translate(localTemplateContext, value)))
                } else {
                    // child node
                    if (a.type != StandardTypes.propertyList.name) {
                        // simple child
                        node.children.add(IGMView.IGMNode().apply {
                            name = a.key!!
                            if (a.value?.isNotBlank() == true) {
                                var value = a.value?.removeSurrounding("'")?.removeSurrounding("\"") ?: ""
                                if (a.key in listOf("text")) {
                                    value = template.translate(localTemplateContext, value)
                                }
                                val elementAttributeList = if (StandardLayoutElements.contains(a.key!!)) StandardLayoutElements.valueOf(a.key!!).attributes else listOf()
                                val defaultAttributeName = elementAttributeList.getOrElse(0) { "default" }
                                attributes.add(Pair(defaultAttributeName, value))
                            }
                        })
                    } else {
                        // compound child
                        node.children.add(propertyToNode(a, localTemplateContext))
                    }
                }
            }
        } else if (prop.value?.isNotBlank() == true) {
            node.text = template.translate(templateContext, prop.value!!)
        }
        return node
    }

}