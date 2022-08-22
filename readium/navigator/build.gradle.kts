import ModuleDependency.Project.shared

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
    // FIXME: This doesn't pass the lint because some resources don"t start with r2_ yet. We need to rename all resources for the next major version.
//    resourcePrefix "r2_"

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
        getByName(Flavors.BuildTypes.RELEASE) {
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
            create<MavenPublication>(Flavors.BuildTypes.RELEASE) {
                from(components.getByName(Flavors.BuildTypes.RELEASE))
                groupId = AndroidConfig.GROUP_ID
                artifactId = "readium-navigator"
                artifact(tasks.findByName("sourcesJar"))
                artifact(tasks.findByName("javadocsJar"))
            }
        }
    }
}

dependencies {
    implementation(fileTree(mapOf("dir" to "libs", "include" to listOf("*.jar"))))

    api(shared())

    implementation(libs.activity.ktx)
    implementation(libs.appcompat)
    implementation(libs.androidx.browser)
    implementation(libs.constraint.layout)
    implementation(libs.androidx.core)
    implementation(libs.fragment.ktx)
    implementation(libs.legacy.ui)
    implementation(libs.legacy.v4)
    implementation(libs.bundles.lifecycle)
    implementation(libs.recyclerview)
    implementation(libs.androidx.media)
    implementation(libs.androidx.viewpager2)
    implementation(libs.webkit)
    // Needed to avoid a crash with API 31, see https://stackoverflow.com/a/69152986/1474476
    implementation(libs.androidx.work.runtime)
    implementation(libs.rtl.viewpager)
    api(libs.pdf.viewer)
    // ChrisBane/PhotoView ( for the Zoom handling )
    implementation(libs.photoview)

    implementation(libs.bundles.media2)
    // ExoPlayer is used by the Audio Navigator.
    api(libs.bundles.exoplayer)
    implementation(libs.material)
    implementation(libs.timber)
    implementation(libs.shopgun.utils)
    implementation(libs.joda.time)
    implementation(libs.bundles.coroutines)
    // AM NOTE: needs to stay this version for now (June 24,2020)
    //noinspection GradleDependency
    implementation(libs.jsoup)

    // Tests
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.expresso.core)
}
