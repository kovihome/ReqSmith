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

package dev.reqsmith.composer.parser

import org.antlr.v4.runtime.CharStreams
import org.junit.jupiter.api.Test
import java.nio.file.Paths

class ReqMParserLexerTest {

    @Test fun testConstructor() {
        val cwd = Paths.get("").toAbsolutePath().toString()
        println("cwd = $cwd")
        val s = CharStreams.fromFileName("src/test/resources/basics.reqm")
        val lexer = ReqMParserLexer(s)
        assert(true)
    }
}