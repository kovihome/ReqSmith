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

import dev.reqsmith.composer.generator.entities.InternalGeneratorModel
import dev.reqsmith.composer.parser.entities.Application
import dev.reqsmith.composer.parser.entities.View

interface FrameworkBuilder {
    /**
     * Build an application IGM from ReqM application elem
     * @param app The ReqM application model
     * @param igm IGM container, which will hold the application IGM
     */
    fun buildApplication(app: Application, igm: InternalGeneratorModel)

    /**
     * Build a view IGM from ReqM view model
     * @param view The ReqM view model
     * @param igm IGM container, which will hold the view IGM
     * @param templateContext Templating context to substitute string values
     */
    fun buildView(view: View, igm: InternalGeneratorModel, templateContext: Map<String, String>)

    /**
     * Get the view language
     * @return The view language plugin ID
     */
    fun getViewLanguage(): String

    /**
     * Get the folder name of view resources (web pages, etc.)
     * @return The view folder name, relative to resources folder
     */
    fun getViewFolder(): String

    /**
     * Returns build script elements (plugins, dependecies) belongs to this framework
     * @param buildScriptUpdates The elements of the build script
     */
    fun collectBuildScriptElement(buildScriptUpdates: Map<String, MutableList<String>>)

    /**
     * Process additional resources
     * @param resourcesFolderName Folder name for the project resources
     */
    fun processResources(resourcesFolderName: String)
}