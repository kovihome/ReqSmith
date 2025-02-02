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

package dev.reqsmith.composer.generator.plugin.template

import dev.reqsmith.composer.common.plugin.PluginDef
import dev.reqsmith.composer.common.plugin.PluginType

class ThymeleafTemplateBuilder: HtmlTemplateBuilder() {
    override fun definition() = PluginDef("template.html.thymeleaf", PluginType.Template)

    override fun htmlNamespace() = Pair("th", "http://www.thymeleaf.org")

    override fun htmlAttribute(tagName: String, attributeName: String, value: String): Pair<String, String> {
        return when (tagName) {
            "form" -> when (attributeName) {
                "form_object" -> Pair("th:object", "\${${value}}")
                else -> Pair(attributeName, value)
            }
            "input" -> when (attributeName) {
                "form_member" -> Pair("th:field", "*{${value}}")
                "value" -> Pair("th:value", "*{${value}}")
                else -> Pair(attributeName, value)
            }
            "select" -> when (attributeName) {
                "form_member" -> Pair("th:field", "*{${value}}")
                else -> Pair(attributeName, value)
            }
            "tr" -> when (attributeName) {
                "loop_var" -> Pair("th:each", "$value:\${${value}s}")
                "if_empty" -> Pair("th:if", "\${#lists.isEmpty(${value}s)}")
                "if_not_empty" -> Pair("th:if", "\${not #lists.isEmpty(${value}s)}")
                else -> Pair(attributeName, value)
            }
            "td" -> when (attributeName) {
                "loop_member" -> Pair("th:text", "\${$value}")
                else -> Pair(attributeName, value)
            }
            "a" -> when (attributeName) {
                "href" -> {
                    val url = value.substringBefore('?')
                    val pathId = value.substringAfter('?').substringBefore('=')
                    val pathVar = value.substringAfter('=')
                    Pair("th:href", "@{$url($pathId=\${$pathVar})}")
                }
                else -> Pair(attributeName, value)
            }
            else -> Pair(attributeName, value)
        }
    }

}