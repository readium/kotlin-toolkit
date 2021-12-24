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
