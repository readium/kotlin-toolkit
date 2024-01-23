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

    implementation(libs.androidx.appcompat)
    implementation(libs.timber)
    implementation(libs.joda.time)
    implementation("nl.komponents.kovenant:kovenant:3.3.0")
    implementation(libs.kotlinx.coroutines.core)

    // Tests
    testImplementation(libs.junit)

    androidTestImplementation(libs.androidx.ext.junit)
    androidTestImplementation(libs.androidx.expresso.core)

    testImplementation(libs.robolectric)
}
