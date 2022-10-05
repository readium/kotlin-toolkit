/*
 * Copyright 2021 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

plugins {
    id("com.android.application")
    id("kotlin-android")
    id("kotlin-kapt")
    id("kotlin-parcelize")
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
        kotlinCompilerExtensionVersion = libs.versions.compose.compiler.get()
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
    implementation(libs.legacy.v4)

    implementation(project(":readium:shared"))
    implementation(project(":readium:streamer"))
    implementation(project(":readium:navigator"))
    implementation(project(":readium:navigator-media2"))
    implementation(project(":readium:opds"))
    implementation(project(":readium:lcp"))
    // Only required if you want to support PDF files using PDFium.
    implementation(project(":readium:adapters:pdfium"))

    implementation(libs.compose.activity)
    implementation(libs.activity.ktx)
    implementation(libs.appcompat)
    implementation(libs.browser)
    implementation(libs.cardview)
    implementation(libs.compose.animation)
    implementation(libs.compose.foundation)
    implementation(libs.compose.material.icons)
    implementation(libs.compose.material)
    implementation(libs.compose.theme.adapter)
    implementation(libs.constraint.layout)
    implementation(libs.core)
    implementation (libs.datastore.preferences)
    implementation(libs.fragment.ktx)
    implementation(libs.lifecycle.livedata)
    implementation(libs.lifecycle.runtime)
    implementation(libs.lifecycle.viewmodel)
    implementation(libs.lifecycle.viewmodel.compose)
    implementation(libs.lifecycle.common)
    implementation(libs.navigation.fragment)
    implementation(libs.navigation.ui)
    implementation(libs.paging)
    implementation(libs.recyclerview)
    implementation(libs.viewpager2)
    implementation(libs.webkit)
    implementation("com.github.edrlab.nanohttpd:nanohttpd:master-SNAPSHOT") {
        exclude(group = "org.parboiled")
    }
    implementation("com.github.edrlab.nanohttpd:nanohttpd-nanolets:master-SNAPSHOT") {
        exclude(group = "org.parboiled")
    }
    implementation(libs.material)
    implementation(libs.timber)
    // AM NOTE: needs to stay this version for now (June 24,2020)
    implementation(libs.picasso)
    implementation(libs.joda.time)
    implementation(libs.coroutines.core)
    // AM NOTE: needs to stay this version for now (June 24,2020)
    implementation(libs.jsoup)

    implementation(libs.bundles.media2)

    // Room database
    implementation(libs.bundles.room)
    kapt(libs.room.compiler)

    implementation(libs.lifecycle.extensions)

    debugImplementation(libs.compose.ui)
    implementation(libs.compose.ui.tooling)

    // Tests
    testImplementation(libs.junit)

    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.expresso.core)
}
