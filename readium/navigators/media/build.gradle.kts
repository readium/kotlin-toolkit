/*
 * Copyright 2022 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

plugins {
    id("readium.library-conventions")
}

android {
    namespace = "org.readium.navigator.media"
}

dependencies {
    api(project(":readium:navigators:media:readium-navigator-media-common"))
    api(project(":readium:navigators:media:readium-navigator-media-audio"))
    api(project(":readium:navigators:media:readium-navigator-media-tts"))
}
