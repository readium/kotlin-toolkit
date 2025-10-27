/*
 * Copyright 2018 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

plugins {
    id("readium.library-conventions")
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "org.readium.r2.navigator"

    buildFeatures {
        viewBinding = true
    }

    kotlinOptions {
    }
}

kotlin {
    compilerOptions {
        // See https://github.com/readium/kotlin-toolkit/pull/525#issuecomment-2300084041
        freeCompilerArgs.add("-Xconsistent-data-class-copy-visibility")
    }
}

dependencies {
    api(project(":readium:readium-shared"))

    implementation(files("libs/PhotoView-2.3.0.jar"))

    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.browser)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.core)
    implementation(libs.androidx.fragment.ktx)
    implementation(libs.androidx.legacy.ui)
    implementation(libs.androidx.lifecycle.common)
    implementation(libs.androidx.recyclerview)
    implementation(libs.bundles.media3)
    implementation(libs.androidx.webkit)

    implementation(libs.timber)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.jsoup)

    // Tests
    testImplementation(libs.junit)

    testImplementation(libs.kotlin.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.robolectric)
}
