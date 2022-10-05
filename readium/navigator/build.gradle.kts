/*
 * Copyright 2018 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

plugins {
    id("com.android.library")
    id("kotlin-android")
    id("kotlin-parcelize")
    id("maven-publish")
    id("org.jetbrains.dokka")
    id("org.jetbrains.kotlin.plugin.serialization")
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

    implementation(libs.activity.ktx)
    implementation(libs.appcompat)
    implementation(libs.browser)
    implementation(libs.constraint.layout)
    implementation(libs.core)
    implementation(libs.fragment.ktx)
    implementation(libs.legacy.ui)
    implementation(libs.legacy.v4)
    implementation(libs.bundles.lifecycle)
    implementation(libs.recyclerview)
    implementation(libs.media)
    implementation(libs.viewpager2)
    implementation(libs.webkit)
    // Needed to avoid a crash with API 31, see https://stackoverflow.com/a/69152986/1474476
    implementation("androidx.work:work-runtime-ktx:2.7.1")
    implementation("com.duolingo.open:rtl-viewpager:1.0.3")
    // ChrisBane/PhotoView ( for the Zoom handling )
    implementation(libs.photoview)

    implementation(libs.bundles.media2)
    // ExoPlayer is used by the Audio Navigator.
    api(libs.bundles.exoplayer)
    implementation(libs.material)
    implementation(libs.timber)
    implementation("com.shopgun.android:utils:1.0.9")
    implementation(libs.joda.time)
    implementation(libs.bundles.coroutines)
    implementation(libs.kotlin.serialization)
    implementation(libs.jsoup)

    // Tests
    testImplementation(libs.junit)

    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.expresso.core)
    testImplementation(libs.kotlin.junit)
    testImplementation(libs.coroutines.test)
    testImplementation(libs.robolectric)
}
