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

import dev.reqsmith.composer.common.Log
import dev.reqsmith.composer.parser.entities.*
import dev.reqsmith.composer.parser.enumeration.StandardTypes

class ReferenceExtractor {

    /**
     * Traverse reqm model and extract the following references:
     * - item name
     * - item reference
     * - property type
     *
     * @param source ReqM model
     * @return All references extracted from the model
     */
    fun extract(source: ReqMSource): References {
        return references(source)
    }

    private fun references(source: ReqMSource): References {
        Log.info("------------------- extract references from ${source.sourceFileName} ------------------")
        val refs = References()
        if (source.applications.isNotEmpty()) {
            extractApplication(source.applications[0], refs)
        }
        source.modules.forEach { extractModule(it, refs) }
        source.actors.forEach { extractActor(it, refs) }
        source.classes.forEach { extractClasss(it, refs) }
        source.entities.forEach { extractEntity(it, refs) }
        source.actions.forEach { extractAction(it, refs) }
        Log.info("---------------------------------------------------------------------------------------")
        return refs
    }

    private fun extractAction(action: Action, refs: References) {
        Log.info(" action ${action.qid}")
        refs.items.add(Ref(Ref.Type.acn, action.qid!!, null, action.sourceFileName))
        extractActionDefinition(action.definition, action.qid!!, refs)
    }

    private fun extractModule(module: Modul, refs: References) {
        Log.info(" module ${module.qid}")
        refs.items.add(Ref(Ref.Type.mod, module.qid!!, null, module.sourceFileName))
        extractSourceRef(module.qid!!, module.sourceRef, refs)
        extractDefinition(module.definition, module.qid!!, refs)
    }

    private fun extractApplication(application: Application, refs: References) {
        Log.info(" application ${application.qid}")
        refs.items.add(Ref(Ref.Type.app, application.qid!!, null, application.sourceFileName))
        extractSourceRef(application.qid!!, application.sourceRef, refs)
        extractDefinition(application.definition, application.qid!!, refs)
    }

    private fun extractActor(act: Actor, refs: References) {
        Log.info(" actor ${act.qid}")
        refs.items.add(Ref(Ref.Type.act, act.qid!!, null, act.sourceFileName))
        extractSourceRef(act.qid!!, act.sourceRef, refs)
        extractDefinition(act.definition, act.qid!!, refs)
    }

    private fun extractClasss(cls: Classs, refs: References) {
        Log.info(" class ${cls.qid}")
        refs.items.add(Ref(Ref.Type.cls, cls.qid!!, null, cls.sourceFileName))
        extractSourceRef(cls.qid!!, cls.parent, refs)
        extractSourceRef(cls.qid!!, cls.sourceRef, refs)
        extractDefinition(cls.definition, cls.qid!!, refs)
    }

    private fun extractEntity(ent: Entity, refs: References) {
        Log.info(" entity ${ent.qid}")
        refs.items.add(Ref(Ref.Type.ent, ent.qid!!, null, ent.sourceFileName))
        extractSourceRef(ent.qid!!, ent.parent, refs)
        extractSourceRef(ent.qid!!, ent.sourceRef, refs)
        extractDefinition(ent.definition, ent.qid!!, refs)
    }

    private fun extractSourceRef(referredQid: QualifiedId, sourceRef: QualifiedId?, refs: References) {
        if (sourceRef != null && sourceRef != QualifiedId.Undefined) {
            val internalType =
                refs.findItem(Ref.Type.cls, sourceRef.id!!) || refs.findItem(Ref.Type.ent, sourceRef.id!!)
            if (!internalType) {
                Log.info("  srcRef $sourceRef")
                refs.sourceRefs.add(Ref(Ref.Type.src, referredQid, sourceRef))
            }
        }
    }

    private fun extractDefinition(definition: Definition, qid: QualifiedId, refs: References) {
        definition.properties.forEach { prop ->
            if (prop.type != null && !refs.types.any { it.qid.id == prop.type }) {
                // filter out standard and internal types
                val internalType = StandardTypes.has(prop.type!!) || refs.findItem(
                    Ref.Type.cls,
                    prop.type!!
                ) || refs.findItem(Ref.Type.ent, prop.type!!)
                if (!internalType) {
                    Log.info("  type ${prop.type}")
                    refs.types.add(Ref(Ref.Type.typ, qid, QualifiedId(prop.type!!)))
                }
            }
        }
    }

    private fun extractActionDefinition(definition: ActionDefinition, qid: QualifiedId, refs: References) {
        definition.actionCalls.forEach { prop ->
            Log.info("  actionCall ${prop.actionName}")
            refs.sourceRefs.add(Ref(Ref.Type.src, qid, QualifiedId(prop.actionName)))
        }
    }

}