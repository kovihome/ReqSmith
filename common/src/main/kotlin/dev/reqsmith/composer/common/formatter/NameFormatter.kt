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

package dev.reqsmith.composer.common.formatter

import kotlin.text.removeSurrounding

/**
 * General name formatter
 */
object NameFormatter {

    /**
     * Convert an identifier to displayable name
     * @param id The identifier
     * @return Displayable name
     */
    fun toDisplayText(id: String): String {
        return id.replace(Regex("(?<=.)([A-Z])"), " $1").replaceFirstChar { it.uppercase() }
    }

    fun deliterateText(s: String): String = s.removeSurrounding("'").removeSurrounding("\"")
}