/*
 * ReqSmith - Build application from requirements
 * Copyright (c) 2023-2024. Kovi <kovihome86@gmail.com>
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

package dev.reqsmith.composer.repository.api.entities

import dev.reqsmith.composer.parser.entities.Ref

class RepositoryIndex {

    enum class RecordType {
        content, dependency
    }

    data class IndexRecord(var recType: RecordType, var itemType: Ref.Type, var name: String, var filename: String?) {
        // content/dependency flag (con, dep)
        // item type (app, act, cls, ent)
        // name
        // filename
        override fun toString(): String {
            return "$recType:$itemType:$name"
        }

    }

    // folder contents (applications, actors, classes, entities)
    // folder dependecies (external srcRefs, types)
    val index : MutableList<IndexRecord> = ArrayList()

}
