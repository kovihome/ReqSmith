tasks.register("<taskName>") {
    sourceFiles = fileTree("<reqmSourceDir>") {
        include "**/*.reqm"
    }
    inputs.files(sourceFiles).skipWhenEmpty()
    outputs.dir("<reqmOutputDir>")
    doLast {
        exec {
            commandLine(<commandAndArgs>)
        }
    }
}

tasks.named("build") {
    dependsOn("<taskName>")
}
