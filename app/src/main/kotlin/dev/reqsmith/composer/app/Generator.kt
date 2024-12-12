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

package dev.reqsmith.composer.app

import dev.reqsmith.composer.common.Log
import dev.reqsmith.composer.common.Project
import dev.reqsmith.composer.common.plugin.PluginManager
import dev.reqsmith.composer.common.plugin.PluginType
import dev.reqsmith.composer.common.templating.Template
import dev.reqsmith.composer.generator.CodeGenerator
import dev.reqsmith.composer.generator.GeneratorModelBuilder
import dev.reqsmith.composer.generator.ViewGenerator
import dev.reqsmith.composer.generator.plugin.language.LanguageBuilder
import dev.reqsmith.composer.parser.entities.ReqMSource
import java.util.*

class Generator(
    private val project: Project,
    private val reqMSource: ReqMSource,
    private val appHome: String,
    private val lang: String
) {

    private val langBuilder = PluginManager.get<LanguageBuilder>(PluginType.Language, lang)
    private val srcPath = project.srcPath(lang)

    fun generate(): Boolean {
        Log.title("Generate source code")
        Log.info("Generated source path: $srcPath")

        // create internal generator model
        Log.debug("Build InternalGeneratorModel...")
        val gmb = GeneratorModelBuilder(reqMSource)
        val igm = gmb.build()
        Log.info("=============== InternalGeneratorModel ===============\n$igm")
        Log.info("======================================================\n")

        // generate the source code
        Log.debug("Generate SourceCode...")
        val success = CodeGenerator(langBuilder, project).generate(igm, getFileHeader())

        // generate views
        var successView = true
        if (reqMSource.views.isNotEmpty()) {
            val viewLangBuilder = PluginManager.get<LanguageBuilder>(PluginType.Language, gmb.viewGeneratorName)
            successView = ViewGenerator(viewLangBuilder, project).generate(igm)
        }

        // update build script
//        val buildScriptUpdates = mapOf(
//            "plugins" to listOf(
//                "org.springframework.boot:3.4.0",
//                "io.spring.dependency-management:1.1.6"
//            ),
//            "dependencies" to listOf(
//                "org.springframework.boot:spring-boot-starter-web",
//                "com.fasterxml.jackson.module:jackson-module-kotlin",
//                "org.jetbrains.kotlin:kotlin-reflect"
//            )
//        )
        val buildScriptUpdates = mapOf(
            "plugins" to mutableListOf<String>(),
            "dependencies" to mutableListOf()
        )
        langBuilder.collectBuildScriptElement(buildScriptUpdates)
        gmb.codeBuilder?.collectBuildScriptElement(buildScriptUpdates)

        generateBuildScripts(buildScriptUpdates)

        return success.and(successView)
    }

    /**
     * Generate/update build script
     */
    private fun generateBuildScripts(buildScriptUpdates: Map<String, List<String>>) {
        val version = reqMSource.applications[0].definition.properties.find { it.key == "version" }?.value ?: "0.1.0"
        project.updateBuildScript(appHome, lang, reqMSource.applications[0].qid.toString(), version, buildScriptUpdates)
    }

//    private fun getImports(qid: QualifiedId, definition: Definition): String {
//        val imports: MutableSet<String> = HashSet()
//        if (qid.domain != null) {
//            imports.add(qid.toString())
//        }
//        definition.properties.forEach {
//            if (it.type != null && !StandardTypes.has(it.type!!)) {
//                imports.add(it.type!!)
//            }
//        }
//
//        val sb = StringBuilder()
//        imports.forEach { sb.append("import $it\n") }
//        sb.append("\n")
//        return sb.toString()
//    }

    private fun getFileHeader(): String {
        // TODO: use different licence model templates
        val thisApplication = reqMSource.applications[0]
        val appName = thisApplication.qid?.id ?: "NamelessApplication"
        val description =
            thisApplication.definition.properties.find { it.key == "description" }?.value ?: "No description"
        val author = thisApplication.definition.properties.find { it.key == "author" }?.value ?: "Author"
        val authorMail =
            thisApplication.definition.properties.find { it.key == "author-mail" }?.value ?: "author@reqsmith.dev"
        val context = mapOf(
            "appName" to appName,
            "description" to description,
            "year" to Calendar.getInstance()[Calendar.YEAR].toString(),
            "author" to author,
            "mail" to authorMail
        )
        return Template().translateFile(context, "templates/gnu_file_header.st")
    }

}
