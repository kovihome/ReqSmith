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

open class ElementBase {

    var sourceFileName : String = ""
    var col : Int = 0
    var row : Int = 0
    var errors : MutableList<String> = ArrayList()
    private var refCount : Int = 0

    fun increaseRefCount() {
        refCount += 1
    }

    fun decreaseRefCount() {
        if (refCount > 0) refCount -= 1
    }

    /**
     * Returns the element coordinates (filename, row, col)
     */
    fun coords() = "${sourceFileName}:${row}:${col}"

}
