/*
 * Module: r2-shared-kotlin
 * Developers: Quentin Gliosca
 *
 * Copyright (c) 2020. Readium Foundation. All rights reserved.
 * Use of this source code is governed by a BSD-style license which is detailed in the
 * LICENSE file present in the project repository where this source code is maintained.
 */

@file:OptIn(InternalReadiumApi::class)

package org.readium.r2.shared.util.archive

import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.readium.r2.shared.InternalReadiumApi
import org.readium.r2.shared.extensions.readFully
import org.readium.r2.shared.util.Try
import org.readium.r2.shared.util.Url
import org.readium.r2.shared.util.io.CountingInputStream

@OptIn(InternalReadiumApi::class)
internal class JavaZip(private val archive: ZipFile) :
    Package {

    private inner class Entry(private val entry: ZipEntry) : Package.Entry {
        override val path: String get() = entry.name

        override val length: Long? get() = entry.size.takeUnless { it == -1L }

        override val compressedLength: Long?
            get() =
                if (entry.method == ZipEntry.STORED || entry.method == -1)
                    null
                else
                    entry.compressedSize.takeUnless { it == -1L }

        override suspend fun read(range: LongRange?): ByteArray =
            withContext(Dispatchers.IO) {
                if (range == null)
                    readFully()
                else
                    readRange(range)
            }

        private suspend fun readFully(): ByteArray =
            archive.getInputStream(entry).use {
                it.readFully()
            }

        private fun readRange(range: LongRange): ByteArray =
            stream(range.first).readRange(range)

        /**
         * Reading an entry in chunks (e.g. from the HTTP server) can be really slow if the entry
         * is deflated in the archive, because we can't jump to an arbitrary offset in a deflated
         * stream. This means that we need to read from the start of the entry for each chunk.
         *
         * To alleviate this issue, we cache a stream which will be reused as long as the chunks are
         * requested in order.
         *
         * See this issue for more info: https://github.com/readium/r2-shared-kotlin/issues/129
         */
        private fun stream(fromIndex: Long): CountingInputStream {
            // Reuse the current stream if it didn't exceed the requested index.
            stream
                ?.takeIf { it.count <= fromIndex }
                ?.let { return it }

            stream?.close()

            return CountingInputStream(archive.getInputStream(entry))
                .also { stream = it }
        }

        private var stream: CountingInputStream? = null

        override suspend fun close() {
            withContext(Dispatchers.IO) {
                stream?.close()
            }
        }
    }

    override suspend fun entries(): List<Package.Entry> =
        archive.entries().toList().filterNot { it.isDirectory }.mapNotNull { Entry(it) }

    override suspend fun entry(path: String): Package.Entry {
        val entry = archive.getEntry(path)
            ?: throw Exception("No file entry at path $path.")

        return Entry(entry)
    }

    override suspend fun close() = withContext(Dispatchers.IO) {
        archive.close()
    }
}

internal class JavaZipArchiveFactory {

    suspend fun open(url: Url): Try<Package, Exception> =
        withContext(Dispatchers.IO) {
            try {
                if (url.scheme != "file") {
                    throw Exception("Unsupported protocol.")
                }

                val archive = JavaZip(ZipFile(url.path))
                Try.success(archive)
            } catch (e: Exception) {
                Try.failure(e)
            }
        }

    suspend fun open(file: File): Try<Package, Exception> =
        withContext(Dispatchers.IO) {
            try {
                val archive = JavaZip(ZipFile(file))
                Try.success(archive)
            } catch (e: Exception) {
                Try.failure(e)
            }
        }
}
