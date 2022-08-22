/*
 * Copyright 2018 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

plugins {
    id(Plugins.ANDROID_LIBRARY)
    id(Plugins.KOTLIN_ANDROID)
    id(Plugins.KOTLIN_PARCELIZE)
    id(Plugins.MAVEN_PUBLISH)
    id(Plugins.DOKKA)
}

android {
    compileSdk = AndroidConfig.COMPILE_SDK_VERSION
    defaultConfig {
        minSdk = AndroidConfig.MIN_SDK_VERSION
        targetSdk = AndroidConfig.TARGET_SDK_VERSION
        testInstrumentationRunner = AndroidConfig.TEST_INSTRUMENTATION_RUNNER
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        freeCompilerArgs = freeCompilerArgs + "-Xopt-in=kotlin.RequiresOptIn"
        allWarningsAsErrors = true
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

    implementation(libs.appcompat)
    implementation(libs.androidx.browser)
    implementation("com.github.kittinunf.fuel:fuel-android:2.3.1")
    implementation("com.github.kittinunf.fuel:fuel:2.3.1")
    implementation(libs.timber)
    implementation(libs.joda.time)
    implementation(libs.bundles.kovenant)
    implementation("org.jetbrains.kotlin:kotlin-reflect:1.6.10")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.6.0")
    implementation(libs.jsoup)

    // Tests
    testImplementation(libs.junit)
    testImplementation(libs.assertj)
    testImplementation(libs.kotlin.junit)
    testImplementation(libs.coroutines.test)
    testImplementation(libs.robolectric)

    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.expresso.core)
}
