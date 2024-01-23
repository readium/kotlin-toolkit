/*
 * Copyright 2022 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

plugins {
    id("readium.library-conventions")
}

android {
    namespace = "org.readium.adapter.pspdfkit"
}

dependencies {
    api(project(":readium:adapters:pspdfkit:readium-adapter-pspdfkit-document"))
    api(project(":readium:adapters:pspdfkit:readium-adapter-pspdfkit-navigator"))
}
