/*
 * Copyright 2023 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

@file:OptIn(InternalReadiumApi::class)

package org.readium.r2.shared.util.zip

import kotlinx.coroutines.withContext
import okio.IOException
import okio.Path.Companion.toPath
import org.readium.r2.shared.InternalReadiumApi
import org.readium.r2.shared.extensions.findInstance
import org.readium.r2.shared.util.AbsoluteUrl
import org.readium.r2.shared.util.Try
import org.readium.r2.shared.util.archive.ArchiveOpener
import org.readium.r2.shared.util.data.Container
import org.readium.r2.shared.util.data.ReadError
import org.readium.r2.shared.util.data.ReadException
import org.readium.r2.shared.util.data.Readable
import org.readium.r2.shared.util.file.fileSystem
import org.readium.r2.shared.util.format.Format
import org.readium.r2.shared.util.format.Specification
import org.readium.r2.shared.util.io.IoDispatcher
import org.readium.r2.shared.util.resource.Resource
import org.readium.r2.shared.util.zip.compress.archivers.zip.ZipFile
import org.readium.r2.shared.util.zip.jvm.SeekableByteChannel

/**
 * An [ArchiveOpener] able to open a ZIP archive served through a stream (e.g. HTTP server,
 * content URI, etc.).
 */
internal class StreamingZipArchiveProvider {

    suspend fun sniffOpen(source: Readable): Try<Container<Resource>, ArchiveOpener.SniffOpenError> {
        return try {
            val container = openBlob(source, ::ReadException, null)
            Try.success(container)
        } catch (exception: Exception) {
            exception.findInstance<ReadException>()
                ?.let { Try.failure(ArchiveOpener.SniffOpenError.Reading(it.error)) }
                ?: Try.failure(ArchiveOpener.SniffOpenError.NotRecognized)
        }
    }

    suspend fun open(
        format: Format,
        source: Readable,
    ): Try<Container<Resource>, ArchiveOpener.OpenError> {
        if (!format.conformsTo(Specification.Zip)) {
            return Try.failure(
                ArchiveOpener.OpenError.FormatNotSupported(format)
            )
        }

        return try {
            val container = openBlob(
                source,
                ::ReadException,
                (source as? Resource)?.sourceUrl
            )
            Try.success(container)
        } catch (exception: Exception) {
            val error = exception.findInstance<ReadException>()
                ?.let { ArchiveOpener.OpenError.Reading(it.error) }
                ?: ArchiveOpener.OpenError.Reading(ReadError.Decoding(exception))

            Try.failure(error)
        }
    }

    private suspend fun openBlob(
        readable: Readable,
        wrapError: (ReadError) -> IOException,
        sourceUrl: AbsoluteUrl?,
    ): Container<Resource> = withContext(IoDispatcher) {
        val datasourceChannel = ReadableChannelAdapter(readable, wrapError)
        val channel = wrapBaseChannel(datasourceChannel)
        val zipFile = ZipFile(channel, true)
        val sourceScheme = (readable as? Resource)?.sourceUrl?.scheme
        val cacheEntryMaxSize =
            when {
                sourceScheme?.isContent ?: false -> 5242880
                else -> 0
            }
        StreamingZipContainer(zipFile, sourceUrl, cacheEntryMaxSize)
    }

    internal suspend fun openFile(file: AbsoluteUrl): Container<Resource> = withContext(IoDispatcher) {
        val path = checkNotNull(file.path) { "Expected a file URL, got: $file" }.toPath()
        val fileChannel = FileChannelAdapter(fileSystem.openReadOnly(path))
        try {
            val channel = wrapBaseChannel(fileChannel)
            StreamingZipContainer(ZipFile(channel, true), file)
        } catch (e: Throwable) {
            // We own the file handle: close it on any failure to initialize the container, or
            // every failed open/sniff would leak a file descriptor.
            fileChannel.close()
            throw e
        }
    }

    private suspend fun wrapBaseChannel(channel: SeekableByteChannel): SeekableByteChannel {
        val size = channel.size()
        return if (size < CACHE_ALL_MAX_SIZE) {
            CachingReadableChannel(channel, 0)
        } else {
            val cacheStart = size - CACHED_TAIL_SIZE
            val cachingChannel = CachingReadableChannel(channel, cacheStart)
            cachingChannel.cache()
            BufferedReadableChannel(cachingChannel, DEFAULT_BUFFER_SIZE)
        }
    }

    companion object {

        private const val CACHE_ALL_MAX_SIZE = 5242880

        private const val CACHED_TAIL_SIZE = 65557

        private const val DEFAULT_BUFFER_SIZE = 8192
    }
}
