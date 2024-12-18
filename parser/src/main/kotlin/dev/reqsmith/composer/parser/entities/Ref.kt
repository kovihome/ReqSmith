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

package dev.reqsmith.composer.parser.entities

/**
 * Reference
 *
 * @param type Item type
 * @param qid ID of the item, which contains the reference
 * @param referred The ID of the reference, or null for the item record
 * @param filename Filename of the reference or the item, if the reference is null
 */
data class Ref(val type: Type, val qid: QualifiedId, val referred: QualifiedId?, val filename: String? = null) {

    /**
     * Reference type
     *
     * @property app Application item
     * @property mod Module item
     * @property act Actor item
     * @property cls Class item
     * @property ent Entity item
     * @property acn Action item
     * @property vie View item
     * @property ftr Feature item
     * @property sty Style item
     * @property src SrcRef of an item
     * @property par Parent of an item
     * @property typ Type name of a property
     */
    enum class Type {
        app, mod, act, cls, ent, acn, vie, ftr, sty, src, par, typ
    }

    override fun toString(): String {
        return "type=$type, qid=$qid, referred=$referred, filename=$filename"
    }

}
