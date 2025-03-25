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

package dev.reqsmith.composer.common.configuration

import java.time.LocalDateTime
import java.util.jar.Manifest

/**
 * Holds application information loading from manifest.mf
 */
object ApplicationInfo {
    lateinit var system: String
    lateinit var name: String
    lateinit var version: String
    lateinit var description: String
    lateinit var buildTime: String
    lateinit var vendor: String

    init {
        loadManifest()
    }

    private fun loadManifest() {
        val resource = object {}.javaClass.getResourceAsStream("/META-INF/MANIFEST.MF")
        var manifest = resource?.use { Manifest(it) }
        val manifestVendor = manifest?.mainAttributes?.getValue("Implementation-Vendor")
        if (manifestVendor == null || !manifestVendor.startsWith("ReqSmith")) { manifest = null }

        version = (manifest?.mainAttributes?.getValue("Implementation-Version") ?: "0.1.0.1").substringBeforeLast('.')
        val fullDescription = manifest?.mainAttributes?.getValue("Implementation-Title") ?: "ReqSmith::forge - Requirement composer and code generator"
        description = fullDescription.substringAfter("- ").replaceFirstChar { it.uppercaseChar() }
        val sysAndName = fullDescription.substringBefore(' ').split("::")
        system = sysAndName[0]
        name = sysAndName[1]
        vendor = manifestVendor ?: "ReqSmith"
        buildTime = manifest?.mainAttributes?.getValue("Build-Timestamp") ?: LocalDateTime.now().toString().substringBefore('.')
    }

    fun printTitle() = "$system::$name, version $version"

}