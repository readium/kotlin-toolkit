/*
 * Module: r2-shared-kotlin
 * Developers: Quentin Gliosca
 *
 * Copyright (c) 2020. Readium Foundation. All rights reserved.
 * Use of this source code is governed by a BSD-style license which is detailed in the
 * LICENSE file present in the project repository where this source code is maintained.
 */

package org.readium.r2.shared.fetcher

import org.readium.r2.shared.publication.Link
import java.io.File
import java.io.InputStream
import java.io.RandomAccessFile
import java.nio.channels.Channels
import java.nio.channels.FileChannel

class FileFetcher(private val paths: Map<String, String>) : Fetcher {

    constructor(href: String, path: String) : this(mapOf(href to path))

    override fun get(link: Link): Resource {
        for ((href, path) in paths) {
            if (link.href.startsWith(href)) {
                val resourcePath = File(path, link.href.removePrefix(href)).canonicalPath
                // Make sure that the requested resource is [path] or one of its descendant.
                if (resourcePath.startsWith(path)) {
                    return try {
                        val channel = RandomAccessFile(path, "r").channel
                        FileResource(link, channel)
                    } catch (e: Exception) {
                        NullResource(link)
                    }
                }
            }
        }
        return NullResource(link)
    }

    private class FileResource(override val link: Link, private val channel: FileChannel) : ResourceImpl() {

        override fun stream(): InputStream? =
            try {
                Channels.newInputStream(channel).buffered()
            } catch (e: Exception) {
                null
            }

        override val metadataLength: Long? by lazy {
           try {
               channel.size()
           } catch (e : Exception) {
               null
            }
        }
    }
}
