import java.net.URI
import java.util.Properties

/*
 * Copyright 2024 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

plugins {
    `kotlin-dsl`
    `maven-publish`
}

repositories {
    gradlePluginPortal()
    google()
    mavenCentral()
}

dependencies {
    implementation(libs.plugin.android)
    implementation(libs.plugin.kotlin)
    implementation(libs.plugin.maven.publish)
}

publishing {
    publications {
        withType<MavenPublication>().all {
            groupId = "org.readium.r2"
            artifactId = "${project.name}-android"
            version = System.getenv("VERSION") ?: "123.456.789"
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