/*
 * Copyright 2023 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.shared.resource

import java.io.File
import java.io.IOException
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.readium.r2.shared.error.Try
import org.readium.r2.shared.extensions.addPrefix
import org.readium.r2.shared.extensions.readFully
import org.readium.r2.shared.extensions.tryOrLog
import org.readium.r2.shared.util.io.CountingInputStream
import org.readium.r2.shared.util.mediatype.MediaType

/**
 * A [Container] representing a Zip archive.
 */
public interface ZipContainer : Container {

    public interface Entry : Container.Entry {

        /**
         * Compressed data length.
         */
        public val compressedLength: Long?
    }
}

internal class JavaZipContainer(private val archive: ZipFile, source: File) : ZipContainer {

    private inner class FailureEntry(override val path: String) : ZipContainer.Entry {

        override val compressedLength: Long? = null

        override val key: String = path
        override val file: File? = null

        // FIXME: Implement with a sniffer.
        override suspend fun mediaType(): ResourceTry<MediaType?> =
            Try.success(null)

        override suspend fun name(): ResourceTry<String?> =
            Try.failure(Resource.Exception.NotFound())

        override suspend fun length(): ResourceTry<Long> =
            Try.failure(Resource.Exception.NotFound())

        override suspend fun read(range: LongRange?): ResourceTry<ByteArray> =
            Try.failure(Resource.Exception.NotFound())

        override suspend fun close() {
        }
    }

    private inner class Entry(private val entry: ZipEntry) : ZipContainer.Entry {

        override val path: String =
            entry.name.addPrefix("/")

        override val key: String = path
        override val file: File? = null

        // FIXME: Implement with a sniffer.
        override suspend fun mediaType(): ResourceTry<MediaType?> =
            Try.success(null)

        override suspend fun name(): ResourceTry<String?> =
            ResourceTry.success(File(path).name)

        override suspend fun length(): Try<Long, Resource.Exception> =
            entry.size.takeUnless { it == -1L }
                ?.let { Try.success(it) }
                ?: Try.failure(Resource.Exception.Other(Exception("Unsupported operation")))

        override val compressedLength: Long? =
            if (entry.method == ZipEntry.STORED || entry.method == -1)
                null
            else
                entry.compressedSize.takeUnless { it == -1L }

        override suspend fun read(range: LongRange?): Try<ByteArray, Resource.Exception> =
            try {
                withContext(Dispatchers.IO) {
                    val bytes =
                        if (range == null)
                            readFully()
                        else
                            readRange(range)
                    Try.success(bytes)
                }
            } catch (e: IOException) {
                Try.failure(Resource.Exception.Unavailable(e))
            } catch (e: Exception) {
                Try.failure(Resource.Exception.wrap(e))
            }

        private suspend fun readFully(): ByteArray =
            withContext(Dispatchers.IO) {
                archive.getInputStream(entry)
                    .use {
                        it.readFully()
                    }
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
                tryOrLog { stream?.close() }
            }
        }
    }

    override val file: File = source

    override suspend fun name(): ResourceTry<String> =
        ResourceTry.success(file.name)

    override suspend fun entries(): List<Container.Entry> =
        archive.entries().toList()
            .filterNot { it.isDirectory }
            .mapNotNull { Entry(it) }

    override suspend fun entry(path: String): Container.Entry {
        return archive.getEntry(path.removePrefix("/"))
            ?.let { Entry(it) }
            ?: FailureEntry(path)
    }

    override suspend fun close() {
        tryOrLog {
            withContext(Dispatchers.IO) {
                archive.close()
            }
        }
    }
}
