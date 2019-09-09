// TODO WIP

/*
 * Module: r2-streamer-kotlin
 * Developers: Aferdita Muriqi
 *
 * Copyright (c) 2019. Readium Foundation. All rights reserved.
 * Use of this source code is governed by a BSD-style license which is detailed in the
 * LICENSE file present in the project repository where this source code is maintained.
 */

package org.readium.r2.streamer.container

import org.readium.r2.shared.RootFile
import org.readium.r2.shared.drm.DRM
import org.readium.r2.streamer.parser.DiViNaParser
import java.io.File


class ContainerDiViNa : DiViNaContainer, DirectoryContainer {

    override var rootFile: RootFile
    override var drm: DRM? = null
    override var successCreated: Boolean = false

    constructor(path: String) {

        if (File(path).exists()) {
            successCreated = true
        }
        rootFile = RootFile(rootPath = path, mimetype = DiViNaParser.mimetypeDiViNa)
    }

}