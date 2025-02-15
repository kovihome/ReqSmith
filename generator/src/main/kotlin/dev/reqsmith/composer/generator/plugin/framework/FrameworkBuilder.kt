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

package dev.reqsmith.composer.generator.plugin.framework

import dev.reqsmith.model.ProjectModel
import dev.reqsmith.model.igm.InternalGeneratorModel
import dev.reqsmith.model.reqm.Application
import dev.reqsmith.model.reqm.Entity
import dev.reqsmith.model.reqm.Feature
import dev.reqsmith.model.reqm.View

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
    fun buildView(view: View, igm: InternalGeneratorModel, templateContext: MutableMap<String, String>)

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
     * Retrieves the folder name containing artistic or visual resources (e.g., images, styles, etc.)
     * @return The folder name, relative to the resources directory.
     */
    fun getArtFolder(): String

    /**
     * Returns build script elements (plugins, dependecies) belongs to this framework
     * @param buildScriptUpdates The elements of the build script
     */
    fun collectBuildScriptElement(buildScriptUpdates: Map<String, MutableList<String>>)

    /**
     * Process additional resources
     * @param reqmResourcesFolderName Source folder name for the project resources
     * @param buildResourcesFolderName Build folder name for the project resources
     * @param projectModel The Project Model
     */
    fun processResources(reqmResourcesFolderName: String, buildResourcesFolderName: String, projectModel: ProjectModel)

    /**
     * Apply a feature on an entity
     * @param ent The entity on which the feature have to be applied
     * @param igm The full IGM structure
     * @param feature The feature which have to be applied on the entity
     */
    fun applyFeatureOnEntity(ent: Entity, igm: InternalGeneratorModel, feature: Feature)
}