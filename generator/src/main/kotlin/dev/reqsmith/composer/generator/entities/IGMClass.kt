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

package dev.reqsmith.composer.generator.entities

class IGMClass(val id: String) {
    var parent: String = ""
    var mainClass: Boolean = false
    val actions : MutableMap<String, IGMAction> = HashMap()
    val members: MutableMap<String, IGMClassMember> = HashMap()
    val annotations: MutableList<String> = ArrayList()
    val imports: MutableList<String> = ArrayList()

    fun getAction(actionId: String): IGMAction {
        return actions.getOrPut(actionId) { IGMAction(actionId) }
    }

    fun getMember(memberId: String): IGMClassMember {
        return members.getOrPut(memberId) { IGMClassMember(memberId) }
    }

    override fun toString(): String {
        val sb = StringBuilder("IGMClass $id")
        if (parent.isNotBlank()) sb.append("is $parent")
        if (mainClass) sb.append(" (main)")
        sb.append("\n")
        members.forEach { sb.append("${it.value}\n") }
        actions.forEach { sb.append("${it.value}\n") }
        return sb.toString()
    }

    fun addImport(import: String) {
        if (!imports.contains(import)) {
            imports.add(import)
        }
    }

}
