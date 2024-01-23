/*
 * Copyright 2018 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

plugins {
    id("readium.library-conventions")
}

android {
    namespace = "org.readium.r2.streamer"
}

dependencies {
    api(project(":readium:readium-shared"))

    api(files("libs/nanohttpd-2.3.2.jar", "libs/nanohttpd-nanolets-2.3.2.jar"))

    implementation(libs.androidx.appcompat)
    @Suppress("GradleDependency")
    implementation(libs.timber)
    // AM NOTE: conflicting support libraries, excluding these
    implementation("com.mcxiaoke.koi:core:0.5.5") {
        exclude(module = "support-v4")
    }
    // useful extensions (only ~100k)
    implementation("com.mcxiaoke.koi:async:0.5.5") {
        exclude(module = "support-v4")
    }
    implementation(libs.joda.time)
    implementation(libs.kotlinx.coroutines.core)

    // Tests
    testImplementation(libs.junit)

    androidTestImplementation(libs.androidx.ext.junit)
    androidTestImplementation(libs.androidx.expresso.core)
    testImplementation(libs.assertj)
    testImplementation(libs.robolectric)
}
