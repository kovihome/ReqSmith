/*
 * ReqSmith - Build application from requirements
 * Copyright (c) 2024-2026. Kovi <kovihome86@gmail.com>
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
import dev.reqsmith.composer.common.plugin.Plugin
import dev.reqsmith.composer.common.plugin.PluginDef
import dev.reqsmith.composer.common.plugin.PluginManager
import dev.reqsmith.composer.common.plugin.PluginType
import dev.reqsmith.composer.common.plugin.language.LanguageBuilder
import dev.reqsmith.composer.generator.plugin.template.HtmlTemplateBuilder
import dev.reqsmith.model.igm.IGMClass
import dev.reqsmith.model.igm.IGMEnumeration
import dev.reqsmith.model.igm.IGMStyle
import dev.reqsmith.model.igm.IGMView
import kotlinx.html.*
import kotlinx.html.stream.createHTML

private const val DOCTYPE = "<!DOCTYPE html>\n"

open class HtmlBuilder : LanguageBuilder, Plugin {
    override val extension: String = "html"
    override val language: String = "html"
    override var artPathPrefix: String = ""
    override val viewArts: MutableSet<String> = HashSet()

    lateinit var templateBuilder : HtmlTemplateBuilder

    override fun definition(): PluginDef {
        return PluginDef(language, PluginType.Language)
    }

    override fun createView(view: IGMView): String {
        templateBuilder = PluginManager.getBest("html", PluginType.Template, "template")

        // apply view level style
        findStyles(view).forEach { style ->
            view.imports.add(style.url!!)
        }

        // build source code in string builder
        return DOCTYPE + createHTML(prettyPrint = true).html {
            head {
                meta { charset = Charsets.UTF_8.name() }
                meta {
                    name = "viewport"
                    content = "width=device-width, initial-scale=1.0"
                }
                title { text(view.id) }
                view.imports.filter { it.endsWith(".css") }.forEach {
                    link {
                        href = it
                        rel = "stylesheet"
                    }
                }
                view.imports.filter { it.endsWith(".js") }.forEach {
                    script {
                        src = it
                    }
                }
            }
            body {
                if (!view.styleRef.isNullOrBlank()) {
                    classes = setOf(view.styleRef!!.lowercase())
                }
                unsafe {
                    raw(createNode(view.layout))
                }
            }
        }
    }

    private fun findStyles(view: IGMView): List<IGMStyle> {
        val styleRefs = mutableSetOf<String>()
        if (!view.styleRef.isNullOrBlank()) {
            styleRefs.add(view.styleRef!!)
        }
        styleRefs.addAll(findStyleRefsInNode(view.layout))

        val styles = mutableListOf<IGMStyle>()
        styleRefs.forEach { ref ->
            val style = WholeProject.projectModel.igm.findStyle(ref)
            if (!style?.url.isNullOrBlank()) {
                styles.add(style)
            }
        }
        return styles
    }

    private fun findStyleRefsInNode(node: IGMView.IGMNode): Set<String> {
        val styleRefs = mutableSetOf<String>()
        if (!node.styleRef.isNullOrBlank()) {
            styleRefs.add(node.styleRef!!)
            styleRefs.add("${node.styleRef}-${node.name}")
        }
        node.children.forEach { child ->
            styleRefs.addAll(findStyleRefsInNode(child))
        }
        return styleRefs
    }

    fun createNode(node: IGMView.IGMNode): String {
        return when (node.name) {
            "button" -> createButton(node)
            "datatable" -> createDatatable(node)
            "footer" -> createFooter(node)
            "form" -> createForm(node)
            "header" -> createHeader(node)
            "image" -> createImage(node)
            "linkButton" -> createLinkButton(node)
            "linkGroup" -> createLinkGroup(node)
            "menu" -> createMenu(node)
            "navigation" -> createNavigation(node)
            "panel" -> createPanel(node)
            "spacer" -> createSpacer(node)
            "text" -> createText(node)
            else -> {
                if (node.children.isNotEmpty()) {
                    createHTML().div {
                        id = node.name
                        node.children.forEach {
                            unsafe { raw(createNode(it)) }
                        }
                    }
                } else {
                    createHTML(true).p { text("Unknown node: ${node.name}") }
                }
            }
        }
    }

    /**
     * Creates an image layout element
     *
     * Valid attributes are:
     * - src: resource name (file name, link or framework resource name)
     * - alt: alternative text
     * - size: image size in a size style notation
     */
    open fun createImage(node: IGMView.IGMNode): String {
        return createHTML(true).div {

        }
    }

    /**
     * Creates a button layout element
     *
     * Valid attributes are:
     * - title: button title
     * - to: action or view
     * - icon: image resource name for button icon
     */
    open fun createButton(node: IGMView.IGMNode): String {
        return createHTML(true).div {

        }
    }

    open fun createMenu(node: IGMView.IGMNode): String {
        return createHTML(true).div {

        }
    }

    open fun createNavigation(node: IGMView.IGMNode): String {
        return createHTML(true).nav {

        }
    }

    open fun createDatatable(node: IGMView.IGMNode): String {
        return createHTML(true).div {

        }
    }

    /**
     * Creates a form for an entity
     *
     * Valid attributes are:
     * - title: form title
     * - data: the entity which is managed by this form
     */
    open fun createForm(node: IGMView.IGMNode): String {
        return createHTML(true).div {

        }
    }

    open fun createPanel(node: IGMView.IGMNode): String {
        return createHTML(true).div {

        }
    }

    open fun createFooter(node: IGMView.IGMNode): String {
        return createHTML(true).div {

        }
    }

    open fun createHeader(node: IGMView.IGMNode): String {
        return createHTML(true).div { 
            
        }
    }

    open fun createText(node: IGMView.IGMNode): String {
        return createHTML(true).div {

        }
    }

    open fun createLinkGroup(node: IGMView.IGMNode): String {
        return createHTML(true).div {

        }
    }

    /**
     * Creates a link button layout element
     *
     * Valid attributes are:
     * - title: button title
     * - to: action or view
     * - icon: image resource name for button icon
     */
    open fun createLinkButton(node: IGMView.IGMNode): String {
        return createHTML(true).div {

        }
    }

    open fun createSpacer(node: IGMView.IGMNode): String {
        return createHTML(true).div {

        }
    }

    override fun addClass(cls: IGMClass, indent: Int): String {
        throw NotImplementedError()
    }

    override fun addEnumeration(enum: IGMEnumeration, indent: Int): String {
        throw NotImplementedError()
    }

    override fun addComment(text: String, indent: Int): String {
        throw NotImplementedError()
    }

    override fun addImport(imported: String, indent: Int): String {
        TODO("Not yet implemented")
    }

    override fun typeMapper(type: String?): String {
        TODO("Not yet implemented")
    }

    override fun collectBuildScriptElement(buildScriptUpdates: Map<String, MutableList<String>>) {
        TODO("Not yet implemented")
    }

    override fun createStyle(style: IGMStyle): String {
        TODO("Not yet implemented")
    }


}