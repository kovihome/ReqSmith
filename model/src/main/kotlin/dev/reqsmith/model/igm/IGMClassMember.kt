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

package dev.reqsmith.model.igm

import dev.reqsmith.model.igm.IGMAction.IGMAnnotation

class IGMClassMember(val memberId: String) {

    lateinit var type: String
    var enumerationType: Boolean = false
    var listOf: Boolean = false
    lateinit var optionality: String
    var value: String? = null

    val annotations: MutableList<IGMAnnotation> = ArrayList()

    override fun toString(): String {
        val sb = StringBuilder("    IGMClassMember $memberId:")
        sb.append(if (listOf) "listOf $type" else type)
        value?.let { sb.append(" = $value") }
        return sb.toString()
    }
}
