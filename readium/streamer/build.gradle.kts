/*
 * Copyright 2018 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

plugins {
    id("readium.multiplatform-conventions")
}

kotlin {
    androidLibrary {
        namespace = "org.readium.r2.streamer"
    }

    sourceSets {
        commonMain.dependencies {
            api(project(":readium:readium-shared"))
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.okio)
        }

        androidMain.dependencies {
            implementation(libs.kotlinx.coroutines.android)
        }

        commonTest.dependencies {
            implementation(kotlin("test"))
            implementation(libs.kotlinx.coroutines.test)
        }

        getByName("androidHostTest").dependencies {
            implementation(libs.junit)
            implementation(libs.kotlin.junit)
        }
    }
}
