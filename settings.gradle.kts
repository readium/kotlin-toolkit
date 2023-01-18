/*
 * Copyright 2021 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

pluginManagement {
    repositories {
        gradlePluginPortal()
        google()
        mavenLocal()
        mavenCentral()
        maven(url = "https://jcenter.bintray.com")
        maven(url = "https://s3.amazonaws.com/repo.commonsware.com")
    }

    // Setting the plugin versions here doesn't work with AGP Upgrade Assistant, but we need
    // it to integrate Readium in submodules.
    // See https://github.com/readium/kotlin-toolkit/pull/97
    plugins {
        id("com.android.application") version ("7.3.1")
        id("com.android.library") version ("7.3.1")
        id("io.github.gradle-nexus.publish-plugin") version ("1.1.0")
        id("org.jetbrains.dokka") version ("1.7.20")
        id("org.jetbrains.kotlin.android") version ("1.7.20")
        id("org.jetbrains.kotlin.plugin.serialization") version ("1.7.10")
        id("org.jlleitschuh.gradle.ktlint") version ("11.0.0")
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenLocal()
        mavenCentral()
        maven(url = "https://jcenter.bintray.com")
        maven(url = "https://s3.amazonaws.com/repo.commonsware.com")
        maven(url = "https://customers.pspdfkit.com/maven")
    }
}

rootProject.name = "Readium"

include(":readium:adapters:pdfium:pdfium-document")
project(":readium:adapters:pdfium:pdfium-document")
    .name = "readium-adapter-pdfium-document"

include(":readium:adapters:pdfium:pdfium-navigator")
project(":readium:adapters:pdfium:pdfium-navigator")
    .name = "readium-adapter-pdfium-navigator"

include(":readium:adapters:pspdfkit:pspdfkit-document")
project(":readium:adapters:pspdfkit:pspdfkit-document")
    .name = "readium-adapter-pspdfkit-document"

include(":readium:adapters:pspdfkit:pspdfkit-navigator")
project(":readium:adapters:pspdfkit:pspdfkit-navigator")
    .name = "readium-adapter-pspdfkit-navigator"

include(":readium:lcp")
project(":readium:lcp")
    .name = "readium-lcp"

include(":readium:navigator")
project(":readium:navigator")
    .name = "readium-navigator"

include(":readium:navigator-media2")
project(":readium:navigator-media2")
    .name = "readium-navigator-media2"

include(":readium:opds")
project(":readium:opds")
    .name = "readium-opds"

include(":readium:shared")
project(":readium:shared")
    .name = "readium-shared"

include(":readium:streamer")
project(":readium:streamer")
    .name = "readium-streamer"

if (System.getenv("JITPACK") == null) {
    include("test-app")
}
