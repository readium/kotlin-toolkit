/*
 * Copyright 2023 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.shared.util.asset

import android.content.ContentResolver
import android.content.Context
import android.provider.MediaStore
import java.io.File
import kotlin.Exception
import kotlin.String
import kotlin.let
import kotlin.takeUnless
import org.readium.r2.shared.extensions.queryProjection
import org.readium.r2.shared.util.AbsoluteUrl
import org.readium.r2.shared.util.Either
import org.readium.r2.shared.util.Error as SharedError
import org.readium.r2.shared.util.FilesystemError
import org.readium.r2.shared.util.MessageError
import org.readium.r2.shared.util.NetworkError
import org.readium.r2.shared.util.ThrowableError
import org.readium.r2.shared.util.Try
import org.readium.r2.shared.util.Url
import org.readium.r2.shared.util.getOrElse
import org.readium.r2.shared.util.mediatype.CompositeMediaTypeSniffer
import org.readium.r2.shared.util.mediatype.MediaType
import org.readium.r2.shared.util.mediatype.MediaTypeHints
import org.readium.r2.shared.util.mediatype.MediaTypeRetriever
import org.readium.r2.shared.util.mediatype.MediaTypeSniffer
import org.readium.r2.shared.util.mediatype.MediaTypeSnifferContentError
import org.readium.r2.shared.util.mediatype.MediaTypeSnifferError
import org.readium.r2.shared.util.resource.ArchiveFactory
import org.readium.r2.shared.util.resource.ArchiveProvider
import org.readium.r2.shared.util.resource.CompositeArchiveFactory
import org.readium.r2.shared.util.resource.Container
import org.readium.r2.shared.util.resource.ContainerMediaTypeSnifferContent
import org.readium.r2.shared.util.resource.FileResourceFactory
import org.readium.r2.shared.util.resource.FileZipArchiveProvider
import org.readium.r2.shared.util.resource.Resource
import org.readium.r2.shared.util.resource.ResourceError
import org.readium.r2.shared.util.resource.ResourceFactory
import org.readium.r2.shared.util.resource.ResourceMediaTypeSnifferContent
import org.readium.r2.shared.util.toUrl

/**
 * Retrieves an [Asset] instance providing reading access to the resource(s) of an asset stored at a
 * given [Url].
 */
public class AssetRetriever(
    private val mediaTypeRetriever: MediaTypeRetriever,
    private val resourceFactory: ResourceFactory,
    private val contentResolver: ContentResolver,
    archiveProviders: List<ArchiveProvider> = listOf(FileZipArchiveProvider(mediaTypeRetriever))
) {
    private val archiveSniffer: MediaTypeSniffer =
        CompositeMediaTypeSniffer(archiveProviders)

    private val archiveFactory: ArchiveFactory =
        CompositeArchiveFactory(archiveProviders)

    public companion object {
        public operator fun invoke(context: Context): AssetRetriever {
            val mediaTypeRetriever = MediaTypeRetriever()
            return AssetRetriever(
                mediaTypeRetriever = mediaTypeRetriever,
                resourceFactory = FileResourceFactory(mediaTypeRetriever),
                archiveProviders = emptyList(),
                contentResolver = context.contentResolver
            )
        }
    }

    public sealed class Error(
        override val message: String,
        override val cause: SharedError?
    ) : SharedError {

        public class SchemeNotSupported(
            public val scheme: Url.Scheme,
            cause: SharedError? = null
        ) : Error("Scheme $scheme is not supported.", cause) {

            public constructor(scheme: Url.Scheme, exception: Exception) :
                this(scheme, ThrowableError(exception))
        }

        public class NotFound(
            public val url: AbsoluteUrl,
            cause: SharedError?
        ) : Error("Asset could not be found at $url.", cause) {

            public constructor(url: AbsoluteUrl, exception: Exception) :
                this(url, ThrowableError(exception))
        }

        public class InvalidAsset(cause: SharedError?) :
            Error("Asset looks corrupted.", cause) {

            public constructor(exception: Exception) :
                this(ThrowableError(exception))
        }

        public class ArchiveFormatNotSupported(cause: SharedError?) :
            Error("Archive factory does not support this kind of archive.", cause) {

            public constructor(exception: Exception) :
                this(ThrowableError(exception))
        }

        public class Forbidden(
            public val url: AbsoluteUrl,
            cause: SharedError?
        ) : Error("Access to asset at url $url is forbidden.", cause) {

            public constructor(url: AbsoluteUrl, exception: Exception) :
                this(url, ThrowableError(exception))
        }

        public class Network(public override val cause: NetworkError) :
            Error("A network error occurred.", cause)

        public class Filesystem(public override val cause: FilesystemError) :
            Error("A filesystem error occurred.", cause)

        public class OutOfMemory(error: OutOfMemoryError) :
            Error(
                "There is not enough memory on the device to load the asset.",
                ThrowableError(error)
            )

        public class Unknown(error: SharedError) :
            Error("Something unexpected happened.", error)
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

        return retrieveArchiveAsset(url, resource, mediaType, containerType)
    }
    private suspend fun retrieveArchiveAsset(
        url: AbsoluteUrl,
        resource: Resource,
        mediaType: MediaType,
        containerType: MediaType
    ): Try<Asset.Container, Error> {
        val container = archiveFactory.create(resource)
            .getOrElse { error -> return Try.failure(error.toAssetRetrieverError(url)) }

        val asset = Asset.Container(
            mediaType = mediaType,
            containerType = containerType,
            container = container
        )

        return Try.success(asset)
    }

    private fun ArchiveFactory.Error.toAssetRetrieverError(url: AbsoluteUrl): Error =
        when (this) {
            is ArchiveFactory.Error.UnsupportedFormat ->
                Error.ArchiveFormatNotSupported(this)

            is ArchiveFactory.Error.ResourceError ->
                cause.wrap(url)

            is ArchiveFactory.Error.PasswordsNotSupported ->
                Error.ArchiveFormatNotSupported(this)
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

    private fun ResourceError.wrap(url: AbsoluteUrl): Error =
        when (this) {
            is ResourceError.Forbidden ->
                Error.Forbidden(url, this)

            is ResourceError.NotFound ->
                Error.InvalidAsset(this)

            is ResourceError.Network ->
                Error.Network(cause)

            is ResourceError.OutOfMemory ->
                Error.OutOfMemory(cause.throwable)

            is ResourceError.Other ->
                Error.Unknown(this)

            is ResourceError.InvalidContent ->
                Error.InvalidAsset(this)

            is ResourceError.Filesystem ->
                Error.Filesystem(cause)
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

        val mediaType = retrieveMediaType(url, Either.Left(resource))
            .getOrElse { return Try.failure(it.wrap(url)) }

        return archiveSniffer.sniffResource(ResourceMediaTypeSnifferContent(resource))
            .fold(
                { containerType ->
                    retrieveArchiveAsset(url, mediaType = mediaType, containerType = containerType)
                },
                { error ->
                    when (error) {
                        MediaTypeSnifferError.NotRecognized ->
                            Try.success(Asset.Resource(mediaType, resource))
                        is MediaTypeSnifferError.SourceError ->
                            Try.failure(error.wrap(url))
                    }
                }
            )
    }

    private fun MediaTypeSnifferError.wrap(url: AbsoluteUrl) = when (this) {
        is MediaTypeSnifferError.SourceError ->
            when (cause) {
                is MediaTypeSnifferContentError.Filesystem ->
                    Error.Filesystem(cause.cause)
                is MediaTypeSnifferContentError.Forbidden ->
                    Error.Forbidden(url, cause.cause)
                is MediaTypeSnifferContentError.Network ->
                    Error.Network(cause.cause)
                is MediaTypeSnifferContentError.NotFound ->
                    Error.NotFound(url, cause.cause)
                is MediaTypeSnifferContentError.ArchiveError ->
                    Error.InvalidAsset(cause)
                is MediaTypeSnifferContentError.TooBig ->
                    Error.OutOfMemory(cause.cause.throwable)
                is MediaTypeSnifferContentError.Unknown ->
                    Error.Unknown(cause)
            }
        MediaTypeSnifferError.NotRecognized ->
            Error.Unknown(MessageError("Cannot determine media type."))
    }

    private suspend fun retrieveMediaType(
        url: AbsoluteUrl,
        asset: Either<Resource, Container>
    ): Try<MediaType, MediaTypeSnifferError> {
        suspend fun retrieve(hints: MediaTypeHints): Try<MediaType, MediaTypeSnifferError> =
            mediaTypeRetriever.retrieve(
                hints = hints,
                content = when (asset) {
                    is Either.Left -> ResourceMediaTypeSnifferContent(asset.value)
                    is Either.Right -> ContainerMediaTypeSnifferContent(asset.value)
                }
            )

        retrieve(MediaTypeHints(fileExtensions = listOfNotNull(url.extension)))
            .onSuccess { return Try.success(it) }
            .onFailure { error ->
                if (error is MediaTypeSnifferError.SourceError) {
                    return Try.failure(error)
                }
            }

        // Falls back on the [contentResolver] in case of content Uri.
        // Note: This is done after the heavy sniffing of the provided [sniffers], because
        // otherwise it will detect JSON, XML or ZIP formats before we have a chance of sniffing
        // their content (for example, for RWPM).

        if (url.isContent) {
            val contentHints = MediaTypeHints(
                mediaType = contentResolver.getType(url.uri)
                    ?.let { MediaType(it) }
                    ?.takeUnless { it.matches(MediaType.BINARY) },
                fileExtension = contentResolver
                    .queryProjection(url.uri, MediaStore.MediaColumns.DISPLAY_NAME)
                    ?.let { filename -> File(filename).extension }
            )

            retrieve(contentHints)
                .getOrNull()
                ?.let { return Try.success(it) }
        }

        return Try.failure(MediaTypeSnifferError.NotRecognized)
    }
}
