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
    namespace = "org.readium.navigators.pdf"

    composeOptions {
        kotlinCompilerExtensionVersion = libs.versions.androidx.compose.compiler.get()
    }
    buildFeatures {
        compose = true
    }
}

dependencies {
    api(project(":readium:readium-shared"))
    api(project(":readium:readium-navigator"))
    api(project(":readium:navigators:readium-navigator-common"))

    implementation(libs.androidx.fragment.ktx)
    implementation(libs.androidx.fragment.compose)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.bundles.compose)
    implementation(libs.timber)
    implementation(libs.kotlinx.coroutines.android)
}
