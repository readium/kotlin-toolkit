/*
 * Copyright 2026 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

import com.vanniktech.maven.publish.SonatypeHost

plugins {
    // FIXME: For now, we cannot use the versions catalog in precompiled scripts: https://github.com/gradle/gradle/issues/15383
    kotlin("multiplatform")
    id("com.android.kotlin.multiplatform.library") // NOT com.android.library — AGP 9 rejects it with KMP
    id("com.vanniktech.maven.publish")
    kotlin("plugin.parcelize")
    id("org.jetbrains.dokka")
}

group = property("pom.groupId") as String

kotlin {
    explicitApi()

    androidLibrary {
        compileSdk = (property("android.compileSdk") as String).toInt()
        minSdk = (property("android.minSdk") as String).toInt()

        androidResources.enable = true // shared uses R.string; disabled by default

        withHostTestBuilder {
        }.configure {
            isIncludeAndroidResources = true // Robolectric
        }

        // Downstream modules are JVM 11; the default here is 21 and breaks inlining.
        compilations.configureEach {
            compileTaskProvider.configure {
                compilerOptions {
                    jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_11)
                }
            }
        }
    }

    iosArm64()
    iosSimulatorArm64()

    // TODO(phase-01): add the Parcelize `additionalAnnotation` compiler-plugin option
    // for the Readium-owned expect annotation once it exists.

    compilerOptions {
        freeCompilerArgs.add("-Xannotation-default-target=param-property")
        languageVersion = org.jetbrains.kotlin.gradle.dsl.KotlinVersion.KOTLIN_2_3
        allWarningsAsErrors = true
    }
}

// commonTest reads fixture files through the `Fixtures` helper, which locates
// them with this environment variable.
val fixturesDir = layout.projectDirectory.dir("src/commonTest/fixtures").asFile.absolutePath

tasks.withType<Test>().configureEach {
    failOnNoDiscoveredTests = false
    environment("READIUM_FIXTURES_DIR", fixturesDir)
}

tasks.withType<org.jetbrains.kotlin.gradle.targets.native.tasks.KotlinNativeSimulatorTest>().configureEach {
    // simctl only forwards environment variables prefixed with SIMCTL_CHILD_.
    environment("SIMCTL_CHILD_READIUM_FIXTURES_DIR", fixturesDir)
}

mavenPublishing {
    coordinates(
        groupId = group.toString(),
        artifactId = property("pom.artifactId") as String,
        version = property("pom.version") as String
    )

    pom {
        name.set(property("pom.artifactId") as String)
        description.set("A toolkit for ebooks, audiobooks and comics written in Kotlin")
        url.set("https://github.com/readium/kotlin-toolkit")
        licenses {
            license {
                name.set("BSD-3-Clause license")
                url.set("https://github.com/readium/kotlin-toolkit/blob/main/LICENSE")
            }
        }
        developers {
            developer {
                id.set("aferditamuriqi")
                name.set("Aferdita Muriqi")
                email.set("aferdita.muriqi@gmail.com")
            }
            developer {
                id.set("mickael-menu")
                name.set("Mickaël Menu")
                email.set("mickael.menu@gmail.com")
            }
            developer {
                id.set("qnga")
                name.set("Quentin Gliosca")
                email.set("quentin.gliosca@gmail.com")
            }
        }
        scm {
            url.set("https://github.com/readium/kotlin-toolkit")
            connection.set("scm:git:github.com/readium/kotlin-toolkit.git")
            developerConnection.set("scm:git:ssh://github.com/readium/kotlin-toolkit.git")
        }
    }

    publishToMavenCentral(SonatypeHost.CENTRAL_PORTAL)
    signAllPublications()
}
