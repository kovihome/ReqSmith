/*
 * ReqSmith - Build application from requirements
 * Copyright (c) 2023-2026. Kovi <kovihome86@gmail.com>
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

import dev.reqsmith.composer.common.Log
import dev.reqsmith.model.REQM_GENERAL_ATTRIBUTE_FEATURE_REFERENCE
import dev.reqsmith.model.enumeration.StandardTypes
import dev.reqsmith.model.reqm.*
import java.io.FileOutputStream
import java.io.OutputStreamWriter

private const val TAB_SIZE = 4

class ReqMWriter {

    private val eol = System.lineSeparator()

    fun writeReqM(source : ReqMSource, outFileName : String, reqmHeader: String) {
        OutputStreamWriter(FileOutputStream(outFileName)).use { outs ->
            writeComment(outs, reqmHeader)
            source.applications.forEach { writeApplication(it, outs) }
            source.modules.forEach { writeModul(it, outs) }
            source.actors.forEach { writeActor(it, outs) }
            source.classes.forEach { writeClasss(it, outs) }
            source.entities.forEach { writeEntity(it, outs) }
            source.actions.forEach { writeAction(it, outs) }
            source.views.forEach { writeView(it, outs) }
            source.styles.forEach { writeStyle(it, outs) }
            source.features.forEach { writeFeature(it, outs) }
        }
    }

    private fun writeStyle(style: Style, outs: OutputStreamWriter) {
        outs.write("style ")
        writeQualifiedId(style.qid, outs)
        writeSourceRef(style.sourceRef, outs)
        writeDefinition(style.definition, outs)
        outs.write("\n")
    }

    private fun writeFeature(feature: Feature, outs: OutputStreamWriter) {
        outs.write("feature ")
        writeQualifiedId(feature.qid, outs)
        writeSourceRef(feature.sourceRef, outs)
        writeDefinition(feature.definition, outs)
        outs.write("\n")
    }

    private fun writeView(view: View, outs: OutputStreamWriter) {
        outs.write("view ")
        writeQualifiedId(view.qid, outs)
        writeParent(view.parent, outs)
        writeSourceRef(view.sourceRef, outs)
        writeDefinition(view.definition, outs)
        outs.write("\n")
    }

    private fun writeModul(modul: Modul, outs: OutputStreamWriter) {
        outs.write("modul ")
        writeQualifiedId(modul.qid, outs)
        writeSourceRef(modul.sourceRef, outs)
        writeDefinition(modul.definition, outs)
        outs.write("\n")
    }

    private fun writeAction(action: Action, outs: OutputStreamWriter) {
        action.owner?.let { outs.write("// Owner: ${action.owner}\n") }
        outs.write("action ")
        writeQualifiedId(action.qid, outs)
        writeActionDefinition(action.definition, outs)
        outs.write("\n")
    }

    private fun writeActionDefinition(definition: ActionDefinition, outs: OutputStreamWriter) {
        if (definition != ActionDefinition.Undefined) {
            outs.write("{\n")
            definition.actionCalls.forEach {
                outs.write(tabs(1))
                outs.write("${it.actionName} ")
                outs.write (it.parameters.joinToString(", ") { s -> s.value!! })
                outs.write("\n")
            }
            outs.write("}\n")
        } else {
            outs.write("\n")
        }
    }

    private fun writeEntity(ent: Entity, outs: OutputStreamWriter) {
        outs.write("entity ")
        writeQualifiedId(ent.qid, outs)
        writeParent(ent.parent, outs)
        writeSourceRef(ent.sourceRef, outs)
        writeDefinition(ent.definition, outs)
        outs.write("\n")
    }

    private fun writeParent(parent: QualifiedId, outs: OutputStreamWriter) {
        if (parent != QualifiedId.Undefined) {
            outs.write("is ")
            writeQualifiedId(parent, outs)
        }
    }

    private fun writeClasss(cls: Classs, outs: OutputStreamWriter) {
        outs.write("class ")
        writeQualifiedId(cls.qid, outs)
        writeParent(cls.parent, outs)
        writeSourceRef(cls.sourceRef, outs)
        if (cls.atomic) {
            outs.write("atomic ")
        }
        if (cls.enumeration) {
            outs.write("enumeration ")
            writeEnumerationDefinition(cls.definition, outs)
        } else {
            writeDefinition(cls.definition, outs)
        }
        outs.write("\n")
    }

    private fun writeEnumerationDefinition(definition: Definition, outs: OutputStreamWriter) {
        if (definition != Definition.Undefined) {
            outs.write("{\n")
            outs.write(tabs(1))
            val s = definition.properties.joinToString(", ") { it.key!! }
            outs.write(s)
            outs.write("\n")
            outs.write("}\n")
        } else {
            outs.write("\n")
        }
    }

    private fun writeActor(act: Actor, outs: OutputStreamWriter) {
        outs.write("actor ")
        writeQualifiedId(act.qid, outs)
        writeSourceRef(act.sourceRef, outs)
        writeDefinition(act.definition, outs)
        outs.write("\n")
    }

    private fun writeApplication(application: Application, outs: OutputStreamWriter) {
        outs.write("application ")
        writeQualifiedId(application.qid, outs)
        writeSourceRef(application.sourceRef, outs)
        writeDefinition(application.definition, outs)
        outs.write("\n")
    }

    private fun writeSourceRef(sourceRef: QualifiedId?, outs: OutputStreamWriter) {
        if (sourceRef != QualifiedId.Undefined) {
            outs.write("from ")
            writeQualifiedId(sourceRef, outs)
        }
    }

    private fun writeDefinition(definition: Definition, outs: OutputStreamWriter) {
        if (definition != Definition.Undefined) {
            outs.write("{\n")
            definition.featureRefs.forEach { writeFeatureRef(it, outs, 1) }
            definition.properties.forEach { writeProperty(it, outs, 1) }
            outs.write("}\n")
        } else {
            outs.write("\n")
        }
    }

    private fun writeFeatureRef(featureRef: FeatureRef, outs: OutputStreamWriter, tc: Int) {
        outs.write(tabs(tc))
        outs.write("@")
        writeQualifiedId(featureRef.qid, outs)
        if (featureRef.properties.isNotEmpty()) {
            if (featureRef.properties[0].key == REQM_GENERAL_ATTRIBUTE_FEATURE_REFERENCE) {
                // TODO: az effective modellben már nincs "reference", a feature első property-jére lett lecserélve
                //  ha csak egy property van, és az megegyezik a feature első property-jével,
                //  akkor kell ezt a kiiratási formát választani
                outs.write(": ${featureRef.properties[0].value}\n")
            } else {
                outs.write(" {\n")
                featureRef.properties.forEach { writeProperty(it, outs, tc + 1) }
                outs.write("${tabs(tc)}}")
            }
        }
        outs.write("\n")
    }

    private fun tabs(count: Int) : String {
        return "".padStart(count * TAB_SIZE)
    }

    private fun writeProperty(prop: Property, outs: OutputStreamWriter, tc: Int) {
        outs.write(tabs(tc))
        outs.write(prop.key!!)
        when (prop.type) {
            StandardTypes.valueList.name -> {
                outs.write(" {\n")
                prop.valueList.forEach {
                    outs.write(tabs(tc+1))
                    outs.write("${it}\n")
                }
                outs.write(tabs(tc))
                outs.write("}")
            }
            StandardTypes.propertyList.name -> {
                outs.write(" {\n")
                prop.simpleAttributes.forEach {
                    writeProperty(it, outs, tc+1)
                }
                outs.write(tabs(tc))
                outs.write("}")
            }
            else -> {
                when {
                    prop.valueList.isNotEmpty() -> {
                        outs.write(": ${prop.valueList.joinToString(", ")}")
                    }
                    prop.value != null -> {
                        outs.write(": ${prop.value}")
                    }
                    prop.optionality != null || prop.type != null -> {
                        outs.write(": ")
                        if (prop.optionality != null) {
                            outs.write(prop.optionality!!)
                            outs.write(" ")
                        }
                        if (prop.type != null) {
                            if (prop.listOf) {
                                outs.write("listOf ")
                            }
                            outs.write(prop.type!!)
                        }
                    }
                }
            }
        }
        outs.write("\n")
    }

    private fun writeQualifiedId(qid: QualifiedId?, outs: OutputStreamWriter) {
        if (qid != null && qid != QualifiedId.Undefined) {
            qid.domain?.let { outs.write(it) }
            if (qid.domain != null && qid.id != null) {
                outs.write(".")
            }
            qid.id?.let { outs.write(it) }
            outs.write(" ")
        }
    }

    private fun writeComment(outs: OutputStreamWriter, s: String) {
        outs.write("/*$eol")
        s.lines().forEach { outs.write("** $it$eol") }
        outs.write("*/$eol$eol")
    }

    fun printReqMSource(source : ReqMSource) {
        source.applications.forEach { app ->
            Log.info(app.toString())
            printDefinition(app.definition)
        }
        source.modules.forEach { mod ->
            Log.info(mod.toString())
            printDefinition(mod.definition)
        }
        source.actors.forEach { act ->
            Log.info(act.toString())
            printDefinition(act.definition)
        }
        source.classes.forEach { cls ->
            Log.info(cls.toString())
            printDefinition(cls.definition)
        }
        source.entities.forEach { ent ->
            Log.info(ent.toString())
            printDefinition(ent.definition)
        }
        source.actions.forEach {
            Log.info(it.toString())
            printActionDefinition(it.definition)
        }
        source.views.forEach {
            Log.info(it.toString())
            printDefinition(it.definition)
        }
        source.styles.forEach {
            Log.info(it.toString())
            printDefinition(it.definition)
        }
        source.features.forEach {
            Log.info(it.toString())
            printDefinition(it.definition)
        }
    }

    private fun printActionDefinition(definition: ActionDefinition) {
        definition.actionCalls.forEach { Log.info("  - $it")}
    }

    private fun printDefinition(definition: Definition) {
        definition.featureRefs.forEach { Log.info("  - $it") }
        definition.properties.forEach { Log.info("  - $it")}
    }

}