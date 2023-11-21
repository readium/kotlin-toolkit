/*
 * Copyright 2023 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.shared.util.asset

import java.io.File
import org.readium.r2.shared.util.AbsoluteUrl
import org.readium.r2.shared.util.MessageError
import org.readium.r2.shared.util.Try
import org.readium.r2.shared.util.Url
import org.readium.r2.shared.util.archive.ArchiveFactory
import org.readium.r2.shared.util.archive.ArchiveProvider
import org.readium.r2.shared.util.archive.FileZipArchiveProvider
import org.readium.r2.shared.util.getOrElse
import org.readium.r2.shared.util.mediatype.MediaType
import org.readium.r2.shared.util.mediatype.MediaTypeHints
import org.readium.r2.shared.util.mediatype.MediaTypeRetriever
import org.readium.r2.shared.util.mediatype.MediaTypeSnifferError
import org.readium.r2.shared.util.resource.Resource
import org.readium.r2.shared.util.resource.invoke
import org.readium.r2.shared.util.toUrl
import org.readium.r2.shared.util.tryRecover

/**
 * Retrieves an [Asset] instance providing reading access to the resource(s) of an asset stored at a
 * given [Url].
 */
public class AssetRetriever(
    private val resourceFactory: ResourceFactory = FileResourceFactory(),
    private val archiveProvider: ArchiveProvider = FileZipArchiveProvider(),
    private val mediaTypeRetriever: MediaTypeRetriever = MediaTypeRetriever()
) {

    public sealed class Error(
        override val message: String,
        override val cause: org.readium.r2.shared.util.Error?
    ) : org.readium.r2.shared.util.Error {

        public class SchemeNotSupported(
            public val scheme: Url.Scheme,
            cause: org.readium.r2.shared.util.Error? = null
        ) : Error("Scheme $scheme is not supported.", cause)

        public class ArchiveFormatNotSupported(cause: org.readium.r2.shared.util.Error) :
            Error("Archive providers do not support this kind of archive.", cause)

        public class ReadError(override val cause: org.readium.r2.shared.util.data.ReadError) :
            Error("An error occurred when trying to read asset.", cause)
    }

    /**
     * Retrieves an asset from a known media and asset type.
     */
    public suspend fun retrieve(
        url: AbsoluteUrl,
        mediaType: MediaType,
        containerType: MediaType?
    ): Try<Asset, Error> {
        return when (containerType) {
            null ->
                retrieveResourceAsset(url, mediaType)

            else ->
                retrieveArchiveAsset(url, mediaType, containerType)
        }
    }

    private suspend fun retrieveArchiveAsset(
        url: AbsoluteUrl,
        mediaType: MediaType,
        containerType: MediaType
    ): Try<Asset.Container, Error> {
        val resource = retrieveResource(url, containerType)
            .getOrElse { return Try.failure(it) }

        archiveProvider.sniffHints(MediaTypeHints(mediaType = containerType))
            .onFailure {
                return Try.failure(
                    Error.ArchiveFormatNotSupported(
                        MessageError("Container type $containerType not recognized.")
                    )
                )
            }

        return retrieveArchiveAsset(resource, MediaTypeHints(mediaType = mediaType), containerType)
    }
    private suspend fun retrieveArchiveAsset(
        resource: Resource,
        mediaTypeHints: MediaTypeHints,
        containerType: MediaType
    ): Try<Asset.Container, Error> {
        val container = archiveProvider.create(resource)
            .mapFailure { error ->
                when (error) {
                    is ArchiveFactory.Error.ReadError ->
                        Error.ReadError(error.cause)
                    else ->
                        Error.ArchiveFormatNotSupported(error)
                }
            }
            .getOrElse { return Try.failure(it) }

        val mediaType = mediaTypeRetriever
            .retrieve(mediaTypeHints, container)
            .getOrElse { error ->
                when (error) {
                    MediaTypeSnifferError.NotRecognized ->
                        MediaType.BINARY
                    is MediaTypeSnifferError.Read ->
                        return Try.failure(Error.ReadError(error.cause))
                }
            }

        val asset = Asset.Container(
            mediaType = mediaType,
            containerType = containerType,
            container = container
        )

        return Try.success(asset)
    }

    private suspend fun retrieveResourceAsset(
        url: AbsoluteUrl,
        mediaType: MediaType
    ): Try<Asset.Resource, Error> {
        return retrieveResource(url, mediaType)
            .map { resource ->
                Asset.Resource(
                    mediaType,
                    resource
                )
            }
    }

    private suspend fun retrieveResource(
        url: AbsoluteUrl,
        mediaType: MediaType
    ): Try<Resource, Error> {
        return resourceFactory.create(url, mediaType)
            .mapFailure { error ->
                when (error) {
                    is ResourceFactory.Error.SchemeNotSupported ->
                        Error.SchemeNotSupported(error.scheme, error)
                }
            }
    }

    /* Sniff unknown assets */

    /**
     * Retrieves an asset from a local file.
     */
    public suspend fun retrieve(file: File): Try<Asset, Error> =
        retrieve(file.toUrl())

    /**
     * Retrieves an asset from a [Url].
     */
    public suspend fun retrieve(url: AbsoluteUrl): Try<Asset, Error> {
        val resource = resourceFactory.create(url)
            .getOrElse {
                return Try.failure(
                    when (it) {
                        is ResourceFactory.Error.SchemeNotSupported ->
                            Error.SchemeNotSupported(it.scheme)
                    }
                )
            }

        val properties = resource.properties()
            .getOrElse { return Try.failure(Error.ReadError(it)) }

        val containerType = archiveProvider.sniffHints(
            MediaTypeHints(properties)
        )
            .tryRecover {
                archiveProvider.sniffBlob(resource)
            }.getOrElse { error ->
                when (error) {
                    MediaTypeSnifferError.NotRecognized ->
                        null
                    is MediaTypeSnifferError.Read ->
                        return Try.failure(Error.ReadError(error.cause))
                }
            }

        if (containerType == null) {
            val mediaType = resource.mediaType()
                .getOrElse { return Try.failure(Error.ReadError(it)) }
            return Try.success(Asset.Resource(mediaType, resource))
        }

        val hints = MediaTypeHints(fileExtension = url.extension)

        return retrieveArchiveAsset(resource, mediaTypeHints = hints, containerType = containerType)
    }
}
