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
}

dependencies {
    api(project(":readium:readium-shared"))

    implementation(files("libs/PhotoView-2.3.0.jar"))

    implementation(libs.androidx.activity.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.browser)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.core)
    implementation(libs.androidx.fragment.ktx)
    implementation(libs.androidx.legacy.ui)
    implementation(libs.androidx.legacy.v4)
    implementation(libs.bundles.lifecycle)
    implementation(libs.androidx.recyclerview)
    implementation(libs.androidx.media)
    implementation(libs.bundles.media3)
    implementation(libs.androidx.viewpager2)
    implementation(libs.androidx.webkit)

    implementation(libs.bundles.media2)
    // ExoPlayer is used by the Audio Navigator.
    api(libs.bundles.exoplayer)
    implementation(libs.google.material)
    implementation(libs.timber)
    implementation(libs.joda.time)
    implementation(libs.bundles.coroutines)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.jsoup)

    // Tests
    testImplementation(libs.junit)

    androidTestImplementation(libs.androidx.ext.junit)
    androidTestImplementation(libs.androidx.expresso.core)
    testImplementation(libs.kotlin.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.robolectric)
}
