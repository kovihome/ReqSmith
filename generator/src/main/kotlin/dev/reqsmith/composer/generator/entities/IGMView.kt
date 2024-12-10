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

class IGMView(val id: String) {
    var layout = IGMNode()
    val imports: MutableList<String> = ArrayList()

    class IGMNode {
        var name: String = ""
        var text : String = ""
        var link: String = "#"
//        var tag: String = ""
        val attributes: MutableList<Pair<String, String>> = ArrayList()
//        var classes: MutableList<String> = ArrayList()
        val children : MutableList<IGMNode> = ArrayList()

        fun addNode(name: String, tag: String): IGMNode {
            val childNode = IGMNode().apply {
                this@IGMNode.name = name
//                this@IGMNode.tag = tag
            }
            children.add(childNode)
            return childNode
        }

        override fun toString(): String {
            return "$name { ${children.joinToString(", ") { it.name }} }"
        }
    }

    override fun toString(): String {
        val sb = StringBuilder("IGMView $id\n")
        sb.append("    $layout")
        return sb.toString()
    }

}

