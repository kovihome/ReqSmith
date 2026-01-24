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

private const val ENTITIES_PATHNAME = "entities"

private const val CONTROLLER_PATHNAME = "controller"

private const val CONTROLLER_NAME_POSTFIX = "Controller"

private const val SERVICE_PATHNAME = "service"

private const val SERVICE_NAME_POSTFIX = "Service"

private const val REPOSITORY_PATHNAME = "repository"

private const val REPOSITORY_NAME_POSTFIX = "Repository"

class SourceArchitecture {

    fun controllerName(baseName: String) = "${baseName.replace(ENTITIES_PATHNAME, CONTROLLER_PATHNAME)}$CONTROLLER_NAME_POSTFIX"

    fun serviceName(baseName: String) = "${baseName.replace(ENTITIES_PATHNAME, SERVICE_PATHNAME)}$SERVICE_NAME_POSTFIX"

    fun repositoryName(baseName: String) = "${baseName.replace(ENTITIES_PATHNAME, REPOSITORY_PATHNAME)}$REPOSITORY_NAME_POSTFIX"

}