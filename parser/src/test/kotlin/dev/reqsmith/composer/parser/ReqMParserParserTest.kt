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
import org.antlr.v4.runtime.CommonTokenStream
import org.junit.jupiter.api.Test
import dev.reqsmith.composer.common.exceptions.ReqMParsingException
import dev.reqsmith.composer.parser.entities.ReqMSource

class ReqMParserParserTest {

    private fun parseReqMFile(fileName: String): ReqMParserParser.ReqmContext? {
        val s = CharStreams.fromFileName(fileName)
        val lexer = ReqMParserLexer(s)
        val tokens = CommonTokenStream(lexer)
        val parser = ReqMParserParser(tokens)
        return parser.reqm()
    }

    @Test fun testParseBasics() {
        val tree = parseReqMFile("src/test/resources/basics.reqm")
        assert(true)
    }

    @Test fun testParseEntities() {
        val tree = parseReqMFile("src/test/resources/entities.reqm")
        assert(true)
    }

    @Test fun testLoadBasics() {
        val parser = ReqMParser()
        val s = ReqMSource()
        try {
            parser.parseReqMTree("src/test/resources/basics.reqm", s)
        } catch (e: ReqMParsingException) {
            e.parserErrors.forEach { println("basics.reqm - $it") }
            throw e
        }
        assert(s.applications.isNotEmpty())
        assert(s.applications[0].qid?.id.equals("Sample"))
        assert(s.applications[0].qid?.domain.equals("dev.reqsmith.test"))
        assert(s.actors.isNotEmpty())
        assert(s.actors[0].qid?.id.equals("User"))
    }

    @Test fun testLoadEntities() {
        val parser = ReqMParser()
        val s = ReqMSource()
        parser.parseReqMTree("src/test/resources/entities.reqm", s)
        assert(s.classes.isNotEmpty())
        assert(s.classes[0].qid?.id.equals("AtomicType"))
        assert(s.entities.isNotEmpty())
        assert(s.entities[0].qid?.id.equals("EmptyEntity"))
    }

    @Test fun testLoadViews() {
        val parser = ReqMParser()
        val s = ReqMSource()
        parser.parseReqMTree("src/test/resources/view.reqm", s)
        assert(s.views.isNotEmpty())
        assert(s.views[0].qid?.id.equals("Terminal"))
    }

}
