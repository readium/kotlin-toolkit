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
        allWarningsAsErrors = true
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
}

afterEvaluate {
    publishing {
        publications {
            create<MavenPublication>("release") {
                from(components.getByName("release"))
                groupId = "com.github.readium"
                artifactId = "readium-shared"
                artifact(tasks.findByName("sourcesJar"))
                artifact(tasks.findByName("javadocsJar"))
            }
        }
    }
}

dependencies {
    implementation(fileTree(mapOf("dir" to "libs", "include" to listOf("*.jar"))))

    implementation("androidx.appcompat:appcompat:1.4.1")
    implementation("androidx.browser:browser:1.4.0")
    implementation("com.github.kittinunf.fuel:fuel-android:2.3.1")
    implementation("com.github.kittinunf.fuel:fuel:2.3.1")
    implementation("com.jakewharton.timber:timber:5.0.1")
    implementation("joda-time:joda-time:2.10.14")
    implementation("nl.komponents.kovenant:kovenant-android:3.3.0")
    implementation("nl.komponents.kovenant:kovenant-combine:3.3.0")
    implementation("nl.komponents.kovenant:kovenant-core:3.3.0")
    implementation("nl.komponents.kovenant:kovenant-functional:3.3.0")
    implementation("nl.komponents.kovenant:kovenant-jvm:3.3.0")
    implementation("nl.komponents.kovenant:kovenant:3.3.0")
    implementation("org.jetbrains.kotlin:kotlin-reflect:1.6.21")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.6.2")
    implementation("org.jsoup:jsoup:1.15.1")

    // Tests
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.assertj:assertj-core:3.23.1")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit:1.6.21")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.6.2")
    testImplementation("org.robolectric:robolectric:4.8.1")

    androidTestImplementation("org.jetbrains.kotlin:kotlin-test-junit:1.6.10")
    androidTestImplementation("androidx.test.ext:junit:1.1.3")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.4.0")
}
