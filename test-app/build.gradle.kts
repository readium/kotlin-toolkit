import ModuleDependency.Project.lcp
import ModuleDependency.Project.navigator
import ModuleDependency.Project.navigatorMedia2
import ModuleDependency.Project.opds
import ModuleDependency.Project.shared
import ModuleDependency.Project.streamer

/*
 * Copyright 2021 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

plugins {
    id(Plugins.ANDROID_APPLICATION)
    id(Plugins.KOTLIN_ANDROID)
    id(Plugins.KAPT)
    id(Plugins.KOTLIN_PARCELIZE)
}

android {

    compileSdk = AndroidConfig.COMPILE_SDK_VERSION
    defaultConfig {

        minSdk = AndroidConfig.MIN_SDK_VERSION
        targetSdk = AndroidConfig.TARGET_SDK_VERSION

        applicationId = AndroidConfig.APP_ID

        versionName = AndroidConfig.VERSION_NAME

        testInstrumentationRunner = AndroidConfig.TEST_INSTRUMENTATION_RUNNER
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
        freeCompilerArgs = freeCompilerArgs + "-Xopt-in=kotlin.RequiresOptIn"
    }
    buildFeatures {
        viewBinding = true
    }
    buildTypes {
        getByName(Flavors.BuildTypes.RELEASE) {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android.txt"))
        }
    }
    packagingOptions {
        resources.excludes.add("META-INF/*")
    }

    sourceSets {
        getByName(Flavors.Default.MAIN) {
            java.srcDirs("src/main/java")
            res.srcDirs("src/main/res")
            assets.srcDirs("src/main/assets")
        }
    }
}

dependencies {
    implementation(fileTree(mapOf("dir" to "libs", "include" to listOf("*.jar"))))
    implementation(libs.kotlin.stdlib.jdk8)
    implementation(libs.legacy.v4)

    implementation(shared())
    implementation(streamer())
    implementation(navigator())
    implementation(navigatorMedia2())

    implementation(opds())
    implementation(lcp())

    implementation(libs.androidx.core)
    implementation(libs.activity.ktx)
    implementation(libs.appcompat)
    implementation(libs.androidx.browser)
    implementation(libs.androidx.cardview)
    implementation(libs.constraint.layout)
    implementation(libs.fragment.ktx)
    implementation(libs.lifecycle.livedata)
    implementation(libs.lifecycle.runtime)
    implementation(libs.lifecycle.viewmodel)
    implementation(libs.bundles.navigation)
    implementation(libs.androidx.paging)
    implementation(libs.recyclerview)
    implementation(libs.androidx.viewpager2)
    implementation(libs.webkit)
    implementation(libs.bundles.nanohttpd) {
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
    kapt(libs.lifecycle.compiler)

    // Tests
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.expresso.core)
}
