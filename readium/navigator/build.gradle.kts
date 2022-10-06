/*
 * Copyright 2018 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

plugins {
    id("com.android.library")
    kotlin("android")
    kotlin("plugin.parcelize")
    id("maven-publish")
    id("org.jetbrains.dokka")
    kotlin("plugin.serialization")
}

android {
    // FIXME: This doesn't pass the lint because some resources don't start with readium_ yet. We need to rename all resources for the next major version.
//    resourcePrefix "readium_"

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
    buildTypes {
        getByName("release") {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android.txt"))
        }
    }
    buildFeatures {
        viewBinding = true
    }
    namespace = "org.readium.r2.navigator"
}

publishing {
    publications {
        create<MavenPublication>("release") {
            groupId = "com.github.readium"
            artifactId = "readium-navigator"
            artifact(tasks.findByName("sourcesJar"))
            artifact(tasks.findByName("javadocsJar"))

            afterEvaluate {
                from(components.getByName("release"))
            }
        }
    }
}

dependencies {
    implementation(fileTree(mapOf("dir" to "libs", "include" to listOf("*.jar"))))

    api(project(":readium:shared"))

    implementation(libs.androidx.activity.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.browser)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.core)
    implementation(libs.androidx.fragment.ktx)
    implementation(libs.androidx.legacy.ui)
    implementation(libs.androidx.legacy.v4)
    implementation(libs.bundles.lifecycle)
    implementation(libs.androidx.recyclerview)
    implementation(libs.androidx.media)
    implementation(libs.androidx.viewpager2)
    implementation(libs.androidx.webkit)
    // Needed to avoid a crash with API 31, see https://stackoverflow.com/a/69152986/1474476
    implementation("androidx.work:work-runtime-ktx:2.7.1")
    implementation("com.duolingo.open:rtl-viewpager:1.0.3")
    // ChrisBane/PhotoView ( for the Zoom handling )
    implementation(libs.photoview)

    implementation(libs.bundles.media2)
    // ExoPlayer is used by the Audio Navigator.
    api(libs.bundles.exoplayer)
    implementation(libs.google.material)
    implementation(libs.timber)
    implementation("com.shopgun.android:utils:1.0.9")
    implementation(libs.joda.time)
    implementation(libs.bundles.coroutines)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.jsoup)

    // Tests
    testImplementation(libs.junit)

    androidTestImplementation(libs.androidx.ext.junit)
    androidTestImplementation(libs.androidx.expresso.core)
    testImplementation(libs.kotlin.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.robolectric)
}
