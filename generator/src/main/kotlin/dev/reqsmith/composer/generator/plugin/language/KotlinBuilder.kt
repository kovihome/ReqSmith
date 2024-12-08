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

package dev.reqsmith.composer.generator.plugin.language

import dev.reqsmith.composer.common.plugin.Plugin
import dev.reqsmith.composer.common.plugin.PluginDef
import dev.reqsmith.composer.common.plugin.PluginType
import dev.reqsmith.composer.generator.entities.*
import dev.reqsmith.composer.parser.enumeration.Optionality
import dev.reqsmith.composer.parser.enumeration.StandardTypes

class KotlinBuilder : LanguageBuilder, Plugin {

    class Variable(val name: String, val type: String)

    override val extension: String = "kt"
    override val language: String = "kotlin"

    private val tabsize = 4

    override fun definition(): PluginDef {
        return PluginDef(language, PluginType.Language)
    }

    private fun prefix(size : Int) : String = if (size > 0) " ".repeat(size) else ""

    private fun typeMapper(type : String?) : String = when (type) {
        null -> "String"
        StandardTypes.string.name -> "String"
        StandardTypes.stringLiteral.name -> "String"
        StandardTypes.versionNumber.name -> "String"
        StandardTypes.integer.name -> "Int"
        else -> type
    }

    override fun addComment(text: String, indent:Int): String {
        val pre = prefix(indent)
        val sb = StringBuilder("$pre/*\n")
        text.split("\n").forEach { sb.append("$pre** $it\n") }
        sb.append("$pre*/\n")
        return sb.toString()
    }

    private fun addClassMethod(sb: StringBuilder, id: String?, params: List<IGMAction.IGMActionParam>, statements: List<IGMAction.IGMActionStmt>, indent:Int) {
        val pre = prefix(indent)
        val pa = params.joinToString(",") { if (it.listof) "${it.name}: Array<${it.type}>" else "${it.name}: ${it.type}" }
        sb.append(pre).append("fun ").append(id).append("(").append(pa).append(") {\n")
        val pre2 = prefix(indent+tabsize)
        statements.forEach { st ->
            sb.append(pre2)
            when (st.actionName) {
                "call" -> {
                    val calledfunction = st.parameters[0].value
                    val params = st.parameters.subList(1, st.parameters.size)
                    val classs = calledfunction.substringBeforeLast('.', "")
                    val func = calledfunction.substringAfterLast('.')
                    if (classs.isNotEmpty()) {
                        val pkg = classs.substringBeforeLast('.', "")
                        val cls = classs.substringAfterLast('.')
                        if (pkg.isNotEmpty()) {
                            addImport(classs)
                        }
                        val clsVar = cls.lowercase()
                        sb.append("val $clsVar = $cls()\n$pre2$clsVar.")
                    }
                    sb.append("$func (${params.joinToString(",") { p -> p.format() }})")
                }
                "print" -> {
                    sb.append("println (${st.parameters.joinToString(",") { p -> p.format() }})")
                }
                else -> {
                    sb.append("${st.actionName} (${st.parameters.joinToString(",") { p -> p.format() }})")
                }
            }
            sb.append("\n")
        }
        sb.append(pre).append("}\n\n")
    }

    override fun addClass(cls: IGMClass, indent: Int): String {
//        println("add class ${qid}")
        val pre = prefix(indent)
        // class signature
        val clsname = cls.id.substringAfterLast('.')
        val sb = StringBuilder("${pre}class $clsname")
        if (cls.parent.isNotBlank()) {
            sb.append(" : ${cls.parent}")
        }
        sb.append(" {\n\n")

        // class members
        cls.members.forEach {
            addClassMember(sb, it.value, indent+tabsize)
        }

        cls.actions.forEach {
            // TODO: method content
            if (!it.value.isMain) {
                addClassMethod(sb, it.value.actionId, it.value.parameters, it.value.statements, indent + tabsize)
            }
        }

        // closure
        sb.append("\n${pre}}\n")

        // main class
        if (cls.mainClass) {
            sb.append("\n\n")
            cls.actions.filter { it.value.isMain }.forEach { (_, action) ->
                addClassMethod(sb, action.actionId, action.parameters, action.statements, indent)
            }
        }

        return sb.toString()
    }

    override fun addEnumeration(enum: IGMEnumeration, indent: Int): String {
        val pre = prefix(indent)

        // class signature
        val enumname = enum.enumId.substringAfterLast('.')
        val sb = StringBuilder("${pre}enum class $enumname")
        if (enum.parent.isNotBlank()) {
            sb.append(" : ${enum.parent}")
        }
        sb.append(" {\n\n")

        // enum values
        sb.append(addEnumValues(enum.values, indent + tabsize))

        // closure
        sb.append("\n${pre}}\n")
        return sb.toString()
    }

    override fun addImport(imported: String, indent: Int): String {
        return "import $imported"
    }

    override fun createView(view: IGMView): String {
        TODO("Not yet implemented")
    }

    private fun addEnumValues(values: List<String>, indent: Int): Any {
        val pre = prefix(indent)
        val enums = values.joinToString (", ") { it }
        return "$pre$enums\n"
    }

    private fun addClassMember(sb: StringBuilder, property: IGMClassMember, indent: Int) {
        // var key: Type
        // var key: Type?  --> property.optionality
        // val key: MultableList<Type> = ArrayList() --> property.listOf
        // val key = value --> property.type == stringLiteral
        sb.append("${prefix(indent)} ")
        if (property.type == StandardTypes.stringLiteral.name || property.listOf) {
            sb.append("val ")
        } else {
            sb.append("var ")
        }
        sb.append(property.memberId)
        if (property.type == StandardTypes.stringLiteral.name) {
            sb.append(" = \"${property.value}\"\n")
        } else {
            val type = typeMapper(property.type)
            sb.append(": ")
            if (property.listOf) {
                sb.append("MutableList<$type> = ArrayList()\n")
            } else {
                sb.append(type)
                if (property.optionality == Optionality.Optional.name) {
                    sb.append("?")
                }
                sb.append("\n")
            }
        }
    }

}