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
import dev.reqsmith.composer.common.Folders
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

                // check resource file existence, download internet resource
                if (!exists(resourceFileName)) {
                    Log.warning("Resource $resourceFileName is not exists. (${it.coords()})")
                    getDefault(ResourceType.style, resourceFileName)
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
                if (internalExists(internalResourceName)) {
                    return true
                }
                // Download external resource
                try {
                    downloadResource(url, internalResourceName)
                } catch (_: Exception) {
                    return false
                }
                return true
            } catch (_: Exception) {
                return localExists(resourceName)
            }
        }
        return false
    }

    /**
     * Check existence of the local resource file in the project resources folder
     *
     * @param resourceName Internal name of the project resource, relative to the project internal resource folder
     * @return *true* - if the resource file exists, *false* - otherwise
     */
    private fun localExists(resourceName: String): Boolean = File(Folders.ProjectResources.absFilePath(resourceName)).exists()

    /**
     * Check existence of the internal resource file in the build folder resources folder
     *
     * @param internalResourceName Internal name of the project resource, relative to the project internal resource folder
     * @return *true* - if the internal resource file exists, *false* - otherwise
     */
    private fun internalExists(internalResourceName: String): Boolean = File(Folders.BuildForgeResources.absFilePath(internalResourceName)).exists()


    /**
     * Download external resource to the project resources folder
     *
     * @param url URL of the external resource
     * @param internalResourceName Internal name of the project resource, relative to the project internal resource folder
     */
    private fun downloadResource(url: URL, internalResourceName: String) {
        val resourcePath = Folders.BuildForgeResources.absFilePath(internalResourceName) // internalResourceFileName(internalResourceName)
        val resourceFolder = resourcePath.substringBeforeLast("/")
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
        return if (isExternalResource(resourceName))
            Folders.BuildForgeResources.absFilePath(internalName(URI(resourceName).toURL()))
        else
            Folders.ProjectResources.absFilePath(resourceName)
    }

    /**
     * Get internal name for external resource URL
     *
     * @param url URL of the external resource
     * @return Internal name of the resource
     */
    private fun internalName(url: URL): String {
        val fileName = url.toExternalForm().substringAfterLast("/")
        return "$ART_FOLDER_NAME/$fileName"
    }

    /**
     * Get default resource of the given type
     *
     * @param resourceType Type of the resource
     * @param resourceName Name of the resource
     */
    fun getDefault(resourceType: ResourceType, resourceName: String) {
        when (resourceType) {
            ResourceType.image -> {
                val defaultImagePath = Folders.RepoResourcesArt.absFilePath("default-image.png")
                val artFolder = Folders.BuildForgeResourcesArt.absPath()
                val targetName = NameFormatter.deliterateText(resourceName)
                val targetPath = "$artFolder/$targetName"

                // copy default image to art folder
                if (!Project.ensureFolderExists(artFolder, null)) {
                    Log.warning("Cannot ensure art folder $artFolder exists.")
                    return
                }
                try {
                    val sourceFile = File(defaultImagePath)
                    if (sourceFile.exists()) {
                        sourceFile.copyTo(File(targetPath), overwrite = true)
                    } else {
                        Log.warning("Default image not found at `$defaultImagePath`")
                    }
                } catch (e: Exception) {
                    Log.warning("Failed to copy default image: ${e.message}")
                }
            }

            ResourceType.style, ResourceType.view -> {
                val styleFile = File(resourceName)
                if (!Project.ensureFolderExists(styleFile.parent, null)) {
                    Log.warning("Cannot ensure art folder ${styleFile.parent} exists.")
                    return
                }
                try {
                    styleFile.createNewFile()
                } catch (e: Exception) {
                    Log.warning("Failed to create empty style file $styleFile: ${e.message}")
                }
            }
        }
    }

    fun getImageResource(resourceName: String, frameworkResourceFinder: (String) -> String = { "" }): Resource {
        return if (exists(resourceName)) {
            if (isExternalResource(resourceName)) {
                val localPath = getLocalFileName(resourceName)
                val realResourceName = "$ART_FOLDER_NAME/${localPath.substringAfterLast("/")}"
                Resource(ResourceType.image, ResourceSourceType.EXTERNAL, realResourceName, localPath)
            } else {
                Resource(ResourceType.image, ResourceSourceType.PROJECT, resourceName)
            }
        } else {
            val artResourceName = "$ART_FOLDER_NAME/$resourceName"
            if (exists(artResourceName)) {
                Resource(ResourceType.image, ResourceSourceType.PROJECT, artResourceName)
            } else {
                val frameworkResourceName = frameworkResourceFinder(resourceName)
                if (frameworkResourceName.isBlank()) {
                    // TODO: itt nem kell a warning, csak a validációnál nincsenek ellenőrizva a képek
                    Log.warning("Image $resourceName is not exists; default image will be used.")
                    getDefault(ResourceType.image, resourceName)
                    Resource(ResourceType.image, ResourceSourceType.PROJECT, "$ART_FOLDER_NAME/$resourceName")
                } else {
                    Resource(ResourceType.image, ResourceSourceType.FRAMEWORK, frameworkResourceName)
                }
            }
        }
    }

}
