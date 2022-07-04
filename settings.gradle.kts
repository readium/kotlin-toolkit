/*
 * Copyright 2021 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

// FIXME: Android Studio doesn't support the gradle/libs.versions.toml2 well yet.
//enableFeaturePreview("VERSION_CATALOGS")

pluginManagement {
    repositories {
        gradlePluginPortal()
        google()
        jcenter()
        mavenLocal()
        mavenCentral()
        maven(url = "https://jitpack.io")
        maven(url = "https://s3.amazonaws.com/repo.commonsware.com")
    }

    // Setting the plugin versions here doesn't work with AGP Upgrade Assistant, but we need
    // it to integrate Readium in submodules.
    // See https://github.com/readium/kotlin-toolkit/pull/97
    plugins {
        id("com.android.application") version ("7.2.1")
        id("com.android.library") version ("7.2.1")
        id("org.jetbrains.kotlin.android") version ("1.6.21")
        id("org.jetbrains.dokka") version ("1.6.20")
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        jcenter()
        mavenLocal()
        mavenCentral()
        maven(url = "https://jitpack.io")
        maven(url = "https://s3.amazonaws.com/repo.commonsware.com")
        maven(url = "https://customers.pspdfkit.com/maven")
    }
}

rootProject.name = "Readium"

include(":readium:adapters:pdfium:pdfium-document")
include(":readium:adapters:pdfium:pdfium-navigator")
include(":readium:adapters:pspdfkit:pspdfkit-document")
include(":readium:adapters:pspdfkit:pspdfkit-navigator")
include(":readium:lcp")
include(":readium:navigator")
include(":readium:navigator-media2")
include(":readium:opds")
include(":readium:shared")
include(":readium:streamer")

if (System.getenv("JITPACK") == null) {
    include("test-app")
}
