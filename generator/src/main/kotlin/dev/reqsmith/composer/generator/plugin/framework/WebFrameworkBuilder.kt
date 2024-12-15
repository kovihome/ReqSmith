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

package dev.reqsmith.composer.generator.plugin.framework

import dev.reqsmith.composer.common.plugin.PluginDef
import dev.reqsmith.composer.common.plugin.PluginType
import dev.reqsmith.composer.common.templating.Template
import dev.reqsmith.composer.generator.entities.IGMView
import dev.reqsmith.composer.generator.entities.InternalGeneratorModel
import dev.reqsmith.composer.parser.entities.Property
import dev.reqsmith.composer.parser.entities.View
import dev.reqsmith.composer.parser.enumeration.StandardTypes

open class WebFrameworkBuilder : BaseFrameworkBuilder() {

    override fun definition(): PluginDef {
        return PluginDef("framework.web", PluginType.Framework)
    }

    override fun getViewLanguage(): String = "html"

    override fun getViewFolder(): String  = "html"

    override fun buildView(viewModel: View, igm: InternalGeneratorModel, templateContext: Map<String, String>) {
        val view = igm.getView(viewModel.qid.toString())
        val layout = viewModel.definition.properties.find { it.key == "layout" }
        view.layout = layout?.let { propertyToNode(it, templateContext) }!!
    }

    private fun propertyToNode(prop: Property, templateContext: Map<String, String>): IGMView.IGMNode {
        val node = IGMView.IGMNode().apply {
            name = prop.key!!
        }
        val template = Template()
        if (prop.type == StandardTypes.propertyList.name) {
            node.attributes.addAll(prop.simpleAttributes.filter { it.type != StandardTypes.propertyList.name }.map {
                var value = it.value?.removeSurrounding("'")?.removeSurrounding("\"") ?: ""
                if (it.key == "text") {
                    value = template.translate(templateContext, value)
                }
                it.key!! to value
            })
            prop.simpleAttributes.filter{ it.type == StandardTypes.propertyList.name }
                .forEach { node.children.add(propertyToNode(it, templateContext)) }
        } else if (prop.value?.isNotBlank() == true) {
            node.text = template.translate(templateContext, prop.value!!)
        }
        return node
    }


}