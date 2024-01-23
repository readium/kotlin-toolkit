/*
 * Copyright 2022 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

plugins {
    id("readium.library-conventions")
}

android {
    namespace = "org.readium.adapter.pspdfkit.document"
}

dependencies {
    api(project(":readium:readium-shared"))

    implementation(libs.androidx.core)
    implementation(libs.timber)
    implementation(libs.pspdfkit)
    implementation(libs.bundles.coroutines)

    testImplementation(libs.junit)

    androidTestImplementation(libs.androidx.ext.junit)
    androidTestImplementation(libs.androidx.expresso.core)
}
