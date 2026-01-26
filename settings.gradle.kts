/*
 * Copyright 2021 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenLocal()
        mavenCentral()
        maven(url = "https://s3.amazonaws.com/repo.commonsware.com")
        maven(url = "https://customers.pspdfkit.com/maven")
        maven(url = "https://jitpack.io")
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

include(":readium:navigators:web:common")
project(":readium:navigators:web:common")
    .name = "readium-navigator-web-common"

include(":readium:navigators:web:internals")
project(":readium:navigators:web:internals")
    .name = "readium-navigator-web-internals"

include(":readium:navigators:web:reflowable")
project(":readium:navigators:web:reflowable")
    .name = "readium-navigator-web-reflowable"

include(":readium:navigators:web:fixedlayout")
project(":readium:navigators:web:fixedlayout")
    .name = "readium-navigator-web-fixedlayout"

include(":readium:navigators:media:common")
project(":readium:navigators:media:common")
    .name = "readium-navigator-media-common"

include(":readium:navigators:media:audio")
project(":readium:navigators:media:audio")
    .name = "readium-navigator-media-audio"

include(":readium:navigators:media:tts")
project(":readium:navigators:media:tts")
    .name = "readium-navigator-media-tts"

include(":readium:navigators:media:readaloud")
project(":readium:navigators:media:readaloud")
    .name = "readium-navigator-media-readaloud"

include(":readium:adapters:exoplayer:audio")
project(":readium:adapters:exoplayer:audio")
    .name = "readium-adapter-exoplayer-audio"

include(":readium:adapters:exoplayer:readaloud")
project(":readium:adapters:exoplayer:readaloud")
    .name = "readium-adapter-exoplayer-readaloud"

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
include(":demos:navigator")
