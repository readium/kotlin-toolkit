/*
 * Copyright 2021 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

enableFeaturePreview("VERSION_CATALOGS")

pluginManagement {
    repositories {
        gradlePluginPortal()
    }
}

rootProject.name = "Readium"

include(":readium:shared")
include(":readium:streamer")
include(":readium:navigator")
include(":readium:opds")
include(":readium:lcp")

if (System.getenv("JITPACK") == null) {
    include("test-app")
}

dependencyResolutionManagement {
    versionCatalogs {
        create("libs") {
            // Kotlin
            version("kotlin", "1.6.10")
            alias("kotlin-gradle").to("org.jetbrains.kotlin", "kotlin-gradle-plugin").versionRef("kotlin")

            // Dokka
            version("dokka", "1.5.30")
            alias("dokka-gradle").to("org.jetbrains.dokka", "dokka-gradle-plugin").versionRef("dokka")

            // Room database
            version("room", "2.4.0")
            alias("room-runtime").to("androidx.room", "room-runtime").versionRef("room")
            alias("room-ktx").to("androidx.room", "room-ktx").versionRef("room")
            alias("room-compiler").to("androidx.room", "room-compiler").versionRef("room")
            bundle("room", listOf("room-runtime", "room-compiler"))
        }
    }
}
