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
    id("reqsmith.composer.kotlin-library-conventions")
    antlr
}

dependencies {
    implementation(project(":model"))
    implementation(project(":common"))
    antlr("org.antlr:antlr4:4.13.2") // use ANTLR version 4
    implementation("com.github.ben-manes.caffeine:caffeine:3.2.1")
}

tasks.generateGrammarSource {
    maxHeapSize = "64m"
    outputDirectory = File (outputDirectory.absolutePath + "/dev/reqsmith/composer/parser")
    arguments = arguments + listOf("-long-messages", "-package", "dev.reqsmith.composer.parser", "-visitor")
}

tasks.compileKotlin {
    dependsOn (tasks.generateGrammarSource)
}

tasks.compileTestKotlin {
    dependsOn (tasks.generateTestGrammarSource)
}
