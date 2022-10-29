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
    testOptions {
        unitTests.isIncludeAndroidResources = true
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

    api(project(":readium:readium-shared"))

    implementation("androidx.activity:activity-ktx:1.6.0")
    implementation("androidx.appcompat:appcompat:1.5.1")
    implementation("androidx.browser:browser:1.4.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    implementation("androidx.core:core-ktx:1.9.0")
    implementation("androidx.fragment:fragment-ktx:1.5.3")
    implementation("androidx.legacy:legacy-support-core-ui:1.0.0")
    implementation("androidx.legacy:legacy-support-v4:1.0.0")
    implementation("androidx.lifecycle:lifecycle-livedata-ktx:2.5.1")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.5.1")
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.5.1")
    implementation("androidx.lifecycle:lifecycle-viewmodel-savedstate:2.5.1")
    implementation("androidx.recyclerview:recyclerview:1.2.1")
    implementation("androidx.media:media:1.6.0")
    implementation("androidx.viewpager2:viewpager2:1.0.0")
    implementation("androidx.webkit:webkit:1.5.0")
    // Needed to avoid a crash with API 31, see https://stackoverflow.com/a/69152986/1474476
    implementation("androidx.work:work-runtime-ktx:2.7.1")
    implementation("com.duolingo.open:rtl-viewpager:1.0.3")
    // ChrisBane/PhotoView ( for the Zoom handling )
    implementation("com.github.chrisbanes:PhotoView:2.3.0")

    implementation("androidx.media2:media2-session:1.2.1")
    implementation("androidx.media2:media2-player:1.2.1")
    // ExoPlayer is used by the Audio Navigator.
    api("com.google.android.exoplayer:exoplayer-core:2.18.1")
    api("com.google.android.exoplayer:exoplayer-ui:2.18.1")
    api("com.google.android.exoplayer:extension-mediasession:2.18.1")
    api("com.google.android.exoplayer:extension-media2:2.18.1")
    api("com.google.android.exoplayer:extension-workmanager:2.18.1")
    implementation("com.google.android.material:material:1.6.1")
    implementation("com.jakewharton.timber:timber:5.0.1")
    implementation("com.shopgun.android:utils:1.0.9")
    implementation("joda-time:joda-time:2.10.14")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.6.4")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.6.4")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.4.0")
    implementation("org.jsoup:jsoup:1.15.2")

    // Tests
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit:1.7.10")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.6.4")
    testImplementation("org.robolectric:robolectric:4.8.1")
    androidTestImplementation("androidx.test.ext:junit:1.1.3")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.4.0")
}
