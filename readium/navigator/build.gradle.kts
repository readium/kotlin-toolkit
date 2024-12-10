import java.net.URI
import java.util.Properties

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
    namespace = "org.readium.r2.navigator"

    buildFeatures {
        viewBinding = true
    }

    kotlinOptions {
        // See https://github.com/readium/kotlin-toolkit/pull/525#issuecomment-2300084041
        freeCompilerArgs = freeCompilerArgs + ("-Xconsistent-data-class-copy-visibility")
    }
}

dependencies {
    implementation(libs.booco.readium.libs.shared)

    implementation(files("libs/PhotoView-2.3.0.jar"))

    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.browser)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.core)
    implementation(libs.androidx.fragment.ktx)
    implementation(libs.androidx.legacy.ui)
    implementation(libs.androidx.lifecycle.common)
    implementation(libs.androidx.recyclerview)
    implementation(libs.androidx.media)
    implementation(libs.bundles.media3)
    implementation(libs.androidx.webkit)

    implementation(libs.bundles.media2)
    // ExoPlayer is used by the Audio Navigator.
    api(libs.bundles.exoplayer)
    implementation(libs.timber)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.jsoup)

    // Tests
    testImplementation(libs.junit)

    testImplementation(libs.kotlin.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.robolectric)
}

fun Project.publishing(configure: PublishingExtension.() -> Unit) =
    extensions.configure<PublishingExtension>(configure)

publishing {
    publications {
        withType<MavenPublication>().all {
            groupId = "org.readium.r2"
            artifactId = "${project.name}-android"
            version = System.getenv("VERSION") ?: "123.456.780"
        }
    }

    repositories {
        maven {
            val type = System.getenv("VERSION_TYPE") ?: "snapshots"
            url = URI("s3://booco-android-readium/maven/$type")
            val prop by lazy {
                File("shared.properties")
                    .takeIf { it.exists() }
                    ?.let { Properties().apply { load(it.inputStream()) } }
            }
            credentials(AwsCredentials::class) {
                accessKey = "BOOCO_AWS_ACCESS_KEY_ID"
                    .let { prop?.getProperty(it) ?: System.getenv(it) }
                secretKey = "BOOCO_AWS_SECRET_ACCESS_KEY"
                    .let { prop?.getProperty(it) ?: System.getenv(it) }
            }
        }
    }
}