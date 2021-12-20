/*
 * Copyright 2021 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

import org.jetbrains.dokka.gradle.DokkaTaskPartial

buildscript {
    extra.set("kotlin_version", "1.6.10")
    val kotlin_version = "1.6.10"
    val dokka_version = "1.6.0"

    repositories {
        google()
        jcenter()
        mavenLocal()
        mavenCentral()
        maven(url = "https://jitpack.io")
        maven(url = "https://s3.amazonaws.com/repo.commonsware.com")
    }
    dependencies {
        classpath("com.android.tools.build:gradle:7.0.4")
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:$kotlin_version")
        classpath("org.jetbrains.dokka:dokka-gradle-plugin:$dokka_version")
    }
}

//plugins {
//    id("org.jetbrains.dokka") version ("1.5.30")
//}
apply(plugin="org.jetbrains.dokka")

allprojects {
    repositories {
        google()
        jcenter()
        mavenLocal()
        mavenCentral()
        maven(url = "https://jitpack.io")
        maven(url = "https://s3.amazonaws.com/repo.commonsware.com")
    }
}

subprojects {
    tasks.register<Jar>("javadocsJar") {
        archiveClassifier.set("javadoc")
    }

    tasks.register<Jar>("sourcesJar") {
        archiveClassifier.set("sources")
        from("src/main/java", "src/main/resources")
    }
}

tasks.register("clean", Delete::class).configure {
    delete(rootProject.buildDir)
}

tasks.register("cleanDocs", Delete::class).configure {
    delete("${project.rootDir}/docs/readium", "${project.rootDir}/docs/index.md")
}

tasks.withType<DokkaTaskPartial>().configureEach {
    outputDirectory.set(file("${project.rootDir}/docs"))
    dokkaSourceSets {
        configureEach {
            reportUndocumented.set(false)
            skipEmptyPackages.set(false)
            skipDeprecated.set(true)
        }
    }
}
//
//tasks.named("dokkaGfmMultimodule").configure {
//    outputDirectory.set(file("${project.rootDir}/docs"))
//}
