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

package dev.reqsmith.composer.composer

import dev.reqsmith.composer.common.Log
import dev.reqsmith.composer.common.exceptions.ReqMParsingException
import dev.reqsmith.composer.parser.ReqMParser
import dev.reqsmith.composer.parser.enumeration.Optionality
import dev.reqsmith.model.enumeration.StandardTypes
import dev.reqsmith.composer.repository.api.RepositoryFinder
import dev.reqsmith.composer.repository.api.entities.ItemCollection
import dev.reqsmith.composer.repository.api.entities.RepositoryIndex
import dev.reqsmith.model.reqm.*

class ModelMerger(private val finder: RepositoryFinder) {
    private val parser = ReqMParser()

    /**
     * Merge referenced repository items to project items
     *
     * @param reqmsrc Project source in ReqM format
     * @return Dependencies in ReqM structure
     */
    fun merge(reqmsrc: ReqMSource): ReqMSource {
        val dependencies = ReqMSource()
        reqmsrc.applications.forEach { app ->
            // collect reference applications
            if (app.sourceRef != null && app.sourceRef != QualifiedId.Undefined) {
                val apps = collectApplicationSources(app.sourceRef!!)
                dependencies.applications.addAll(apps)
                // TODO: sort the list by score
                mergeApplicationRef(app, apps)
            }

            // resolve dependencies
            val modules = resolveDependencies(app)
            dependencies.modules.addAll(modules)

            // resolve event sources
            val eventActions = resolveEventActions(app, reqmsrc)
            dependencies.actions.addAll(eventActions)
        }
        reqmsrc.actors.forEach { act ->
            // collect reference actors
            if (act.sourceRef != null && act.sourceRef != QualifiedId.Undefined) {
                val acts = collectActorSources(act.sourceRef!!)
                dependencies.actors.addAll(acts)
                // TODO: sort the list by score
                mergeActorRef(act, acts)
            }
        }
        reqmsrc.classes.forEach { cls ->
            // collect reference classes
            if (cls.sourceRef != null && cls.sourceRef != QualifiedId.Undefined) {
                val clss = collectClassSources(cls.sourceRef!!)
                dependencies.classes.addAll(clss)
                // TODO: sort the list by score
                mergeClassRef(cls, clss)
            }
        }
        reqmsrc.entities.forEach { ent ->
            // collect reference entities
            if (ent.sourceRef != null && ent.sourceRef != QualifiedId.Undefined) {
                val ents = collectEntitySources(ent.sourceRef!!)
                dependencies.entities.addAll(ents)
                // TODO: sort the list by score
                mergeEntityRef(ent, ents)
            }
        }
        // add dependendent actions to the dependecies
        reqmsrc.actions.forEach { act ->
            act.definition.actionCalls.forEach {
                val actdep = collectActionSources(it.actionName)
                dependencies.actions.addAll(actdep)
            }
            
        }

        // add dependent views
        reqmsrc.views.forEach { view ->
            collectViewDependencies(view, dependencies)
        }

        // TODO: merge features and styles

        return dependencies
    }

    private fun collectViewDependencies(view: View, dependencies: ReqMSource) {
        if (view.sourceRef != null && view.sourceRef != QualifiedId.Undefined) {
            collectViewSources(view.sourceRef!!, dependencies)
            mergeViewRef(view, dependencies)
        }
        val layout = view.definition.properties.find { it.key == "layout" }
        layout?.simpleAttributes?.let { collectViewLayoutDeps(it, dependencies) }

        collectFeatures(view.definition.featureRefs, dependencies)

    }

    private fun collectFeatures(featureRefs: List<FeatureRef>, dependencies: ReqMSource) {
        featureRefs.forEach { featureRef ->
            val ic = finder.find(Ref.Type.ftr, featureRef.qid.id!!, featureRef.qid.domain)
            var ix = 0
            while (ix < ic.items.size) {
                val item = ic.items[ix]
                Log.debug("${item.itemType} ${item.name} in ${item.filename}")
                val reqmSource = parseFile(item.filename!!)
                val feature = reqmSource.features.find { item.name == it.qid.toString() }
                if (feature != null) {
                    feature.increaseRefCount()
                    dependencies.features.add(feature)
                    addDependecyToList(feature.sourceRef, Ref.Type.ftr, ic)
                }
                ix++
            }
        }
    }

    private fun mergeViewRef(view: View, dependencies: ReqMSource) {
        var refId = view.sourceRef
        while (refId != null && refId != QualifiedId.Undefined) {
            val refView = dependencies.views.find { it.qid.toString() == refId.toString() }
            if (refView != null) {
                mergeProperties(view.definition.properties, refView.definition.properties)
                refId = refView.sourceRef
            } else {
                refId = null
            }
        }
    }

    private fun collectViewSources(sourceRef: QualifiedId, dependencies: ReqMSource) {
        val ic = finder.find(Ref.Type.vie, sourceRef.id!!, sourceRef.domain)
        ic.items.forEach {
            if (dependencies.views.none { d -> it.name == d.qid.toString() }) {
                val reqmSource = parseFile(it.filename!!)
                val depView = reqmSource.views.find { v -> v.qid!!.id == it.name }
                if (depView != null) {
                    dependencies.views.add(depView)
                    if (depView.sourceRef != null && depView.sourceRef != QualifiedId.Undefined) {
                        collectViewSources(depView.sourceRef!!, dependencies)
                    }
                    collectViewDependencies(depView, dependencies)
                }
            }
        }
    }

    private fun parseFile(filename: String): ReqMSource {
        val actReqmSource = ReqMSource()
        val ok = parser.parseReqMTree(filename, actReqmSource)
        if (!ok) throw ReqMParsingException("Parsing errors found during merge; see previous errors")
        return actReqmSource
    }

    private fun collectViewLayoutDeps(properties: MutableList<Property>, dependencies: ReqMSource) {
        properties.forEach {
            collectViewSources(QualifiedId(it.key), dependencies)
            if (it.type == StandardTypes.propertyList.name) {
                collectViewLayoutDeps(it.simpleAttributes, dependencies)
            } else if (it.value == null) {
                val depView = dependencies.views.find { dep -> dep.qid.toString() == it.key }
                if (depView != null) {
                    it.type = StandardTypes.propertyList.name
                    it.simpleAttributes.addAll(depView.definition.properties)
                    Log.warning("View layout element ${it.key} is undefined, but found in dependencies; attributes merged")
                } else {
                    // TODO: esetleg lehetne egy StandardLayoutElements enum, abban is lehetne keresni
                    Log.warning("View layout element ${it.key} is undefined")
                }
            }
        }
    }

    private fun resolveEventActions(app: Application, reqmsrc: ReqMSource): Collection<Action> {
        val dependentActions : MutableList<Action> = ArrayList()
        val events = app.definition.properties.find { it.key == "events" }
        events?.simpleAttributes?.forEach { attr ->
            val actionName = attr.value
            // is this action exists in reqmsrc?
            val srcAction = reqmsrc.actions.find { it.qid!!.id == actionName }
            if (srcAction == null) {
                // not exists, search dependencies for it
                val ic = finder.find(Ref.Type.acn, actionName!!, null)
                if (ic.items.isNotEmpty()) {
                    var ix = 0
                    while (ix < ic.items.size) {
                        val item = ic.items[ix]
                        val actReqmSource = parseFile(item.filename!!)
                        val act = actReqmSource.actions.find { item.name == it.qid.toString() }
                        if (act != null) {
                            dependentActions.add(act)
                            act.increaseRefCount()
                            act.definition.actionCalls.forEach { call ->
                                when {
                                    call.actionName == "call" -> {
                                        // TODO
                                    }
                                    else -> {
                                        addDependecyToList(QualifiedId(call.actionName), Ref.Type.acn, ic)
                                    }
                                }
                            }
                        } else {
                            Log.error("Index problem with ${item.filename}; do reindexing the repository")
                        }
                        ix++
                    }
                } else {
                    // no actions found for this event, create an empty one, and send warning
                    dependentActions.add(Action().apply { qid = QualifiedId(actionName) })
                    Log.warning("Action $actionName is not found for event ${attr.key}; create an empty action")
                }
            }
        }
        return dependentActions
    }

    private fun collectActionSources(actionName: String): Collection<Action> {
        val actions : MutableList<Action> = ArrayList()
        val ic = finder.find(Ref.Type.acn, actionName, null)
        ic.items.forEach { item ->
            Log.debug("${item.itemType} ${item.name} in ${item.filename}")
            val reqmSource = parseFile(item.filename!!)
            val act = reqmSource.actions.find { item.name == it.qid.toString() }
            if (act != null) {
                act.increaseRefCount()
                actions.add(act)
            }
        }
        return actions
    }

    private fun resolveDependencies(app: Application): List<Modul> {
        val modules: MutableList<Modul> = ArrayList()
        val dependencies = app.definition.properties.find { it.key == "dependencies" }
        dependencies?.let {
            it.simpleAttributes.forEach { dep ->
                modules.addAll(collectModuleSource(dep.key))
            }
        }
        return modules
    }

    private fun mergeEntityRef(ent: Entity, refEnts: List<Entity>) {
        refEnts.forEach {
            mergeProperties(ent.definition.properties, it.definition.properties)
        }
    }

    private fun addDependecyToList(sourceRef: QualifiedId?, itemType: Ref.Type, ic: ItemCollection) {
        if (sourceRef != null && sourceRef != QualifiedId.Undefined) {
            if (ic.items.any { it.name == sourceRef.toString() && it.itemType == itemType }) return
            val ic2 = finder.find(itemType, sourceRef.id!!, sourceRef.domain)
            if (ic2.items.isNotEmpty()) {
                ic2.items[0].recType = RepositoryIndex.RecordType.content
                ic.items.add(ic2.items[0])
            }
        }
    }

    private fun collectEntitySources(ref: QualifiedId): List<Entity> {
        val entities : MutableList<Entity> = ArrayList()
        val ic = finder.find(Ref.Type.ent, ref.id!!, ref.domain)
        var ix = 0
        while (ix < ic.items.size) {
            val item = ic.items[ix]
            Log.debug("${item.itemType} ${item.name} in ${item.filename}")
            val reqmSource = parseFile(item.filename!!)
            val ent = reqmSource.entities.find { item.name == it.qid.toString() }
            if (ent != null) {
                ent.increaseRefCount()
                entities.add(ent)
                addDependecyToList(ent.sourceRef, Ref.Type.ent, ic)
            }
            ix++
        }
        return entities
    }

    private fun mergeClassRef(cls: Classs, refClss: List<Classs>) {
        refClss.forEach {
            mergeProperties(cls.definition.properties, it.definition.properties)
        }
    }

    private fun collectClassSources(ref: QualifiedId): List<Classs> {
        val classes : MutableList<Classs> = ArrayList()
        val ic = finder.find(Ref.Type.cls, ref.id!!, ref.domain)
        var ix = 0
        while (ix < ic.items.size) {
            val item = ic.items[ix]
            Log.debug("${item.itemType} ${item.name} in ${item.filename}")
            val reqmSource = parseFile(item.filename!!)
            val cls = reqmSource.classes.find { item.name == it.qid.toString() }
            if (cls != null) {
                cls.increaseRefCount()
                classes.add(cls)
                addDependecyToList(cls.sourceRef, Ref.Type.cls, ic)
            }
            ix++
        }
        return classes
    }

    private fun mergeActorRef(act: Actor, refActs: List<Actor>) {
        refActs.forEach {
            mergeProperties(act.definition.properties, it.definition.properties)
        }
    }

    private fun collectActorSources(ref: QualifiedId): List<Actor> {
        val actors : MutableList<Actor> = ArrayList()
        val ic = finder.find(Ref.Type.act, ref.id!!, ref.domain)
        var ix = 0
        while (ix < ic.items.size) {
            val item = ic.items[ix]
            Log.debug("${item.itemType} ${item.name} in ${item.filename}")
            val reqmSource = parseFile(item.filename!!)
            val act = reqmSource.actors.find { item.name == it.qid.toString() }
            if (act != null) {
                act.increaseRefCount()
                actors.add(act)
                addDependecyToList(act.sourceRef, Ref.Type.act, ic)
            }
            ix++
        }
        return actors
    }

    /**
     * Collect the referenced item and its parents
     *
     * @param ref Reference of the project application item
     * @return List of the referenced application items
     */
    private fun collectApplicationSources(ref: QualifiedId): List<Application> {
        val applications : MutableList<Application> = ArrayList()
        val ic = finder.find(Ref.Type.app, ref.id!!, ref.domain)
        var ix = 0
        while (ix < ic.items.size) {
            val item = ic.items[ix]
            Log.debug("${item.itemType} ${item.name} in ${item.filename}")
            val reqmSource = parseFile(item.filename!!)
            val app = reqmSource.applications.find { item.name == it.qid.toString() }
            if (app != null) {
                app.increaseRefCount()
                applications.add(app)
                addDependecyToList(app.sourceRef, Ref.Type.app, ic)
            }
            ix++
        }
        return applications
    }

    private fun collectModuleSource(key: String?): List<Modul> {
        val ref = QualifiedId(key)
        val moduls : MutableList<Modul> = ArrayList()
        val ic = finder.find(Ref.Type.mod, ref.id!!, ref.domain)
        var ix = 0
        while (ix < ic.items.size) {
            val item = ic.items[ix]
            Log.debug("${item.itemType} ${item.name} in ${item.filename}")
            val reqmSource = parseFile(item.filename!!)
            val mod = reqmSource.modules.find { item.name == it.qid.toString() }
            if (mod != null) {
                mod.increaseRefCount()
                moduls.add(mod)
                addDependecyToList(mod.sourceRef, Ref.Type.mod, ic)
            }
            ix++
        }
        return moduls
    }

    /**
     * Merge referenced application items to the project item
     *
     * @param app Project application item
     * @param refApps List of referenced application items
     */
    private fun mergeApplicationRef(app: Application, refApps: List<Application>) {
        refApps.forEach {
            if (app.sourceRef != QualifiedId.Undefined && app.sourceRef?.id == it.qid?.id && app.sourceRef?.domain.isNullOrBlank() && !it.qid?.domain.isNullOrBlank()) {
                app.sourceRef?.domain = it.qid?.domain
            }
            if (app.definition == Definition.Undefined && it.definition != Definition.Undefined) {
                app.definition = Definition()
            }
            mergeProperties(app.definition.properties, it.definition.properties)
        }
    }

    /**
     * Merge source item properties into destination one
     *
     * @param destPropList Destination (project) item properties
     * @param srcPropList Source (referenced) item properties
     */
    private fun mergeProperties(destPropList: MutableList<Property>, srcPropList: List<Property>) {
        srcPropList.forEach { d ->
            val p = destPropList.find { it.key == d.key }
            if (p != null) {
                if (p.type == null) {
                    p.type = d.type
                    p.listOf = d.listOf
                }
                if (p.optionality == Optionality.Undefined.name) {
                    p.optionality = d.optionality
                }
            } else {
                destPropList.add(d)
            }
        }
    }

    // TODO: find a programmatical way to collect resource items
    private val layoutResources = mapOf(
        "header" to listOf("logo")
    )

}