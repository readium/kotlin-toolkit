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
        kotlinCompilerExtensionVersion = "1.2.0-rc02"
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
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8:1.6.21")
    implementation("androidx.legacy:legacy-support-v4:1.0.0")

    implementation(project(":readium:shared"))
    implementation(project(":readium:streamer"))
    implementation(project(":readium:navigator"))
    implementation(project(":readium:navigator-media2"))
    implementation(project(":readium:opds"))
    implementation(project(":readium:lcp"))
    // Only required if you want to support PDF files using PDFium.
    implementation(project(":readium:adapters:pdfium"))

    implementation("androidx.activity:activity-compose:1.4.0")
    implementation("androidx.activity:activity-ktx:1.4.0")
    implementation("androidx.appcompat:appcompat:1.4.1")
    implementation("androidx.browser:browser:1.4.0")
    implementation("androidx.cardview:cardview:1.0.0")
    implementation("androidx.compose.animation:animation:1.1.1")
    implementation("androidx.compose.foundation:foundation:1.2.0-rc02")
    implementation("androidx.compose.material:material-icons-extended:1.1.1")
    implementation("androidx.compose.material:material:1.1.1")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    implementation("androidx.core:core-ktx:1.7.0")
    implementation("androidx.fragment:fragment-ktx:1.4.1")
    implementation("androidx.lifecycle:lifecycle-livedata-ktx:2.4.1")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.4.1")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.4.1")
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.4.1")
    implementation("androidx.navigation:navigation-fragment-ktx:2.4.2")
    implementation("androidx.navigation:navigation-ui-ktx:2.4.2")
    implementation("androidx.paging:paging-runtime-ktx:3.1.1")
    implementation("androidx.recyclerview:recyclerview:1.2.1")
    implementation("androidx.viewpager2:viewpager2:1.0.0")
    implementation("androidx.webkit:webkit:1.4.0")
    implementation("com.github.edrlab.nanohttpd:nanohttpd:master-SNAPSHOT") {
        exclude(group = "org.parboiled")
    }
    implementation("com.github.edrlab.nanohttpd:nanohttpd-nanolets:master-SNAPSHOT") {
        exclude(group = "org.parboiled")
    }
    implementation("com.google.android.material:material:1.6.1")
    implementation("com.jakewharton.timber:timber:5.0.1")
    // AM NOTE: needs to stay this version for now (June 24,2020)
    implementation("com.squareup.picasso:picasso:2.71828")
    implementation("joda-time:joda-time:2.10.14")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.6.2")
    // AM NOTE: needs to stay this version for now (June 24,2020)
    implementation("org.jsoup:jsoup:1.15.1")

    implementation("androidx.media2:media2-session:1.2.1")
    implementation("androidx.media2:media2-player:1.2.1")

    // Room database
    val roomVersion = "2.4.2"
    implementation("androidx.room:room-runtime:$roomVersion")
    implementation("androidx.room:room-ktx:$roomVersion")
    kapt("androidx.room:room-compiler:$roomVersion")

    implementation("androidx.lifecycle:lifecycle-extensions:2.2.0")
    kapt("androidx.lifecycle:lifecycle-compiler:2.4.1")

    debugImplementation("androidx.compose.ui:ui-tooling:1.1.1")
    implementation("androidx.compose.ui:ui-tooling-preview:1.1.1")

    // Tests
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.3")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.4.0")
}
