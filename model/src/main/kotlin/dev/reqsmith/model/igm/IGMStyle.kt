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

package dev.reqsmith.model.igm

class IGMStyle(val id: String) {

    var inline: Boolean = false
    var url: String? = null
    val attributes: MutableList<IGMStyleAttribute> = mutableListOf()

    class IGMStyleAttribute(val key: String) {
        var value: String? = null
        val attributes: MutableList<IGMStyleAttribute> = mutableListOf()

        fun print(tabsize: Int): String {
            val tab = " ".repeat(tabsize)
            val sb = StringBuilder()
            if (attributes.isNotEmpty()) {
                sb.append("${tab}IGMStyleAttribute $key\n")
                attributes.forEach {
                    sb.append(it.print(tabsize+4))
                }
            } else {
                sb.append("${tab}$key: $value\n")
            }

            return sb.toString()
        }
    }

    fun print(tabsize: Int = 0): String {
        val tab = " ".repeat(tabsize)
        val sb = StringBuilder("${tab}IGMStyle $id")
        if (inline) {
            sb.append(" (inline)")
        }
        sb.append("\n")
        attributes.forEach { sb.append(it.print(tabsize+4)) }
        return sb.toString()
    }

}
