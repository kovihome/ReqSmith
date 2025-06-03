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

import dev.reqsmith.composer.common.WholeProject
import dev.reqsmith.composer.common.formatter.NameFormatter
import dev.reqsmith.composer.common.plugin.PluginDef
import dev.reqsmith.composer.common.plugin.PluginType
import dev.reqsmith.model.enumeration.StandardEvents
import dev.reqsmith.model.enumeration.StandardStyleElements
import dev.reqsmith.model.igm.IGMClass
import dev.reqsmith.model.igm.IGMView
import kotlinx.html.*
import kotlinx.html.stream.createHTML
import kotlin.math.max

private const val FORM_COLUMN_SIZE_CLS = "col-md-6"

private const val ALIGN_ITEMS_CENTER = "align-items-center"

class BootstrapHtmlBuilder: HtmlBuilder() {

    private val nf = NameFormatter()

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

    private fun calculateBackgroundColor(styleRef: String?, defaultBackgroundClass: String): String {
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
                    img { src = "$artPathPrefix/${attr["logo"]}"; alt = "logo"; classes = setOf("me-3"); style = "height: 128px;"
                        viewArts.add(attr["logo"] ?: "")
                        WholeProject.projectModel.igm.addResource("static/$artPathPrefix", "$artPathPrefix/${attr["logo"]}") // TODO static
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
            classes = collectViewLayoutElementStyles(node)

            // write multiline text
            node.getAttr("default")?.let { t ->
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
            classes = setOf("col-md-$colSize", "mb-3", "mb-md-0")

            h5 { text(groupTitle) }
            ul { classes = setOf("list-unstyled")
                node.attributes.filter { it.first == "to" }.forEach { link ->
                    val linkText = link.second.replace("_", " ")
                    li {
                        a { href = checkLink(link.second); classes = setOf("text-white", "text-decoration-none"); text(linkText) }
                    }
                }
            }
        }
    }

    override fun createLinkButton(node: IGMView.IGMNode): String {
        return createHTML(true).a {
            classes = setOf("btn", "btn-primary")
            role = "button"
            href = checkLink(node.attributes.find { it.first == "to" }?.second)
            text(node.attributes.find { it.first == "title" }?.second ?: "LinkButton")
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
        return createHTML(true).div {
            classes = setOf("container", "mt-4")
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
        val entityName = attr["data"] ?: ""
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
                        label { htmlFor = member.memberId; classes = setOf(FORM_COLUMN_SIZE_CLS, "col-form-label"); text("${nf.toDisplayText(member.memberId)}:") }
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
                                            option { value = it; text(nf.toDisplayText(it)) }
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
        val entityName = attr["data"] ?: ""
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
                    a { href = "${checkLink(createForm)}?id=0"; classes = setOf("btn", "btn-primary", "btn-sm"); text("Add $entityName") }
                }
            }
            table {
                classes = setOf("table", "table-hover")
                thead {
                    tr {
                        entity.members.filter { it.memberId != "id" }.forEach { member ->
                            th {
                                scope = ThScope.col
                                text(nf.toDisplayText(member.memberId))
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

}