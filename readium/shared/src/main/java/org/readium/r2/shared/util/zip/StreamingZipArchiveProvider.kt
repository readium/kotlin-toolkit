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
import org.readium.r2.shared.util.AbsoluteUrl
import org.readium.r2.shared.util.Error
import org.readium.r2.shared.util.Try
import org.readium.r2.shared.util.datasource.DataSource
import org.readium.r2.shared.util.mediatype.MediaType
import org.readium.r2.shared.util.mediatype.MediaTypeHints
import org.readium.r2.shared.util.mediatype.MediaTypeRetriever
import org.readium.r2.shared.util.mediatype.MediaTypeSnifferContentException
import org.readium.r2.shared.util.mediatype.MediaTypeSnifferContentException.Companion.unwrapMediaTypeSnifferContentException
import org.readium.r2.shared.util.mediatype.MediaTypeSnifferError
import org.readium.r2.shared.util.mediatype.ResourceMediaTypeSnifferContent
import org.readium.r2.shared.util.mediatype.asDataSource
import org.readium.r2.shared.util.resource.ArchiveFactory
import org.readium.r2.shared.util.resource.ArchiveProvider
import org.readium.r2.shared.util.resource.Container
import org.readium.r2.shared.util.resource.Resource
import org.readium.r2.shared.util.resource.ResourceError
import org.readium.r2.shared.util.resource.ResourceException
import org.readium.r2.shared.util.resource.ResourceException.Companion.unwrapResourceException
import org.readium.r2.shared.util.resource.asDataSource
import org.readium.r2.shared.util.toUrl
import org.readium.r2.shared.util.zip.compress.archivers.zip.ZipFile
import org.readium.r2.shared.util.zip.jvm.SeekableByteChannel

/**
 * An [ArchiveFactory] able to open a ZIP archive served through a stream (e.g. HTTP server,
 * content URI, etc.).
 */
public class StreamingZipArchiveProvider(
    private val mediaTypeRetriever: MediaTypeRetriever
) : ArchiveProvider {

    override fun sniffHints(hints: MediaTypeHints): Try<MediaType, MediaTypeSnifferError.NotRecognized> {
        if (hints.hasMediaType("application/zip") ||
            hints.hasFileExtension("zip")
        ) {
            return Try.success(MediaType.ZIP)
        }

        return Try.failure(MediaTypeSnifferError.NotRecognized)
    }

    override suspend fun sniffResource(resource: ResourceMediaTypeSnifferContent): Try<MediaType, MediaTypeSnifferError> {
        val datasource = resource.asDataSource()

        return try {
            openDataSource(datasource, ::MediaTypeSnifferContentException, null)
            Try.success(MediaType.ZIP)
        } catch (exception: Exception) {
            when (val e = exception.unwrapMediaTypeSnifferContentException()) {
                is MediaTypeSnifferContentException ->
                    Try.failure(MediaTypeSnifferError.SourceError(e.error))
                else ->
                    Try.failure(MediaTypeSnifferError.NotRecognized)
            }
        }
    }

    override suspend fun create(
        resource: Resource,
        password: String?
    ): Try<Container, ArchiveFactory.Error> {
        if (password != null) {
            return Try.failure(ArchiveFactory.Error.PasswordsNotSupported())
        }

        return try {
            val container = openDataSource(
                resource.asDataSource(),
                ::ResourceException,
                resource.source
            )
            Try.success(container)
        } catch (exception: Exception) {
            when (val e = exception.unwrapResourceException()) {
                is ResourceException ->
                    Try.failure(ArchiveFactory.Error.ResourceError(e.error))
                else ->
                    Try.failure(ArchiveFactory.Error.ResourceError(ResourceError.InvalidContent(e)))
            }
        }
    }

    private suspend fun<E : Error> openDataSource(
        dataSource: DataSource<E>,
        wrapError: (E) -> IOException,
        sourceUrl: AbsoluteUrl?
    ): Container = withContext(Dispatchers.IO) {
        val datasourceChannel = DatasourceChannel(dataSource, wrapError)
        val channel = wrapBaseChannel(datasourceChannel)
        val zipFile = ZipFile(channel, true)
        ChannelZipContainer(zipFile, sourceUrl, mediaTypeRetriever)
    }

    internal suspend fun openFile(file: File): Container = withContext(Dispatchers.IO) {
        val fileChannel = FileChannelAdapter(file, "r")
        val channel = wrapBaseChannel(fileChannel)
        ChannelZipContainer(ZipFile(channel), file.toUrl(), mediaTypeRetriever)
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
