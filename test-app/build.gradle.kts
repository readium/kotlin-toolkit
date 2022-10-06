/*
 * Copyright 2021 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

plugins {
    id("com.android.application")
    kotlin("android")
    kotlin("kapt")
    kotlin("plugin.parcelize")
}

android {
    compileSdk = 33
    defaultConfig {
        minSdk = 21
        targetSdk = 33

        applicationId = "org.readium.r2reader"

        versionName = "2.2.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        ndk.abiFilters.add("armeabi-v7a")
        ndk.abiFilters.add("arm64-v8a")
        ndk.abiFilters.add("x86")
        ndk.abiFilters.add("x86_64")
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
        freeCompilerArgs = freeCompilerArgs + "-opt-in=kotlin.RequiresOptIn"
    }
    composeOptions {
        kotlinCompilerExtensionVersion = libs.versions.androidx.compose.compiler.get()
    }
    buildFeatures {
        viewBinding = true
        compose = true
    }
    buildTypes {
        getByName("release") {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android.txt"))
        }
    }
    packagingOptions {
        resources.excludes.add("META-INF/*")
    }

    sourceSets {
        getByName("main") {
            java.srcDirs("src/main/java")
            res.srcDirs("src/main/res")
            assets.srcDirs("src/main/assets")
        }
    }
}

dependencies {
    implementation(fileTree(mapOf("dir" to "libs", "include" to listOf("*.jar"))))
    implementation(libs.kotlin.stdlib)
    implementation(libs.androidx.legacy.v4)

    implementation(project(":readium:shared"))
    implementation(project(":readium:streamer"))
    implementation(project(":readium:navigator"))
    implementation(project(":readium:navigator-media2"))
    implementation(project(":readium:opds"))
    implementation(project(":readium:lcp"))
    // Only required if you want to support PDF files using PDFium.
    implementation(project(":readium:adapters:pdfium"))

    implementation(libs.androidx.compose.activity)
    implementation(libs.androidx.activity.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.browser)
    implementation(libs.androidx.cardview)
    implementation(libs.androidx.compose.animation)
    implementation(libs.androidx.compose.foundation)
    implementation(libs.androidx.compose.material.icons)
    implementation(libs.androidx.compose.material)
    implementation(libs.androidx.compose.theme.adapter)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.core)
    implementation (libs.androidx.datastore.preferences)
    implementation(libs.androidx.fragment.ktx)
    implementation(libs.androidx.lifecycle.livedata)
    implementation(libs.androidx.lifecycle.runtime)
    implementation(libs.androidx.lifecycle.viewmodel)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.common)
    implementation(libs.androidx.navigation.fragment)
    implementation(libs.androidx.navigation.ui)
    implementation(libs.androidx.paging)
    implementation(libs.androidx.recyclerview)
    implementation(libs.androidx.viewpager2)
    implementation(libs.androidx.webkit)
    implementation("com.github.edrlab.nanohttpd:nanohttpd:master-SNAPSHOT") {
        exclude(group = "org.parboiled")
    }
    implementation("com.github.edrlab.nanohttpd:nanohttpd-nanolets:master-SNAPSHOT") {
        exclude(group = "org.parboiled")
    }
    implementation(libs.google.material)
    implementation(libs.timber)
    // AM NOTE: needs to stay this version for now (June 24,2020)
    implementation(libs.picasso)
    implementation(libs.joda.time)
    implementation(libs.kotlinx.coroutines.core)
    // AM NOTE: needs to stay this version for now (June 24,2020)
    implementation(libs.jsoup)

    implementation(libs.bundles.media2)

    // Room database
    implementation(libs.bundles.room)
    kapt(libs.androidx.room.compiler)

    implementation(libs.androidx.lifecycle.extensions)

    debugImplementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.tooling)

    // Tests
    testImplementation(libs.junit)

    androidTestImplementation(libs.androidx.ext.junit)
    androidTestImplementation(libs.androidx.expresso.core)
}
