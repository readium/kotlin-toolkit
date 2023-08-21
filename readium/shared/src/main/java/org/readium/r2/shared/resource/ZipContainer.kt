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
import org.json.JSONObject
import org.readium.r2.shared.JSONable
import org.readium.r2.shared.error.Try
import org.readium.r2.shared.error.getOrElse
import org.readium.r2.shared.extensions.addPrefix
import org.readium.r2.shared.extensions.optNullableBoolean
import org.readium.r2.shared.extensions.optNullableLong
import org.readium.r2.shared.extensions.readFully
import org.readium.r2.shared.extensions.toMap
import org.readium.r2.shared.extensions.tryOrLog
import org.readium.r2.shared.util.Url
import org.readium.r2.shared.util.io.CountingInputStream
import org.readium.r2.shared.util.mediatype.EpubMediaTypeSniffer.sniff
import org.readium.r2.shared.util.mediatype.MediaType
import org.readium.r2.shared.util.mediatype.MediaTypeHints
import org.readium.r2.shared.util.mediatype.MediaTypeSniffer
import org.readium.r2.shared.util.toUrl

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

/**
 * Holds information about how the resource is stored in the archive.
 *
 * @param entryLength The length of the entry stored in the archive. It might be a compressed length
 *        if the entry is deflated.
 * @param isEntryCompressed Indicates whether the entry was compressed before being stored in the
 *        archive.
 */
public data class ArchiveProperties(
    val entryLength: Long,
    val isEntryCompressed: Boolean
) : JSONable {

    override fun toJSON(): JSONObject = JSONObject().apply {
        put("entryLength", entryLength)
        put("isEntryCompressed", isEntryCompressed)
    }

    public companion object {
        public fun fromJSON(json: JSONObject?): ArchiveProperties? {
            json ?: return null

            val entryLength = json.optNullableLong("entryLength")
            val isEntryCompressed = json.optNullableBoolean("isEntryCompressed")
            if (entryLength == null || isEntryCompressed == null) {
                return null
            }
            return ArchiveProperties(
                entryLength = entryLength,
                isEntryCompressed = isEntryCompressed
            )
        }
    }
}

private const val archiveKey = "archive"

public val Resource.Properties.archive: ArchiveProperties?
    get() = (this[archiveKey] as? Map<*, *>)
        ?.let { ArchiveProperties.fromJSON(JSONObject(it)) }

public var Resource.Properties.Builder.archive: ArchiveProperties?
    get() = (this[archiveKey] as? Map<*, *>)
        ?.let { ArchiveProperties.fromJSON(JSONObject(it)) }
    set(value) {
        if (value == null) {
            remove(archiveKey)
        } else {
            put(archiveKey, value.toJSON().toMap())
        }
    }

internal class JavaZipContainer(
    private val archive: ZipFile,
    file: File,
    private val mediaTypeSniffer: MediaTypeSniffer
) : ZipContainer {

    private inner class FailureEntry(override val path: String) : ZipContainer.Entry {

        override val compressedLength: Long? = null

        override val source: Url? = null

        override suspend fun mediaType(): ResourceTry<MediaType> =
            Try.success(
                mediaTypeSniffer.sniff(
                    hints = MediaTypeHints(fileExtension = File(path).extension),
                    content = ResourceMediaTypeSnifferContent(this)
                ) ?: MediaType.BINARY
            )

        override suspend fun properties(): ResourceTry<Resource.Properties> =
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

        override val source: Url? = null

        override suspend fun mediaType(): ResourceTry<MediaType> =
            Try.success(
                mediaTypeSniffer.sniff(
                    hints = MediaTypeHints(fileExtension = File(path).extension),
                    content = ResourceMediaTypeSnifferContent(this)
                ) ?: MediaType.BINARY
            )

        override suspend fun properties(): ResourceTry<Resource.Properties> =
            ResourceTry.success(
                Resource.Properties {
                    archive = ArchiveProperties(
                        entryLength = compressedLength
                            ?: length().getOrElse { return ResourceTry.failure(it) },
                        isEntryCompressed = compressedLength != null
                    )
                }
            )

        override suspend fun length(): Try<Long, Resource.Exception> =
            entry.size.takeUnless { it == -1L }
                ?.let { Try.success(it) }
                ?: Try.failure(Resource.Exception.Other(Exception("Unsupported operation")))

        override val compressedLength: Long? =
            if (entry.method == ZipEntry.STORED || entry.method == -1) {
                null
            } else {
                entry.compressedSize.takeUnless { it == -1L }
            }

        override suspend fun read(range: LongRange?): Try<ByteArray, Resource.Exception> =
            try {
                withContext(Dispatchers.IO) {
                    val bytes =
                        if (range == null) {
                            readFully()
                        } else {
                            readRange(range)
                        }
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

    override val source: Url = file.toUrl()

    override suspend fun entries(): Set<Container.Entry> =
        archive.entries().toList()
            .filterNot { it.isDirectory }
            .mapNotNull { Entry(it) }
            .toSet()

    override fun get(path: String): Container.Entry =
        archive.getEntry(path.removePrefix("/"))
            ?.let { Entry(it) }
            ?: FailureEntry(path)

    override suspend fun close() {
        tryOrLog {
            withContext(Dispatchers.IO) {
                archive.close()
            }
        }
    }
}
