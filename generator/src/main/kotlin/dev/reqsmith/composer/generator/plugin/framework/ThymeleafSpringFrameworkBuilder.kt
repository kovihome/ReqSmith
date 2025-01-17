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

package dev.reqsmith.composer.generator.plugin.framework

import dev.reqsmith.composer.common.Project
import dev.reqsmith.composer.common.configuration.ConfigManager
import dev.reqsmith.composer.common.plugin.Plugin
import dev.reqsmith.composer.common.plugin.PluginDef
import dev.reqsmith.composer.common.plugin.PluginType
import dev.reqsmith.composer.generator.TemplateContextCollector
import dev.reqsmith.model.ProjectModel
import dev.reqsmith.model.enumeration.StandardTypes
import dev.reqsmith.model.igm.IGMAction
import dev.reqsmith.model.igm.InternalGeneratorModel
import dev.reqsmith.model.reqm.View
import java.io.File

open class ThymeleafSpringFrameworkBuilder : SpringFrameworkBuilder(), Plugin {
    override fun definition(): PluginDef {
        return PluginDef("framework.web.spring.thymeleaf", PluginType.Framework)
    }

    override fun getViewLanguage(): String = "html"

    override fun getViewFolder(): String = "templates"

    override fun buildView(viewModel: View, igm: InternalGeneratorModel, templateContext: MutableMap<String, String>) {
        // create a controller class for this view
        val domainName = if (!viewModel.qid?.domain.isNullOrBlank()) viewModel.qid?.domain else /* TODO: application domain */ ConfigManager.defaults["domainName"]
        val className = viewModel.qid!!.id!!
        igm.getClass("$domainName.controller.${className}Controller").apply {
            annotations.add(IGMAction.IGMAnnotation("Controller"))
            imports.add("org.springframework.stereotype.Controller")
            getAction(className.lowercase()).apply {
                this.annotations.add(IGMAction.IGMAnnotation("GetMapping").apply {
                    parameters.add(IGMAction.IGMActionParam("/${className}.${getViewLanguage()}", StandardTypes.stringLiteral.name))
                })
                imports.add("org.springframework.web.bind.annotation.GetMapping")
                parameters.add(IGMAction.IGMActionParam("model", "Model"))
                imports.add("org.springframework.ui.Model")
                returnType = "String"
                val viewTemplateContext = TemplateContextCollector().getItemTemplateContext(viewModel.qid, viewModel.definition.properties, "view").apply {
                    putAll(templateContext)
                }
                viewTemplateContext.forEach {
                    statements.add(IGMAction.IGMActionStmt("call" ).withParam("model.addAttribute").withParam(it.key, StandardTypes.stringLiteral.name).withParam(it.value, StandardTypes.stringLiteral.name))
                }
                statements.add(IGMAction.IGMActionStmt("return").withParam(className, StandardTypes.stringLiteral.name))
            }
        }
    }

    override fun collectBuildScriptElement(buildScriptUpdates: Map<String, MutableList<String>>) {
        super.collectBuildScriptElement(buildScriptUpdates)
        buildScriptUpdates["dependencies"]?.addAll(listOf(
            "org.springframework.boot:spring-boot-starter-thymeleaf"
        ))
    }

    override fun addSpringApplicationProperties() {
        super.addSpringApplicationProperties()
        applicationProperties.setProperty("spring.thymeleaf.prefix", "classpath:/${getViewFolder()}/")
        applicationProperties.setProperty("spring.thymeleaf.suffix", ".${getViewLanguage()}")
    }

    override fun processResources(reqmResourcesFolderName: String, buildResourcesFolderName: String, projectModel: ProjectModel) {
        super.processResources(reqmResourcesFolderName, buildResourcesFolderName, projectModel)

        // copy templates
        projectModel.source.views.forEach { view ->
            view.definition.featureRefs.find { it.qid.toString() == "Template" }?.let { fr ->
                fr.properties.find { it.key == "file" }?.value?.let { fileName ->
                    val fn = fileName.removeSurrounding("'").removeSurrounding("\"")
                    val destFileName = "$buildResourcesFolderName/$fn"
                    projectModel.resources.add(Pair("$reqmResourcesFolderName/$fn", destFileName))
                }
            }
        }

        // copy arts
        val copyFrom = "$reqmResourcesFolderName/art"
        val copyTo = "$buildResourcesFolderName/${super.getViewFolder()}/art"   // TODO: get art folder name from project
        Project.ensureFolderExists(copyTo, null)

        File(copyFrom).listFiles()?.filter { it.isFile }?.forEach { file ->
            projectModel.resources.add(Pair("$copyFrom/${file.name}", "$copyTo/${file.name}"))
        }
    }

}