/*
 * Module: r2-shared-kotlin
 * Developers: Quentin Gliosca
 *
 * Copyright (c) 2020. Readium Foundation. All rights reserved.
 * Use of this source code is governed by a BSD-style license which is detailed in the
 * LICENSE file present in the project repository where this source code is maintained.
 */

package org.readium.r2.shared.fetcher

import android.os.Build
import org.readium.r2.shared.publication.Link
import java.io.File
import java.io.FileNotFoundException
import java.io.IOException
import java.io.InputStream
import java.net.URL
import java.nio.file.Files
import java.nio.file.attribute.BasicFileAttributes
import java.util.zip.ZipEntry
import java.util.zip.ZipException
import java.util.zip.ZipFile

interface Fetcher {

    fun stream(link: Link): InputStream?

    fun fetch(link: Link): ByteArray? = stream(link)?.use {
        try {
            it.readBytes()
        } catch (e: IOException) {
            null
        }
    }

    fun lengthOf(link: Link): Long? = fetch(link)?.size?.toLong()

    fun close() {}
}

class ZipFetcher(val archive: ZipFile) : Fetcher {

    companion object {
        fun fromPath(path: String): ZipFetcher? = try {
            ZipFetcher(ZipFile(path))
        } catch (e: Exception) {
            null
        }
    }

    override fun stream(link: Link): InputStream? {
        val entry = entryFromHref(link.href)
        return try {
            archive.getInputStream(entry)
        } catch (e: ZipException) {
            null
        } catch (e: IOException) {
            null
        } // IllegalStateException is raised if the archive is closed
    }

    override fun lengthOf(link: Link): Long? {
        val sizeFromDescriptor = entryFromHref(link.href)?.size?.takeIf { it != -1L }
        return sizeFromDescriptor ?: super.lengthOf(link) // the size can be not known
    }

    override fun close() {
        archive.close()
    }

    private fun entryFromHref(href: String): ZipEntry? =
        archive.getEntry(href.removePrefix("/"))
}

class FileFetcher(val files: Map<String, String>) : Fetcher {

    constructor(file: String, href: String) : this(mapOf(href to file))

    override fun stream(link: Link): InputStream? = files[link.href]?.let { streamOfFile(File(it)) }

    override fun lengthOf(link: Link): Long? = files[link.href]?.let { lengthOfFile(File(it)) }
}

class DirectoryFetcher(val directory: String) : Fetcher {

    companion object {
        fun fromPath(path: String): DirectoryFetcher? = if (File(path).isDirectory) fromPath(path) else null
    }

    override fun stream(link: Link): InputStream? = streamOfFile(fileFromHref(link.href))

    override fun lengthOf(link: Link): Long? = lengthOfFile(fileFromHref(link.href))

    private fun fileFromHref(href: String) = File(directory, href.removePrefix("/"))

}

private fun streamOfFile(file: File): InputStream? =
    try {
        file.inputStream().buffered()
    } catch (e: FileNotFoundException) {
        null
    }

private fun lengthOfFile(file: File, parent: String? = null): Long? =
    if (Build.VERSION.SDK_INT > 25) {
        // this version reads file's attributes in bulk, so consistency is ensured
        try {
            val attributes = Files.readAttributes(file.toPath(), BasicFileAttributes::class.java)
            if (attributes.isRegularFile) attributes.size() else null
        } catch (e: Exception) {
            null
        }
    } else if (!file.isFile) {
         null
    } else {
        file.length().takeUnless { it == 0L }
    }

class HttpFetcher : Fetcher {

    override fun stream(link: Link): InputStream? =
        try {
            URL(link.href).openConnection().getInputStream()
        } catch (e: Exception) {
            null
        }

    override fun lengthOf(link: Link): Long? {
        val length =
            try {
                if (Build.VERSION.SDK_INT > 23)
                    URL(link.href).openConnection().contentLengthLong
                else
                    URL(link.href).openConnection().contentLength.toLong()
            } catch (e: Exception) {
                null
            }
        return length.takeUnless { it == -1L } ?: super.lengthOf(link)

    }
}

class FilteredFetcher(val fetcher: Fetcher, val filters: Collection<ContentFilter>) : Fetcher {

    override fun stream(link: Link): InputStream? {
        val originalStream = fetcher.stream(link) ?: return null
        return filters.toList().fold(originalStream) { stream, filter ->
            if (filter.acceptsLink(link)) filter.filter(stream, link) else stream
        }
    }

    override fun close() {
        fetcher.close()
    }
}

class CompositeFetcher(val selector: (Link) -> Fetcher, val children: Collection<Fetcher>) : Fetcher {
    /* FIXME: `children` argument is required for `close`, but `selector` can enclose other `Fetcher`s
               Should `selector` return an index? It could be out of range.
    */

    constructor(local: Fetcher, remote: Fetcher)
            : this({ if (hrefIsRemote(it.href)) remote else local }, listOf(local, remote))

    override fun stream(link: Link): InputStream? = selector(link).stream(link)

    override fun fetch(link: Link): ByteArray? = selector(link).fetch(link)

    override fun lengthOf(link: Link): Long? = selector(link).lengthOf(link)

    override fun close() {
        children.forEach(Fetcher::close)
    }
}

private fun hrefIsRemote(href: String) = !href.startsWith("/")
