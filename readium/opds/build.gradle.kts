/*
 * Copyright 2018 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

plugins {
    id("readium.library-conventions")
}

android {
    namespace = "org.readium.r2.opds"
}

dependencies {
    api(project(":readium:readium-shared"))

    implementation(libs.timber)
    implementation(libs.kotlinx.coroutines.android)

    // Tests
    testImplementation(libs.junit)
    testImplementation(libs.robolectric)
}
