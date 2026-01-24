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

import dev.reqsmith.composer.parser.ReqMParserLexer
import dev.reqsmith.composer.parser.ReqMParserParser
import org.antlr.v4.runtime.CharStream
import org.antlr.v4.runtime.CharStreams
import org.antlr.v4.runtime.CommonTokenStream

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

/**
 * Parses ReqM script from CharStream and returns the parse tree
 * @param cs CharStream containing ReqM script
 * @return Parse tree root node
 */
fun parseReqMCharStream(cs: CharStream): ReqMParserParser.ReqmContext? {
    val lexer = ReqMParserLexer(cs)
    val tokens = CommonTokenStream(lexer)
    val parser = ReqMParserParser(tokens)
    return parser.reqm()
}

/**
 * Parses ReqM script from string and returns the parse tree
 * @param reqmScript ReqM script as string
 * @return Parse tree root node
 */
fun parseReqMString(reqmScript: String): ReqMParserParser.ReqmContext? {
    val s = CharStreams.fromString(reqmScript)
    return parseReqMCharStream(s)
}

/**
 * Parses ReqM script from file and returns the parse tree
 * @param fileName Path to the ReqM script file
 * @return Parse tree root node
 */
fun parseReqMFile(fileName: String): ReqMParserParser.ReqmContext? {
    val s = CharStreams.fromFileName(fileName)
    return parseReqMCharStream(s)
}

