/*
 * ReqSmith - Build application from requirements
 * Copyright (c) 2025-2026. Kovi <kovihome86@gmail.com>
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

package dev.reqsmith.model.enumeration

enum class StandardStyleElements(val attributes: List<StandardStyleAttributes> = listOf()) {

    font(listOf(
        StandardStyleAttributes.face,
        StandardStyleAttributes.size,
    )),
    text(listOf(
        StandardStyleAttributes.color,
        StandardStyleAttributes.format,
        StandardStyleAttributes.align
    )),
    border(listOf(
        StandardStyleAttributes.color,
        StandardStyleAttributes.format,
        StandardStyleAttributes.margin,
        StandardStyleAttributes.padding,
        StandardStyleAttributes.size
    )),
    background(listOf(
        StandardStyleAttributes.color,
        StandardStyleAttributes.image
    ))
    ;

    fun hasAttribute(a: String) = StandardStyleAttributes.contains(a) && attributes.contains(StandardStyleAttributes.valueOf(a))

    companion object {
        fun contains(s: String) = StandardStyleElements.entries.map { it.name }.contains(s)
    }

}