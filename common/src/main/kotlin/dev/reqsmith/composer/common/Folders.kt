/*
 * ReqSmith - Build application from requirements
 * Copyright (c) 2025-2026. Kovi <kovihome86@gmail.com>
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

package dev.reqsmith.composer.common

enum class Folders(val path: String = "") {

    /**
     *  The source files for the project.
     */
    ProjectMain(WholeProject.project.buildSystem.sourceFolder),

    /**
     * The ReqSmith requirement files.
     */
    ProjectReqm("${WholeProject.project.buildSystem.sourceFolder}/${REQM_FOLDER_NAME}"),

    /**
     * Resource files for the project.
     */
    ProjectResources(WholeProject.project.buildSystem.resourceFolder),

    /**
     * The resource art files.
     */
    ProjectResourcesArt("${WholeProject.project.buildSystem.resourceFolder}/${ART_FOLDER_NAME}"),

    /**
     * The generated files by ReqSmith Forge.
     */
    BuildForge("${WholeProject.project.buildSystem.buildFolder}/${INTERNAL_FORGE_FOLDER_NAME}"),

    /**
     * The Effective Requirement Model files.
     */
    BuildForgeReqm("${WholeProject.project.buildSystem.buildFolder}/${INTERNAL_FORGE_FOLDER_NAME}/${REQM_FOLDER_NAME}"),

    /**
     * The Internal Generator Model files.
     */
    BuildForgeIgm("${WholeProject.project.buildSystem.buildFolder}/${INTERNAL_FORGE_FOLDER_NAME}/${IGM_FOLDER_NAME}"),

    /**
     * The generated resource files for the project.
     */
    BuildForgeResources("${WholeProject.project.buildSystem.buildFolder}/${INTERNAL_FORGE_FOLDER_NAME}/${WholeProject.project.buildSystem.RESOURCE_FOLDER_NAME}"),

    /**
     * The generated/downloaded resource art files.
     */
    BuildForgeResourcesArt("${WholeProject.project.buildSystem.buildFolder}/${INTERNAL_FORGE_FOLDER_NAME}/${WholeProject.project.buildSystem.RESOURCE_FOLDER_NAME}/${ART_FOLDER_NAME}"),

    /**
     * The generated source files for the project.
     */
    BuildSrc("${WholeProject.project.buildSystem.buildFolder}/src"),

    /**
     * The StdRepo resources folder.
     */
    RepoResources("stdlib/resources"),

    /**
     * The StdRepo resource art files.
     */
    RepoResourcesArt("stdlib/resources/art")
    ;

    fun absPath(): String = "${if (name.startsWith("Repo")) WholeProject.appHome else WholeProject.project.projectFolder}/$path"

    fun filePath(fileName: String): String = "$path/$fileName"

    fun absFilePath(fileName: String): String = "${absPath()}/$fileName"

    companion object {
        fun printALl() {
            Folders.entries.forEach { folder ->
                Log.debug("Folder ${folder.name} path: ${folder.absPath()}")
            }
        }
    }

//## Forge application folders
//
//Relative to the Forge application root.
//
//| Folder Name               | Description                   |
//|---------------------------|-------------------------------|
//| **stdlib**                | StdRepo root                  |
//| **stdlib/applications**   | Standard application types    |
//| **stdlib/frameworks**     | Standard frameworks           |
//| **stdlib/modules**        | Standard modules              |
//| **stdlib/features**       | Standard features             |
//| **stdlib/resources**      | Standard resources            |
//| **stdlib/resources/art**  | Standard resource art files   |

}