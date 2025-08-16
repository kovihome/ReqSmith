/*
 * ReqSmith - Build application from requirements
 * Copyright (c) 2025. Kovi <kovihome86@gmail.com> 
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

package dev.reqsmith.composer.common.resource

import dev.reqsmith.composer.common.WholeProject
import java.io.File
import java.net.MalformedURLException
import java.net.URL

private const val EXTERNAL_RESOURCE_FOLDER = "downloaded"

/**
 * Manage internal or external resources
 *
 * Internal resources are in the project's resources folder
 * External resources are defined in URL form
 */
object ResourceManager {

    /**
     * Check existence of the resource
     *
     * External resources will be downloaded to resources folder with an internal name,
     * so subsequent query will find them there.
     *
     * @param resourceName The name of the resource
     */
    fun exists(resourceName: String): Boolean {
        if (resourceName.isNotBlank()) {
            // is external resource?
            try {
                val url = URL(resourceName)
                val internalResourceName = internalName(url)
                if (localExists(internalResourceName)) {
                    return true
                }
                try {
                    downloadResource(url, internalResourceName)
                } catch (_: Exception) {
                    return false
                }
                return true
            } catch (_: MalformedURLException) {
                return localExists(resourceName)
            }
        }
        return false
    }

    private fun resourceFileName(internalResourceName: String): String {
        val projectFolder = WholeProject.project.projectFolder
        val resourceFolder = WholeProject.project.buildSystem.resourceFolder
        val fn =  "${projectFolder}/${resourceFolder}/${internalResourceName}"
        return fn

    }

    private fun localExists(internalResourceName: String): Boolean {
        return File(resourceFileName(internalResourceName)).exists()
    }

    private fun downloadResource(url: URL, internalResourceName: String) {
        val resourceData = url.readBytes()
        File(internalResourceName).writeBytes(resourceData)
    }

    private fun internalName(url: URL): String {
        // TODO
        return "$EXTERNAL_RESOURCE_FOLDER/${url.toExternalForm()}"
    }

    /**
     * Get default resource of the given type
     *
     * @param resourceType Type of the resource
     */
    fun getDefault(resourceType: ResourceType): String {
        // TODO
        return ""
    }

}
