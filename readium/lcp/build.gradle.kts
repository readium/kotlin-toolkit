/*
 * Copyright 2018 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

plugins {
    id("com.android.library")
    kotlin("android")
    kotlin("plugin.parcelize")
    kotlin("kapt")
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
        allWarningsAsErrors = true
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
    namespace = "org.readium.r2.lcp"
}

rootProject.ext["publish.artifactId"] = "readium-lcp"
apply(from = "$rootDir/scripts/publish-module.gradle")

dependencies {
    implementation(libs.kotlinx.coroutines.core)

    api(project(":readium:readium-shared"))

    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.core)
    implementation(libs.google.material)
    implementation(libs.timber)
    implementation("com.mcxiaoke.koi:async:0.5.5") {
        exclude(module = "support-v4")
    }
    implementation("com.mcxiaoke.koi:core:0.5.5") {
        exclude(module = "support-v4")
    }
    implementation(libs.joda.time)
    implementation("org.zeroturnaround:zt-zip:1.15")
    implementation(libs.androidx.browser)

    implementation(libs.bundles.room)
    kapt(libs.androidx.room.compiler)
    kapt("org.jetbrains.kotlinx:kotlinx-metadata-jvm:0.7.0")

    // Tests
    testImplementation(libs.junit)

    androidTestImplementation(libs.androidx.ext.junit)
    androidTestImplementation(libs.androidx.expresso.core)
}
