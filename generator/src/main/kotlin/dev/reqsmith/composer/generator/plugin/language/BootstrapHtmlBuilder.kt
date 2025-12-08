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

package dev.reqsmith.composer.generator.plugin.language

import dev.reqsmith.composer.common.ART_FOLDER_NAME
import dev.reqsmith.composer.common.WholeProject
import dev.reqsmith.composer.common.formatter.NameFormatter
import dev.reqsmith.composer.common.plugin.PluginDef
import dev.reqsmith.composer.common.plugin.PluginType
import dev.reqsmith.composer.common.resource.Resource
import dev.reqsmith.composer.common.resource.ResourceManager
import dev.reqsmith.composer.common.resource.ResourceSourceType
import dev.reqsmith.model.enumeration.StandardEvents
import dev.reqsmith.model.enumeration.StandardLayoutElements
import dev.reqsmith.model.enumeration.StandardStyleElements
import dev.reqsmith.model.enumeration.VIEW_LAYOUT_ELEMENT_ATTR_DATA
import dev.reqsmith.model.enumeration.VIEW_LAYOUT_ELEMENT_ATTR_TITLE
import dev.reqsmith.model.enumeration.VIEW_LAYOUT_ELEMENT_ATTR_TO
import dev.reqsmith.model.igm.IGMClass
import dev.reqsmith.model.igm.IGMView
import kotlinx.html.*
import kotlinx.html.stream.createHTML
import kotlin.math.max
import kotlin.text.isNullOrBlank
import kotlin.text.startsWith

private const val FORM_COLUMN_SIZE_CLS = "col-md-6"

private const val ALIGN_ITEMS_CENTER = "align-items-center"

class BootstrapHtmlBuilder: HtmlBuilder() {

    private var currentViewStyleRef: String? = null

    override fun definition() = PluginDef("html.bootstrap", PluginType.Language)

    override fun createView(view: IGMView): String {
        view.imports.addAll(listOf(
            "https://cdn.jsdelivr.net/npm/bootstrap@5.3.0/dist/css/bootstrap.min.css",
            "https://cdn.jsdelivr.net/npm/bootstrap-icons@1.11.3/font/bootstrap-icons.min.css",
            "https://cdn.jsdelivr.net/npm/bootstrap@5.3.0/dist/js/bootstrap.bundle.min.js",
            // for date control
//            "https://cdnjs.cloudflare.com/ajax/libs/bootstrap-datepicker/1.9.0/css/bootstrap-datepicker.min.css",
//            "https://code.jquery.com/jquery-3.6.0.min.js",
//            "https://cdnjs.cloudflare.com/ajax/libs/bootstrap-datepicker/1.9.0/js/bootstrap-datepicker.min.js"
        ))

        currentViewStyleRef = view.styleRef

        return super.createView(view)
    }

    fun getBootstrapImageResource(resourceName: String): String {
        return if (resourceName.all { it.isLetter() }) resourceName else ""
    }

    override fun createImage(node: IGMView.IGMNode): String {
        val attr = node.attributes.toMap()
        return createHTML(true).div {
            collectViewLayoutElementStyles(node).takeIf { it.isNotEmpty() }?.let { classes = it }
            val image: Resource = ResourceManager.getImageResource(attr["src"] ?: "")
            img {
                src = image.name
                attr["alt"]?.let { alt = it }
                if (attr.containsKey("size")) {
                    parseSize(attr["size"] ?: "")?.let {
                        if (it.width > 0) {
                            width = "${it.width}${it.unit}"
                        }
                        if (it.height > 0) {
                            height = "${it.height}${it.unit}"
                        }
                    }
                }
                if (image.source == ResourceSourceType.PROJECT) {
                    val imageName = if (image.name.startsWith("$ART_FOLDER_NAME/")) { image.name.substringAfter("$ART_FOLDER_NAME/") } else { image.name }
                    viewArts.add(imageName)
                } else if (image.source == ResourceSourceType.EXTERNAL && image.localPath.isNotBlank()) {
                    viewArts.add(image.localPath)
                }
            }
        }
    }

    data class Size(val width: Int, val height: Int, val unit: String)

    /**
     * Parse size notation
     *
     * TODO: move this function along with Size class into a general helper class
     */
    fun parseSize(input: String): Size? {
        if (input.isBlank()) return null

        val trimmed = input.trim().lowercase()

        // Csak egy szám (pl. "16")
        val singleNumber = Regex("""^(\d+)$""")
        singleNumber.matchEntire(trimmed)?.let {
            val n = it.groupValues[1].toInt()
            return Size(n, n, "px")
        }

        // Egy szám "px"-szel (pl. "16px")
        val singlePx = Regex("""^(\d+)px$""")
        singlePx.matchEntire(trimmed)?.let {
            val n = it.groupValues[1].toInt()
            return Size(n, n, "px")
        }

        // Két szám "x"-szel (pl. "16x16", "16x16px")
        val double = Regex("""^(\d+)[x×](\d+)(px)?$""")
        double.matchEntire(trimmed)?.let {
            val w = it.groupValues[1].toInt()
            val h = it.groupValues[2].toInt()
            return Size(w, h, "px")
        }

        // Csak szélesség (pl. "w=100" vagy "w=100px")
        val widthOnly = Regex("""^w\s*\s*(\d+)(px)?$""")
        widthOnly.matchEntire(trimmed)?.let {
            val w = it.groupValues[1].toInt()
            return Size(w, 0, "px")
        }

        // Csak magasság (pl. "h=200" vagy "h=200px")
        val heightOnly = Regex("""^h\s*\s*(\d+)(px)?$""")
        heightOnly.matchEntire(trimmed)?.let {
            val h = it.groupValues[1].toInt()
            return Size(0, h, "px")
        }

        // Mindkettő (pl. "w=100,h=200" vagy "w=100px,h=200px")
        val both = Regex("""^w\s*\s*(\d+)(px)?\s*,\s*h\s*\s*(\d+)(px)?$""")
        both.matchEntire(trimmed)?.let {
            val w = it.groupValues[1].toInt()
            val h = it.groupValues[3].toInt()
            return Size(w, h, "px")
        }

        return null
    }

    private fun addIconResource(resourceName: String): String {
        val iconResource = ResourceManager.getImageResource(resourceName) { getBootstrapImageResource(it) }
        return when (iconResource.source) {
            ResourceSourceType.FRAMEWORK -> {
                createHTML(true).i {
                    classes = setOf("bi", "bi-${iconResource.name}", "me-2")
                }
            }
            ResourceSourceType.EXTERNAL -> {
                createHTML(true).img {
                    src = iconResource.name
                }
            }
            else -> {
                createHTML(true).img {
                    src = iconResource.name
                    viewArts.add(iconResource.name)
                }
            }
        }
    }

    override fun createButton(node: IGMView.IGMNode): String {
        val attr = node.attributes.toMap()
        return createHTML(true).a {
            href = checkLink(attr[VIEW_LAYOUT_ELEMENT_ATTR_TO])
            button {
                type = ButtonType.button
                classes = mutableSetOf("btn", "btn-primary").apply {
                    // calculate color class
                    // add(calculateTextColor(node.styleRef, "btn-primary"))
                    addAll(collectViewLayoutElementStyles(node))
                }
                if (attr.contains("icon")) {
                    unsafe { raw(addIconResource(attr["icon"] ?: "")) }
                }
                text(attr["title"] ?: "Button")
            }
        }
    }

    override fun createMenu(node: IGMView.IGMNode): String {
        val attr = node.attributes.toMap()
        return createHTML(true).div {
            text("[menu spaceholder]")
        }
    }

    override fun createNavigation(node: IGMView.IGMNode): String {
        val attr = node.attributes.toMap()
        return createHTML(true).nav {
            classes = mutableSetOf("navbar", "navbar-expand-md").apply {
                add(calculateBackgroundColor(node.styleRef, "bg-secondary-subtle"))
                addAll(collectViewLayoutElementStyles(node))
            }
            attributes["data-bs-theme"] = "light" // TODO: összhangba kell hozni a bg colorral
            div {
                classes = mutableSetOf("container")
                // title
                val navTitle = attr["title"]
                if (!navTitle.isNullOrBlank()) {
                    a { classes = setOf("navbar-brand"); href = "#"; text(navTitle) }
                }
                // home
                // breadcrumb
                // menu
                node.children.find { it.name == "menu" }?.let { menu ->
                    val menuTitle = menu.attributes.find { it.first == VIEW_LAYOUT_ELEMENT_ATTR_TITLE }?.second
                    if (navTitle.isNullOrBlank() && !menuTitle.isNullOrBlank()) {
                        val startPage = WholeProject.projectModel.getStartPage()
                        a { classes = setOf("navbar-brand"); href = checkFormLink(startPage); text(menuTitle) }
                    }
//                    button {
//                        classes = setOf("navbar-toggler")
//                        type = ButtonType.button
//                        attributes["data-bs-toggle"] = "collapse"
//                        attributes["data-bs-target"] = "#navbarNav"
//                        attributes["aria-expanded"] = "false"
//                        attributes["aria-label"] = "Toggle navigation"
//                        span { classes = setOf("navbar-toggler-icon") }
//                    }
                    div {
                        classes = setOf("collapse", "navbar-collapse")
//                        id = "navbarNav"
                        ul {
                            classes = setOf("navbar-nav")
                            menu.children.filter { !StandardLayoutElements.menu.attributes.contains(it.name) }.forEach { menuItem ->
                                if (menuItem.children.isEmpty()) {
                                    li {
                                        classes = setOf("nav-item")
                                        val link = menuItem.text.ifBlank { menuItem.attributes.find { c -> c.first == "default" }?.second ?: "#" }
                                        val linkText = NameFormatter.toDisplayText(menuItem.name.replace("_", " "))
                                        a { href = checkFormLink(link); classes = setOf("nav-link"); text(linkText) }
                                    }
                                } else {
                                    li {
                                        classes = setOf("nav-item", "dropdown")
                                        val linkText = menuItem.name.replace("_", " ")
                                        a {
                                            href = "#"
                                            classes = setOf("nav-link", "dropdown-toggle")
                                            role = "button"
                                            attributes["data-bs-toggle"] = "dropdown"
                                            attributes["aria-expanded"] = "false"
                                            text(linkText)
                                        }
                                        ul {
                                            classes = setOf("dropdown-menu")
                                            menuItem.children.forEach { subitem ->
                                                li {
                                                    when (subitem.name) {
                                                        "spacer" -> {
                                                            hr { classes = setOf("dropdown-divider") }
                                                        }
                                                        else -> {
                                                            classes = setOf("dropdown-item")
                                                            val link = subitem.text.ifBlank { subitem.attributes.find { c -> c.first == "default" }?.second ?: "#" }
                                                            val linkText = NameFormatter.toDisplayText(subitem.name.replace("_", " "))
                                                            a { href = checkFormLink(link); classes = setOf("nav-link"); text(linkText) }
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }

                            }
                        }
                    }
                }
                // search
                // select
                // cta
            }
        }
    }

    private fun calculateBackgroundColor(styleRef: String?, defaultBackgroundClass: String = ""): String {
        if (!styleRef.isNullOrBlank()) {
            // check if the background attribute exists in style class
            WholeProject.projectModel.igm.styles[styleRef]?.let { style ->
                if (style.attributes.any { it.key == StandardStyleElements.background.name}) {
                    return styleRef.lowercase()
                }
            }
        }
        return defaultBackgroundClass
    }

    override fun createHeader(node: IGMView.IGMNode): String {
        val attr = node.attributes.toMap()
        return createHTML(true).header {
            // apply style
            classes = mutableSetOf("py-3").apply {
                add(calculateBackgroundColor(node.styleRef, "bg-light"))
                addAll(collectViewLayoutElementStyles(node))
            }

            div { classes = setOf("container", "d-flex", ALIGN_ITEMS_CENTER)
                if (attr.contains("logo")) {
                    img { src = "$ART_FOLDER_NAME/${attr["logo"]}"; alt = "logo"; classes = setOf("me-3"); style = "height: 128px;"
                        viewArts.add(attr["logo"] ?: "")
                    }
                }
                if (attr.contains("title")) {
                    h1 { classes = setOf("m-0", "fs-4"); text(attr["title"] ?: "(Title comes here)") }
                }
            }
        }
    }

    override fun typeMapper(type: String?): String {
        TODO("Not yet implemented")
    }

    override fun collectBuildScriptElement(buildScriptUpdates: Map<String, MutableList<String>>) {
        TODO("Not yet implemented")
    }

    override fun createText(node: IGMView.IGMNode): String {
        return createHTML(true).p {
            // apply style
            collectViewLayoutElementStyles(node).takeIf { it.isNotEmpty() }?.let { classes = it }

            // write multiline text
            node.getAttr("label")?.let { t ->
                val texts = t.split("\\n")
                text(texts[0])
                if (texts.size > 1) {
                    texts.subList(1, texts.size).forEach {
                        br()
                        text(it)
                    }
                }
            }
        }
    }

    private fun collectViewLayoutElementStyles(node: IGMView.IGMNode): MutableSet<String> {
        val allStyleClasses = mutableSetOf<String>()
        if (!node.styleRef.isNullOrBlank()) {
            allStyleClasses.add(node.styleRef!!.lowercase())
        }
        val cvsr = if (!node.styleRef.isNullOrBlank()) node.styleRef!!.lowercase() else currentViewStyleRef?.lowercase()
        if (cvsr != null) {
            findViewLayoutElementStyle(cvsr, node.name.lowercase())?.let {
                allStyleClasses.add(it)
            }
        }
        return allStyleClasses
    }

    private fun findViewLayoutElementStyle(viewStyleRef: String, elementName: String): String? {
        val elementStyleClass = "$viewStyleRef-$elementName"
        return if (WholeProject.generatorData.availableStyleClasses.contains(elementStyleClass)) elementStyleClass else null
    }

    private fun checkFormLink(link: String?): String {
        val checkedLink = checkLink(link)
        return if (link != null && isFormView(link)) "$checkedLink?id=0" else checkedLink
    }

    private fun checkLink(link: String?): String {
        return when {
            link.isNullOrBlank() -> "#"
            link.startsWith("http") -> link
            existingView(link) -> "$link.html"
            else -> "#"
        }
    }

    override fun createLinkGroup(node: IGMView.IGMNode): String {
        val groupTitle = node.attributes.find { it.first == "title" }?.second ?: "Links"
        val colSize = node.attributes.find { it.first == "colSize" }?.second ?: "6"
        return createHTML(true).div {
            classes = mutableSetOf("col-md-$colSize", "mb-3", "mb-md-0").apply {
                addAll(collectViewLayoutElementStyles(node))
            }
            h5 { text(groupTitle) }
            ul { classes = setOf("list-unstyled")
                node.attributes.filter { it.first == VIEW_LAYOUT_ELEMENT_ATTR_TO }.forEach { link ->
                    val linkText = link.second.replace("_", " ")
                    li {
                        a { href = checkLink(link.second); classes = setOf("text-white", "text-decoration-none"); text(linkText) }
                    }
                }
            }
        }
    }

    override fun createLinkButton(node: IGMView.IGMNode): String {
        val attr = node.attributes.toMap()
        return createHTML(true).a {
            classes = mutableSetOf("btn").apply {
                // calculate color class
                // add(calculateTextColor(node.styleRef, "btn-primary"))
                add("btn-primary")
                addAll(collectViewLayoutElementStyles(node))
            }
            role = "button"
            href = checkLink(attr[VIEW_LAYOUT_ELEMENT_ATTR_TO])
            if (attr.contains("icon")) {
                unsafe { raw(addIconResource(attr["icon"] ?: "")) }
            }
            text(attr["title"] ?: "LinkButton")
        }
    }

    override fun createSpacer(node: IGMView.IGMNode): String {
        val default = node.attributes.find { it.first == "default" }?.second ?: ""
        return when (default) {
            "line" -> createHTML(true).hr {
            }
            else -> createHTML(true).div {
                classes = setOf("pb-3")
            }
        }
    }

    override fun createPanel(node: IGMView.IGMNode): String {
        val attr = node.attributes.toMap()
        return createHTML(true).div {
            classes = mutableSetOf("container", "mt-4").apply {
                add(calculateBackgroundColor(node.styleRef))
                addAll(collectViewLayoutElementStyles(node))
                attr["align"]?.let {
                    add("text-$it")
                }
            }
            node.children.forEach {
                unsafe { raw(createNode(it)) }
            }
        }
    }

    override fun createFooter(node: IGMView.IGMNode): String {
        val attr = node.attributes.toMap()
        return createHTML(true).footer {
            classes = mutableSetOf("text-white", "py-4", "mt-5", "fixed-bottom").apply {
                add(calculateBackgroundColor(node.styleRef, "bg-dark"))
                addAll(collectViewLayoutElementStyles(node))
            }

            div { classes = setOf("container")
                div { classes = setOf("row")

                    // footer link groups
                    val linkGroups = node.children.filter { it.name == "linkGroup" }
                    val ngroups = linkGroups.size
                    val colSize = max(6 / ngroups, 1)
                    linkGroups.forEach { linkGroup ->
                        linkGroup.attributes.add(Pair("colSize", colSize.toString()))
                        unsafe { raw(createLinkGroup(linkGroup)) }
                    }

                    //
                    div { classes = setOf("col-md-${12-colSize*ngroups}", "text-md-end")

                        // footer info (copyright)
                        val copyrightList = node.attributes.filter { it.first == "copyrightText" }
                        copyrightList.forEach {
                            p { classes = setOf("mb-0")
                                val copyText = if (attr.containsKey(it.second)) attr.getOrDefault(it.second, it.second) else it.second
                                text(copyText)
                            }
                        }

                        // social media icons
                        val socialMediaLinks = node.attributes.filter { it.first.endsWith("Link") }
                        socialMediaLinks.let { links ->
                            div { classes = setOf("mt-3")
                                listOf("facebook", "twitter", "linkedin", "youtube", "github").forEach { media ->
                                    val linkValue = links.find { it.first == "${media}Link" }?.second
                                    val mediaClass = "bi-${media}"
                                    a { classes = setOf("me-3")
                                        if (!linkValue.isNullOrBlank()) {
                                            href = linkValue
                                        }
                                        i { classes = setOf("bi", mediaClass); style = "font-size:1.5rem;color:white;" }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    override fun createForm(node: IGMView.IGMNode): String {
        val attr = node.attributes.toMap()
        val entityName = attr[VIEW_LAYOUT_ELEMENT_ATTR_DATA] ?: ""
        val entityVar = entityName.replaceFirstChar { it.lowercase() }
        val title = attr["title"] ?: "Add new $entityName"

        // collect entity members
        val entity = getEntity(entityName)

        return createHTML(true).div {
            classes = mutableSetOf("container-sm", ALIGN_ITEMS_CENTER, FORM_COLUMN_SIZE_CLS).apply {
                addAll(collectViewLayoutElementStyles(node))
            }

            h5 { classes = setOf("mb-3"); text(title) }
            form {
                action = "/data/${entityName.lowercase()}/${StandardEvents.submitForm}"
                method = FormMethod.post
                with(templateBuilder.htmlAttribute("form", "form_object", entityVar)) {
                    attributes[first] = second
                }

                entity.members.filter { it.memberId != "id" }.forEach { member ->

                    val enumList = getEnumeration(member.type)
                    // control types: text, select, check, radio, range, date
                    val controlType = when (member.type) {
                        "String" -> "text"
                        "Int", "Long" -> "text"
                        "Boolean" -> "checkbox"
                        "Date" -> "date"
                        else -> {
                            if (enumList.isNotEmpty()) {
                                "select"
                            } else {
                                "text"
                            }
                        }
                    }

                    div { classes = setOf("row", "mb-3")
                        label { htmlFor = member.memberId; classes = setOf(FORM_COLUMN_SIZE_CLS, "col-form-label"); text("${NameFormatter.toDisplayText(member.memberId)}:") }
                        when (controlType) {
                            "text" -> {
                                div {
                                    classes = setOf(FORM_COLUMN_SIZE_CLS)
                                    input { id = member.memberId; type = InputType.text; name = member.memberId; classes = setOf("form-control")
                                        with(templateBuilder.htmlAttribute("input", "form_member", member.memberId)) {
                                            attributes[first] = second
                                        }
//                                        value = memberType
                                    }
                                }
                            }
                            "select" -> {
                                div {
                                    classes = setOf(FORM_COLUMN_SIZE_CLS)
                                    select {
                                        id = member.memberId; name = member.memberId; classes = setOf("form-select")
                                        with(templateBuilder.htmlAttribute("select", "form_member", member.memberId)) {
                                            attributes[first] = second
                                        }
                                        option { value = ""; selected = true; text("Select...")  }
                                        enumList.forEach {
                                            option { value = it; text(NameFormatter.toDisplayText(it)) }
                                        }
                                    }
                                }
                            }
                            "checkbox" -> {
                                div {
                                    classes = setOf(FORM_COLUMN_SIZE_CLS)
                                    input {
                                        id = member.memberId; type = InputType.checkBox; name = member.memberId; classes = setOf("form-check-input")
                                        with(templateBuilder.htmlAttribute("input", "form_member", member.memberId)) {
                                            attributes[first] = second
                                        }
                                    }
                                }
                            }
                            "date" -> {
                                div {
                                    classes = setOf(FORM_COLUMN_SIZE_CLS)
                                    input {
                                        id = member.memberId; type = InputType.date; name = member.memberId; classes = setOf("form-control")
                                        with(templateBuilder.htmlAttribute("input", "form_member", member.memberId)) {
                                            attributes[first] = second
                                        }
                                    }
                                }
                            }
                            else -> {
                                throw IllegalArgumentException("Unknown control type: $controlType")
                            }
                        }
                    }

                }
                input { id = "id"; type = InputType.hidden; name = "id"
                    with(templateBuilder.htmlAttribute("input", "value", "id")) {
                        attributes[first] = second
                    }
                }
                button { classes = setOf("btn", "btn-primary"); type = ButtonType.submit; text("Submit") }
                button { classes = setOf("btn", "btn-outline-secondary", "ms-3"); type = ButtonType.reset; text("Reset") }
            }
        }
    }

    override fun createDatatable(node: IGMView.IGMNode): String {
        // get node attributes
        val attr = node.attributes.toMap()
        val title = attr["title"] ?: "Data Table"
        val entityName = attr[VIEW_LAYOUT_ELEMENT_ATTR_DATA] ?: ""
        val createForm = attr["createForm"] ?: "${entityName}Form.html"
        // collect entity members
        val entity = getEntity(entityName)
        return createHTML(true).div {
            classes = mutableSetOf("container-sm", ALIGN_ITEMS_CENTER).apply {
                addAll(collectViewLayoutElementStyles(node))
            }
            div {
                classes = setOf("row", "mb-2")
                div { classes = setOf(FORM_COLUMN_SIZE_CLS, "h4"); text(title) }
                div { classes = setOf(FORM_COLUMN_SIZE_CLS, "align-items-end", "d-flex", "flex-row-reverse")
                    a { href = checkFormLink(createForm); classes = setOf("btn", "btn-primary", "btn-sm"); text("Add $entityName") }
                }
            }
            table {
                classes = setOf("table", "table-hover")
                thead {
                    tr {
                        entity.members.filter { it.memberId != "id" }.forEach { member ->
                            th {
                                scope = ThScope.col
                                text(NameFormatter.toDisplayText(member.memberId))
                            }
                        }
                        th {
                            scope = ThScope.col
                            text("Actions")
                        }
                    }
                }
                tbody {
                    val entityVar = entityName.replaceFirstChar { it.lowercase() }
                    tr {
                        with(templateBuilder.htmlAttribute("tr", "loop_var", entityVar)) {
                            attributes[first] = second
                        }
                        with(templateBuilder.htmlAttribute("tr", "if_not_empty", entityVar)) {
                            attributes[first] = second
                        }
                        entity.members.filter { it.memberId != "id" }.forEach { member ->
                            td {
                                with(templateBuilder.htmlAttribute("td", "loop_member", "$entityVar.${member.memberId}")) {
                                    attributes[first] = second
                                }
                            }
                        }
                        td {
                            a { classes = setOf("btn", "btn-primary", "btn-sm")
                                with(templateBuilder.htmlAttribute("a", "href", "${checkLink(createForm)}?id=$entityVar.id")) {
                                    attributes[first] = second
                                }
                                text("Modify")
                            }
                            a { classes = setOf("btn", "btn-outline-danger", "btn-sm", "ms-1")
                                with(templateBuilder.htmlAttribute("a", "href", "/data/${entityName.lowercase()}/${StandardEvents.deleteItem.name}?id=$entityVar.id")) {
                                    attributes[first] = second
                                }
                                text("Delete")
                            }
                        }
                    }
                    // message line, if no items in the result
                    tr {
                        with(templateBuilder.htmlAttribute("tr", "if_empty", entityVar)) {
                            attributes[first] = second
                        }
                        td {
                            colSpan = "${entity.members.size}"
                            div { classes = setOf("alert", "alert-warning", "text-center"); role = "alert"
                                text("No ${entityName}s stored in the database.")
                            }
                        }

                    }
                }
            }
        }
    }

    private fun getEntity(entityName: String): IGMClass {
        val className = WholeProject.projectModel.igm.classes.keys.find { it.endsWith(entityName) }
        return if (className != null) WholeProject.projectModel.igm.getClass(className) else IGMClass(entityName)
    }

    private fun getEnumeration(enumName: String): List<String> {
        val enumerationName = WholeProject.projectModel.igm.enumerations.keys.find { it.endsWith(enumName) }
        return if (enumerationName != null) WholeProject.projectModel.igm.getEnumeration(enumerationName).values else listOf()
    }

    private fun existingView(viewName: String): Boolean {
        return WholeProject.projectModel.igm.views.keys.any { it.endsWith(viewName) }
    }

    private fun isFormLayoutNode(node: IGMView.IGMNode): Boolean {
        if (node.name == StandardLayoutElements.form.name) return true
        node.children.forEach { childNode ->
            if (isFormLayoutNode(childNode)) return true
        }
        return false
    }

    private fun isFormView(link: String): Boolean {
        val view = WholeProject.projectModel.igm.views[link]
        return if (view != null) isFormLayoutNode(view.layout) else false
    }

}