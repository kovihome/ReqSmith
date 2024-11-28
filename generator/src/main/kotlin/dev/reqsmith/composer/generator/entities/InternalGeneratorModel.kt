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

class InternalGeneratorModel(val rootPackage: String) {
    // optional parameters
    private var fileHeader = ""

    // model containers
    val classes: MutableMap<String, IGMClass> = HashMap()
    val enumerations: MutableMap<String, IGMEnumeration> = HashMap()
//    val events: MutableMap<String, IGMEvent> = HashMap()

    // builder for optional parameters
    fun fileHeader(headerText: String): InternalGeneratorModel {
        this.fileHeader = headerText
        return this
    }

    // container functions
    fun getClass(id: String): IGMClass {
        return classes.getOrPut(id) { IGMClass(id) }
    }

    fun getEnumeration(enumId: String): IGMEnumeration {
        return enumerations.getOrPut(enumId) { IGMEnumeration(enumId) }
    }

//    fun getEvent(eventId: String): IGMEvent {
//        return events.getOrPut(eventId) { IGMEvent(eventId) }
//    }

    override fun toString(): String {
        val sb = StringBuilder("=============== InternalGeneratorModel ===============\npackage $rootPackage\n")
        classes.forEach { sb.append("${it.value}\n") }
        enumerations.forEach { sb.append("${it.value}\n") }
//        events.forEach { sb.append("${it.value}\n") }
        sb.append("======================================================\n")
        return sb.toString()
    }

}