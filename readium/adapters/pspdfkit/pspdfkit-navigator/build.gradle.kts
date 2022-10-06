/*
 * Copyright 2022 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

plugins {
    id("com.android.library")
    kotlin("android")
    kotlin("plugin.parcelize")
    id("maven-publish")
    id("org.jetbrains.dokka")
}

android {
    resourcePrefix = "readium_"

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
    namespace = "org.readium.adapters.pspdfkit.navigator"
}

publishing {
    publications {
        create<MavenPublication>("release") {
            groupId = "com.github.readium"
            artifactId = "readium-adapter-pspdfkit-navigator"
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
    api(project(":readium:navigator"))
    api(project(":readium:adapters:pspdfkit:pspdfkit-document"))

    implementation(libs.androidx.fragment.ktx)
    implementation(libs.timber)
    implementation(libs.pspdfkit)
    implementation(libs.bundles.coroutines)

    testImplementation(libs.junit)

    androidTestImplementation(libs.androidx.ext.junit)
    androidTestImplementation(libs.androidx.expresso.core)
}
