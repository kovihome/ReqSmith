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

package dev.reqsmith.model.reqm

import dev.reqsmith.model.FEATURE_STYLE
import dev.reqsmith.model.FEATURE_STYLE_ATTRIBUTE_STYLE
import dev.reqsmith.model.FEATURE_TEMPLATE
import dev.reqsmith.model.enumeration.StandardTypes

open class FeatureRef : ElementBase() {
    var qid : QualifiedId = QualifiedId()
    val properties : MutableList<Property> = ArrayList()

    object Undefined : FeatureRef()

    override fun toString(): String {
        return "FeatureRef(qid=$qid)"
    }

    companion object {
        fun style(styleName: String) = FeatureRef().apply {
            qid = QualifiedId(FEATURE_STYLE)
            properties.add(Property().apply {
                key = FEATURE_STYLE_ATTRIBUTE_STYLE
                value = styleName
                type = StandardTypes.stringLiteral.name
            })
        }
        fun template(templateName: String) = FeatureRef().apply {
            qid = QualifiedId(FEATURE_TEMPLATE)
            properties.add(Property().apply {
                key = "templateView"
                value = templateName
                type = StandardTypes.stringLiteral.name
            })
        }
    }
}
