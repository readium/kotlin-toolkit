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
    id(Plugins.KAPT)
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
        jvmTarget = "1.8"
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
                artifactId = "readium-lcp"
                artifact(tasks.findByName("sourcesJar"))
                artifact(tasks.findByName("javadocsJar"))
            }
        }
    }
}

dependencies {
    implementation(fileTree(mapOf("dir" to "libs", "include" to listOf("*.jar"))))

    implementation(libs.coroutines.core)

    api(shared())

    implementation(libs.constraint.layout)
    implementation(libs.androidx.core)
    implementation(libs.material)
    implementation(libs.timber)
    implementation(libs.bundles.mcxiaoke) {
        exclude(module = "support-v4")
    }
    implementation(libs.joda.time)
    implementation(libs.zeroturnaround)
    implementation(libs.androidx.browser)

    // Room database
    implementation(libs.bundles.room)
    kapt(libs.room.compiler)

    // Tests
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.expresso.core)
}
