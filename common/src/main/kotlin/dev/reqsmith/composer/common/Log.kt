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

package dev.reqsmith.composer.common

object Log {

    enum class LogLevel(val level: Int) {
        NORMAL(0),
        INFO(1),
        DEBUG(2)
    }

    var level = LogLevel.NORMAL

    private const val CTITLE: String = "\u001B[1;32m" // light green
    private const val CINFO: String = ""
    private const val CDEBUG: String = "\u001B[0;37m" // grey
    private const val CWARN: String = "\u001B[1;36m" // light blue
    private const val CERROR: String = "\u001B[1;31m" // light red
    private const val COFF: String = "\u001B[0m"

    fun title(text: String) {
        println("$CTITLE$text$COFF")
    }

    fun text(text: String) {
        println(text)
    }

    fun info(text: String) {
        if (level.level >= LogLevel.INFO.level) println("info: $text")
    }

    fun debug(text: String) {
        if (level.level >= LogLevel.DEBUG.level) println("${CDEBUG}debug: $text$COFF")
    }

    fun warning(text: String) {
        println("${CWARN}Warning: $text$COFF")
    }

    fun error(text: String) {
        println("${CERROR}Error: $text$COFF")
    }

}