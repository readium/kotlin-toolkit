/*
 * Copyright 2022 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

plugins {
    id("com.android.library")
    kotlin("android")
    kotlin("plugin.parcelize")
}

android {
    resourcePrefix = "readium_"

    compileSdk = 34

    defaultConfig {
        minSdk = 21
        targetSdk = 34
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = JavaVersion.VERSION_17.toString()
        freeCompilerArgs = freeCompilerArgs + listOf(
            "-opt-in=kotlin.RequiresOptIn",
            "-opt-in=org.readium.r2.shared.InternalReadiumApi"
        )
    }
    buildTypes {
        getByName("release") {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android.txt"))
        }
    }
    buildFeatures {
        viewBinding = true
    }
    namespace = "org.readium.navigators.media.audio"
}

kotlin {
    explicitApi()
}

rootProject.ext["publish.artifactId"] = "readium-navigator-media-audio"
apply(from = "$rootDir/scripts/publish-module.gradle")

dependencies {
    api(project(":readium:navigators:media:readium-navigator-media-common"))

    implementation(libs.androidx.media3.common)
    implementation(libs.androidx.media3.session)

    implementation(libs.androidx.core)
    implementation(libs.timber)
    implementation(libs.bundles.coroutines)
}
