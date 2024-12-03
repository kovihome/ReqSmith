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

package dev.reqsmith.composer.parser

import org.antlr.v4.runtime.CharStreams
import org.antlr.v4.runtime.CommonTokenStream
import org.antlr.v4.runtime.RecognitionException
import org.antlr.v4.runtime.Token
import dev.reqsmith.composer.common.Log
import dev.reqsmith.composer.common.exceptions.ReqMParsingException
import dev.reqsmith.composer.parser.entities.*
import dev.reqsmith.composer.parser.enumeration.StandardTypes
import java.io.File
import java.io.IOException

class ReqMParser {

    fun parseFolder(folder: String, reqmSource: ReqMSource) : Boolean {
        val files = File(folder).listFiles()?.filter { it.extension == "reqm" }
        if (files.isNullOrEmpty()) return false

        var hasErrors = false
        files.forEach { file ->
            try {
                Log.info("Parsing file ${file.absolutePath}")
                parseReqMTree(file.absolutePath, reqmSource)
            } catch (e: IOException) {
                Log.error("Parsing $file was failed - ${e.localizedMessage}")
                // TODO: handle IO exception
                hasErrors = true
            } catch (e: RecognitionException) {
                Log.error("Parsing $file was failed - ${e.localizedMessage}")
                // TODO: handle parse errors
                hasErrors = true
            } catch (e: ReqMParsingException) {
                Log.error(e.message!!)
                e.parserErrors.forEach { Log.error("$file - $it") }
                hasErrors = true
            }
        }
        return !hasErrors
    }

    fun parseReqMTree(filePath: String, reqmSource: ReqMSource) {
        val tree = loadReqMTree(filePath)

        for (stat in tree.topStat()) {
            parseApplication(stat.application(), reqmSource)
            parseModule(stat.module(), reqmSource)
            parseActor(stat.actor(), reqmSource)
            parseClass(stat.class_(), reqmSource)
            parseEntity(stat.entity(), reqmSource)
            parseAction(stat.action(), reqmSource)
            parseView(stat.view(), reqmSource)
            // TODO: parseFeature(stat.feature(), reqmSource)
            // TODO: parseStyle(stat.style(), reqmSource)
        }
    }

    private fun parseView(view: ReqMParserParser.ViewContext?, reqmSource: ReqMSource) {
        view?.let {
            val vw = View()
            saveSourceInfo(vw, view.start)
            vw.qid = parseQualifiedId(view.qualifiedId())
            vw.sourceRef = parseSourceDef(view.sourceRef())
            vw.definition = parseViewDefinitionClosure(view.viewDefinitionClosure())
            reqmSource.views.add(vw)
        }
    }

    private fun parseViewDefinitionClosure(viewDefinitionClosure: ReqMParserParser.ViewDefinitionClosureContext?): Definition {
        return if (viewDefinitionClosure != null && viewDefinitionClosure.viewProperty().isNotEmpty()) {
            val definition = Definition()
            viewDefinitionClosure.viewProperty().forEach { 
                definition.properties.add(parseViewProperty(it))
            }
            definition
        } else {
            Definition.Undefined
        }
    }

    private fun parseViewProperty(viewProperty: ReqMParserParser.ViewPropertyContext): Property {
        if (viewProperty.simpleTypelessProperty() != null) {
            return parseSimpleTypelessProperty(viewProperty.simpleTypelessProperty())
        } else if (viewProperty.compundViewProperty() != null) {
            val property = Property()
            viewProperty.compundViewProperty().viewProperty().forEach {
                property.simpleAttributes.add(parseViewProperty(it))
            }
            property.key = viewProperty.compundViewProperty().qualifiedId().text
            property.type = StandardTypes.propertyList.name
            return property
        } else {
            return Property.Undefined
        }
    }

    private fun parseTypelessDefinitionClosure(typelessDefinitionClosure: ReqMParserParser.TypelessDefinitionClosureContext?): Definition {
        return if (typelessDefinitionClosure != null && typelessDefinitionClosure.typelessProperty().isNotEmpty()) {
            val definition = Definition()
            saveSourceInfo(definition, typelessDefinitionClosure.start)
            typelessDefinitionClosure.typelessProperty().forEach { 
                definition.properties.add(parseTypelessProperty(it))
            }
            definition
        } else {
            Definition.Undefined
        }
    }

    private fun parseSimpleTypelessProperty(property: ReqMParserParser.SimpleTypelessPropertyContext): Property {
        val prop = Property()
        saveSourceInfo(prop, property.start)
        prop.key = property.qualifiedId().text
        prop.value = property.propertyValue()?.text
//        if (property.propertyValue() != null) {
//            when {
//                property.propertyValue().StringLiteral() != null -> {
//                    prop.type = StandardTypes.stringLiteral.name
//                    prop.value = property.propertyValue().StringLiteral().text
//                }
//                property.propertyValue().SemanticVersionNumber() != null -> {
//                    prop.type = StandardTypes.versionNumber.name
//                    prop.value = property.propertyValue().SemanticVersionNumber().text
//                }
//                property.propertyValue().INT() != null -> {
//                    prop.type = "numeric"
//                    prop.value = property.propertyValue().text
//                }
//                property.propertyValue().propertyType() != null -> {
//                    prop.type = property.propertyValue().propertyType().ID().text
//                    prop.listOf = property.propertyValue().propertyType().KWLISTOF() != null
//                }
//                property.propertyValue().qualifiedId() != null -> {
//                    prop.type = "valiable"
//                    prop.value = property.propertyValue().text
//                }
//            }
//        }
        return prop
    }

    private fun parseTypelessProperty(property: ReqMParserParser.TypelessPropertyContext?): Property {
        if (property != null) {
            if (property.simpleTypelessProperty() != null) {
                return parseSimpleTypelessProperty(property.simpleTypelessProperty())
            } else if (property.compoundTypelessProperty() != null) {
                val prop = Property()
                saveSourceInfo(prop, property.start)
                prop.type = StandardTypes.propertyList.name
                prop.key = property.compoundTypelessProperty().qualifiedId().text
                property.compoundTypelessProperty().simpleTypelessProperty().forEach {
                    val p = parseSimpleTypelessProperty(it)
                    prop.simpleAttributes.add(p)
                }
                return prop
            } else {
                return Property.Undefined
            }
        } else {
            return Property.Undefined
        }
    }

    private fun parseAction(action: ReqMParserParser.ActionContext?, reqmSource: ReqMSource) {
        action?.let {
            val acn = Action()
            saveSourceInfo(acn, action.start)
            acn.qid = parseQualifiedId(action.qualifiedId())
//            acn.sourceRef = parseSourceDef(action.sourceRef())
            acn.definition = parseActionDefinitionClosure(action.actionDefinitionClosure())
            reqmSource.actions.add(acn)
        }
    }

    private fun parseActionDefinitionClosure(actionDefinitionClosure: ReqMParserParser.ActionDefinitionClosureContext?): ActionDefinition {
        return if (actionDefinitionClosure != null && actionDefinitionClosure.actionCall().isNotEmpty()) {
            val definition = ActionDefinition()
            saveSourceInfo(definition, actionDefinitionClosure.start)
            actionDefinitionClosure.actionCall().forEach { call ->
                definition.actionCalls.add(parseActionCall(call))
            }
            definition
        } else {
            ActionDefinition.Undefined
        }
    }

    private fun parseActionCall(call: ReqMParserParser.ActionCallContext?): ActionCall {
        return if (call != null) {
            val ac = ActionCall()
            ac.actionName = call.ID().text
            call.paramList().paramValue().forEach {
                val prop = Property()
                when {
                    it.StringLiteral() != null -> {
                        prop.type = "stringLiteral"
                        prop.value = it.StringLiteral().text
                    }
                    it.SemanticVersionNumber() != null -> {
                        prop.type = "versionNumber"
                        prop.value = it.SemanticVersionNumber().text
                    }
                    it.INT() != null -> {
                        prop.type = "numeric"
                        prop.value = it.text
                    }
                    it.qualifiedId() != null -> {
                        prop.type = "variable"
                        prop.value = it.text
                    }
                    else -> {
                        prop.type = "unknown"
                        prop.value = it.text
                    }
                }
                ac.parameters.add(prop)
            }
            ac
        } else {
            ActionCall.Undefined
        }
    }

    private fun parseModule(module: ReqMParserParser.ModuleContext?, reqmSource: ReqMSource) {
        module?.let {
            val mod = Modul()
            saveSourceInfo(mod, module.start)
            mod.qid = parseQualifiedId(module.qualifiedId())
            mod.sourceRef = parseSourceDef(module.sourceRef())
            mod.definition = parseApplicationDefinitionClosure(module.applicationDefinitionClosure())
            reqmSource.modules.add(mod)
        }
    }

    private fun loadReqMTree(filePath: String): ReqMParserParser.ReqmContext {
        val s = CharStreams.fromFileName(filePath)

        // setup lexer and tokenize input
        val lexer = ReqMParserLexer(s)
        lexer.removeErrorListeners()
        val lexerErrorListener = ReqMLexerErrorListener()
        lexer.addErrorListener(lexerErrorListener)
        val tokens = CommonTokenStream(lexer)

        // setup parser and parse source
        val parser = ReqMParserParser(tokens)
        parser.removeErrorListeners()
        val reqmErrorListener = ReqMErrorListener()
        parser.addErrorListener(reqmErrorListener)
        val tree =  parser.reqm()

        // handle lexer and parser errors
        if (lexerErrorListener.errors.isNotEmpty() || reqmErrorListener.errors.isNotEmpty()) {
            throw ReqMParsingException("${reqmErrorListener.errors.size + lexerErrorListener.errors.size} error(s) has occured").apply {
                parserErrors.addAll(lexerErrorListener.errors)
                parserErrors.addAll(reqmErrorListener.errors)
            }
        }

        return tree
    }

    private fun parseActor(actor: ReqMParserParser.ActorContext?, reqmSource: ReqMSource) {
        actor?.let {
            val act = Actor()
            saveSourceInfo(act, actor.start)
            act.qid = parseQualifiedId(actor.qualifiedId())
            act.sourceRef = parseSourceDef(actor.sourceRef())
            act.definition = parseTypelessDefinitionClosure(actor.typelessDefinitionClosure())
            reqmSource.actors.add(act)
        }

    }

    private fun parseClass(classs: ReqMParserParser.ClassContext?, reqmSource: ReqMSource) {
        classs?.let {
            val cls = Classs()
            saveSourceInfo(cls, classs.start)
            cls.qid = parseQualifiedId(classs.qualifiedId())
            cls.atomic = classs.KWATOMIC() != null
            cls.enumeration = classs.KWENUMERATION() != null
            if (cls.enumeration) {
                cls.definition = parseEnumDefinition(classs.enumDefinitionClosure())
                cls.sourceRef = QualifiedId.Undefined
            } else if (cls.atomic) {
                cls.sourceRef = QualifiedId.Undefined
            } else {
                cls.parent = parseParent(classs.parent())
                cls.sourceRef = parseSourceDef(classs.sourceRef())
                cls.definition = parseSimpleDefinitionClosure(classs.simpleDefinitionClosure())
            }
            reqmSource.classes.add(cls)
        }

    }

    private fun parseParent(parent: ReqMParserParser.ParentContext?): QualifiedId {
        return if (parent != null) {
            parent.qualifiedId()?.let {
                parseQualifiedId(it)
            } ?: QualifiedId.Undefined
        } else {
            QualifiedId.Undefined
        }
    }

    private fun parseEnumDefinition(enumDefinitionClosure: ReqMParserParser.EnumDefinitionClosureContext?): Definition {
        val definition = Definition()
        enumDefinitionClosure?.let {
            saveSourceInfo(definition, enumDefinitionClosure.start)
            val enumList = enumDefinitionClosure.enumList().ID()
            enumList.forEach { value ->
                val property = Property()
                property.key = value.text
                property.type = "enum"
                definition.properties.add(property)
            }
        }
        return definition
    }

    private fun parseEntity(entity: ReqMParserParser.EntityContext?, reqmSource: ReqMSource) {
        entity?.let {
            val ent = Entity()
            saveSourceInfo(ent, entity.start)
            ent.qid = parseQualifiedId(entity.qualifiedId())
            ent.parent = parseParent(entity.parent())
            ent.sourceRef = parseSourceDef(entity.sourceRef())
            ent.definition = parseDefinitionClosure(entity.definitionClosure())
            reqmSource.entities.add(ent)
        }

    }

    private fun parseApplication(application: ReqMParserParser.ApplicationContext?, reqmSource: ReqMSource) {
        application?.let {
            val app = Application()
            saveSourceInfo(app, application.start)
            app.qid = parseQualifiedId(application.qualifiedId())
            app.sourceRef = parseSourceDef(application.sourceRef())
//            app.definition = parseTypelessDefinitionClosure(application.typelessDefinitionClosure())
            app.definition = parseApplicationDefinitionClosure(application.applicationDefinitionClosure())
            reqmSource.applications.add(app)
        }

    }

    private fun parseApplicationDefinitionClosure(applicationDefinitionClosure: ReqMParserParser.ApplicationDefinitionClosureContext?): Definition {
        return if (applicationDefinitionClosure != null && applicationDefinitionClosure.applicationProperty().isNotEmpty()) {
            val definition = Definition()
            saveSourceInfo(definition, applicationDefinitionClosure.start)
            for (property in applicationDefinitionClosure.applicationProperty()) {
                definition.properties.add(parseApplicationProperty(property))
            }
            definition
        } else {
            Definition.Undefined
        }
    }

    private fun parseApplicationProperty(property: ReqMParserParser.ApplicationPropertyContext?): Property {
        if (property != null) {
            if (property.simpleApplicationProperty() != null) {
                return parseSimpleApplicationProperty(property.simpleApplicationProperty())
            } else if (property.compoundTypelessProperty() != null) {
                val prop = Property()
                saveSourceInfo(prop, property.start)
                prop.type = StandardTypes.propertyList.name
                prop.key = property.compoundTypelessProperty().qualifiedId().text
                property.compoundTypelessProperty().simpleTypelessProperty().forEach {
                    val p = parseSimpleTypelessProperty(it)
                    prop.simpleAttributes.add(p)
                }
                return prop
            } else {
                return Property.Undefined
            }
        } else {
            return Property.Undefined
        }
    }

    private fun parseSimpleApplicationProperty(property: ReqMParserParser.SimpleApplicationPropertyContext): Property {
        val prop = Property()
        saveSourceInfo(prop, property.start)
        prop.key = property.qualifiedId().text
        if (property.applicationPropertyValue() != null) {
            when {
                property.applicationPropertyValue().StringLiteral() != null -> {
                    prop.type = StandardTypes.stringLiteral.name
                    prop.value = property.applicationPropertyValue().StringLiteral().text
                }

                property.applicationPropertyValue().SemanticVersionNumber() != null -> {
                    prop.type = StandardTypes.versionNumber.name
                    prop.value = property.applicationPropertyValue().SemanticVersionNumber().text
                }

                property.applicationPropertyValue().INT() != null -> {
                    prop.type = StandardTypes.integer.name
                    prop.value = property.applicationPropertyValue().INT().text
                }

                property.applicationPropertyValue().qualifiedId() != null -> {
                    prop.type = StandardTypes.string.name
                    prop.value = property.applicationPropertyValue().qualifiedId().text
                }
            }
        }
        return prop
    }

    private fun saveSourceInfo(element: ElementBase, start: Token?) {
        start?.let {
            element.row = start.line
            element.col = start.charPositionInLine
            element.sourceFileName = start.inputStream.sourceName
        }
    }

    private fun parseSimpleDefinitionClosure(simpleDefinitionClosure: ReqMParserParser.SimpleDefinitionClosureContext?): Definition {
        return if (simpleDefinitionClosure != null && simpleDefinitionClosure.property().isNotEmpty()) {
            val definition = Definition()
            saveSourceInfo(definition, simpleDefinitionClosure.start)
            simpleDefinitionClosure.property().forEach { property ->
                definition.properties.add(parseProperty(property))
            }
            definition
        } else {
            Definition.Undefined
        }
    }

    private fun parseDefinitionClosure(definitionClosure: ReqMParserParser.DefinitionClosureContext?): Definition {
        return if (definitionClosure != null && definitionClosure.property().isNotEmpty()) {
            val definition = Definition()
            saveSourceInfo(definition, definitionClosure.start)
            for (property in definitionClosure.property()) {
                definition.properties.add(parseProperty(property))
            }
            definition
        } else {
            Definition.Undefined
        }
    }

    private fun parseProperty(property: ReqMParserParser.PropertyContext?): Property {
        return if (property != null) {
            val prop = Property()
            saveSourceInfo(prop, property.start)
            prop.key = property.qualifiedId().text
            if (property.propertyValue() != null) {
                property.optionality()?.let {
                    prop.optionality = it.text
                }
                when {
                    property.propertyValue().StringLiteral() != null -> {
                        prop.type = StandardTypes.stringLiteral.name
                        prop.value = property.propertyValue().StringLiteral().text
                    }
                    property.propertyValue().SemanticVersionNumber() != null -> {
                        prop.type = StandardTypes.versionNumber.name
                        prop.value = property.propertyValue().SemanticVersionNumber().text
                    }
                    property.propertyValue().INT() != null -> {
                        prop.type = StandardTypes.integer.name
                        prop.value = property.propertyValue().INT().text
                    }
                    property.propertyValue().propertyType() != null -> {
                        prop.type = property.propertyValue().propertyType().ID().text
                        prop.listOf = property.propertyValue().propertyType().KWLISTOF() != null
                    }
                    property.propertyValue().qualifiedId() != null -> {
                        prop.type = StandardTypes.variable.name
                        prop.value = property.propertyValue().qualifiedId().text
                    }
                }
            } else if (property.propertyClosure() != null) {
                val definition = parsePropertyDefinition(property.propertyClosure())
                prop.type = definition.type
                prop.listOf = definition.listOf
                prop.optionality = definition.optionality
            }
            prop
        } else {
            Property.Undefined
        }
    }

    private fun parsePropertyDefinition(propertyClosure: ReqMParserParser.PropertyClosureContext): Property {
        return if (propertyClosure.propertyAttribute().isNotEmpty()) {
            val prop = Property()
            for (property in propertyClosure.propertyAttribute()) {
                val attrib = parsePropertyAttribute(property)
                when {
                    attrib.key in listOf("optional", "mandatory") -> {
                        prop.optionality = attrib.key
                    }
                    attrib.key.equals("type") -> {
                        prop.type = attrib.type
                        prop.listOf = attrib.listOf
                    }
                    attrib.type == "variable" -> {
                        prop.type = "valueList"
                        attrib.key?.let { prop.valueList.add(it) }
                    }
                    else -> {
                        TODO("unknown attribute key: ${attrib.key}")
                    }
                }
            }
            prop
        } else {
            Property.Undefined
        }
    }

    private fun parsePropertyAttribute(property: ReqMParserParser.PropertyAttributeContext?): Property {
        val prop = Property()
        property?.let {
            when {
                property.optionality() != null -> {
                    prop.key = property.optionality().text
                }
                property.simpleId() != null -> {
                    prop.key = property.simpleId().ID().text
                    if (property.propertyValue() != null && property.propertyValue().propertyType() != null) {
                        prop.type = property.propertyValue().propertyType().ID().text
                        prop.listOf = property.propertyValue().propertyType().KWLISTOF() != null
                    }
                }
                property.qualifiedId() != null -> {
                    prop.key = property.qualifiedId().text
                    prop.type = "variable"
                }
            }
        }
        return prop
    }

    private fun parseSourceDef(sourceRef: ReqMParserParser.SourceRefContext?): QualifiedId {
        return if (sourceRef != null) {
            sourceRef.qualifiedId()?.let {
                parseQualifiedId(it)
            } ?: QualifiedId.Undefined
        } else {
            QualifiedId.Undefined
        }
    }

    private fun parseQualifiedId(qualifiedId: ReqMParserParser.QualifiedIdContext?): QualifiedId {
        return if (qualifiedId != null && qualifiedId.simpleId().isNotEmpty()) {
            val ids = qualifiedId.simpleId().size
            val qid = QualifiedId(qualifiedId.simpleId()[ids-1].ID().text)
            if (ids > 1) {
                qid.domain = qualifiedId.simpleId().subList(0, ids-1).joinToString(".") {  it.ID().text }
            }
            qid
        } else QualifiedId.Undefined
    }

}