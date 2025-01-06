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

class IGMView(val id: String) {
    var layout = IGMNode()
    val imports: MutableList<String> = ArrayList()

    class IGMNode {
        var name: String = ""
        var text : String = ""
        val attributes: MutableList<Pair<String, String>> = ArrayList()
        val children : MutableList<IGMNode> = ArrayList()

        override fun toString(): String {
            val sb = StringBuilder()
            sb.append("IGMNode $name\n")
            attributes.forEach { sb.append("    ${it.first}: ${it.second}\n") }
            children.forEach {
                sb.append(it.toString().split("\n").joinToString("\n") { "    $it" })
            }
            return sb.toString()
        }
    }

    override fun toString(): String {
        val sb = StringBuilder("IGMView $id\n")
        sb.append(layout.toString().split("\n").joinToString("\n") { "    $it" })
        return sb.toString()
    }

}

