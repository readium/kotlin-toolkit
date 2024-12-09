import java.util.Properties

/*
 * Copyright 2021 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

val prop by lazy {
    File("shared.properties")
        .takeIf { it.exists() }
        ?.let { Properties().apply { load(it.inputStream()) } }
}

val mavenBoocoRepos by lazy {
    listOf(
        "s3://booco-android-libs/maven/snapshots",
        "s3://booco-android-libs/maven/releases",
        "s3://booco-android-readium/maven/snapshots",
        "s3://booco-android-readium/maven/releases",
    )
}

fun RepositoryHandler.mavenBooco() = mavenBoocoRepos
    .forEach {
        maven {
            url = uri(it)
            credentials(AwsCredentials::class) {
                accessKey = "BOOCO_AWS_ACCESS_KEY_ID"
                    .let { System.getenv(it) ?: prop?.getProperty(it) }
                secretKey = "BOOCO_AWS_SECRET_ACCESS_KEY"
                    .let { System.getenv(it) ?: prop?.getProperty(it) }
            }
        }
    }

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenLocal()
        mavenCentral()
        mavenBooco()
        maven(url = "https://s3.amazonaws.com/repo.commonsware.com")
        maven(url = "https://customers.pspdfkit.com/maven")
    }
}

rootProject.name = "Readium"

include(":readium:adapters:pdfium:document")
project(":readium:adapters:pdfium:document")
    .name = "readium-adapter-pdfium-document"

include(":readium:adapters:pdfium:navigator")
project(":readium:adapters:pdfium:navigator")
    .name = "readium-adapter-pdfium-navigator"

include(":readium:adapters:pspdfkit:document")
project(":readium:adapters:pspdfkit:document")
    .name = "readium-adapter-pspdfkit-document"

include(":readium:adapters:pspdfkit:navigator")
project(":readium:adapters:pspdfkit:navigator")
    .name = "readium-adapter-pspdfkit-navigator"

include(":readium:lcp")
project(":readium:lcp")
    .name = "readium-lcp"

include(":readium:navigator")
project(":readium:navigator")
    .name = "readium-navigator"

include(":readium:navigators:common")
project(":readium:navigators:common")
    .name = "readium-navigator-common"

include(":readium:navigators:web")
project(":readium:navigators:web")
    .name = "readium-navigator-web"

include(":readium:navigators:media:common")
project(":readium:navigators:media:common")
    .name = "readium-navigator-media-common"

include(":readium:navigators:media:audio")
project(":readium:navigators:media:audio")
    .name = "readium-navigator-media-audio"

include(":readium:navigators:media:tts")
project(":readium:navigators:media:tts")
    .name = "readium-navigator-media-tts"

include(":readium:adapters:exoplayer:audio")
project(":readium:adapters:exoplayer:audio")
    .name = "readium-adapter-exoplayer-audio"

include(":readium:opds")
project(":readium:opds")
    .name = "readium-opds"

include(":readium:shared")
project(":readium:shared")
    .name = "readium-shared"

include(":readium:streamer")
project(":readium:streamer")
    .name = "readium-streamer"

include("test-app")
// include(":readium:navigators:demo")
