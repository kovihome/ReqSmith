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

package dev.reqsmith.composer.common.configuration

import org.yaml.snakeyaml.DumperOptions
import org.yaml.snakeyaml.LoaderOptions
import org.yaml.snakeyaml.Yaml
import org.yaml.snakeyaml.constructor.Constructor
import org.yaml.snakeyaml.nodes.Tag
import org.yaml.snakeyaml.representer.Representer
import dev.reqsmith.composer.common.Log
import dev.reqsmith.model.FEATURE_TEMPLATE_ATTRIBUTE_DEFAULT_STYLE
import dev.reqsmith.model.FEATURE_TEMPLATE_ATTRIBUTE_TEMPLATE_VIEW
import java.io.FileInputStream
import java.io.FileWriter
import java.nio.file.Files
import kotlin.io.path.Path

/**
 * Configuration manager singleton
 */
object ConfigManager {
    var defaults = mutableMapOf(
        "domainName" to "dev.reqsmith.sample",
        "applicationType" to "applications.CommandLineApplication",
        "propertyType" to "String",
        FEATURE_TEMPLATE_ATTRIBUTE_TEMPLATE_VIEW to "DefaultTemplate",
        FEATURE_TEMPLATE_ATTRIBUTE_DEFAULT_STYLE to "DefaultStyle",

        "framework.base" to "",
        "framework.web" to "spring",
        "feature.template" to "framework.web.spring",
        "feature.persistence" to "framework.web.spring",
        "template.html" to "template.html.thymeleaf",
        "language" to "kotlin",
        "html" to "bootstrap",
        "buildsys" to "gradle"
    )

    private val dumpOpt = DumperOptions().apply {
        isCanonical = false
        defaultFlowStyle = DumperOptions.FlowStyle.BLOCK
        defaultScalarStyle = DumperOptions.ScalarStyle.PLAIN
        indent = 4
        isPrettyFlow = true
        isExplicitStart = false
        isExplicitEnd = false
        isDereferenceAliases = true
    }
    private val representer = Representer(dumpOpt).apply {
        addClassTag(defaults.javaClass, Tag.MAP)
    }
    private val yaml = Yaml(Constructor(defaults.javaClass, LoaderOptions()), representer, dumpOpt)

    private const val DEFAULTS_FILE_NAME = "defaults.yaml"
    private const val DEFAULTS_HEADER = "#\n# ReqSmith::forge configuration for default values\n#\n\n"

    private fun writeDefaultsFile(externalDefaultsFilePath: String) {
        FileWriter(externalDefaultsFilePath).use {
            it.write(DEFAULTS_HEADER)
            yaml.dump(defaults, it)
        }
    }

    /**
     * Loads default values from defaults.yaml config file
     *
     * The config file is external in the application bin folder, if it does not exist, it will be created
     *
     * @param path: application root folder
     */
    fun load(path: String) {
        val externalDefaultsFilePath = "$path/bin/$DEFAULTS_FILE_NAME"
        if (Files.exists(Path(externalDefaultsFilePath))) {
            Log.debug("Defaults file found: $externalDefaultsFilePath")
            // save default defaults
            val savedDefaults = mutableMapOf<String, String>().apply { putAll(defaults) }
            // load defaults file
            FileInputStream(externalDefaultsFilePath).use {
                defaults = yaml.load(it)
            }
            // insert new values from the saved map
            val residualKeys = savedDefaults.keys.minus(defaults.keys)
            if (residualKeys.isNotEmpty()) {
                Log.debug("Some items were missing from default file $externalDefaultsFilePath, update it")
                residualKeys.forEach {
                    defaults[it] = savedDefaults[it]!!
                }
                // save the modified default set
                writeDefaultsFile(externalDefaultsFilePath)
            }

        } else {
            Log.debug("defaults file $externalDefaultsFilePath is not found; creates new one")
            writeDefaultsFile(externalDefaultsFilePath)
        }
    }
}
