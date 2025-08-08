/*
 * Copyright 2022 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

plugins {
    id("readium.library-conventions")
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.compose.compiler)
}

android {
    namespace = "org.readium.navigators.common"

    buildFeatures {
        compose = true
    }
}

dependencies {
    api(project(":readium:readium-shared"))
    api(project(":readium:readium-navigator"))

    api(libs.kotlinx.collections.immutable)

    implementation(libs.androidx.recyclerview)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.bundles.compose)
    implementation(libs.timber)
    implementation(libs.kotlinx.coroutines.android)
}
