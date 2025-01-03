/*
 * ReqSmith - Build application from requirements
 * Copyright (c) 2023-2025. Kovi <kovihome86@gmail.com>
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

class References {
    val items: MutableList<Ref> = ArrayList()
    val sourceRefs: MutableList<Ref> = ArrayList()
//    val types: MutableList<TypeRef> = ArrayList()
    val types: MutableList<Ref> = ArrayList()

    fun findItem(type: Ref.Type, name: String): Boolean {
        return items.any {
            it.type == type && name == (if (it.qid.domain != null) it.qid.domain + "." + it.qid.id else it.qid.id)
        }
//        for (item in items) {
//            val s = if (item.qid.domain != null) item.qid.domain + "." + item.qid.id else item.qid.id
//            if (item.type == type && name == s) {
//                return true
//            }
//        }
//        return false
    }

    override fun toString(): String {
        val sb = StringBuilder()
        sb.append("items:\n")
        for (item in items) {
            sb.append("- ").append(item.type).append(" ")
            if (item.qid.domain != null) {
                sb.append(item.qid.domain).append(".")
            }
            sb.append(item.qid.id).append("\n")
        }
        sb.append("source references:\n")
        for (sr in sourceRefs) {
            sb.append("- ").append(sr.type).append(" ")
            if (sr.qid.domain != null) {
                sb.append(sr.qid.domain).append(".")
            }
            sb.append(sr.qid.id).append("\n")
        }
        sb.append("types:\n")
        for (t in types) {
            sb.append("- ").append(t.qid.id)
//            if (StandardTypes.has(t)) {
//                sb.append(" (std)")
//            } else if (findItem(Ref.Type.cls, t) || findItem(Ref.Type.ent, t)) {
//                sb.append(" (local)")
//            }
            sb.append("\n")
        }
        return sb.toString()
    }


}
