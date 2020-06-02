/*
 * Module: r2-streamer-kotlin
 * Developers: Mickaël Menu
 *
 * Copyright (c) 2020. Readium Foundation. All rights reserved.
 * Use of this source code is governed by a BSD-style license which is detailed in the
 * LICENSE file present in the project repository where this source code is maintained.
 */

package org.readium.r2.streamer.container

import org.readium.r2.shared.RootFile
import org.readium.r2.shared.drm.DRM
import java.io.InputStream

/** A container serving no files, e.g. for a RWPM. */
internal class EmptyContainer(path: String, mimetype: String) : Container {

    override var rootFile: RootFile = RootFile(rootPath = path, mimetype = mimetype)
    override var drm: DRM? = null

    override fun data(relativePath: String): ByteArray {
        throw ContainerError.fileNotFound
    }

    override fun dataLength(relativePath: String): Long {
        throw ContainerError.fileNotFound
    }

    override fun dataInputStream(relativePath: String): InputStream {
        throw ContainerError.fileNotFound
    }

}