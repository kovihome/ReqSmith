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
import dev.reqsmith.composer.common.WholeProject
import dev.reqsmith.composer.common.exceptions.ReqMMergeException
import dev.reqsmith.composer.common.exceptions.ReqMParsingException
import dev.reqsmith.composer.parser.ReqMParser
import dev.reqsmith.composer.parser.enumeration.Optionality
import dev.reqsmith.composer.repository.api.RepositoryFinder
import dev.reqsmith.composer.repository.api.entities.ItemCollection
import dev.reqsmith.composer.repository.api.entities.RepositoryIndex
import dev.reqsmith.model.enumeration.StandardTypes
import dev.reqsmith.model.reqm.*

/**
 * Merge missing requirement elements and collect dependencies from repository into project model
 * @param finder Repository finder
 */
class ModelMerger(private val finder: RepositoryFinder) {
    private val parser = ReqMParser()
    private val errors: MutableList<String> = ArrayList()

    /**
     * Merge referenced repository items to project items
     *
     * @throws ReqMMergeException - holds all merge errors
     */
    fun merge() {
        WholeProject.projectModel.source.applications.forEach { app ->
            // collect reference applications
            if (app.sourceRef != null && app.sourceRef != QualifiedId.Undefined) {
                val apps = collectApplicationSources(app.sourceRef!!)
                if (apps.isNotEmpty()) {
                    WholeProject.projectModel.dependencies.applications.addAll(apps)
                    // TODO: sort the list by score
                    mergeApplicationRef(app, apps)
                } else {
                    errors.add("Source reference ${app.sourceRef} is not found for application ${app.qid} (${app.coords()}). (${app.coords()})")
                }
            }

            // resolve dependencies
            val modules = resolveDependencies(app)
            WholeProject.projectModel.dependencies.modules.addAll(modules)

            // resolve event sources
            val eventActions = resolveEventActions(app)
            WholeProject.projectModel.dependencies.actions.addAll(eventActions)
        }
        WholeProject.projectModel.source.actors.forEach { act ->
            // collect reference actors
            if (act.sourceRef != null && act.sourceRef != QualifiedId.Undefined) {
                val acts = collectActorSources(act.sourceRef!!)
                if (acts.isNotEmpty()) {
                    WholeProject.projectModel.dependencies.actors.addAll(acts)
                    // TODO: sort the list by score
                    mergeActorRef(act, acts)
                } else {
                    errors.add("Source reference ${act.sourceRef} is not found for actor ${act.qid} (${act.coords()}). (${act.coords()})")
                }
            }
        }
        WholeProject.projectModel.source.classes.forEach { cls ->
            // collect reference classes
            if (cls.sourceRef != null && cls.sourceRef != QualifiedId.Undefined) {
                val clss = collectClassSources(cls.sourceRef!!)
                if (clss.isNotEmpty()) {
                    WholeProject.projectModel.dependencies.classes.addAll(clss)
                    // TODO: sort the list by score
                    mergeClassRef(cls, clss)
                } else {
                    errors.add("Source reference ${cls.sourceRef} is not found for class ${cls.qid} (${cls.coords()}). (${cls.coords()})")
                }
            }
        }
        WholeProject.projectModel.source.entities.forEach { ent ->
            // collect reference entities
            if (ent.sourceRef != null && ent.sourceRef != QualifiedId.Undefined) {
                val ents = collectEntitySources(ent.sourceRef!!)
                if (ents.isNotEmpty()) {
                    WholeProject.projectModel.dependencies.entities.addAll(ents)
                    // TODO: sort the list by score
                    mergeEntityRef(ent, ents)
                } else {
                    errors.add("Source reference ${ent.sourceRef} is not found for entity ${ent.qid} (${ent.coords()}). (${ent.coords()})")
                }
            }
            collectFeatures(ent.definition.featureRefs)
        }
        // add dependent actions to the dependencies
        WholeProject.projectModel.source.actions.forEach { act ->
            act.definition.actionCalls.forEach {
                val actdep = collectActionSources(it.actionName)
                WholeProject.projectModel.dependencies.actions.addAll(actdep)
            }
            
        }

        // add dependent views
        WholeProject.projectModel.source.views.forEach { view ->
            collectViewDependencies(view)
            resolveViewTemplates(view)
            resolveViewPropertiesInLayout(view)
        }

        // TODO: merge styles

        // throw errors
        if (errors.isNotEmpty()) {
            throw ReqMMergeException("Merge failed.", errors)
        }
    }

    private fun resolveViewTemplates(view: View) {
        // get template view name
        val templateViewName = view.definition.featureRefs.find { it.qid.toString() == "Template" }?.let { featureRef ->
            featureRef.properties.find { it.key == "templateView" }?.type
        }

        // find template view
        if (templateViewName != null) {
            var templateView = WholeProject.projectModel.source.views.find { it.qid?.id == templateViewName }
            if (templateView == null) {
                templateView = WholeProject.projectModel.dependencies.views.find { it.qid?.id == templateViewName }
                if (templateView == null) {
                    val ic = finder.find(Ref.Type.vie, templateViewName, null)
                    if (ic.items.isNotEmpty()) {
                        if (ic.items.size > 1) {
                            errors.add("More than one template view reference $templateViewName was found.")
                        } else {
                            val icItem = ic.items[0]
                            val reqmSource = parseFile(icItem.filename!!)
                            val depView = reqmSource.views.find { v -> v.qid!!.id == icItem.name }
                            if (depView != null) {
                                WholeProject.projectModel.dependencies.views.add(depView)
                                collectViewDependencies(depView)
                                templateView = depView
                            }
                        }
                    }
                } else {
                    collectViewDependencies(templateView)
                }
            }

            // inject view layout into template layout
            if (templateView == null) {
                errors.add("Template view reference $templateViewName is not found.")
            } else if (templateView.parent.toString() != "template") {
                errors.add("Template view $templateViewName is not 'template' type view.")
            } else {
                val templateLayout = templateView.definition.properties.find { it.key == "layout" }
                if (templateLayout != null) {
                    var viewLayout = view.definition.properties.find { it.key == "layout" }
                    if (viewLayout == null) {
                        viewLayout = Property()
                    }

                    val nodesContainsContent = searchProperties(templateLayout) { p -> p.simpleAttributes.any { it.key == "content" } }
                    if (nodesContainsContent.isNotEmpty()) {
                        val newProperties: MutableList<Property> = mutableListOf()
                        nodesContainsContent[0].simpleAttributes.forEach { ta ->
                            if (ta.key != "content") {
                                newProperties.add(ta)
                            } else {
                                viewLayout.simpleAttributes.forEach { newProperties.add(it) }
                            }
                        }
                        viewLayout.simpleAttributes.clear()
                        viewLayout.simpleAttributes.addAll(newProperties)
                    } else {
                        errors.add("Template view $templateViewName has no 'content' layout element.")
                    }
                } else {
                    errors.add("Template view $templateViewName has no layout properties.")
                }
            }
        }
    }

    private fun searchProperties(node: Property, expr: (Property) -> Boolean): List<Property> {
        val nodeList = mutableListOf<Property>()
        if (expr(node)) nodeList.add(node)
        node.simpleAttributes.forEach { child ->
            nodeList.addAll(searchProperties(child, expr))
        }
        return nodeList
    }

    private fun resolveViewPropertiesInLayout(view: View) {
        view.definition.properties.find { it.key == "layout" }?.let { layout ->
            resolveNodeProperties(layout, view.definition.properties)
        }
    }

    private fun resolveNodeProperties(node: Property, properties: MutableList<Property>) {
        node.simpleAttributes.filter { it.type != StandardTypes.propertyList.name  }.forEach { attr ->
            attr.value?.let {
                properties.find { it.key == attr.value }?.let { prop ->
                    attr.value = prop.value
                }
            }
        }
        node.simpleAttributes.filter { it.type == StandardTypes.propertyList.name  }.forEach { child ->
            resolveNodeProperties(child, properties)
        }
    }

    private fun collectViewDependencies(view: View) {
        if (view.sourceRef != null && view.sourceRef != QualifiedId.Undefined) {
            collectViewSources(view.sourceRef!!)
            mergeViewRef(view)
        }
        val layout = view.definition.properties.find { it.key == "layout" }
        layout?.simpleAttributes?.let { collectViewLayoutDeps(it) }

        collectFeatures(view.definition.featureRefs)

    }

    private fun collectFeatures(featureRefs: List<FeatureRef>) {
        featureRefs.forEach { featureRef ->
            val ic = finder.find(Ref.Type.ftr, featureRef.qid.id!!, featureRef.qid.domain)
            if (ic.items.isEmpty()) errors.add("Feature reference $featureRef is not found.")
            var ix = 0
            while (ix < ic.items.size) {
                val item = ic.items[ix]
                Log.debug("${item.itemType} ${item.name} in ${item.filename}")
                val reqmSource = parseFile(item.filename!!)
                val feature = reqmSource.features.find { item.name == it.qid.toString() }
                if (feature != null) {
                    feature.increaseRefCount()
                    WholeProject.projectModel.dependencies.features.add(feature)
                    addDependencyToList(feature.sourceRef, Ref.Type.ftr, ic)
                }
                ix++
            }
        }
    }

    private fun mergeViewRef(view: View) {
        val refView = WholeProject.projectModel.dependencies.views.find { it.qid.toString() == view.sourceRef.toString() }
        if (refView != null) {
            mergeProperties(view.definition.properties, refView.definition.properties)
        }
    }

    private fun collectViewSources(sourceRef: QualifiedId) {
        val ic = finder.find(Ref.Type.vie, sourceRef.id!!, sourceRef.domain)
        if (ic.items.isEmpty()) errors.add("Source reference $sourceRef is not found.")
        ic.items.forEach {
            if (WholeProject.projectModel.dependencies.views.none { d -> it.name == d.qid.toString() }) {
                val reqmSource = parseFile(it.filename!!)
                val depView = reqmSource.views.find { v -> v.qid!!.id == it.name }
                if (depView != null) {
                    WholeProject.projectModel.dependencies.views.add(depView)
                    if (depView.sourceRef != null && depView.sourceRef != QualifiedId.Undefined) {
                        collectViewSources(depView.sourceRef!!)
                    }
                    collectViewDependencies(depView)
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

    private fun collectViewLayoutDeps(properties: MutableList<Property>) {
        properties.forEach { prop ->
            if (!listOf("content", "events").contains(prop.key)) {
                if (prop.type == StandardTypes.propertyList.name || prop.value == null) {
                    collectViewSources(QualifiedId(prop.key))
                    if (prop.type == StandardTypes.propertyList.name /*&& !StandardLayoutElements.contains(prop.key!!)*/) {
                        collectViewLayoutDeps(prop.simpleAttributes)
                    }
                    var depView = WholeProject.projectModel.source.views.find { dep -> dep.qid.toString() == prop.key }
                    if (depView == null) {
                        depView = WholeProject.projectModel.dependencies.views.find { dep -> dep.qid.toString() == prop.key }
                    }
                    if (depView != null) {
                        if (prop.type != StandardTypes.propertyList.name) {
                            prop.type = StandardTypes.propertyList.name
                            prop.simpleAttributes.addAll(depView.definition.properties)
                        } else {
                            depView.definition.properties.forEach { depprop ->
                                if (prop.simpleAttributes.none { it.key == depprop.key }) {
                                    prop.simpleAttributes.add(depprop)
                                }
                            }
                        }
                    } else {
                        // TODO: esetleg lehetne egy StandardLayoutElements enum, abban is lehetne keresni
                        Log.warning("View layout element ${prop.key} is undefined (${prop.coords()})")
                    }

                }
            } else {
                // spaceholder for template
                if (prop.key == "content") prop.type = "#"
            }
        }
    }

    private fun resolveEventActions(app: Application): Collection<Action> {
        val dependentActions : MutableList<Action> = ArrayList()
        val events = app.definition.properties.find { it.key == "events" }
        events?.simpleAttributes?.forEach { attr ->
            val actionName = attr.value
            // is this action exists in reqmsrc?
            val srcAction = WholeProject.projectModel.source.actions.find { it.qid!!.id == actionName }
            if (srcAction == null) {
                // not exists, search dependencies for it
                val ic = finder.find(Ref.Type.acn, actionName!!, null)
                if (ic.items.isEmpty()) errors.add("Action $actionName is not found.")
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
                                        addDependencyToList(QualifiedId(call.actionName), Ref.Type.acn, ic)
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
                    Log.warning("Action $actionName is not found for event ${attr.key}; create an empty action (${attr.coords()})")
                }
            }
        }
        return dependentActions
    }

    private fun collectActionSources(actionName: String): Collection<Action> {
        val actions : MutableList<Action> = ArrayList()
        val ic = finder.find(Ref.Type.acn, actionName, null)
        if (ic.items.isEmpty()) errors.add("Action $actionName is not found.")
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

    private fun addDependencyToList(sourceRef: QualifiedId?, itemType: Ref.Type, ic: ItemCollection) {
        if (sourceRef != null && sourceRef != QualifiedId.Undefined) {
            if (ic.items.any { it.name == sourceRef.toString() && it.itemType == itemType }) return
            val ic2 = finder.find(itemType, sourceRef.id!!, sourceRef.domain)
            if (ic.items.isEmpty()) errors.add("Source reference $sourceRef is not found. (${sourceRef.coords()})")
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
                addDependencyToList(ent.sourceRef, Ref.Type.ent, ic)
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
                addDependencyToList(cls.sourceRef, Ref.Type.cls, ic)
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
                addDependencyToList(act.sourceRef, Ref.Type.act, ic)
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
                addDependencyToList(app.sourceRef, Ref.Type.app, ic)
            }
            ix++
        }
        return applications
    }

    private fun collectModuleSource(key: String?): List<Modul> {
        val ref = QualifiedId(key)
        val moduls : MutableList<Modul> = ArrayList()
        val ic = finder.find(Ref.Type.mod, ref.id!!, ref.domain)
        if (ic.items.isEmpty()) errors.add("Source reference $ref is not found. (${ref.coords()})")
        var ix = 0
        while (ix < ic.items.size) {
            val item = ic.items[ix]
            Log.debug("${item.itemType} ${item.name} in ${item.filename}")
            val reqmSource = parseFile(item.filename!!)
            val mod = reqmSource.modules.find { item.name == it.qid.toString() }
            if (mod != null) {
                mod.increaseRefCount()
                moduls.add(mod)
                addDependencyToList(mod.sourceRef, Ref.Type.mod, ic)
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

}