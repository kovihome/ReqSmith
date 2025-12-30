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

package dev.reqsmith.model.reqm

class ReqMSource : ElementBase() {
    val applications: MutableList<Application> = ArrayList()
    val modules: MutableList<Modul> = ArrayList()
    val actors: MutableList<Actor> = ArrayList()
    val classes: MutableList<Classs> = ArrayList()
    val entities: MutableList<Entity> = ArrayList()
    val actions: MutableList<Action> = ArrayList()
    val views: MutableList<View> = ArrayList()
    val styles: MutableList<Style> = ArrayList()
    val features: MutableList<Feature> = ArrayList()

//    fun getAction(actionName: String, sourceFileName: String? = null): Action? {
//        return actions.find {
//            if (sourceFileName == null) it.qid?.id == actionName
//            else it.qid?.id == actionName && it.sourceFileName == sourceFileName
//        }
//    }

//    fun getStyle(styleName: String): Style? {
//        return styles.find { it.qid?.id == styleName }
//    }

    inline fun <reified T : ElementBase> get(name: String?, sourceFileName: String? = null): T? {
        if (name == null) return null
        when (T::class.simpleName) {
            Application::class.simpleName -> return applications.find { it.qid.toString() == name && (sourceFileName == null || it.sourceFileName == sourceFileName) } as T?
            Modul::class.simpleName -> return modules.find { it.qid.toString() == name && (sourceFileName == null || it.sourceFileName == sourceFileName) } as T?
            Actor::class.simpleName -> return actors.find { it.qid.toString() == name && (sourceFileName == null || it.sourceFileName == sourceFileName) } as T?
            Classs::class.simpleName -> return classes.find { it.qid.toString() == name && (sourceFileName == null || it.sourceFileName == sourceFileName) } as T?
            Entity::class.simpleName -> return entities.find { it.qid.toString() == name && (sourceFileName == null || it.sourceFileName == sourceFileName) } as T?
            Action::class.simpleName -> return actions.find { it.qid.toString() == name && (sourceFileName == null || it.sourceFileName == sourceFileName) } as T?
            View::class.simpleName -> return views.find { it.qid.toString() == name && (sourceFileName == null || it.sourceFileName == sourceFileName) } as T?
            Style::class.simpleName -> return styles.find { it.qid.toString() == name && (sourceFileName == null || it.sourceFileName == sourceFileName) } as T?
            Feature::class.simpleName -> return features.find { it.qid.toString() == name && (sourceFileName == null || it.sourceFileName == sourceFileName) } as T?
        }
        return TODO("Unknown ReqM element type")
    }

    inline fun <reified T : ElementBase> get(qid: QualifiedId?, sourceFileName: String? = null): T? = get<T>(qid?.toString(), sourceFileName)

    fun addAll(newDeps: ReqMSource) {
        applications.addAll(newDeps.applications)
        modules.addAll(newDeps.modules)
        actors.addAll(newDeps.actors)
        classes.addAll(newDeps.classes)
        entities.addAll(newDeps.entities)
        actions.addAll(newDeps.actions)
        views.addAll(newDeps.views)
        styles.addAll(newDeps.styles)
        features.addAll(newDeps.features)
    }

    fun clear() {
        applications.clear()
        modules.clear()
        actors.clear()
        classes.clear()
        entities.clear()
        actions.clear()
        views.clear()
        styles.clear()
        features.clear()
    }
}