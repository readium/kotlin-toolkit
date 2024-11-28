/*
 * Copyright 2021 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

import java.net.URI
import java.util.Properties
import org.jetbrains.dokka.gradle.DokkaTaskPartial

plugins {
    alias(libs.plugins.dokka)
    alias(libs.plugins.ktlint)
    alias(libs.plugins.compose.compiler) apply false
    `maven-publish`
}

subprojects {
    if (name != "test-app") {
        apply(plugin = "org.jetbrains.dokka")
    }
    apply(plugin = "org.jlleitschuh.gradle.ktlint")

    ktlint {
        android.set(true)
    }
}

tasks.register("cleanDocs", Delete::class).configure {
    delete(
        "${project.rootDir}/docs/readium",
        "${project.rootDir}/docs/index.md",
        "${project.rootDir}/site"
    )
}

tasks.withType<DokkaTaskPartial>().configureEach {
    dokkaSourceSets {
        configureEach {
            reportUndocumented.set(false)
            skipEmptyPackages.set(false)
            skipDeprecated.set(true)
        }
    }
}

tasks.named<org.jetbrains.dokka.gradle.DokkaMultiModuleTask>("dokkaGfmMultiModule").configure {
    outputDirectory.set(file("${projectDir.path}/docs"))
}

fun Project.publishing(configure: PublishingExtension.() -> Unit) =
    extensions.configure<PublishingExtension>(configure)

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
