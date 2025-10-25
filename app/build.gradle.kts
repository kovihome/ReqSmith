/*
 * ReqSmith - Build application from requirements
 * Copyright (c) 2023-2025. Kovi <kovihome86@gmail.com>
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

plugins {
    id("reqsmith.composer.kotlin-application-conventions")
}

// TODO: axion-release plugin for version management

dependencies {
    implementation(project(":model"))
    implementation(project(":common"))
    implementation(project(":parser"))
    implementation(project(":validator"))
    implementation(project(":composer"))
    implementation(project(":generator"))
    implementation(project(":repository-finder"))

    runtimeOnly(project(":plugin-css"))

    implementation(libs.kotlin.reflect)
    implementation(libs.kotlin.cli)
    implementation(libs.commons.text)
}

application {
    applicationName = "forge"
    version = project.version
    // Define the main class for the application.
    mainClass.set("dev.reqsmith.composer.app.AppKt")
}

//tasks.installDist {
//    into("D:/temp/qqq/composer-0.1.0")
//}
