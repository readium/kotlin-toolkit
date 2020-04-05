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
import java.lang.ref.WeakReference
import java.nio.channels.Channels
import java.util.LinkedList

class FileFetcher(private val paths: Map<String, String>) : Fetcher {

    constructor(href: String, path: String) : this(mapOf(href to path))

    private val openResources: MutableList<WeakReference<Resource>> = LinkedList()

    override fun get(link: Link): Resource {
        for ((href, path) in paths) {
            if (link.href.startsWith(href)) {
                val resourcePath = File(path, link.href.removePrefix(href)).canonicalPath
                // Make sure that the requested resource is [path] or one of its descendant.
                if (resourcePath.startsWith(path)) {
                    try {
                        val file = RandomAccessFile(resourcePath, "r")
                        val resource = FileResource(link, file)
                        openResources.add(WeakReference(resource))
                        return resource
                    } catch (e: Exception) {
                    }
                }
            }
        }
        return NullResource(link)
    }

    override fun close() {
        openResources.mapNotNull(WeakReference<Resource>::get).forEach { it.close() }
        openResources.clear()
    }

    private class FileResource(override val link: Link, private val file: RandomAccessFile) : ResourceImpl() {

        override fun stream(): InputStream = Channels.newInputStream(file.channel).buffered()

        override val metadataLength: Long = file.length()

        override fun close() = file.close()
    }
}
