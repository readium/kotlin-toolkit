/*
 * Module: r2-streamer-kotlin
 * Developers: Mickaël Menu
 *
 * Copyright (c) 2020. Readium Foundation. All rights reserved.
 * Use of this source code is governed by a BSD-style license which is detailed in the
 * LICENSE file present in the project repository where this source code is maintained.
 */

package org.readium.r2.streamer.server

import android.content.res.AssetManager
import android.net.Uri
import org.readium.r2.shared.extensions.isParentOf
import org.readium.r2.shared.util.mediatype.MediaType
import java.io.File
import java.io.InputStream

/**
 * Files to be served from the application's assets.
 *
 * @param basePath Base path (ignoring host) from where the files are served.
 * @param fallbackMediaType Media type which will be used for responses when it can't be determined
 *        from the served file.
 */
internal class Assets(
    private val assetManager: AssetManager,
    private val basePath: String,
    private val fallbackMediaType: MediaType = MediaType.BINARY
) {
    private val assets: MutableList<Pair<String, File>> = mutableListOf()

    fun add(href: String, path: String) {
        // Inserts at the beginning to take precedence over already registered assets.
        assets.add(0, Pair(href, File("/$path").canonicalFile))
    }

    fun find(uri: Uri): ServedAsset? {
        val path = uri.path?.removePrefix(basePath) ?: return null

        for ((href, file) in assets) {
            if (path.startsWith(href)) {
                val requestedFile = File(file, path.removePrefix(href)).canonicalFile
                // Makes sure that the requested file is `file` or one of its descendant.
                if (file == requestedFile || file.isParentOf(requestedFile)) {
                    val mediaType = MediaType.of(fileExtension = requestedFile.extension) ?: fallbackMediaType
                    return ServedAsset(assetManager.open(requestedFile.path.removePrefix("/")), mediaType)
                }
            }
        }

        return null
    }

    data class ServedAsset(
        val stream: InputStream,
        val mediaType: MediaType
    )

}
