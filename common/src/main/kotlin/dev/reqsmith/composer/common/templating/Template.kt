/*
 * ReqSmith - Build application from requirements
 * Copyright (c) 2024. Kovi <kovihome86@gmail.com>
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

package dev.reqsmith.composer.common.templating

import org.stringtemplate.v4.ST
import dev.reqsmith.composer.common.Log
import java.io.FileNotFoundException

/**
 * String and File Template Management
 *
 * 
 */
class Template {

    fun translate(context: Map<String, String>, ins: String) : String {
//        return if (containsThymeleafTemplateString(ins)) translateWithThymeLeaf(context, ins) else ins
        return translateWithStringTemplate(context, ins)
    }

    fun translateFile(context: Map<String, String>, templateFileName: String) : String {
        val uri = javaClass.getResource("/$templateFileName")
        if (uri == null) {
            Log.error("Template file $templateFileName is not exists.")
            throw FileNotFoundException(templateFileName)
        }
        val fileContent = uri.readText()
        val translatedContent = translate(context, fileContent)
        return translatedContent
    }

    /**
     * TODO: Ez majd a Thymeleaf template plugin része lesz
     */
//    fun translateWithThymeLeaf(context : Map<String, String>, ins: String) : String {
//        val templateResolver = StringTemplateResolver().apply {
//            templateMode = TemplateMode.TEXT
//        }
//        val templateEngine = TemplateEngine().apply {
//            setTemplateResolver(templateResolver)
//        }
//        val thymeleafContext = Context().apply {
//            setVariables(context)
//        }
//
//        // Process the template with the context
//        val outs = templateEngine.process(ins, thymeleafContext)
//        return outs
//    }

    private fun translateWithStringTemplate(context : Map<String, String>, ins: String) : String {
        val st = ST(ins)
        context.forEach {
            st.add(it.key, it.value)
        }
        val outs = st.render()
        return outs
    }

//    fun containsThymeleafTemplateString(ins: String) : Boolean {
//        return ins.contains("\${")
//    }
}
