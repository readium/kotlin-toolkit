/*
 * Copyright 2021 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

plugins {
    alias(libs.plugins.dokka)
    alias(libs.plugins.ktlint)
    alias(libs.plugins.compose.compiler) apply false
}

subprojects {
    apply(plugin = "org.jlleitschuh.gradle.ktlint")

    ktlint {
        android.set(true)
        version = "1.8.0"
    }

    afterEvaluate {
        if (tasks.findByName("clean") == null) {
            tasks.register<Delete>("clean") {
                delete(layout.buildDirectory)
            }
        }
    }
}

dokka {
    dokkaSourceSets.configureEach {
        reportUndocumented.set(false)
        skipEmptyPackages.set(false)
        skipDeprecated.set(true)
    }
}

tasks.register("cleanDocs", Delete::class).configure {
    delete(
        "${project.rootDir}/docs/api",
        "${project.rootDir}/docs/index.md",
        "${project.rootDir}/site"
    )
}

dependencies {
    subprojects
        .filter { it.name != "test-app" && !it.path.startsWith(":demos") }
        .filter { it.buildFile.exists() }
        .forEach { dokka(project(it.path)) }
}
