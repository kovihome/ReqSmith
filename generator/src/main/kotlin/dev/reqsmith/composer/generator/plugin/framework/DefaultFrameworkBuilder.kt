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

import dev.reqsmith.composer.generator.entities.IGMClass
import dev.reqsmith.composer.generator.entities.InternalGeneratorModel
import dev.reqsmith.composer.parser.entities.Application
import dev.reqsmith.composer.parser.entities.Definition
import dev.reqsmith.composer.parser.entities.Property
import dev.reqsmith.composer.parser.entities.ReqMSource

open class DefaultFrameworkBuilder(val reqmSource: ReqMSource) : FrameworkBuilder {
    override fun buildApplication(app: Application, igm: InternalGeneratorModel) {
        val cls = igm.getClass(app.qid.toString())
        cls.mainClass = true

        processEvents(app.definition, cls, igm)

//        val starter = app.definition.properties.find { it.key == "start" }?.value ?: "start"
//        val action = cls.getAction(starter)

    }

    private fun processEvents(definition: Definition, cls: IGMClass, igm: InternalGeneratorModel) {
        val events = definition.properties.find { it.key == "events" }
        events?.let {
            it.simpleAttributes.forEach {prop ->
                processEvent(prop, cls, igm)
//                prop.value?.let { it1 -> addActionToClass(it1, cls) }
//                val event = igm.getEvent(prop.key!!)
//                event.action = "${cls.id}.${prop.value}"
            }
        }
    }

    protected open fun processEvent(prop: Property, cls: IGMClass, igm: InternalGeneratorModel) {

    }

}