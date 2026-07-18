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
        commonMain.dependencies {
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.kotlinx.datetime)
            api(libs.kotlinx.serialization.json)
            implementation(libs.ktor.client.core)
            implementation(libs.okio)
            implementation(libs.uri.kmp)
            implementation(libs.xmlutil.core)
        }

        androidMain.dependencies {
            implementation(libs.androidx.annotation)
            implementation(libs.kotlin.reflect)
            implementation(libs.kotlinx.coroutines.android)
            implementation(libs.jsoup)
            implementation(libs.ktor.client.okhttp)
        }

        iosMain.dependencies {
            implementation(libs.ktor.client.darwin)
        }

        commonTest.dependencies {
            implementation(kotlin("test"))
            implementation(libs.kotlinx.coroutines.test)
            implementation(libs.ktor.client.mock)
        }

        getByName("androidHostTest").dependencies {
            implementation(libs.junit)
            implementation(libs.assertj)
            implementation(libs.kotlin.junit)
            implementation(libs.robolectric)
        }
    }
}
