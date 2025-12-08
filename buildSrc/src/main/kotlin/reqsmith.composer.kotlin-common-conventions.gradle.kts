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

import java.time.LocalDateTime

plugins {
    // Apply the org.jetbrains.kotlin.jvm Plugin to add support for Kotlin.
    kotlin("jvm")
    id("com.github.ben-manes.versions")
}

val buildNumber = providers.exec {
    commandLine("git rev-list --count HEAD".split(" "))
}.standardOutput.asText.get().replace(Regex("[^0-9]"), "")

allprojects {
    group = "dev.reqsmith.composer"
    version = "0.5.0"
    description = "ReqSmith::forge - build application source code from requirement model"
    extensions.add("vendor", "ReqSmith Dev")
    extensions.add("email", "kovihome86@gmail.com")
    extensions.add("buildNumber", buildNumber)

    tasks.withType<Jar> {
        archiveBaseName = "reqsmith-${project.name}"

        // Configure the manifest for all subprojects
        manifest {
            attributes (
                mapOf(
                    "Implementation-Title" to project.description,
                    "Implementation-Version" to "${project.version}.${project.extensions["buildNumber"]}",
                    "Implementation-Vendor" to project.extensions["vendor"],
                    "Build-Timestamp" to LocalDateTime.now().toString().substringBefore('.'),
                    "Built-By" to "${System.getProperty("user.name")} <${project.extensions["email"]}>",
                    "Gradle-Version" to gradle.gradleVersion,
                    "Java-Version" to JavaVersion.current().toString(),
//                  "Class-Path" to configurations.runtimeClasspath.get().joinToString(" ") { it.name }
                )
            )
        }
    }

}

repositories {
    // Use Maven Central for resolving dependencies.
    mavenCentral()
}

configurations.all {
    resolutionStrategy {
        force("org.apache.commons:commons-lang3:3.18.0")
    }
}

dependencies {

    // Use JUnit Jupiter for testing.
    testImplementation("org.junit.jupiter:junit-jupiter:5.12.0")

    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.named<Test>("test") {
    // Use JUnit Platform for unit tests.
    useJUnitPlatform()

    maxParallelForks = 8
}
