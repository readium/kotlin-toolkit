/*
 * Copyright 2018 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

plugins {
    id("com.android.library")
    kotlin("android")
    kotlin("plugin.parcelize")
}

android {
    compileSdk = 33
    defaultConfig {
        minSdk = 21
        targetSdk = 33
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
        freeCompilerArgs = freeCompilerArgs + listOf(
            "-opt-in=kotlin.RequiresOptIn",
            "-opt-in=org.readium.r2.shared.InternalReadiumApi"
        )
    }
    testOptions {
        unitTests.isIncludeAndroidResources = true
    }
    buildTypes {
        getByName("release") {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android.txt"))
        }
    }
    namespace = "org.readium.r2.opds"
}

rootProject.ext["publish.artifactId"] = "readium-opds"
apply(from = "$rootDir/scripts/publish-module.gradle")

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
