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
    val shouldDocument = name != "test-app" && !path.startsWith(":demos")

    if (shouldDocument) {
        apply(plugin = "org.jetbrains.dokka")
    }
    apply(plugin = "org.jlleitschuh.gradle.ktlint")

    ktlint {
        android.set(true)
    }
}

tasks.register("cleanDocs", Delete::class).configure {
    delete(
        "${project.rootDir}/docs/api",
        "${project.rootDir}/docs/index.md",
        "${project.rootDir}/site"
    )
}

subprojects {
    val shouldDocument = name != "test-app" && !path.startsWith(":demos")

    if (shouldDocument) {
        pluginManager.withPlugin("org.jetbrains.dokka") {
            configure<org.jetbrains.dokka.gradle.DokkaExtension> {
                dokkaSourceSets.configureEach {
                    reportUndocumented.set(false)
                    skipEmptyPackages.set(false)
                    skipDeprecated.set(true)
                }
            }
        }
    }
}

dependencies {
    subprojects.filter { it.name != "test-app" && !it.path.startsWith(":demos") }.forEach {
        dokka(project(it.path))
    }
}
