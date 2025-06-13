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

package dev.reqsmith.composer.parser

import org.antlr.v4.runtime.CharStreams
import org.antlr.v4.runtime.CommonTokenStream
import org.junit.jupiter.api.Test
import dev.reqsmith.composer.common.exceptions.ReqMParsingException
import dev.reqsmith.model.reqm.Definition
import dev.reqsmith.model.reqm.QualifiedId
import dev.reqsmith.model.reqm.ReqMSource
import org.junit.jupiter.api.assertDoesNotThrow

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

    @Test
    fun testLoadApplication() {
        val s = loadReqMFile("src/test/resources/application.reqm")
        assert(s.applications.isNotEmpty())
        assert(s.applications.size == 1)

//      application dev.reqsmith.test.Sample from applications.CommandLineApplication
        val app = s.applications[0]
        assert(app.qid!!.id == "Sample")
        assert(app.qid!!.domain == "dev.reqsmith.test")
        assert(app.sourceRef != null && s.applications[0].sourceRef != QualifiedId.Undefined)
        assert(app.sourceRef!!.id == "CommandLineApplication")
        assert(app.sourceRef!!.domain == "applications")
        val p = app.definition.properties
        assert(p.isNotEmpty())

//      title: "TestApplication"
        val title = p.find { it.key == "title" }
        assert(title!!.value!!.isNotBlank())
//      description: 'Sample application for test'
        val desc = p.find { it.key == "description" }
        assert(desc!!.value!!.isNotBlank())
//      version: 1.0.0
        val ver = p.find { it.key == "version" }
        assert(ver!!.value!!.isNotBlank())
//      events {
//              applicationStart: start
//      }
        val ev = p.find { it.key == "events" }
        assert(ev!!.simpleAttributes.isNotEmpty())
        assert(ev.simpleAttributes.size == 1)
        val sa = ev.simpleAttributes[0]
        assert(sa.key == "applicationStart")
        assert(sa.value == "start")
//        options {
//            simpleOption {
//                short: s
//                type: boolean
//                description: "Simple"
//            }
//            argument {
//                type: filename
//                description: "Description for the argument"
//                multiple: true
//            }
//            command {
//                type: command
//                description: "Description for this command"
//                action: commandAction
//            }
        val op = p.find { it.key == "options" }
        assert(op!!.simpleAttributes.isNotEmpty())
        val opa = op.simpleAttributes
        assert(opa.find { it.key == "simpleOption" }!!.simpleAttributes.size == 3)
        assert(opa.find { it.key == "argument" }!!.simpleAttributes.size == 3)
        assert(opa.find { it.key == "command" }!!.simpleAttributes.size == 3)
    }

    @Test fun testLoadEntities() {
        val s = loadReqMFile("src/test/resources/entities.reqm")
        assert(s.classes.isNotEmpty())
        assert(s.classes[0].qid?.id.equals("AtomicType"))
        assert(s.entities.isNotEmpty())
        assert(s.entities[0].qid?.id.equals("EmptyEntity"))
    }

    private fun loadReqMFile(filename: String): ReqMSource {
        val parser = ReqMParser()
        val s = ReqMSource()
        parser.parseReqMTree(filename, s)
        return s
    }

    @Test fun testLoadViews() {
        val s = loadReqMFile("src/test/resources/view.reqm")
        assert(s.views.isNotEmpty())
        assert(s.views[0].qid?.id.equals("SimplePage"))
    }

    @Test fun testLoadFeatures() {
        // load feature.reqm model
        val parser = ReqMParser()
        val s = ReqMSource()
        assertDoesNotThrow {
            parser.parseReqMTree("src/test/resources/feature.reqm", s)
        }

        // check feature item
        assert(s.features.isNotEmpty())
        val f = s.features[0]
        assert(f.qid?.id.equals("Resource"))
        assert(f.definition != Definition.Undefined)
        assert(f.definition.properties.isNotEmpty())
        val pFile = f.definition.properties.find { it.key == "file" }
        assert(pFile != null)
        assert(pFile?.value == null)
        val pType = f.definition.properties.find { it.key == "templateType" }
        assert(pType != null)
        assert(pType?.value != null && pType.value == "'st4'")
        val pGen = f.definition.properties.find { it.key == "generator" }
        assert(pGen != null)
        assert(pGen?.value != null && pGen.value == "'framework.template'")

        // check templated view item
        assert(s.views.isNotEmpty())
        val v = s.views[0]
        assert(v.qid?.id.equals("TemplatePage"))
        assert(v.definition != Definition.Undefined)
        assert(v.definition.featureRefs.isNotEmpty())
        val frTemp = v.definition.featureRefs.find { it.qid.toString() == "Resource" }
        assert(frTemp != null)
        assert(frTemp?.properties!!.isNotEmpty())
        val fpFile = frTemp.properties.find { it.key == "file" }
        assert(fpFile != null)
        assert(fpFile?.value == "'templates/test.st'")
    }
}
