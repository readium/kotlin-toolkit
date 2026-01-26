/*
 * Copyright 2022 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

plugins {
    id("readium.library-conventions")
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "org.readium.navigators.media.readaloud"
}

dependencies {
    api(project(":readium:navigators:readium-navigator-common"))
    api(project(":readium:navigators:media:readium-navigator-media-common"))

    implementation(libs.androidx.media3.common)
    implementation(libs.androidx.media3.session)

    implementation(libs.timber)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.serialization.json)
}
