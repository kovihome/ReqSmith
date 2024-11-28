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

import dev.reqsmith.composer.generator.entities.IGMAction
import dev.reqsmith.composer.generator.entities.IGMClass
import dev.reqsmith.composer.generator.entities.InternalGeneratorModel
import dev.reqsmith.composer.parser.entities.Property
import dev.reqsmith.composer.parser.entities.ReqMSource

class BaseFrameworkBuilder(reqmSource: ReqMSource) : DefaultFrameworkBuilder(reqmSource) {
//    override fun buildApplication(app: Application, igm: InternalGeneratorModel) {
//        super.buildApplication(app, igm)
//
//        val cls = igm.getClass(app.qid.toString())
//
//
//    }

    override fun processEvent(prop: Property, cls: IGMClass, igm: InternalGeneratorModel) {
        when (prop.key) {
            "applicationStart" -> {
                // create main action
                val main = cls.getAction("main").apply {
                    isMain = true
                }
                val param = IGMAction.IGMActionParam("args", "String", true)
                main.parameters.add(param)
                val call = IGMAction.IGMActionStmt("call").withParam("${cls.id}.${prop.value}")
                main.statements.add(call)
            }
            else -> super.processEvent(prop, cls, igm)
        }

    }

}
