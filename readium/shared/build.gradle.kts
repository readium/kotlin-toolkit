/*
 * Copyright 2018 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

plugins {
    id("readium.multiplatform-conventions")
    alias(libs.plugins.kotlin.serialization)
}

kotlin {
    androidLibrary {
        namespace = "org.readium.r2.shared"
    }

    sourceSets {
        androidMain.dependencies {
            api(project(":readium:readium-shared-zip-legacy"))
            implementation(libs.androidx.annotation)
            implementation(libs.timber)
            implementation(libs.kotlin.reflect)
            implementation(libs.kotlinx.coroutines.android)
            implementation(libs.kotlinx.datetime)
            implementation(libs.kotlinx.serialization.json)
            implementation(libs.jsoup)
        }

        commonTest.dependencies {
            implementation(kotlin("test"))
            implementation(libs.okio)
        }

        getByName("androidHostTest").dependencies {
            implementation(libs.junit)
            implementation(libs.assertj)
            implementation(libs.kotlin.junit)
            implementation(libs.kotlinx.coroutines.test)
            implementation(libs.robolectric)
        }
    }
}
