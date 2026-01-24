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

package dev.reqsmith.model.igm

class IGMClass(val id: String) {
    var interfaceType = false
    var parent: String = ""
    val parentClasses: MutableList<String> = mutableListOf()
    var mainClass: Boolean = false
    val actions : MutableList<IGMAction> = mutableListOf()
    val members: MutableList<IGMClassMember> = mutableListOf()
    val annotations: MutableList<IGMAction.IGMAnnotation> = mutableListOf()
    val imports: MutableList<String> = mutableListOf()
    val ctorParams: MutableList<IGMAction.IGMActionParam> = mutableListOf()

    fun getAction(actionId: String): IGMAction {
        return actions.find { it.actionId == actionId } ?: IGMAction(actionId).also { actions.add(it) }
    }

    fun getMember(memberId: String): IGMClassMember {
        return members.find { it.memberId == memberId } ?: IGMClassMember(memberId).also { members.add(it) }
    }

    fun print(tabsize: Int = 0): String {
        val tab = " ".repeat(tabsize)
        val sb = StringBuilder("${tab}IGMClass $id")
        if (parent.isNotBlank()) sb.append(" is $parent")
        if (mainClass) sb.append(" (main)")
        sb.append("\n")
        members.forEach { sb.append(it.print(tabsize+4)) }
        actions.forEach { sb.append(it.print(tabsize+4)) }
        return sb.toString()
    }

    fun addImport(import: String): String {
        if (!imports.contains(import)) {
            imports.add(import)
        }
        return import.substringAfterLast('.')
    }

}
