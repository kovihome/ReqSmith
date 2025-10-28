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

package dev.reqsmith.cssplugin.resource

import com.helger.css.decl.CSSSelectorSimpleMember
import com.helger.css.decl.CSSStyleRule
import com.helger.css.reader.CSSReader
import com.helger.css.reader.CSSReaderSettings
import dev.reqsmith.composer.common.Log
import dev.reqsmith.composer.common.WholeProject
import dev.reqsmith.composer.common.plugin.Plugin
import dev.reqsmith.composer.common.plugin.PluginDef
import dev.reqsmith.composer.common.plugin.PluginType
import dev.reqsmith.composer.common.plugin.resource.ResourceParser
import dev.reqsmith.model.enumeration.StandardTypes
import dev.reqsmith.model.reqm.Property
import dev.reqsmith.model.reqm.QualifiedId
import dev.reqsmith.model.reqm.Style
import java.io.File

val cssPropMap = mapOf(
    "font-family" to "font-face",
    "font-style" to "text-format",
    "font-weight" to "text-format",
    "font-stretch" to "text-format",
    "font-variant-caps" to "text-format",
    "text-decoration" to "text-format",
    "color" to "text-color",
    "border-style" to "border-format",
    "border-radius" to "border-format",
    "border-width" to "border-size",
    "margin" to "border-margin",
    "padding" to "border-padding",
    "background-size" to "background-image",
    "background-position" to "background-image",
    "background-repeat" to "background-image"
)

class CssResourceParser : ResourceParser, Plugin {

    override fun definition(): PluginDef {
        return PluginDef("css", PluginType.Resource)
    }

    override fun parseResourceFile(qid: QualifiedId?, resourceType: String, resourceFileName: String): List<Style> {
        Log.info("Parsing css resource file $resourceFileName")
        val cssSettings = CSSReaderSettings().setFallbackCharset(Charsets.UTF_8).setUseSourceLocation(false)
        val css = CSSReader.readFromFile(File(resourceFileName), cssSettings)
        val additionalStyleElements = mutableListOf<Style>()
        css?.allRules?.forEach { rule ->
            if (rule is CSSStyleRule) {
                val groups = mutableListOf<Property>()
                rule.allDeclarations.forEach { declaration ->
                    val propertyName = declaration.property
                    val styleValue = declaration.expression.asCSSString
                    val styleElement = mapCssProperty2StyleElement(propertyName)
                    val se = styleElement.split("-", limit = 2)
                    val styleGroup = if (se.size == 2) se[0] else ""
                    val styleAttribute = if (se.size == 2) se[1] else se[0]

                    val groupProp = groups.find { it.key == styleGroup } ?: Property().apply {
                        key = styleGroup
                        type = StandardTypes.propertyList.name
                        groups.add(this)
                    }
                    groupProp.simpleAttributes.add(Property().apply {
                        key = styleAttribute
                        type = StandardTypes.string.name
                        value = styleValue
                    })
                }
                rule.allSelectors.forEach { selector ->
                    selector.allMembers.forEach { member ->
                        if (member is CSSSelectorSimpleMember) {
                            val s = member.value
                            if (s.contains('.')) {
                                val styleClass = s.split(".")[1]
                                var styleElem = WholeProject.projectModel.source.styles.find { it.qid.toString().lowercase() == styleClass }
                                if (styleElem == null) {
                                    styleElem = Style().apply {
                                                this.qid = QualifiedId(styleClass)
                                                Log.debug("Generate new style element $styleClass")
                                            }
                                    additionalStyleElements.add(styleElem)
                                }
                                styleElem.definition.properties.addAll(groups)
                            }
                        } else {
                            Log.debug("Unknown selector type ${member.javaClass}: $member")
                        }
                    }
                }
            } else {
                Log.debug("Unknown css rule type ${rule.javaClass}: $rule")
            }
        }
        return additionalStyleElements
    }

    private fun mapCssProperty2StyleElement(propertyName: String): String {
        return cssPropMap.getOrDefault(propertyName, propertyName)
    }

}