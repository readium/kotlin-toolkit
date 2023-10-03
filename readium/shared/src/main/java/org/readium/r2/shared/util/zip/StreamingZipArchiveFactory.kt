/*
 * Copyright 2023 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.shared.util.zip

import java.io.File
import org.readium.r2.shared.util.Try
import org.readium.r2.shared.util.mediatype.MediaTypeRetriever
import org.readium.r2.shared.util.resource.ArchiveFactory
import org.readium.r2.shared.util.resource.Container
import org.readium.r2.shared.util.resource.Resource
import org.readium.r2.shared.util.toUrl
import org.readium.r2.shared.util.zip.compress.archivers.zip.ZipFile
import org.readium.r2.shared.util.zip.jvm.SeekableByteChannel

/**
 * An [ArchiveFactory] able to open a ZIP archive served through a stream (e.g. HTTP server,
 * content URI, etc.).
 */
public class StreamingZipArchiveFactory(
    private val mediaTypeRetriever: MediaTypeRetriever
) : ArchiveFactory {

    override suspend fun create(
        resource: Resource,
        password: String?
    ): Try<Container, ArchiveFactory.Error> {
        if (password != null) {
            return Try.failure(ArchiveFactory.Error.PasswordsNotSupported())
        }

        return try {
            val resourceChannel = ResourceChannel(resource)
            val channel = wrapBaseChannel(resourceChannel)
            val zipFile = ZipFile(channel, true)
            val channelZip = ChannelZipContainer(zipFile, resource.source, mediaTypeRetriever)
            Try.success(channelZip)
        } catch (e: Resource.Exception) {
            Try.failure(ArchiveFactory.Error.ResourceReading(e))
        } catch (e: Exception) {
            Try.failure(ArchiveFactory.Error.FormatNotSupported(e))
        }
    }

    internal fun openFile(file: File): Container {
        val fileChannel = FileChannelAdapter(file, "r")
        val channel = wrapBaseChannel(fileChannel)
        return ChannelZipContainer(ZipFile(channel), file.toUrl(), mediaTypeRetriever)
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

    public companion object {

        private const val CACHE_ALL_MAX_SIZE = 5242880

        private const val CACHED_TAIL_SIZE = 65557
    }
}
