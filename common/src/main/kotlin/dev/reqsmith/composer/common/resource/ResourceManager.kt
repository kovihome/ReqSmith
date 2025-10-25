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

import dev.reqsmith.composer.common.ART_FOLDER_NAME
import dev.reqsmith.composer.common.Log
import dev.reqsmith.composer.common.Project
import dev.reqsmith.composer.common.WholeProject
import dev.reqsmith.composer.common.formatter.NameFormatter
import dev.reqsmith.composer.common.plugin.PluginManager
import dev.reqsmith.composer.common.plugin.PluginType
import dev.reqsmith.composer.common.plugin.resource.ResourceParser
import dev.reqsmith.model.FEATURE_RESOURCE
import dev.reqsmith.model.reqm.Style
import dev.reqsmith.model.reqm.View
import java.io.File
import java.net.URI
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
     * Process resources
     *
     * - validate resource existence
     * - download internet resource
     * - parse resource int reqm element
     */
    fun processInputResources() {
        with(WholeProject.projectModel.source) {
            val additionalStyleElements = mutableListOf<Style>()
//            views.forEach { processViewInputResources(it) }
            styles.forEach { processStyleInputResources(it, additionalStyleElements) }
            styles.addAll(additionalStyleElements)
        }
    }

    fun processViewInputResources(view: View) {
        TODO("Not yet implemented")
    }

    fun processStyleInputResources(style: Style, additionalStyleElements: MutableList<Style>) {
        style.definition.featureRefs.find { it.qid.toString() == FEATURE_RESOURCE }?.let {
            val prop = it.properties.getOrNull(0)
            if (prop != null) {
                // get the resource type and filename
                var resourceType = prop.key!!
                val resourceFileName = NameFormatter.deliterateText(prop.value ?: "")
                if (listOf("default", "file").contains(resourceType)) {
                    resourceType = resourceFileName.substringAfterLast(".") // file extension
                }

                // check resource file exitence, download internet resource
                if (!ResourceManager.exists(resourceFileName)) {
                    Log.warning("Resource $resourceFileName is not exists. (${it.coords()})")
                    // TODO: copy default resource file with the name of 'resourceName' into the resource folder
                }

                // parse resource (if parseable)
                try {
                    val resourceParser : ResourceParser = PluginManager.getBest("", PluginType.Resource, resourceType)
                    val additionalStyles = resourceParser.parseResourceFile(style.qid, resourceType, getLocalFileName(resourceFileName))
                    additionalStyleElements.addAll(additionalStyles)
                } catch (_: Exception) {
                    Log.warning("No appropriate resource parser plugin for resource type $resourceType")
                }

            } else {
                Log.warning("Resource has no file attribute. (${it.coords()})")
            }
        }
    }

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
                val url = URI(resourceName).toURL()
                val internalResourceName = internalName(url)
                if (localExists(internalResourceName)) {
                    return true
                }
//              Downloading external resources is temporary turned off
//                try {
//                    downloadResource(url, internalResourceName)
//                } catch (_: Exception) {
//                    return false
//                }
                return true
            } catch (_: Exception) {
                return localExists(resourceName)
            }
        }
        return false
    }

    /**
     * Returns full resource file path (project source file)
     *
     * @param internalResourceName Internal name of the project resource, relative to the project resource folder
     * @return Full path of the resource file
     */
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
        val resourcePath = resourceFileName(internalResourceName)
        val resourceFolder = resourcePath.replace('\\', '/').substringBeforeLast("/")
        val errors = mutableListOf<String>()
        Project.ensureFolderExists(resourceFolder, errors)
        if (errors.isEmpty()) {
            val resourceData = url.readBytes()
            File(resourcePath).writeBytes(resourceData)
        }
    }

    private fun isExternalResource(resourceName: String): Boolean {
        return resourceName.contains("://") && listOf("http", "https", "ftp").contains(resourceName.substringBefore("://"))
    }

    private fun getLocalFileName(resourceName: String): String {
        return if (isExternalResource(resourceName)) internalName(URI(resourceName).toURL()) else resourceFileName(resourceName)
    }

    private fun internalName(url: URL): String {
        val fileName = url.toExternalForm().substringAfterLast("/")
        return "$EXTERNAL_RESOURCE_FOLDER/$fileName"
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

    fun getImageResource(resourceName: String, frameworkResourceFinder: (String) -> String): Resource {
        return if (exists(resourceName)) {
            Resource(ResourceType.image, ResourceSourceType.PROJECT, resourceName)
        } else {
            val artResourceName = "$ART_FOLDER_NAME/$resourceName"
            if (exists(artResourceName)) {
                Resource(ResourceType.image, ResourceSourceType.PROJECT, artResourceName)
            } else {
                Resource(ResourceType.image, ResourceSourceType.FRAMEWORK, frameworkResourceFinder(resourceName))
            }
        }
    }

}
