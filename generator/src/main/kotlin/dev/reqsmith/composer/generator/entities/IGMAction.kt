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

import dev.reqsmith.composer.parser.enumeration.StandardTypes

open class IGMAction(val actionId: String) {

    class IGMActionStmt(val actionName: String) {
        val parameters: MutableList<IGMStmtParam> = ArrayList()
        fun withParam(param: String, ptype: String = StandardTypes.string.name) : IGMActionStmt {
            parameters.add(IGMStmtParam(ptype, param))
            return this
        }

        override fun toString(): String {
            return "$actionName ${parameters.joinToString(", ")}"
        }
    }

    class IGMActionParam(val name: String, val type: String, val listof: Boolean = false) {
        override fun toString(): String {
            return "$name: ${if (listof) "listOf" else ""} $type"
        }
    }

    class IGMStmtParam (private val type: String, val value: String) {

        fun format(): String {
            return when (type) {
                StandardTypes.string.name -> {
                   if ((value.startsWith('\'') and value.endsWith('\'')) || (value.startsWith('\"') and value.endsWith('\"')))
                       "\"${value.substring(1, value.length-1)}\""
                   else
                       value
                }
                StandardTypes.stringLiteral.name -> {
                    "\"${value.substring(1, value.length-1)}\""
                }
                else -> {
                    value
                }
            }
        }

        override fun toString(): String {
            return value
        }
    }

    val statements : MutableList<IGMActionStmt> = ArrayList()
    val parameters: MutableList<IGMActionParam> = ArrayList()
    var isMain: Boolean = false

    override fun toString(): String {
        val mainfun = if (isMain) "(main)" else ""
        val sb = StringBuilder("    IGMAction $actionId (${parameters.joinToString(",")}) $mainfun\n")
        statements.forEach { sb.append("        $it\n") }
        return sb.toString()
    }

}
