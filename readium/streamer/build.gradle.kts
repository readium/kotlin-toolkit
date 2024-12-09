import java.net.URI
import java.util.Properties

/*
 * Copyright 2018 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

plugins {
    id("readium.library-conventions")
}

android {
    namespace = "org.readium.r2.streamer"
}

dependencies {
    implementation(libs.booco.readium.libs.shared)

    api(files("libs/nanohttpd-2.3.2.jar", "libs/nanohttpd-nanolets-2.3.2.jar"))

    @Suppress("GradleDependency")
    implementation(libs.timber)
    // AM NOTE: conflicting support libraries, excluding these
    implementation("com.mcxiaoke.koi:core:0.5.5") {
        exclude(module = "support-v4")
    }
    implementation(libs.kotlinx.coroutines.android)

    // Tests
    testImplementation(libs.junit)
    testImplementation(libs.kotlin.junit)
    testImplementation(libs.assertj)
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