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

package dev.reqsmith.composer.generator.plugin.framework

import dev.reqsmith.composer.common.plugin.Plugin
import dev.reqsmith.composer.common.plugin.PluginDef
import dev.reqsmith.composer.common.plugin.PluginType
import dev.reqsmith.composer.generator.entities.IGMAction
import dev.reqsmith.composer.generator.entities.IGMClass
import dev.reqsmith.composer.generator.entities.InternalGeneratorModel
import dev.reqsmith.composer.parser.entities.Application
import dev.reqsmith.composer.parser.entities.Property

class SpringFrameworkBuilder : WebFrameworkBuilder(), Plugin {
    override fun definition(): PluginDef {
        return PluginDef("framework.web.spring", PluginType.Framework)
    }

    override fun buildApplication(app: Application, igm: InternalGeneratorModel) {
        super.buildApplication(app, igm)
        val appClass = igm.getClass(app.qid!!.toString())
        appClass.annotations.add("SpringBootApplication")
        appClass.addImport("org.springframework.boot.autoconfigure.SpringBootApplication")

        val mainAction = appClass.getAction("main")
        val call = IGMAction.IGMActionStmt("call").withParam("runApplication<${app.qid!!.id}>").withParam("*${mainAction.parameters[0].name}")
        mainAction.statements.add(call)
        appClass.addImport("org.springframework.boot.runApplication")
    }

}