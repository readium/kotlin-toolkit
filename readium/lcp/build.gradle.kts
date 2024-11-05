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

    kotlinOptions {
        // See https://github.com/readium/kotlin-toolkit/pull/525#issuecomment-2300084041
        freeCompilerArgs = freeCompilerArgs + ("-Xconsistent-data-class-copy-visibility")
    }
}

dependencies {
    implementation(libs.kotlinx.coroutines.android)

    api(project(":readium:readium-shared"))

    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.core)
    implementation(libs.google.material)
    implementation(libs.timber)
    implementation("com.mcxiaoke.koi:async:0.5.5") {
        exclude(module = "support-v4")
    }
    implementation("com.mcxiaoke.koi:core:0.5.5") {
        exclude(module = "support-v4")
    }
    implementation(libs.joda.time)
    implementation(libs.androidx.browser)

    implementation(libs.bundles.room)
    ksp(libs.androidx.room.compiler)

    // Tests
    testImplementation(libs.junit)
    testImplementation(libs.kotlin.junit)
}
