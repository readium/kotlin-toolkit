/*
 * Copyright 2022 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

plugins {
    id("readium.library-conventions")
}

android {
    namespace = "org.readium.navigators.media.common"
}

dependencies {
    api(project(":readium:readium-shared"))
    api(project(":readium:readium-navigator"))

    implementation(libs.androidx.media3.common)
    implementation(libs.timber)
    implementation(libs.kotlinx.coroutines.android)
}
