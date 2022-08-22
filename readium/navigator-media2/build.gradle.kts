import ModuleDependency.Project.navigator
import ModuleDependency.Project.shared

/*
 * Copyright 2022 Readium Foundation. All rights reserved.
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
    resourcePrefix = "readium_"

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
        jvmTarget = "1.8"
        freeCompilerArgs = freeCompilerArgs + "-Xopt-in=kotlin.RequiresOptIn"
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

afterEvaluate {
    publishing {
        publications {
            create<MavenPublication>("release") {
                from(components.getByName("release"))
                groupId = "com.github.readium"
                artifactId = "readium-navigator-media2"
                artifact(tasks.findByName("sourcesJar"))
                artifact(tasks.findByName("javadocsJar"))
            }
        }
    }
}

dependencies {
    implementation(fileTree(mapOf("dir" to "libs", "include" to listOf("*.jar"))))

    api(shared())
    api(navigator())

    implementation(libs.bundles.coroutines)

    implementation(libs.timber)

    implementation(libs.bundles.media2)

    implementation(libs.exoplayer.core)
    implementation(libs.exoplayer.extension.media2)

    testImplementation(libs.junit)

    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.expresso.core)
}
