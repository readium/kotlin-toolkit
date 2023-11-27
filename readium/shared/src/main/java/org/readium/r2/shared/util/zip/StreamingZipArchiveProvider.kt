/*
 * Copyright 2023 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.shared.util.zip

import java.io.File
import java.io.IOException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.readium.r2.shared.DelicateReadiumApi
import org.readium.r2.shared.extensions.unwrapInstance
import org.readium.r2.shared.util.AbsoluteUrl
import org.readium.r2.shared.util.MessageError
import org.readium.r2.shared.util.Try
import org.readium.r2.shared.util.data.Blob
import org.readium.r2.shared.util.data.Container
import org.readium.r2.shared.util.data.ReadError
import org.readium.r2.shared.util.data.ReadException
import org.readium.r2.shared.util.mediatype.MediaType
import org.readium.r2.shared.util.mediatype.MediaTypeSnifferError
import org.readium.r2.shared.util.resource.ArchiveFactory
import org.readium.r2.shared.util.resource.BlobMediaTypeRetriever
import org.readium.r2.shared.util.resource.Resource
import org.readium.r2.shared.util.resource.ResourceContainer
import org.readium.r2.shared.util.toUrl
import org.readium.r2.shared.util.zip.compress.archivers.zip.ZipFile
import org.readium.r2.shared.util.zip.jvm.SeekableByteChannel

/**
 * An [ArchiveFactory] able to open a ZIP archive served through a stream (e.g. HTTP server,
 * content URI, etc.).
 */
@OptIn(DelicateReadiumApi::class)
internal class StreamingZipArchiveProvider(
    private val mediaTypeRetriever: BlobMediaTypeRetriever? = null
) {

    suspend fun sniffBlob(blob: Blob): Try<MediaType, MediaTypeSnifferError> {
        return try {
            openBlob(blob, ::ReadException, null)
            Try.success(MediaType.ZIP)
        } catch (exception: Exception) {
            when (val e = exception.unwrapInstance(ReadException::class.java)) {
                is ReadException ->
                    Try.failure(MediaTypeSnifferError.Read(e.error))
                else ->
                    Try.failure(MediaTypeSnifferError.NotRecognized)
            }
        }
    }

    suspend fun create(
        mediaType: MediaType,
        blob: Blob
    ): Try<Container<Resource>, ArchiveFactory.Error> {
        if (mediaType != MediaType.ZIP) {
            return Try.failure(
                ArchiveFactory.Error.FormatNotSupported(
                    MessageError("Archive type not supported")
                )
            )
        }

        return try {
            val container = openBlob(
                blob,
                ::ReadException,
                blob.source
            )
            Try.success(container)
        } catch (exception: Exception) {
            when (val e = exception.unwrapInstance(ReadException::class.java)) {
                is ReadException ->
                    Try.failure(ArchiveFactory.Error.ReadError(e.error))
                else ->
                    Try.failure(ArchiveFactory.Error.ReadError(ReadError.Decoding(e)))
            }
        }
    }

    private suspend fun openBlob(
        blob: Blob,
        wrapError: (ReadError) -> IOException,
        sourceUrl: AbsoluteUrl?
    ): Container<Resource> = withContext(Dispatchers.IO) {
        val datasourceChannel = BlobChannel(blob, wrapError)
        val channel = wrapBaseChannel(datasourceChannel)
        val zipFile = ZipFile(channel, true)
        StreamingZipContainer(zipFile, sourceUrl, mediaTypeRetriever)
    }

    internal suspend fun openFile(file: File): ResourceContainer = withContext(Dispatchers.IO) {
        val fileChannel = FileChannelAdapter(file, "r")
        val channel = wrapBaseChannel(fileChannel)
        StreamingZipContainer(ZipFile(channel), file.toUrl(), mediaTypeRetriever)
    }

    private fun wrapBaseChannel(channel: SeekableByteChannel): SeekableByteChannel {
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
    }
}
