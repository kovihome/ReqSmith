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

package dev.reqsmith.composer.parser.entities

class ReqMSource : ElementBase() {
    fun getAction(actionName: String): Action? {
        return actions.find { it.qid?.id == actionName }
    }

    val applications: MutableList<Application> = ArrayList()
    val modules: MutableList<Modul> = ArrayList()
    val actors: MutableList<Actor> = ArrayList()
    val classes: MutableList<Classs> = ArrayList()
    val entities: MutableList<Entity> = ArrayList()
    val actions: MutableList<Action> = ArrayList()
    val views: MutableList<View> = ArrayList()
}
