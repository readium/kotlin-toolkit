/*
 * Copyright 2018 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

plugins {
    id("readium.library-conventions")
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "org.readium.r2.shared"
}

dependencies {
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.browser)
    implementation("com.github.kittinunf.fuel:fuel-android:2.3.1")
    implementation("com.github.kittinunf.fuel:fuel:2.3.1")
    implementation(libs.timber)
    implementation(libs.joda.time)
    implementation("nl.komponents.kovenant:kovenant-android:3.3.0")
    implementation("nl.komponents.kovenant:kovenant-combine:3.3.0")
    implementation("nl.komponents.kovenant:kovenant-core:3.3.0")
    implementation("nl.komponents.kovenant:kovenant-functional:3.3.0")
    implementation("nl.komponents.kovenant:kovenant-jvm:3.3.0")
    implementation("nl.komponents.kovenant:kovenant:3.3.0")
    implementation(libs.kotlin.reflect)
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.jsoup)

    // Tests
    testImplementation(libs.junit)

    androidTestImplementation(libs.androidx.ext.junit)
    androidTestImplementation(libs.androidx.expresso.core)

    testImplementation(libs.assertj)
    testImplementation(libs.kotlin.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.robolectric)

    androidTestImplementation(libs.kotlin.junit)
}
