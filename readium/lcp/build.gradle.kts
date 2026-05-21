import java.io.File
import java.util.Properties

/*
 * Copyright 2018 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

plugins {
    id("readium.library-conventions")
    alias(libs.plugins.ksp)
}

android {
    namespace = "org.readium.r2.lcp"
    defaultConfig {
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }
}

kotlin {
    compilerOptions {
        languageVersion = org.jetbrains.kotlin.gradle.dsl.KotlinVersion.KOTLIN_2_3
        // See https://github.com/readium/kotlin-toolkit/pull/525#issuecomment-2300084041
        freeCompilerArgs.add("-Xconsistent-data-class-copy-visibility")
    }
}

dependencies {
    implementation(libs.kotlinx.coroutines.android)

    api(project(":readium:readium-shared"))

    val lcpDep: String? = System.getenv("LCP_DEPENDENCY") ?: run {
        val localProperties = Properties()
        val localPropertiesFile = File(project.rootDir, "local.properties")
        if (localPropertiesFile.exists()) {
            localPropertiesFile.inputStream().use { localProperties.load(it) }
        }
        localProperties.getProperty("lcp.dependency")
    }
    lcpDep?.let { implementation(it) }

    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.core)
    implementation(libs.google.material)
    implementation(libs.timber)
    implementation(libs.androidx.browser)
    implementation(libs.kotlinx.datetime)

    implementation(libs.bundles.room)
    ksp(libs.androidx.room.compiler)

    // Tests
    testImplementation(libs.junit)
    testImplementation(libs.kotlin.junit)
    testImplementation(libs.mockk)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.robolectric)

    // Instrumented Tests (requires LCP being enabled)
    androidTestImplementation(libs.androidx.runner)
    androidTestImplementation(libs.androidx.junit.ktx)
    androidTestImplementation(libs.kotlinx.coroutines.test)
}
