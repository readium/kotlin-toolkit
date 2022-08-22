/*
 * Copyright 2021 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

enableFeaturePreview("VERSION_CATALOGS")

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

    plugins {
        id("com.android.application") version ("7.1.0")
        id("com.android.library") version ("7.1.0")
        id("org.jetbrains.kotlin.android") version ("1.6.10")
        id("org.jetbrains.dokka") version ("1.6.10")
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
    }
}

rootProject.name = "Readium"

include(":readium:shared")
include(":readium:streamer")
include(":readium:navigator")
include(":readium:navigator-media2")
include(":readium:opds")
include(":readium:lcp")

if (System.getenv("JITPACK") == null) {
    include("test-app")
}
