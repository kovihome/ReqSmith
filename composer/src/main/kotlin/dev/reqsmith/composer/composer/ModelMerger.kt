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
import dev.reqsmith.composer.common.configuration.ConfigManager
import dev.reqsmith.composer.common.exceptions.ReqMMergeException
import dev.reqsmith.composer.common.exceptions.ReqMParsingException
import dev.reqsmith.composer.common.formatter.NameFormatter
import dev.reqsmith.composer.parser.ReqMParser
import dev.reqsmith.composer.parser.enumeration.Optionality
import dev.reqsmith.composer.repository.api.RepositoryFinder
import dev.reqsmith.composer.repository.api.entities.ItemCollection
import dev.reqsmith.composer.repository.api.entities.RepositoryIndex
import dev.reqsmith.model.*
import dev.reqsmith.model.enumeration.StandardLayoutElements
import dev.reqsmith.model.enumeration.StandardTypes
import dev.reqsmith.model.enumeration.VIEW_LAYOUT_ELEMENT_ATTR_DATA
import dev.reqsmith.model.enumeration.VIEW_LAYOUT_ELEMENT_ATTR_TO
import dev.reqsmith.model.reqm.*
import kotlin.collections.contains

const val TEMPLATE_NAME_TO_AVOID_DEFAULT = "avoidDefault"

/**
 * Merge missing requirement elements and collect dependencies from the repository into the project model
 * @param finder Repository finder
 */
class ModelMerger(private val finder: RepositoryFinder) {
    private val parser = ReqMParser()
    private val errors: MutableList<String> = ArrayList()

    data class Reference(val type: Ref.Type, val name: String)

    /**
     * Merge referenced repository items to project items
     *
     * @throws ReqMMergeException - holds all merge errors
     */
    fun merge() {
        val dependencies = mutableSetOf<Reference>()

        // merge applications
        WholeProject.projectModel.source.applications.forEach { app ->
            // collect reference applications
            mergeApplicationSourceReferences(app)
            collectApplicationDependencies(app, dependencies)
        }

        // merge modules
        dependencies.filter { it.type == Ref.Type.byClass<Modul>() }.forEach { mod ->
            mergeModuleSourceReferences(mod)
        }

        // merge views
        val defaultViewTemplate = getDefaultViewTemplate()
        val defaultStyle = getDefaultStyle()
        WholeProject.projectModel.source.views.forEach { view ->
            Log.debug("Merging source view ${view.qid} ...")
            mergeView(view, defaultViewTemplate, defaultStyle, dependencies)
        }
        while (dependencies.any { it.type == Ref.Type.byClass<View>() }) {
            var newDeps = mutableSetOf<Reference>()
            dependencies.filter { it.type == Ref.Type.byClass<View>() }.forEach { view ->
                Log.debug("Merging dependent view ${view.name} ...")
                WholeProject.projectModel.get<View>(view.name) {
                    var newView = getFromRepository<View>(QualifiedId(view.name))
                    if (newView == null) {
                        Log.warning("Missing View for reference ${view.name}; create new view for this link.")
                        newView = View().apply {
                            qid = QualifiedId(view.name)
//                            parent = QualifiedId("static")
                            sourceRef = QualifiedId("MissingView")
                        }
                    }
                    mergeView(newView, defaultViewTemplate, defaultStyle, newDeps)
                    WholeProject.projectModel.source.views.add(newView)
                    newView
                }
            }
            newDeps = newDeps.subtract(dependencies) as MutableSet<Reference>
            dependencies.removeIf { it.type == Ref.Type.byClass<View>() }
            dependencies.addAll(newDeps)
        }

        // merge styles
        WholeProject.projectModel.source.styles.forEach { style ->
            mergeStyleSourceReferences(style)
        }

        // merge entities and classes
        WholeProject.projectModel.source.entities.forEach { ent -> mergeEntity(ent, dependencies) }
        WholeProject.projectModel.source.classes.forEach { cls -> mergeClass(cls, dependencies) }
        while (dependencies.any { it.type == Ref.Type.byClass<Entity>() }) {
            var newDeps = mutableSetOf<Reference>()
            dependencies.filter { it.type == Ref.Type.byClass<Entity>() }.forEach { entity ->
                Log.debug("Merging dependent entity/class ${entity.name} ...")
                val newEntity = WholeProject.projectModel.get<Entity>(entity.name) {
                    val newEntity = getFromRepository<Entity>(QualifiedId(entity.name))
                    if (newEntity != null) {
                        mergeEntity(newEntity, newDeps)
                        WholeProject.projectModel.source.entities.add(newEntity)
                    }
                    newEntity
                }
                if (newEntity == null) {
                    // try class
                    WholeProject.projectModel.get<Classs>(entity.name) {
                        var newClass = getFromRepository<Classs>(QualifiedId(entity.name))
                        if (newClass == null) {
                            Log.warning("Missing Entity/Class for reference ${entity.name}; create new class for this link.")
                            newClass = Classs().apply { qid = QualifiedId(entity.name) ; atomic = true }
                        }
                        if (!newClass.atomic) {
                            mergeClass(newClass, newDeps)
                            WholeProject.projectModel.source.classes.add(newClass)
                        } else {
                            WholeProject.projectModel.dependencies.classes.add(newClass)
                        }
                        newClass
                    }
                }
            }
            newDeps = newDeps.subtract(dependencies) as MutableSet<Reference>
            dependencies.removeIf { it.type == Ref.Type.byClass<Entity>() }
            dependencies.addAll(newDeps)
        }

        // merge actions
        WholeProject.projectModel.source.actions.forEach { act -> mergeAction(act, dependencies) }
        while (dependencies.any { it.type == Ref.Type.byClass<Action>() }) {
            var newDeps = mutableSetOf<Reference>()
            dependencies.filter { it.type == Ref.Type.byClass<Action>() }.forEach { action ->
                Log.debug("Merging dependent action ${action.name} ...")
                WholeProject.projectModel.get<Action>(action.name) {
                    var newAction = getFromRepository<Action>(QualifiedId(action.name))
                    if (newAction == null) {
                        Log.warning("Missing Action for reference ${action.name}; create new view for this link.")
                        newAction = Action().apply { qid = QualifiedId(action.name) }
                    }
                    if (newAction.definition != ActionDefinition.Undefined) {
                        mergeAction(newAction, newDeps)
                        WholeProject.projectModel.source.actions.add(newAction)
                    } else {
                        WholeProject.projectModel.dependencies.actions.add(newAction)
                    }
                    newAction
                }
            }
            newDeps = newDeps.subtract(dependencies) as MutableSet<Reference>
            dependencies.removeIf { it.type == Ref.Type.byClass<Action>() }
            dependencies.addAll(newDeps)
        }

        // merge actors
        WholeProject.projectModel.source.actors.forEach { act ->
            // collect reference actors
            mergeActorSourceReferences(act)
        }

        // update default feature references
//        updateFeatureRefDefaultProperties()

        // throw errors
        if (errors.isNotEmpty()) {
            throw ReqMMergeException("Merge failed.", errors)
        }
    }

    /*
    ******** Application merging ********
    */

    private inline fun <reified T: TopElement> addDependency(dependencies: MutableSet<Reference>, name: String) {
        if (WholeProject.projectModel.get<T>(name) == null) {
            dependencies.add(Reference(Ref.Type.byClass<T>(), name))
        }
    }

    private inline fun <reified T: TopElement> addDependency(dependencies: MutableSet<Reference>, names: List<String>) {
        names.forEach { name ->
            addDependency<T>(dependencies, name)
        }
    }


        /**
     * Collect application dependencies from attributes into the dependencies ReqMSource
     *
     * @param app Application to collect dependencies from
     * @param dependencies ReqMSource to collect dependencies into
     */
    private fun collectApplicationDependencies(app: Application, dependencies: MutableSet<Reference>) {
        // dependencies -> modules
        val modulesNames = getPropertyListValues(app.definition.properties, "dependencies")
        addDependency<Modul>(dependencies, modulesNames)
        // events -> action
        val events = getPropertyListValues(app.definition.properties, REQM_GENERAL_ATTRIBUTE_EVENTS)
        addDependency<Action>(dependencies, events)
        // startView -> view
        getPropertyValue(app.definition.properties, "startView")?.let { startView ->
            addDependency<View>(dependencies, startView)
        }
        // defaultTemplate -> view
        getPropertyValue(app.definition.properties, "defaultTemplate")?.let { defaultTemplateName ->
            addDependency<View>(dependencies, defaultTemplateName)
        }
        // defaultStyle -> style
        getPropertyValue(app.definition.properties, FEATURE_TEMPLATE_ATTRIBUTE_DEFAULT_STYLE)?.let { defaultStyleName ->
            addDependency<Style>(dependencies, defaultStyleName)
        }
    }

    /**
     * Merge application with the source reference chain
     *
     * @param app Project application item
     */
    private fun mergeApplicationSourceReferences(app: Application) {
        mergeSourceReferences(app) { app, refApps ->
            refApps.forEach { refApp ->
                if (WholeProject.projectModel.dependencies.applications.none { it.qid.toString()  == refApp.qid.toString() }) {
                    WholeProject.projectModel.dependencies.applications.add(refApp)
                }
            }
            mergeApplicationRef(app, refApps)
        }
    }

    /**
     * Merge referenced application items to the project item
     *
     * @param app Project application item
     * @param refApps List of referenced application items
     */
    private fun mergeApplicationRef(app: Application, refApps: List<Application>) {
        refApps.forEach {
            mergeSourceRefAttributes(app, it)
            if (app.definition == Definition.Undefined && it.definition != Definition.Undefined) {
                app.definition = Definition()
            }
            mergeProperties(app.definition.properties, it.definition.properties)
            mergeFeatureRefs(app.definition.featureRefs, it.definition.featureRefs)
        }
    }

    private fun mergeSourceRefAttributes(elem: TopElement, ref: TopElement) {
        if (elem.sourceRef != QualifiedId.Undefined && elem.sourceRef?.id == ref.qid?.id && elem.sourceRef?.domain.isNullOrBlank() && !ref.qid?.domain.isNullOrBlank()) {
            elem.sourceRef?.domain = ref.qid?.domain
        } else if (elem.sourceRef == QualifiedId.Undefined && ref.sourceRef != QualifiedId.Undefined) {
            elem.sourceRef = ref.sourceRef
        }
    }

    /*
     ******** Module merging ********
     */

    private fun mergeModuleSourceReferences(modRef: Reference) {
        var modulExisting = WholeProject.projectModel.get<Modul>(QualifiedId(modRef.name))
        if (modulExisting == null) {
           modulExisting = getFromRepository(QualifiedId(modRef.name))
            if (modulExisting != null) {
                WholeProject.projectModel.dependencies.modules.add(modulExisting)
                mergeSourceReferences(modulExisting) { mod, refMods ->
                    refMods.forEach { refMod ->
                        if (WholeProject.projectModel.dependencies.modules.none { it.qid.toString() == refMod.qid.toString() }) {
                            WholeProject.projectModel.dependencies.modules.add(refMod)
                        }
                    }
                    mergeModuleRef(mod, refMods)
                }
            } else {
                errors.add("Missing Modul for reference ${modRef.name}; create new module for this link.")
            }
        }
    }

    private fun mergeModuleRef(mod: Modul, refMods: List<Modul>) {
        refMods.forEach {
            mergeSourceRefAttributes(mod, it)
            if (mod.definition == Definition.Undefined && it.definition != Definition.Undefined) {
                mod.definition = Definition()
            }
            mergeProperties(mod.definition.properties, it.definition.properties)
            mergeFeatureRefs(mod.definition.featureRefs, it.definition.featureRefs)
        }
    }

    /*
     ******** View merging ********
     */

    private fun isNormalView(view: View): Boolean {
        if (view.parent != QualifiedId.Undefined) return false
        if (view.sourceRef == QualifiedId.Undefined) return true
        val srcRef = WholeProject.projectModel.dependencies.get<View>(view.sourceRef)
        return srcRef == null || isNormalView(srcRef)
    }

    fun mergeView(view: View, defaultViewTemplate: View?, defaultStyle: Style?, dependencies: MutableSet<Reference>) {
        // merge phase:
        //  merge sourceRef chain
        mergeViewSourceReferences(view)     // merge srcRefs OK
        //  apply features
        collectFeatures(view.definition.featureRefs)    // collect features from feature refs into dependencies OK
        applyDefaultStyleOnView(view, defaultStyle)     // add default style feature if not exists any styles OK
        applyDefaultViewTemplate(view, defaultViewTemplate)     // add default template feature if not exists any templates OK
        //  apply view template
        resolveViewTemplates(view)                              // apply template OK
        //  apply mergeable widgets
        val layout = view.definition.properties.find { it.key == VIEW_ATTRIBUTE_LAYOUT }
        layout?.simpleAttributes?.let { collectViewLayoutDeps(it) } // merge widgets OK
        resolveViewPropertiesInLayout(view)     // merge submenu OK, collect menu links, collect data entities MOVE TO collect part !!!
        //  extract inline styles
        // reference collection phase:
        //  collect styles ?
        //  collect links -> view references
        //  collect data -> entity references
        collectViewLayoutReferences(layout, dependencies)    // collect view references, collect data entities OK
    }

    /**
     * Collect view layout references to layout attributes DATA,TO, menu items
     * @param prop Property to collect references from
     * @param dependencies ReqMSource to collect references into
     */
    private fun collectViewLayoutReferences(prop: Property?, dependencies: MutableSet<Reference> = mutableSetOf()): MutableSet<Reference> {
        if (prop == null || prop.key == null) return dependencies
        when (prop.key) {
            VIEW_LAYOUT_ELEMENT_ATTR_DATA -> {
                addDependency<Entity>(dependencies, prop.value!!)
            }

            VIEW_LAYOUT_ELEMENT_ATTR_TO -> {
                if (prop.value != null && !prop.value!!.startsWith("http:") && !prop.value!!.startsWith("https:")) {
                    addDependency<View>(dependencies, prop.value!!)
                }
            }

            StandardLayoutElements.menu.name -> {
                getPropertyValuesFromTree(prop, exclude=StandardLayoutElements.menu.attributes).forEach { viewName ->
                    addDependency<View>(dependencies, viewName)
                }
            }

            VIEW_LAYOUT_ELEMENT_CONTENT, VIEW_LAYOUT_ELEMENT_STYLE, REQM_GENERAL_ATTRIBUTE_EVENTS -> {
                // do nothing
            }

            else -> {
                if (prop.type == StandardTypes.propertyList.name) {
                    prop.simpleAttributes.forEach { attr -> collectViewLayoutReferences(attr, dependencies) }
                }
            }
        }
        return dependencies
    }

    private fun applyDefaultStyleOnView(view: View, defaultStyle: Style?) {
        if (defaultStyle != null) {
            val noTemplateFeature = view.definition.featureRefs.none { it.qid.toString() == FEATURE_STYLE }
            if (view.parent == QualifiedId.Undefined && noTemplateFeature) {
                view.definition.featureRefs.add(FeatureRef.style(defaultStyle.qid.toString()))
            }
        }
    }

    private fun applyDefaultViewTemplate(view: View, defaultViewTemplate: View?) {
        if (defaultViewTemplate != null) {
            val noTemplateFeature = view.definition.featureRefs.none { it.qid.toString() == FEATURE_TEMPLATE }
            if ((view.parent == QualifiedId.Undefined || view.parent.toString() == "static") && noTemplateFeature) {
                view.definition.featureRefs.add(FeatureRef.template(defaultViewTemplate.qid.toString()))
            }
        }
    }

    /**
     * Get the default template view
     *
     * @return Default template view  or null, if it is not exist, or is not template
     */
    fun getDefaultViewTemplate(): View? {
        var defaultViewTemplateName = getPropertyValue(WholeProject.projectModel.source.applications[0].definition.properties, "defaultTemplate")
        if (defaultViewTemplateName == null) {
            defaultViewTemplateName = ConfigManager.defaults[FEATURE_TEMPLATE_ATTRIBUTE_TEMPLATE_VIEW]
            if (defaultViewTemplateName == null) {
                defaultViewTemplateName = "DefaultTemplate"
            }
        }
        val view = WholeProject.projectModel.source.get<View>(defaultViewTemplateName)
        if (view != null && view.parent.toString() != VIEW_SUBTYPE_TEMPLATE) {
            Log.warning("View '$defaultViewTemplateName' is exists in the project, but is is not template view (${view.coords()})")
            return null
        }
        return view
    }

    private fun resolveViewTemplates(view: View) {
        // get template view name
        val templateViewName = view.definition.featureRefs.find { it.qid.toString() == FEATURE_TEMPLATE }?.let { featureRef ->
            featureRef.properties.find { it.key == FEATURE_TEMPLATE_ATTRIBUTE_TEMPLATE_VIEW }?.value
        }

        // find template view
        if (templateViewName != null && templateViewName != TEMPLATE_NAME_TO_AVOID_DEFAULT) {
            var templateView = WholeProject.projectModel.get<View>(templateViewName)
            if (templateView == null) {
                templateView = getFromRepository<View>(QualifiedId(templateViewName))
            }
            if (templateView != null) {
                collectViewDependencies(templateView)
            }

            // inject view layout into template layout
            if (templateView == null) {
                errors.add("Template view reference $templateViewName is not found.")
            } else if (templateView.parent.toString() != VIEW_SUBTYPE_TEMPLATE) {
                errors.add("Template view $templateViewName is not 'template' type view.")
            } else {
                val templateLayout = templateView.definition.properties.find { it.key == VIEW_ATTRIBUTE_LAYOUT }
                if (templateLayout != null) {
                    var viewLayout = view.definition.properties.find { it.key == VIEW_ATTRIBUTE_LAYOUT }
                    if (viewLayout == null) {
                        viewLayout = Property()
                    }

                    val nodesContainsContent = searchProperties(templateLayout) { p -> p.simpleAttributes.any { it.key == VIEW_LAYOUT_ELEMENT_CONTENT } }
                    if (nodesContainsContent.isNotEmpty()) {
                        val newProperties: MutableList<Property> = mutableListOf()
                        nodesContainsContent[0].simpleAttributes.forEach { ta ->
                            if (ta.key != VIEW_LAYOUT_ELEMENT_CONTENT) {
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

    private fun resolveViewPropertiesInLayout(view: View) {
        view.definition.properties.find { it.key == VIEW_ATTRIBUTE_LAYOUT }?.let { layout ->
            resolveNodeProperties(layout, view.definition.properties)
        }
    }

    private fun mergeViewSourceReferences(view: View) {
        mergeSourceReferences(view) { view, refViews ->
            refViews.forEach { refView ->
                if (WholeProject.projectModel.dependencies.views.none { it.qid.toString()  == refView.qid.toString() }) {
                    WholeProject.projectModel.dependencies.views.add(refView)
                }
                mergeProperties(view.definition.properties, refView.definition.properties)
                mergeFeatureRefs(view.definition.featureRefs, refView.definition.featureRefs)
            }
        }
    }

    private fun mergeFeatureRefs(featureRefs: MutableList<FeatureRef>, featureRefsFrom: List<FeatureRef>) {
        featureRefsFrom.forEach { featureRef ->
            val existingFeatureRef = featureRefs.find { it.qid.toString() == featureRef.qid.toString() }
            if (existingFeatureRef == null) {
                featureRefs.add(featureRef)
            } else {
                mergeProperties(existingFeatureRef.properties, featureRef.properties)
            }
        }
    }

    /*
     * Merge source reference chain, collect layout dependencies, collect features
     */
    private fun collectViewDependencies(view: View) {
        mergeViewSourceReferences(view)

        val layout = view.definition.properties.find { it.key == VIEW_ATTRIBUTE_LAYOUT }
        layout?.simpleAttributes?.let { collectViewLayoutDeps(it) }

        collectFeatures(view.definition.featureRefs)
    }

    @Deprecated("Use collectSourceReferences instead")
    private fun collectViewSources(sourceRef: QualifiedId, optional: Boolean = false) {
        val ic = finder.find(Ref.Type.vie, sourceRef.id!!, sourceRef.domain)
        if (ic.items.isEmpty() && !optional) errors.add("Source reference $sourceRef is not found.  (${sourceRef.coords()})")
        ic.items.forEach {
            if (WholeProject.projectModel.dependencies.views.none { d -> it.name == d.qid.toString() }) {
                val depView = loadElementByIndex<View>(it)
                if (depView != null) {
                    WholeProject.projectModel.dependencies.views.add(depView)
                    if (depView.sourceRef != null && depView.sourceRef != QualifiedId.Undefined) {
                        collectSourceReferences<View>(depView.sourceRef!!)
                    }
                    collectViewDependencies(depView)
                }
            }
        }
    }

    private fun collectViewLayoutDeps(properties: MutableList<Property>, inStdLayoutElement: Boolean = false) {
        properties.forEach { prop ->
            if (!listOf(VIEW_LAYOUT_ELEMENT_CONTENT, VIEW_LAYOUT_ELEMENT_STYLE, REQM_GENERAL_ATTRIBUTE_EVENTS).contains(prop.key)) {
                if (prop.type == StandardTypes.propertyList.name || prop.value == null) {
                    val isStdLayoutElement = StandardLayoutElements.contains(prop.key!!)
                    collectViewSources(QualifiedId(prop.key), true)
                    if (prop.type == StandardTypes.propertyList.name /*&& !StandardLayoutElements.contains(prop.key!!)*/) {
                        collectViewLayoutDeps(prop.simpleAttributes, inStdLayoutElement || isStdLayoutElement)
                    }
                    var depView = WholeProject.projectModel.source.get<View>(prop.key!!)
                    if (depView == null) {
                        depView = WholeProject.projectModel.dependencies.get(prop.key!!)
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
                    } else if (!isStdLayoutElement && !inStdLayoutElement) {
                        Log.warning("View layout element ${prop.key} is undefined (${prop.coords()})")
                    }

                }
            } else {
                // TODO: this is after template merging, no need for this
                // placeholder for template
                if (prop.key == VIEW_LAYOUT_ELEMENT_CONTENT) prop.type = "#"
            }
        }
    }

    private fun resolveNodeProperties(node: Property, properties: MutableList<Property>) {
        node.simpleAttributes.filter { it.type != StandardTypes.propertyList.name  }.forEach { attr ->
            attr.value?.let {
                properties.find { it.key == attr.value }?.let { prop ->
                    attr.value = prop.value
                }
            }
            when (node.key) {
                StandardLayoutElements.menu.name -> {
                    when {
                        attr.value == null && !StandardLayoutElements.menu.attributes.contains(attr.key!!) -> {
                            // TODO: include widgets only, inherited from menu
                            val submenuName = attr.key!!
                            collectViewSources(QualifiedId(submenuName))
                            WholeProject.projectModel.dependencies.get<View>(submenuName)?.let { submenu ->
                                if (/*submenu.parent.toString() == "widget" &&*/ submenu.sourceRef.toString() == StandardLayoutElements.menu.name) {
                                    val submenuItems = submenu.definition.properties.toList()
                                    val menuTitle = submenuItems.find { it.key == "title" }
                                    attr.key = if (menuTitle != null && menuTitle.value != null) NameFormatter.deliterateText(menuTitle.value!!) else submenuName
                                    attr.simpleAttributes.addAll(submenuItems.filter { !StandardLayoutElements.menu.attributes.contains(it.key) })
                                    attr.type = StandardTypes.propertyList.name
                                    attr.value = null
                                }
                            }
                        }
                    }
                }
            }
        }
        node.simpleAttributes.filter { it.type == StandardTypes.propertyList.name  }.forEach { child ->
            resolveNodeProperties(child, properties)
        }
    }

    /*
     ******** Style merging ********
     */

    private fun mergeStyleSourceReferences(style: Style) {
        mergeSourceReferences(style) { style, refStyles ->
            refStyles.forEach { refStyle ->
                if (WholeProject.projectModel.dependencies.styles.none { it.qid.toString()  == refStyle.qid.toString() }) {
                    WholeProject.projectModel.dependencies.styles.add(refStyle)
                }
            }
            mergeStyleRef(style, refStyles)
        }
    }

    private fun mergeStyleRef(style: Style, refStyles: Collection<Style>, overwriteAttributes: Boolean = false, conflictWarning: Boolean = false) {
        refStyles.forEach {
            mergeProperties(style.definition.properties, it.definition.properties, deepMerge = true, overwriteAttributes = overwriteAttributes, conflictWarning = conflictWarning)
            mergeFeatureRefs(style.definition.featureRefs, it.definition.featureRefs)
        }
    }

    // OK
    fun getDefaultStyle(): Style? {
        var defaultStyleName = getPropertyValue(WholeProject.projectModel.source.applications[0].definition.properties, FEATURE_TEMPLATE_ATTRIBUTE_DEFAULT_STYLE)
        if (defaultStyleName == null) {
            defaultStyleName = ConfigManager.defaults[FEATURE_TEMPLATE_ATTRIBUTE_DEFAULT_STYLE]
            if (defaultStyleName == null) {
                defaultStyleName = "DefaultStyle"
            }
        }
        return WholeProject.projectModel.source.get<Style>(defaultStyleName)
    }

    /*
     ******** Entity merging ********
     */

    private fun mergeEntity(ent: Entity, dependencies: MutableSet<Reference>) {
        // collect reference entities
        if (ent.sourceRef != null && ent.sourceRef != QualifiedId.Undefined) {
            val ents = collectSourceReferences<Entity>(ent.sourceRef!!)
            if (ents.isNotEmpty()) {
                WholeProject.projectModel.dependencies.entities.addAll(ents)
                mergeEntityRef(ent, ents)
            } else {
                errors.add("Source reference ${ent.sourceRef} is not found for entity ${ent.qid} (${ent.coords()}). (${ent.coords()})")
            }
        }
        // apply features on the entity
        ent.definition.featureRefs.forEach { featureRef ->
            resolveFeatureRef(featureRef).forEach { feature ->
                mergeProperties(ent.definition.properties, feature.definition.properties.filter { it.key != REQM_GENERAL_ATTRIBUTE_GENERATOR })
            }
        }
        // collect property types as classes or entities
        ent.definition.properties.forEach { p -> collectPropertyTypes(p, dependencies) }
    }

    private fun mergeEntityRef(ent: Entity, refEnts: List<Entity>) {
        refEnts.forEach {
            mergeProperties(ent.definition.properties, it.definition.properties)
            mergeFeatureRefs(ent.definition.featureRefs, it.definition.featureRefs)
        }
    }

    /*
     ******** Class merging ********
     */

    private fun mergeClass(cls: Classs, dependencies: MutableSet<Reference>) {
        if (cls.sourceRef != null && cls.sourceRef != QualifiedId.Undefined) {
            val clss = collectSourceReferences<Classs>(cls.sourceRef!!)
            if (clss.isNotEmpty()) {
                WholeProject.projectModel.dependencies.classes.addAll(clss)
                // TODO: sort the list by score
                mergeClassRef(cls, clss)
            } else {
                errors.add("Source reference ${cls.sourceRef} is not found for class ${cls.qid} (${cls.coords()}). (${cls.coords()})")
            }
        }
        // collect property types as classes or entities
        if (!cls.enumeration) cls.definition.properties.forEach { p -> collectPropertyTypes(p, dependencies) }
    }

    private fun mergeClassRef(cls: Classs, refClss: List<Classs>) {
        refClss.forEach {
            mergeProperties(cls.definition.properties, it.definition.properties)
            mergeFeatureRefs(cls.definition.featureRefs, it.definition.featureRefs)
        }
    }

    /*
     ******** Action merging ********
     */

    private fun mergeAction(act: Action, dependencies: MutableSet<Reference>) {
        // collect dependent actions
        act.definition.actionCalls.forEach {
            addDependency<Action>(dependencies, it.actionName)
//            val actdep = collectActionSources(it.actionName)
//            WholeProject.projectModel.dependencies.actions.addAll(actdep)
        }
    }

    @Deprecated("Use collectSourceReferences instead")
    private fun collectActionSources(actionName: String): Collection<Action> {
        val actions : MutableList<Action> = ArrayList()
        val act = getFromRepository<Action>(QualifiedId(actionName))
        if (act != null) {
            act.increaseRefCount()
            actions.add(act)
        } else {
            errors.add("Action '$actionName' not found in Repository")
        }
        return actions
    }

    /*
     ******** Actor merging ********
     */

    private fun mergeActorSourceReferences(act: Actor) {
        mergeSourceReferences(act) { actor, refActors ->
            refActors.forEach { refActor ->
                if (WholeProject.projectModel.dependencies.actors.none { it.qid.toString()  == refActor.qid.toString() }) {
                    WholeProject.projectModel.dependencies.actors.add(refActor)
                }
                mergeProperties(actor.definition.properties, refActor.definition.properties)
            }
        }
    }

    /*
     ******** Common merging methods ********
     */

    /*
     * Merge source references for given element
     *
     * @param elem Element with sourceRef to be merged
     * @param block Merging block with element and list of referenced elements
     */
    // OK
    private inline fun <reified T : TopElement> mergeSourceReferences(elem: T, block: (T, List<T>) -> Unit) {
        if (elem.sourceRef != null && elem.sourceRef != QualifiedId.Undefined) {
            val srcRefs = collectSourceReferences<T>(elem.sourceRef!!)
            if (srcRefs.isNotEmpty()) {
                block(elem, srcRefs)
            } else {
                errors.add("Source reference ${elem.sourceRef} is not found for ${T::class.simpleName} ${elem.qid} (${elem.coords()}).")
            }
        }
    }

    private fun collectPropertyTypes(p: Property, dependencies: MutableSet<Reference>) {
        if (p.type != null && !StandardTypes.has(p.type!!)) {
            addDependency<Entity>(dependencies, p.type!!)
        }
    }

    private fun updateFeatureRefDefault(featureRefs: List<FeatureRef>) {
        featureRefs.forEach { ft ->
            val featureName = ft.qid.toString()
            ft.properties.forEach { ftprop ->
                if (ftprop.key == REQM_GENERAL_ATTRIBUTE_FEATURE_REFERENCE) {
                    WholeProject.projectModel.get<Feature>(featureName)?.let { feature ->
                        val defaultProperty = feature.definition.properties.getOrNull(0)
                        if (defaultProperty != null && defaultProperty.key != "generator") {
                            ftprop.key = defaultProperty.key
                        }
                    }
                }
            }
        }
    }

    // OK
    private fun searchProperties(node: Property, expr: (Property) -> Boolean): List<Property> {
        val nodeList = mutableListOf<Property>()
        if (expr(node)) nodeList.add(node)
        node.simpleAttributes.forEach { child ->
            nodeList.addAll(searchProperties(child, expr))
        }
        return nodeList
    }

    private fun resolveFeatureRef(featureRef: FeatureRef): List<Feature> {
        val featureList = mutableListOf<Feature>()
        var feature = WholeProject.projectModel.get<Feature>(featureRef.qid)
        if (feature == null) {
            val ic = finder.find(Ref.Type.byClass<Feature>(), featureRef.qid.id!!, featureRef.qid.domain)
            if (ic.items.isEmpty()) errors.add("Feature reference $featureRef is not found.  (${featureRef.coords()})")
            var ix = 0
            while (ix < ic.items.size) {
                val item = ic.items[ix]
                Log.debug("${item.itemType} ${item.name} in ${item.filename}")
                feature = loadElementByIndex<Feature>(item)
                if (feature != null) {
                    feature.increaseRefCount()
                    WholeProject.projectModel.dependencies.features.add(feature)
                    addDependencyToList(feature.sourceRef, Ref.Type.ftr, ic)
                    featureList.add(feature)
                }
                ix++
            }
        } else {
            feature.increaseRefCount()
            if (!featureList.contains(feature)) featureList.add(feature)
        }
        return featureList
    }

    private fun collectFeatures(featureRefs: List<FeatureRef>) {
        featureRefs.forEach { featureRef ->
            resolveFeatureRef(featureRef)
        }
        updateFeatureRefDefault(featureRefs)
    }

    // OK
    private fun parseFile(filename: String): ReqMSource {
        val actReqmSource = ReqMSource()
        val ok = parser.parseReqMTree(filename, actReqmSource)
        if (!ok) throw ReqMParsingException("Parsing errors found during merge; see previous errors")
        return actReqmSource
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

    /**
     * Collect source reference chain for given element type
     *
     * @param ref Source reference QualifiedId
     * @return List of collected source reference elements
     */
    // OK
    private inline fun <reified T: TopElement> collectSourceReferences(ref: QualifiedId): List<T> {
        val elements = mutableListOf<T>()
        var refId: QualifiedId = ref
        while (refId != QualifiedId.Undefined) {
            var srcRef = WholeProject.projectModel.get<T>(refId)
            if (srcRef == null) {
                srcRef = getFromRepository<T>(refId)
            }
            if (srcRef != null) {
                srcRef.increaseRefCount()
                elements.add(srcRef)
            }
            refId = srcRef?.sourceRef ?: QualifiedId.Undefined
        }
        return elements
    }

    /**
     * Load element from source by repository index
     *
     * @param item Repository index item
     * @return Loaded element or null if not found
     */
    // OK
    private inline fun <reified T: ElementBase> loadElementByIndex(item: ItemCollection.ItemCollectionItem): T? {
        val reqmSource = parseFile(item.filename!!)
        return reqmSource.get<T>(item.name)
    }

    /**
     * Get element from repository
     *
     * @param ref QualifiedId of the element
     * @return Loaded element or null if not found
     */
    // OK
    private inline fun <reified T: ElementBase> getFromRepository(ref: QualifiedId): T? {
        val ic = finder.find(Ref.Type.byClass<T>(), ref.id!!, ref.domain)
        if (ic.items.isEmpty()) {
            return null
        }
        return loadElementByIndex<T>(ic.items[0])
    }

    /**
     * Merge source item properties into destination one
     *
     * @param destPropList Destination (project) item properties
     * @param srcPropList Source (referenced) item properties
     */
    private fun mergeProperties(destPropList: MutableList<Property>, srcPropList: List<Property>, deepMerge: Boolean = false, overwriteAttributes: Boolean = false, conflictWarning: Boolean = false) {
        srcPropList.forEach { d ->
            val p = destPropList.find { it.key == d.key }
            if (p != null) {
                if (d.value != null && d.value!!.isNotBlank()) {
                    if (p.value.isNullOrBlank()) {
                        p.value = d.value
                    } else {
                        if (p.value != d.value) {
                            if (overwriteAttributes) {
                                p.value = d.value
                            } else if (conflictWarning) {
                                Log.warning("Merge conflict: property '${d.key}' has different values ('${p.value}' and '${d.value}'); keeping the first value. (${p.coords()} | ${d.coords()})")
                            }
                        }
                    }
                }
                if (p.type == null) {
                    p.type = d.type
                    p.listOf = d.listOf
                }
                if (p.optionality == Optionality.Undefined.name) {
                    p.optionality = d.optionality
                }
                if (deepMerge && p.type == StandardTypes.propertyList.name) {
                    mergeProperties(p.simpleAttributes, d.simpleAttributes, true)
                }
            } else {
                destPropList.add(d.clone())
            }
        }
    }

    // OK
    private fun getPropertyValue(properties: List<Property>, propertyName: String): String? {
        properties.find { it.key == propertyName }?.let {
            return it.value
        }
        return null
    }

    // OK
    private fun getPropertyListValues(properties: List<Property>, propertyName: String): List<String> {
        properties.find { it.key == propertyName }?.let {
            return it.simpleAttributes.mapNotNull { a -> if (a.value != null) a.value!! else a.key }
        }
        return listOf()
    }

    // OK
    private fun getPropertyValuesFromTree(prop: Property, exclude: List<String>, result: MutableList<String> = mutableListOf()): List<String> {
        prop.simpleAttributes.forEach { attr ->
            if (!exclude.contains(attr.key!!)) {
                if (attr.value != null) {
                    result.add(attr.value!!)
                } else if (attr.type == StandardTypes.propertyList.name) {
                    getPropertyValuesFromTree(attr, exclude, result)
                }
            }
        }
        return result
    }

    /**
     * Get element by name from project model (source or dependencies), or from the repository
     *
     * @param elemName Element name
     * @return Element or null if not found
     */
    // OK
    @Deprecated("It is not used")
    private inline fun <reified T: TopElement> getElementByName(elemName: String): T? {
        return  WholeProject.projectModel.get<T>(QualifiedId(elemName)) {
            getFromRepository(QualifiedId(elemName))
        }
    }

    /**
     * Merge multiple model element instances in the project model
     */
    fun mergeMultipleModelElementInstances() {
        // merge multiple application items
        // error conditions:
        // - multiple srcRefs exist
        // - attributes and features with different values, types
        if (WholeProject.projectModel.source.applications.size > 1) {
            val pivot = WholeProject.projectModel.source.applications[0]
            val mergeableApps = WholeProject.projectModel.source.applications.subList(1, WholeProject.projectModel.source.applications.size).toList()
            mergeApplicationRef(pivot, mergeableApps)
            WholeProject.projectModel.source.applications.removeAll(mergeableApps)
            Log.info("Merged ${mergeableApps.size} duplicate application definitions of '${pivot.qid}'.")
        }

        // merge multiple styles
        val mergeableStyles = WholeProject.projectModel.source.styles.groupingBy { it.qid.toString() }.eachCount().filter { it.value > 1 }.keys
        mergeableStyles.forEach { styleName ->
            val stylesToMerge = WholeProject.projectModel.source.styles.filter { it.qid.toString() == styleName }
            val pivot = stylesToMerge[0]
            val rest = stylesToMerge.subList(1, stylesToMerge.size).toList()
            mergeStyleRef(pivot, rest, conflictWarning = true)
            WholeProject.projectModel.source.styles.removeAll(rest)
            Log.info("Merged ${rest.size} duplicate style definitions of '$styleName'.")
        }

        // merge default style
        val defaultStyleName = ConfigManager.defaults[FEATURE_TEMPLATE_ATTRIBUTE_DEFAULT_STYLE] ?: "DefaultStyle"
        val defaultStyle = WholeProject.projectModel.source.get<Style>(QualifiedId(defaultStyleName))

        val appDefaultStyleProperty = WholeProject.projectModel.source.applications[0].definition.properties.find { it.key == FEATURE_TEMPLATE_ATTRIBUTE_DEFAULT_STYLE }
        val appDefaultStyleName = appDefaultStyleProperty?.value
        if (appDefaultStyleName != null) {
            val appDefaultStyle = WholeProject.projectModel.source.get<Style>(QualifiedId(appDefaultStyleName))
            if (appDefaultStyle != null && defaultStyle != null) {
                mergeStyleRef(defaultStyle, listOf(appDefaultStyle), overwriteAttributes = true, conflictWarning = true)
                WholeProject.projectModel.source.styles.remove(appDefaultStyle)
                appDefaultStyleProperty.value = defaultStyleName
                Log.info("Merged application default style '$appDefaultStyleName' into project default style '$defaultStyleName'.")
            }
        }

        // TODO: merge multiple views, entities, classes, actors, actions, features

        // TODO: merge default view templates

    }

}