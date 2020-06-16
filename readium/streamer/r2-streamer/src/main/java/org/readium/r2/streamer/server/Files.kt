/*
 * Module: r2-streamer-kotlin
 * Developers: Aferdita Muriqi, Clément Baumann
 *
 * Copyright (c) 2018. Readium Foundation. All rights reserved.
 * Use of this source code is governed by a BSD-style license which is detailed in the
 * LICENSE file present in the project repository where this source code is maintained.
 */

package org.readium.r2.streamer.server

import android.net.Uri
import kotlinx.coroutines.runBlocking
import org.readium.r2.shared.extensions.isParentOf
import org.readium.r2.shared.format.Format
import org.readium.r2.shared.format.MediaType
import java.io.File

/**
 * Files to be served from the file system.
 *
 * @param basePath Base path (ignoring host) from where the files are served.
 * @param fallbackMediaType Media type which will be used for responses when it can't be determined
 *        from the served file.
 */
internal class Files(
    private val basePath: String,
    private val fallbackMediaType: MediaType = MediaType.BINARY
) {
    private val files: MutableMap<String, File> = mutableMapOf()

    operator fun set(href: String, file: File) {
        files[href] = file.canonicalFile
    }

    operator fun get(key: String): File? = files[key]

    fun find(uri: Uri): ServedFile? {
        val path = uri.path?.removePrefix(basePath) ?: return null

        for ((href, file) in files) {
            if (path.startsWith(href)) {
                val requestedFile = File(file, path.removePrefix(href)).canonicalFile
                // Makes sure that the requested file is `file` or one of its descendant.
                if (file.isParentOf(requestedFile)) {
                    return ServedFile(requestedFile, fallbackMediaType)
                }
            }
        }

        return null
    }

    data class ServedFile(
        val file: File,
        private val fallbackMediaType: MediaType
    ) {
        val mediaType: MediaType get() = runBlocking { Format.ofFile(file)?.mediaType ?: fallbackMediaType }
    }

}
