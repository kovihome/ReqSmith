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

package dev.reqsmith.composer.parser

import com.github.benmanes.caffeine.cache.Cache
import com.github.benmanes.caffeine.cache.Caffeine
import dev.reqsmith.composer.common.Log
import dev.reqsmith.composer.common.exceptions.ReqMParsingException
import dev.reqsmith.composer.parser.ReqMParserParser.ReqmContext
import dev.reqsmith.model.REQM_GENERAL_ATTRIBUTE_FEATURE_REFERENCE
import dev.reqsmith.model.enumeration.StandardTypes
import dev.reqsmith.model.reqm.*
import org.antlr.v4.runtime.CharStreams
import org.antlr.v4.runtime.CommonTokenStream
import org.antlr.v4.runtime.RecognitionException
import org.antlr.v4.runtime.Token
import java.io.File
import java.io.IOException
import java.util.concurrent.TimeUnit

val parseCache: Cache<String, ReqmContext> = Caffeine.newBuilder().expireAfterWrite(1, TimeUnit.DAYS)
    .expireAfterAccess(1, TimeUnit.DAYS).initialCapacity(10).maximumSize(1000).build()

class ReqMParser {

    /**
     * Parse all ReqM source files in a folder
     * @param folder The folder containing the ReqM source files
     * @param reqmSource The ReqM model holding the file content
     * @return true - when tha parsing was succeeded; false - errors occurred during parsing
     */
    fun parseFolder(folder: String, reqmSource: ReqMSource) : Boolean {
        val files = File(folder).listFiles()?.filter { it.extension == "reqm" }
        if (files.isNullOrEmpty()) return false

        var hasErrors = false
        files.forEach { file ->
            Log.info("Parsing file ${file.absolutePath}")
            hasErrors = hasErrors || !parseReqMTree(file.absolutePath, reqmSource)
        }
        return !hasErrors
    }

    /**
     * Parse a ReqM source file
     * @param filePath The path of the ReqM file
     * @param reqmSource The ReqM model holding the file content
     * @return true - when tha parsing was succeeded; false - errors occurred during parsing
     */
    fun parseReqMTree(filePath: String, reqmSource: ReqMSource): Boolean {

        val tree = try {
            parseCache[filePath, { _ ->
                loadReqMTree(filePath)
            }]
        } catch (e: IOException) {
            Log.error("Parsing $filePath was failed - ${e.localizedMessage}")
            return false
        } catch (e: RecognitionException) {
            Log.error("Parsing $filePath was failed - ${e.localizedMessage}")
            return false
        } catch (e: ReqMParsingException) {
            Log.error(e.message!!)
            e.parserErrors.forEach { Log.error("$filePath - $it") }
            return false
        }

        for (stat in tree.topStat()) {
            parseApplication(stat.application(), reqmSource)
            parseModule(stat.module(), reqmSource)
            parseActor(stat.actor(), reqmSource)
            parseClass(stat.class_(), reqmSource)
            parseEntity(stat.entity(), reqmSource)
            parseAction(stat.action(), reqmSource)
            parseView(stat.view(), reqmSource)
            parseFeature(stat.feature(), reqmSource)
            parseStyle(stat.style(), reqmSource)
        }

        return true
    }

    private fun parseStyle(style: ReqMParserParser.StyleContext?, reqmSource: ReqMSource) {
        style?.let {
            val st = Style().apply {
                saveSourceInfo(this, style.start)
                qid = parseQualifiedId(style.qualifiedId())
//                parent = parseParent(style.parent())
                sourceRef = parseSourceRef(style.sourceRef())
                definition = parseStyleDefinitionClosure(style.styleDefinitionClosure())
            }
            reqmSource.styles.add(st)
        }

    }

    private fun parseStyleDefinitionClosure(styleDefinitionClosure: ReqMParserParser.StyleDefinitionClosureContext) =
        Definition().apply {
            styleDefinitionClosure.featureRef().forEach {
                featureRefs.add(parseFeatureRef(it))
            }
            styleDefinitionClosure.styleProperty().forEach {
                properties.add(parseStyleProperty(it))
            }
        }

    private fun parseStyleProperty(styleProperty: ReqMParserParser.StylePropertyContext): Property {
        return when {
            styleProperty.simpleTypelessProperty() != null -> {
                parseSimpleTypelessProperty(styleProperty.simpleTypelessProperty())
            }
            styleProperty.compoundTypelessProperty() != null -> {
                parseCompoundTypelessProperty(styleProperty.compoundTypelessProperty())
            }
            styleProperty.layoutStyleProperty() != null -> {
                Property().apply {
                    key = styleProperty.layoutStyleProperty().qualifiedId().text
                    type = StandardTypes.propertyList.name
                    styleProperty.layoutStyleProperty().compoundTypelessProperty().forEach {
                        simpleAttributes.add(parseCompoundTypelessProperty(it))
                    }
                }
            }
            else -> {
                Property.Undefined
            }
        }
    }

    private fun parseFeature(feature: ReqMParserParser.FeatureContext?, reqmSource: ReqMSource) {
        feature?.let {
            val ft = Feature().apply {
                saveSourceInfo(this, feature.start)
                qid = parseQualifiedId(feature.qualifiedId())
                sourceRef = parseSourceRef(feature.sourceRef())
                definition = parseDefinitionClosure(feature.definitionClosure())
            }
            reqmSource.features.add(ft)
        }
    }

    private fun parseView(view: ReqMParserParser.ViewContext?, reqmSource: ReqMSource) {
        view?.let {
            val vw = View().apply {
                saveSourceInfo(this, view.start)
                qid = parseQualifiedId(view.qualifiedId())
                parent = parseParent(view.parent())
                sourceRef = parseSourceRef(view.sourceRef())
                definition = parseViewDefinitionClosure(view.viewDefinitionClosure())
            }
            reqmSource.views.add(vw)
        }
    }

    private fun parseViewDefinitionClosure(viewDefinitionClosure: ReqMParserParser.ViewDefinitionClosureContext?): Definition {
        return if (viewDefinitionClosure != null) {
            Definition().apply {
                viewDefinitionClosure.featureRef().forEach {
                    featureRefs.add(parseFeatureRef(it))
                }
                viewDefinitionClosure.viewProperty().forEach {
                    properties.add(parseViewProperty(it))
                }
            }
        } else {
            Definition.Undefined
        }
    }

    private fun parseViewProperty(viewProperty: ReqMParserParser.ViewPropertyContext): Property {
        return if (viewProperty.simpleTypelessProperty() != null) {
            parseSimpleTypelessProperty(viewProperty.simpleTypelessProperty())
        } else if (viewProperty.compoundViewProperty() != null) {
            Property().apply {
                key = viewProperty.compoundViewProperty().qualifiedId().text
                type = StandardTypes.propertyList.name
                viewProperty.compoundViewProperty().viewProperty().forEach {
                    simpleAttributes.add(parseViewProperty(it))
                }
            }
        } else {
            Property.Undefined
        }
    }

    private fun parseTypelessDefinitionClosure(typelessDefinitionClosure: ReqMParserParser.TypelessDefinitionClosureContext?): Definition {
        return if (typelessDefinitionClosure != null) {
            Definition().apply {
                saveSourceInfo(this, typelessDefinitionClosure.start)
                typelessDefinitionClosure.typelessProperty().forEach {
                    properties.add(parseTypelessProperty(it))
                }
            }
        } else {
            Definition.Undefined
        }
    }

    private fun parseSimpleTypelessProperty(property: ReqMParserParser.SimpleTypelessPropertyContext): Property {
        return Property().apply {
            saveSourceInfo(this, property.start)
            key = property.qualifiedId().text
            value = property.propertyValue()?.text
        }
    }

    private fun parseTypelessProperty(property: ReqMParserParser.TypelessPropertyContext): Property {
        return if (property.simpleTypelessProperty() != null) {
            parseSimpleTypelessProperty(property.simpleTypelessProperty())
        } else if (property.compoundTypelessProperty() != null) {
            parseCompoundTypelessProperty(property.compoundTypelessProperty())
        } else {
            Property.Undefined
        }
    }

    private fun parseCompoundTypelessProperty(property: ReqMParserParser.CompoundTypelessPropertyContext): Property = Property().apply {
        saveSourceInfo(this, property.start)
        type = StandardTypes.propertyList.name
        key = property.qualifiedId().text
        property.simpleTypelessProperty().forEach {
            val p = parseSimpleTypelessProperty(it)
            simpleAttributes.add(p)
        }
    }


    private fun parseAction(action: ReqMParserParser.ActionContext?, reqmSource: ReqMSource) {
        action?.let {
            val acn = Action()
            saveSourceInfo(acn, action.start)
            acn.qid = parseQualifiedId(action.qualifiedId())
            acn.definition = parseActionDefinitionClosure(action.actionDefinitionClosure())
            reqmSource.actions.add(acn)
        }
    }

    private fun parseActionDefinitionClosure(actionDefinitionClosure: ReqMParserParser.ActionDefinitionClosureContext?): ActionDefinition {
        return if (actionDefinitionClosure != null) {
            ActionDefinition().apply {
                saveSourceInfo(this, actionDefinitionClosure.start)
                actionDefinitionClosure.actionCall().forEach { call ->
                    actionCalls.add(parseActionCall(call))
                }
            }
        } else {
            ActionDefinition.Undefined
        }
    }

    private fun parseActionCall(call: ReqMParserParser.ActionCallContext): ActionCall {
        return ActionCall().apply {
            actionName = call.ID().text
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
                parameters.add(prop)
            }
        }
    }

    private fun parseModule(module: ReqMParserParser.ModuleContext?, reqmSource: ReqMSource) {
        module?.let {
            val mod = Modul()
            saveSourceInfo(mod, module.start)
            mod.qid = parseQualifiedId(module.qualifiedId())
            mod.sourceRef = parseSourceRef(module.sourceRef())
            mod.definition = parseApplicationDefinitionClosure(module.applicationDefinitionClosure())
            reqmSource.modules.add(mod)
        }
    }

    private fun loadReqMTree(filePath: String): ReqmContext {
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
            act.sourceRef = parseSourceRef(actor.sourceRef())
            act.definition = parseTypelessDefinitionClosure(actor.typelessDefinitionClosure())
            reqmSource.actors.add(act)
        }

    }

    private fun parseClass(classs: ReqMParserParser.ClassContext?, reqmSource: ReqMSource) {
        classs?.let {
            val cls = Classs().apply {
                saveSourceInfo(this, classs.start)
                qid = parseQualifiedId(classs.qualifiedId())
                atomic = classs.KWATOMIC() != null
                enumeration = classs.KWENUMERATION() != null
                if (enumeration) {
                    definition = parseEnumDefinition(classs.enumDefinitionClosure())
                    sourceRef = QualifiedId.Undefined
                } else if (atomic) {
                    sourceRef = QualifiedId.Undefined
                } else {
                    parent = parseParent(classs.parent())
                    sourceRef = parseSourceRef(classs.sourceRef())
                    definition = parseSimpleDefinitionClosure(classs.simpleDefinitionClosure())
                }
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
            val ent = Entity().apply {
                saveSourceInfo(this, entity.start)
                qid = parseQualifiedId(entity.qualifiedId())
                parent = parseParent(entity.parent())
                sourceRef = parseSourceRef(entity.sourceRef())
                definition = parseDefinitionClosure(entity.definitionClosure())
            }
            reqmSource.entities.add(ent)
        }

    }

    private fun parseApplication(application: ReqMParserParser.ApplicationContext?, reqmSource: ReqMSource) {
        application?.let {
            val app = Application().apply {
                saveSourceInfo(this, application.start)
                qid = parseQualifiedId(application.qualifiedId())
                sourceRef = parseSourceRef(application.sourceRef())
                definition = parseApplicationDefinitionClosure(application.applicationDefinitionClosure())
            }
            reqmSource.applications.add(app)
        }

    }

    private fun parseApplicationDefinitionClosure(applicationDefinitionClosure: ReqMParserParser.ApplicationDefinitionClosureContext?): Definition {
        return if (applicationDefinitionClosure != null) {
            Definition().apply {
                saveSourceInfo(this, applicationDefinitionClosure.start)
                applicationDefinitionClosure.applicationProperty().forEach {
                    properties.add(parseApplicationProperty(it))
                }
            }
        } else {
            Definition.Undefined
        }
    }

    private fun parseApplicationProperty(property: ReqMParserParser.ApplicationPropertyContext): Property {
        return if (property.simpleApplicationProperty() != null) {
            parseSimpleApplicationProperty(property.simpleApplicationProperty())
        } else if (property.compoundTypelessProperty() != null) {
            parseCompoundTypelessProperty(property.compoundTypelessProperty())
        } else {
            Property.Undefined
        }
    }

    private fun parseSimpleApplicationProperty(property: ReqMParserParser.SimpleApplicationPropertyContext): Property {
        return Property().apply {
            saveSourceInfo(this, property.start)
            key = property.qualifiedId().text
            if (property.applicationPropertyValue() != null) {
                when {
                    property.applicationPropertyValue().StringLiteral() != null -> {
                        type = StandardTypes.stringLiteral.name
                        value = property.applicationPropertyValue().StringLiteral().text
                    }

                    property.applicationPropertyValue().SemanticVersionNumber() != null -> {
                        type = StandardTypes.versionNumber.name
                        value = property.applicationPropertyValue().SemanticVersionNumber().text
                    }

                    property.applicationPropertyValue().INT() != null -> {
                        type = StandardTypes.integer.name
                        value = property.applicationPropertyValue().INT().text
                    }

                    property.applicationPropertyValue().qualifiedId() != null -> {
                        type = StandardTypes.string.name
                        value = property.applicationPropertyValue().qualifiedId().text
                    }
                }
            }
        }
    }

    private fun saveSourceInfo(element: ElementBase, start: Token?) {
        start?.let {
            element.row = start.line
            element.col = start.charPositionInLine
            element.sourceFileName = start.inputStream.sourceName
        }
    }

    private fun parseSimpleDefinitionClosure(simpleDefinitionClosure: ReqMParserParser.SimpleDefinitionClosureContext?): Definition {
        return if (simpleDefinitionClosure != null) {
            Definition().apply {
                saveSourceInfo(this, simpleDefinitionClosure.start)
                simpleDefinitionClosure.property().forEach { property ->
                    properties.add(parseProperty(property))
                }
            }
        } else {
            Definition.Undefined
        }
    }

    private fun parseDefinitionClosure(definitionClosure: ReqMParserParser.DefinitionClosureContext?): Definition {
        return if (definitionClosure != null) {
            Definition().apply {
                saveSourceInfo(this, definitionClosure.start)
                definitionClosure.featureRef().forEach {
                    featureRefs.add(parseFeatureRef(it))
                }
                definitionClosure.property().forEach {
                    properties.add(parseProperty(it))
                }
            }
        } else {
            Definition.Undefined
        }
    }

    private fun parseFeatureRef(featureRef: ReqMParserParser.FeatureRefContext): FeatureRef {
        return FeatureRef().apply {
            saveSourceInfo(this, featureRef.start)
            qid = QualifiedId(featureRef.qualifiedId()[0].text)
            if (featureRef.qualifiedId().size > 1) {
                // short form of feature reference
                properties.add(Property().apply {
                    key = REQM_GENERAL_ATTRIBUTE_FEATURE_REFERENCE
                    value = featureRef.qualifiedId()[1].text
                    type = StandardTypes.string.name
                })
            } else {
                featureRef.property().forEach {
                    properties.add(parseProperty(it))
                }
            }
        }
    }

    private fun parseProperty(property: ReqMParserParser.PropertyContext): Property {
        return Property().apply {
            saveSourceInfo(this, property.start)
            key = property.qualifiedId().text
            if (property.propertyValue() != null) {
                property.optionality()?.let {
                    optionality = it.text
                }
                when {
                    property.propertyValue().StringLiteral() != null -> {
                        type = StandardTypes.stringLiteral.name
                        value = property.propertyValue().StringLiteral().text
                    }

                    property.propertyValue().SemanticVersionNumber() != null -> {
                        type = StandardTypes.versionNumber.name
                        value = property.propertyValue().SemanticVersionNumber().text
                    }

                    property.propertyValue().INT() != null -> {
                        type = StandardTypes.integer.name
                        value = property.propertyValue().INT().text
                    }

                    property.propertyValue().propertyType() != null -> {
                        type = property.propertyValue().propertyType().ID().text
                        listOf = property.propertyValue().propertyType().KWLISTOF() != null
                    }

                    property.propertyValue().qualifiedId() != null -> {
                        type = StandardTypes.variable.name
                        value = property.propertyValue().qualifiedId().text
                    }
                }
            } else if (property.propertyClosure() != null) {
                val definition = parsePropertyDefinition(property.propertyClosure())
                type = definition.type
                listOf = definition.listOf
                optionality = definition.optionality
                valueList.addAll(definition.valueList)
            }
        }
    }

    private fun parsePropertyDefinition(propertyClosure: ReqMParserParser.PropertyClosureContext): Property {
        return if (propertyClosure.propertyAttribute().isNotEmpty()) {
            Property().apply {
                propertyClosure.propertyAttribute().forEach { propertyAttributeContext ->
                    val attrib = parsePropertyAttribute(propertyAttributeContext)
                    when {
                        attrib.key in listOf("optional", "mandatory") -> {
                            optionality = attrib.key
                        }
                        attrib.key.equals("type") -> {
                            type = attrib.type
                            listOf = attrib.listOf
                        }
                        attrib.type == "variable" -> {
                            type = "valueList"
                            attrib.key?.let { valueList.add(it) }
                        }
                        else -> {
                            TODO("unknown attribute key: ${attrib.key}")
                        }
                    }
                }
            }
        } else {
            Property.Undefined
        }
    }

    private fun parsePropertyAttribute(property: ReqMParserParser.PropertyAttributeContext): Property {
        return Property().apply {
            when {
                property.optionality() != null -> {
                    key = property.optionality().text
                }

                property.simpleId() != null -> {
                    key = property.simpleId().ID().text
                    if (property.propertyValue() != null && property.propertyValue().propertyType() != null) {
                        type = property.propertyValue().propertyType().ID().text
                        listOf = property.propertyValue().propertyType().KWLISTOF() != null
                    }
                }

                property.qualifiedId() != null -> {
                    key = property.qualifiedId().text
                    type = "variable"
                }
            }
        }
    }

    private fun parseSourceRef(sourceRef: ReqMParserParser.SourceRefContext?): QualifiedId {
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
            QualifiedId(qualifiedId.simpleId()[ids-1].ID().text).apply {
                if (ids > 1) {
                    domain = qualifiedId.simpleId().subList(0, ids-1).joinToString(".") {  it.ID().text }
                }
            }
        } else {
            QualifiedId.Undefined
        }
    }

}