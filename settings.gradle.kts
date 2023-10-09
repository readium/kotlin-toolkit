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
        id("com.android.application") version ("8.1.0")
        id("com.android.library") version ("8.1.0")
        id("io.github.gradle-nexus.publish-plugin") version ("1.3.0")
        id("org.jetbrains.dokka") version ("1.8.20")
        id("org.jetbrains.kotlin.android") version ("1.9.0")
        id("org.jetbrains.kotlin.plugin.serialization") version ("1.9.0")
        id("org.jlleitschuh.gradle.ktlint") version ("11.5.1")
        // Make sure to align with the Kotlin version.
        // See https://github.com/google/ksp/releases
        id("com.google.devtools.ksp") version ("1.9.0-1.0.12")
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

include(":readium:adapters:pdfium:document")
project(":readium:adapters:pdfium:document")
    .name = "readium-adapter-pdfium-document"

include(":readium:adapters:pdfium:navigator")
project(":readium:adapters:pdfium:navigator")
    .name = "readium-adapter-pdfium-navigator"

include(":readium:adapters:pspdfkit:document")
project(":readium:adapters:pspdfkit:document")
    .name = "readium-adapter-pspdfkit-document"

include(":readium:adapters:pspdfkit:navigator")
project(":readium:adapters:pspdfkit:navigator")
    .name = "readium-adapter-pspdfkit-navigator"

include(":readium:lcp")
project(":readium:lcp")
    .name = "readium-lcp"

include(":readium:navigator")
project(":readium:navigator")
    .name = "readium-navigator"

include(":readium:navigators:media:common")
project(":readium:navigators:media:common")
    .name = "readium-navigator-media-common"

include(":readium:navigators:media:audio")
project(":readium:navigators:media:audio")
    .name = "readium-navigator-media-audio"

include(":readium:navigators:media:tts")
project(":readium:navigators:media:tts")
    .name = "readium-navigator-media-tts"

include(":readium:navigator-media2")
project(":readium:navigator-media2")
    .name = "readium-navigator-media2"

include(":readium:adapters:exoplayer:audio")
project(":readium:adapters:exoplayer:audio")
    .name = "readium-adapter-exoplayer-audio"

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
