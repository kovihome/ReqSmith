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

package dev.reqsmith.model.reqm

open class QualifiedId(var id: String? = null, var domain: String? = null) : ElementBase() {

    object Undefined : QualifiedId()

    override fun equals(other: Any?): Boolean {
        return if (other is QualifiedId) {
            id == other.id && domain == other.domain
        } else false
    }

    override fun toString(): String {
        return if (domain == null) { "$id" } else { "$domain.$id" }
    }

    companion object {
        fun fromString(s: String) : QualifiedId {
            val id = s.substringAfterLast('.')
            val domain = s.substringBeforeLast('.', "")
            return QualifiedId(id, domain.ifEmpty { null })
        }
    }
}
