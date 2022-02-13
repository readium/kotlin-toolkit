/*
 * Copyright 2021 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

import org.jetbrains.dokka.gradle.DokkaTaskPartial

plugins {
    id("com.android.application") version ("7.1.0") apply false
    id("com.android.library") version ("7.1.0") apply false
    id("org.jetbrains.kotlin.android") version ("1.6.10") apply false
    id("org.jetbrains.dokka") version ("1.6.10") apply true
}

subprojects {
    tasks.register<Jar>("javadocsJar") {
        archiveClassifier.set("javadoc")
    }

    tasks.register<Jar>("sourcesJar") {
        archiveClassifier.set("sources")
        from("src/main/java", "src/main/resources")
    }
}

tasks.register("clean", Delete::class).configure {
    delete(rootProject.buildDir)
}

tasks.register("cleanDocs", Delete::class).configure {
    delete("${project.rootDir}/docs/readium", "${project.rootDir}/docs/index.md")
}

tasks.withType<DokkaTaskPartial>().configureEach {
    dokkaSourceSets {
        configureEach {
            reportUndocumented.set(false)
            skipEmptyPackages.set(false)
            skipDeprecated.set(true)
        }
    }
}

tasks.named<org.jetbrains.dokka.gradle.DokkaMultiModuleTask>("dokkaGfmMultiModule").configure {
    outputDirectory.set(file("${projectDir.path}/docs"))
}
