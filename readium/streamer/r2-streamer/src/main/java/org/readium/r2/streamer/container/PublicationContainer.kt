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
import org.readium.r2.shared.extensions.tryOr
import org.readium.r2.shared.format.MediaType
import org.readium.r2.shared.publication.Publication
import java.io.InputStream

/**
 * Temporary solution to migrate to [Publication.get] while ensuring backward compatibility with
 * [Container].
 */
internal class PublicationContainer(
    private val publication: Publication,
    path: String,
    mediaType: MediaType,
    override var drm: DRM? = null
) : Container {

    override var rootFile = RootFile(rootPath = path, mimetype = mediaType.toString())

    override fun data(relativePath: String): ByteArray {
        return publication.get(relativePath).read().getOrThrow()
    }

    override fun dataLength(relativePath: String): Long =
        tryOr(0) {
            publication.get(relativePath).length.getOrThrow()
        }

    override fun dataInputStream(relativePath: String): InputStream =
        publication.get(relativePath).stream().getOrThrow()

}