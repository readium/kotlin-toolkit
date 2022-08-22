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
    testOptions {
        unitTests.isIncludeAndroidResources = true
    }
    kotlinOptions {
        freeCompilerArgs = freeCompilerArgs + "-Xopt-in=kotlin.RequiresOptIn"
        allWarningsAsErrors = true
    }
    buildTypes {
        getByName(Flavors.BuildTypes.RELEASE) {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android.txt"))
        }
    }
}

afterEvaluate {
    publishing {
        publications {
            create<MavenPublication>(Flavors.BuildTypes.RELEASE) {
                from(components.getByName(Flavors.BuildTypes.RELEASE))
                groupId = AndroidConfig.GROUP_ID
                artifactId = "readium-streamer"
                artifact(tasks.findByName("sourcesJar"))
                artifact(tasks.findByName("javadocsJar"))
            }
        }
    }
}

dependencies {
    implementation(fileTree(mapOf("dir" to "libs", "include" to listOf("*.jar"))))

    api(shared())

    implementation(libs.appcompat)
    @Suppress("GradleDependency")
    implementation(libs.pdfium.android)
    implementation(libs.timber)
    implementation(libs.bundles.nanohttpd) {
        exclude(group = "org.parboiled")
    }
    //AM NOTE: conflicting support libraries, excluding these
    // useful extensions (only ~100k)
    implementation(libs.bundles.mcxiaoke) {
        exclude(module = "support-v4")
    }
    implementation(libs.joda.time)
    implementation(libs.coroutines.core)

    // Tests
    testImplementation(libs.junit)
    testImplementation(libs.assertj)
    testImplementation(libs.robolectric)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.expresso.core)
}
