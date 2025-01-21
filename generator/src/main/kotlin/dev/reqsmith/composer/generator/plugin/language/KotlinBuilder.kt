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

import dev.reqsmith.composer.common.plugin.Plugin
import dev.reqsmith.composer.common.plugin.PluginDef
import dev.reqsmith.composer.common.plugin.PluginType
import dev.reqsmith.composer.parser.enumeration.Optionality
import dev.reqsmith.model.enumeration.StandardTypes
import dev.reqsmith.model.igm.*

class KotlinBuilder : LanguageBuilder, Plugin {

    class LocalVariables {
        private val variables : MutableMap<String, String> = HashMap()
        val imports : MutableList<String> = ArrayList()

        fun exist(variable: String) = variables.containsKey(variable)

        fun setParameters(parameters: List<IGMAction.IGMActionParam>) {
            parameters.forEach {
                if (it.type[0].isUpperCase() && !variables.containsKey(it.name)) variables[it.name] = it.type
            }
        }

        fun instantiateClass(classs: String): String {
            val pkg = classs.substringBeforeLast('.', "")
            val cls = classs.substringAfterLast('.')

            if (cls[0].isLowerCase() && variables.containsKey(cls)) return "$cls."

            if (cls[0].isUpperCase()) {
                variables.forEach { (k, v) -> if (v == cls) return "$k." }
            }

            if (pkg.isNotEmpty() && !imports.contains(classs)) imports.add(classs)

            val clsVar = cls.lowercase()
            variables[clsVar] = cls
            return "val $clsVar = $cls()\n$clsVar."
        }
    }

    override val extension: String = "kt"
    override val language: String = "kotlin"
    override var artPathPrefix: String = ""
    override val viewArts: MutableList<String>
        get() = TODO("Not yet implemented")
    override lateinit var igm: InternalGeneratorModel

    private val tabsize = 4

    override fun definition(): PluginDef {
        return PluginDef(language, PluginType.Language)
    }

    override fun collectBuildScriptElement(buildScriptUpdates: Map<String, MutableList<String>>) {
        buildScriptUpdates["plugins"]?.addAll(listOf(
            "kotlin(\"jvm\"):2.0.20"
        ))
    }

    private fun prefix(size : Int) : String = if (size > 0) " ".repeat(size) else ""

    override fun typeMapper(type : String?) : String = when (type?.lowercase()) {
        null -> "String"
        StandardTypes.string.name -> "String"
        StandardTypes.stringLiteral.name -> "String"
        StandardTypes.versionNumber.name -> "String"
        StandardTypes.integer.name -> "Int"
        StandardTypes.date.name -> "java.time.LocalDate"
        else -> type
    }

    override fun addComment(text: String, indent:Int): String {
        val pre = prefix(indent)
        val sb = StringBuilder("$pre/*\n")
        text.split("\n").forEach { sb.append("$pre** $it\n") }
        sb.append("$pre*/\n")
        return sb.toString()
    }

    private fun addClassMethod(sb: StringBuilder, action: IGMAction, cls: IGMClass, indent:Int) {
        val pre = prefix(indent)
        val localVars = LocalVariables().apply {
            setParameters(action.parameters)
            setParameters(cls.ctorParams)
            setParameters(cls.members.map { IGMAction.IGMActionParam(it.key, it.value.type) })
        }

        // start with annotations
        addAnnotations(sb, action.annotations, indent)
        val pa = parameterList(action.parameters)
        val returns = if (action.returnType != null) {
            if (action.returnType!!.listOf) {
                ": List<${typeMapper(action.returnType!!.type)}> "
            } else {
                ": ${typeMapper(action.returnType!!.type)} "
            }
        } else ""
        sb.append(pre).append("fun ${action.actionId}($pa) $returns{\n")
        val pre2 = prefix(indent+tabsize)
        action.statements.forEach { st ->
            sb.append(pre2)
            when (st.actionName) {
                "call" -> {
                    writeCall(sb, st.parameters, localVars, pre2)
                }
                "set" -> {
                    val varName = st.parameters[0].value
                    if (!localVars.exist(varName)) sb.append("val ")
                    sb.append("$varName = ")
                    writeCall(sb, st.parameters.subList(1, st.parameters.size), localVars, pre2)
                }
                "print" -> {
                    sb.append("println (${st.parameters.joinToString(",") { p -> p.format() }})")
                }
                "return" -> {
                    sb.append("return ${st.parameters[0].format()}")
                }
                else -> {
                    sb.append("${st.actionName} (${st.parameters.joinToString(",") { p -> p.format() }})")
                }
            }
            localVars.imports.forEach { addImport(it) } // TODO: this adds import to nothing
            sb.append("\n")
        }
        sb.append(pre).append("}\n\n")
    }

    private fun writeCall(
        sb: StringBuilder,
        parameters: List<IGMAction.IGMStmtParam>,
        localVars: LocalVariables,
        pre2: String
    ) {
        val calledFunction = parameters[0].value
        val params = parameters.subList(1, parameters.size).toList()
        val classs = calledFunction.substringBeforeLast('.', "")
        val func = calledFunction.substringAfterLast('.')
        if (classs.isNotEmpty()) {
            // check class, if exists in local vars
            val instantiateStmt = localVars.instantiateClass(classs)
            // create local var for this class
            sb.append(instantiateStmt.replace("\n", "\n$pre2"))
        }
        sb.append("$func(${params.joinToString(",") { p -> p.format() }})")
    }

    private fun parameterList(parameters: List<IGMAction.IGMActionParam>, prefix: String? = null): String {
        val pf = if (prefix.isNullOrBlank()) "" else "$prefix "
        return parameters.joinToString(", ") { p->
            var annotations = p.annotations.joinToString(" ") { "@${it.annotationName}" }
            if (annotations.isNotBlank()) annotations = "$annotations "
            if (p.listof) "$annotations$pf${p.name}: Array<${p.type}>" else "$annotations$pf${p.name}: ${p.type}"
        }
    }

    private fun addAnnotations(sb: StringBuilder, annotations: List<IGMAction.IGMAnnotation>, indent: Int) {
        val pre = prefix(indent)
        annotations.forEach {
            sb.append("$pre@${it.annotationName}")
            if (it.parameters.isNotEmpty()) {
                val parList = it.parameters.joinToString(", ") { p ->
                    val pvalue = if (p.value.contains(',')) {
                        if (p.type == StandardTypes.stringLiteral.name) {
                            val literalList = p.value.split(',').joinToString(", ") { v -> "\"$v\"" }
                            "[$literalList]"
                        } else "[${p.value}]"
                    } else {
                        if (p.type == StandardTypes.stringLiteral.name) "\"${p.value}\"" else p.value
                    }
                    when {
                        p.name.isBlank() -> pvalue
                        else -> "${p.name} = $pvalue"
                    }
                }
                sb.append("($parList)")
            }
            sb.append("\n")
        }
    }


    override fun addClass(cls: IGMClass, indent: Int): String {
        val pre = prefix(indent)
        val sb = StringBuilder()
        // start with annotations
        addAnnotations(sb, cls.annotations, indent)
        // class signature
        val clsname = cls.id.substringAfterLast('.')
        sb.append("${pre}${if (cls.interfaceType) "interface" else "class"} $clsname")
        if (cls.ctorParams.isNotEmpty()) {
            sb.append("(${parameterList(cls.ctorParams, "val")})")
        }
        if (cls.parent.isNotBlank()) {
            sb.append(" : ${cls.parent}")
            if (cls.parentClasses.isNotEmpty()) {
                sb.append("<${cls.parentClasses.joinToString { it }}>")
            }
        }
        if (cls.members.isNotEmpty() || cls.actions.isNotEmpty()) {
            sb.append(" {\n\n")

            // class members
            cls.members.forEach {
                addClassMember(sb, it.value, indent + tabsize)
            }

            cls.actions.forEach { (_, action) ->
                if (!action.isMain) {
                    addClassMethod(sb, action, cls, indent + tabsize)
                }
            }

            // closure
            sb.append("\n${pre}}")
        }
        sb.append("\n")

        // main class
        if (cls.mainClass) {
            sb.append("\n\n")
            cls.actions.filter { it.value.isMain }.forEach { (_, action) ->
                addClassMethod(sb, action, cls, indent)
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

        // start with annotations
        addAnnotations(sb, property.annotations, indent)

        sb.append(prefix(indent))
        if (property.type == StandardTypes.stringLiteral.name || property.listOf) {
            sb.append("val ")
        } else {
            sb.append("var ")
        }
        sb.append(property.memberId)
        if (property.type == StandardTypes.stringLiteral.name) {
            sb.append(" = \"${property.value}\"\n")
        } else {
            var type = typeMapper(property.type)
            if (type.contains('.')) type = type.substringAfterLast('.')
            sb.append(": ")
            if (property.listOf) {
                sb.append("MutableList<$type> = ArrayList()\n")
            } else {
                sb.append(type)
                if (property.optionality == Optionality.Optional.name) {
                    sb.append("? = null")
                } else {
                    // default value
                    val defaultValue = when (type) {
                        "String" -> if (property.value.isNullOrBlank()) "\"\"" else "\"${property.value}\""
                        "Int" -> if (property.value.isNullOrBlank()) "0" else "${property.value}"
                        "LocalDate" -> "LocalDate.now()"
                        else -> if (property.enumerationType) "$type.${property.value}" else if (property.value.isNullOrBlank()) "$type()" else "${property.value}"
                    }
                    sb.append(" = $defaultValue")
                }
                sb.append("\n")
            }
        }
        sb.append("\n")
    }

}