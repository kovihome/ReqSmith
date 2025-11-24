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

package dev.reqsmith.model.enumeration

@Suppress("EnumEntryName")
enum class StandardStyleAttributes(val acceptedValues: List<String> = listOf()) {

    @Suppress("EnumEntryName")
    align(listOf(
        "left",
        "center",
        "right",
        "justify"
    )),
    color(StandardColors.entries.map { it.name.lowercase() }),
    face,
    image,
    margin,
    outline,
    padding,
    size,
    spacing,
    format(listOf(
        // font
        "italic", "oblique",
        "bold", "bolder", "lighter",
        "ultra-condensed", "extra-condensed", "condensed", "semi-condensed", "semi-expanded", "expanded", "extra-expanded", "ultra-expanded",
        "small-caps", "all-small-caps", "petite-caps", "all-petite-caps", "unicase", "titling-caps",
        "line-through", "underline",
        // border
        "solid", "dotted", "dashed", "double", "groove", "ridge", "inset", "outset",
        "round", "rounder", "roundest"
    ))
    ;

    companion object {
        fun contains(s: String) = StandardStyleAttributes.entries.map { it.name }.contains(s)
    }

}