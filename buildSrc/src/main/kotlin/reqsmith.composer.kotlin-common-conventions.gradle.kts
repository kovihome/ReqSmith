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

allprojects {
    group = "dev.reqsmith.composer"
    version = "0.3.0"
    description = "ReqSmith::composer - build application source code from requirement model"
//    project.ext {
//        "vendor" to "ReqSmith Dev"
//        "email" to "kovihome86@gmail.com"
//    }

    // Configure the manifest for all subprojects
    tasks.withType<Jar> {
        archiveBaseName = "reqsmith-${project.name}"
        manifest {
            attributes (
//                "Specification-Title" to project.description,
//                "Specification-Version" to project.version,
//                "Specification-Vendor" to project.ext["vendor"],
                "Implementation-Title" to project.description,
                "Implementation-Version" to project.version,
                "Implementation-Vendor" to "ReqSmith Dev", // project.ext["vendor"],
                "Build-Timestamp" to LocalDateTime.now().toString().substringBefore('.'),
                "Built-By" to "${System.getProperty("user.name")} <kovihome86@gmail.com>", // ${project.ext["email"]}
                "Gradle-Version" to gradle.gradleVersion,
                "Java-Version" to JavaVersion.current().toString(),
//                "Class-Path" to configurations.runtimeClasspath.get().joinToString(" ") { it.name }
            )
        }
    }

}

repositories {
    // Use Maven Central for resolving dependencies.
    mavenCentral()
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
