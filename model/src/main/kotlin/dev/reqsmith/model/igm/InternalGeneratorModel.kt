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

class InternalGeneratorModel {
    // model containers
    var rootPackage: String = ""
    val classes: MutableMap<String, IGMClass> = mutableMapOf()
    val enumerations: MutableMap<String, IGMEnumeration> = mutableMapOf()
    val views: MutableMap<String, IGMView> = mutableMapOf()
    val styles: MutableMap<String, IGMStyle> = mutableMapOf()
    private val resources: MutableList<IGMResource> = mutableListOf()

    // container functions
    fun getClass(id: String): IGMClass {
        return classes.getOrPut(id) { IGMClass(id) }
    }

    fun getEnumeration(enumId: String): IGMEnumeration {
        return enumerations.getOrPut(enumId) { IGMEnumeration(enumId) }
    }

    fun getView(viewId: String): IGMView {
        return views.getOrPut(viewId) { IGMView(viewId) }
    }

    fun getStyle(styleId: String): IGMStyle {
        return styles.getOrPut(styleId) { IGMStyle(styleId) }
    }

    fun print(): String {
        val sb = StringBuilder("package $rootPackage\n")
        classes.forEach { sb.append(it.value.print()).append("\n") }
        enumerations.forEach { sb.append(it.value.print()).append("\n") }
        views.forEach { sb.append(it.value.print()).append("\n") }
        styles.forEach { sb.append(it.value.print()).append("\n") }
        resources.forEach { sb.append(it.print()).append("\n") }
        return sb.toString()
    }

    fun addResource(resourceDestinationFolder: String, resourceFileName: String) {
        resources.add(IGMResource(resourceDestinationFolder, resourceFileName))
    }

}