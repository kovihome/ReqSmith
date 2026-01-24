/*
 * ReqSmith - Build application from requirements
 * Copyright (c) 2023-2026. Kovi <kovihome86@gmail.com>
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

open class Property : ElementBase(), Cloneable {

    var key: String? = null
    var listOf: Boolean = false
    var type: String? = null
    var optionality: String? = null
    var value: String? = null
    val valueList: MutableList<String> = ArrayList()
    val simpleAttributes: MutableList<Property> = ArrayList()

    object Undefined : Property()

    override fun toString(): String {
        return "Property(key=$key, listOf=$listOf, type=$type, opt=$optionality)"
    }

    public override fun clone(): Property {
        val newProperty = Property()
        newProperty.key = key
        newProperty.listOf = listOf
        newProperty.type = type
        newProperty.optionality = optionality
        newProperty.value = value
        newProperty.valueList.addAll(valueList)
        newProperty.simpleAttributes.addAll(simpleAttributes.map { it.clone() } as Collection<Property>)
        return newProperty
    }
}
