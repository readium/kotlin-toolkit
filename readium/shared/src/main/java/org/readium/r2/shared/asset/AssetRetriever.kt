/*
 * Copyright 2023 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.shared.asset

import android.content.ContentResolver
import android.net.Uri
import java.io.File
import org.readium.r2.shared.error.ThrowableError
import org.readium.r2.shared.error.Try
import org.readium.r2.shared.error.flatMap
import org.readium.r2.shared.resource.ArchiveFactory
import org.readium.r2.shared.resource.ContainerFactory
import org.readium.r2.shared.resource.Resource
import org.readium.r2.shared.resource.ResourceFactory
import org.readium.r2.shared.util.Url
import org.readium.r2.shared.util.mediatype.*
import org.readium.r2.shared.util.toUrl

class AssetRetriever(
    private val resourceFactory: ResourceFactory,
    private val containerFactory: ContainerFactory,
    private val archiveFactory: ArchiveFactory,
    contentResolver: ContentResolver,
    sniffers: List<Sniffer>
) {

    sealed class Error : org.readium.r2.shared.error.Error {

        class SchemeNotSupported(
            val scheme: String,
            override val cause: org.readium.r2.shared.error.Error?,
        ) : Error() {

            constructor(scheme: String, exception: Exception) :
                this(scheme, ThrowableError(exception))

            override val message: String =
                "Scheme $scheme is not supported."
        }

        class NotFound(
            val url: Url,
            override val cause: org.readium.r2.shared.error.Error?
        ) : Error() {

            constructor(url: Url, exception: Exception) :
                this(url, ThrowableError(exception))

            override val message: String =
                "Asset could not be found at $url."
        }

        class InvalidAsset(
            override val cause: org.readium.r2.shared.error.Error?
        ) : Error() {

            constructor(exception: Exception) :
                this(ThrowableError(exception))

            override val message: String =
                "Asset looks corrupted."
        }

        class ArchiveFormatNotSupported(
            override val cause: org.readium.r2.shared.error.Error?
        ) : Error() {

            constructor(exception: Exception) :
                this(ThrowableError(exception))

            override val message: String =
                "Archive factory does not support this kind of archive."
        }

        class Forbidden(
            val url: Url,
            override val cause: org.readium.r2.shared.error.Error
        ) : Error() {

            constructor(url: Url, exception: Exception) :
                this(url, ThrowableError(exception))

            override val message: String =
                "Access to asset at url $url is forbidden."
        }

        class Unavailable(
            override val cause: org.readium.r2.shared.error.Error
        ) : Error() {

            constructor(exception: Exception) :
                this(ThrowableError(exception))

            override val message: String =
                "Asset seems not to be available at the moment."
        }

        class OutOfMemory(
            error: OutOfMemoryError
        ) : Error() {

            override val message: String =
                "There is not enough memory on the device to load the asset."

            override val cause: org.readium.r2.shared.error.Error =
                ThrowableError(error)
        }

        class Unknown(
            private val exception: Exception
        ) : Error() {

            override val message: String =
                exception.message ?: "Something unexpected happened."

            override val cause: org.readium.r2.shared.error.Error =
                ThrowableError(exception)
        }
    }

    /**
     * Retrieves again a well-known asset.
     */
    suspend fun retrieve(
        url: Url,
        mediaType: MediaType,
        assetType: AssetType
    ): Try<Asset, Error> =
        when (assetType) {
            AssetType.Archive ->
                retrieveArchiveAsset(url, mediaType)
            AssetType.Directory ->
                retrieveDirectoryAsset(url, mediaType)
            AssetType.Resource ->
                retrieveResourceAsset(url, mediaType)
        }

    private suspend fun retrieveArchiveAsset(
        url: Url,
        mediaType: MediaType
    ): Try<Asset.Container, Error> {
        return resourceFactory.create(url)
            .mapFailure { error ->
                when (error) {
                    is ResourceFactory.Error.NotAResource ->
                        Error.NotFound(url, error)
                    is ResourceFactory.Error.Forbidden ->
                        Error.Forbidden(url, error)
                    is ResourceFactory.Error.UnsupportedScheme ->
                        Error.SchemeNotSupported(error.scheme, error)
                }
            }
            .flatMap { resource: Resource ->
                archiveFactory.create(resource, password = null)
                    .mapFailure { error ->
                        when (error) {
                            is ArchiveFactory.Error.FormatNotSupported ->
                                Error.ArchiveFormatNotSupported(error)
                            is ArchiveFactory.Error.ResourceReading ->
                                error.resourceException.wrap(url)
                            is ArchiveFactory.Error.PasswordsNotSupported ->
                                Error.ArchiveFormatNotSupported(error)
                        }
                    }
            }
            .map { container -> Asset.Container(url.filename, mediaType, AssetType.Archive, container) }
    }

    private suspend fun retrieveDirectoryAsset(
        url: Url,
        mediaType: MediaType
    ): Try<Asset.Container, Error> {
        return containerFactory.create(url)
            .map { container ->
                Asset.Container(url.filename, mediaType, AssetType.Directory, container)
            }
            .mapFailure { error ->
                when (error) {
                    is ContainerFactory.Error.NotAContainer ->
                        Error.NotFound(url, error)
                    is ContainerFactory.Error.Forbidden ->
                        Error.Forbidden(url, error)
                    is ContainerFactory.Error.UnsupportedScheme ->
                        Error.SchemeNotSupported(error.scheme, error)
                }
            }
    }

    private suspend fun retrieveResourceAsset(
        url: Url,
        mediaType: MediaType
    ): Try<Asset.Resource, Error> {
        return resourceFactory.create(url)
            .map { resource -> Asset.Resource(url.filename, mediaType, resource) }
            .mapFailure { error ->
                when (error) {
                    is ResourceFactory.Error.NotAResource ->
                        Error.NotFound(url, error)
                    is ResourceFactory.Error.Forbidden ->
                        Error.Forbidden(url, error)
                    is ResourceFactory.Error.UnsupportedScheme ->
                        Error.SchemeNotSupported(error.scheme, error)
                }
            }
    }

    private fun Resource.Exception.wrap(url: Url): Error =
        when (this) {
            is Resource.Exception.Forbidden ->
                Error.Forbidden(url, this)
            is Resource.Exception.NotFound ->
                Error.InvalidAsset(this)
            is Resource.Exception.Unavailable, Resource.Exception.Offline ->
                Error.Unavailable(this)
            is Resource.Exception.OutOfMemory ->
                Error.OutOfMemory(cause)
            is Resource.Exception.Other ->
                Error.Unknown(this)
            else -> Error.Unknown(this)
        }

    /* Sniff unknown assets */

    private val snifferContextFactory: UrlSnifferContextFactory =
        UrlSnifferContextFactory(resourceFactory, containerFactory, archiveFactory, contentResolver)

    private val mediaTypeRetriever: MediaTypeRetriever =
        MediaTypeRetriever(resourceFactory, containerFactory, archiveFactory, contentResolver, sniffers)

    /**
     * Retrieves an asset from a local file.
     */
    suspend fun retrieve(
        file: File,
        mediaType: String? = null,
        fileExtension: String? = null,
    ): Asset? =
        retrieve(
            file,
            mediaTypes = listOfNotNull(mediaType),
            fileExtensions = listOfNotNull(fileExtension)
        )

    /**
     * Retrieves an asset from a local file.
     */
    suspend fun retrieve(
        file: File,
        mediaTypes: List<String>,
        fileExtensions: List<String>,
    ): Asset? {
        val context = snifferContextFactory
            .createContext(
                file.toUrl(),
                mediaTypes = mediaTypes,
                fileExtensions = listOf(file.extension) + fileExtensions
            ) ?: return null

        return retrieve(context, file.name)
    }

    /**
     * Retrieves an asset from an Uri.
     */
    suspend fun retrieve(
        uri: Uri,
        mediaType: String? = null,
        fileExtension: String? = null,
    ): Asset? =
        retrieve(
            uri,
            mediaTypes = listOfNotNull(mediaType),
            fileExtensions = listOfNotNull(fileExtension)
        )

    /**
     * Retrieves an asset from a Uri.
     */
    suspend fun retrieve(
        uri: Uri,
        mediaTypes: List<String>,
        fileExtensions: List<String>,
    ): Asset? {
        val url = uri.toUrl()
            ?: return null

        return retrieve(url, mediaTypes, fileExtensions)
    }

    /**
     * Retrieves an asset from a Url.
     */
    suspend fun retrieve(
        url: Url,
        mediaType: String? = null,
        fileExtension: String? = null
    ): Asset? {
        return retrieve(url, listOfNotNull(mediaType), listOfNotNull(fileExtension))
    }

    /**
     * Retrieves an asset from a Url.
     */
    private suspend fun retrieve(
        url: Url,
        mediaTypes: List<String>,
        fileExtensions: List<String>
    ): Asset? {
        val context = snifferContextFactory
            .createContext(url, mediaTypes, fileExtensions + url.extension)
            ?: return null

        return retrieve(context, url.filename)
    }

    private suspend fun retrieve(
        context: ContentAwareSnifferContext,
        fallbackName: String
    ): Asset? {

        val mediaType = mediaTypeRetriever.doRetrieve(
            { context },
            context.mediaTypes.map(MediaType::toString),
            context.fileExtensions
        ) ?: return null

        return when (context) {
            is ContainerSnifferContext ->
                Asset.Container(
                    name = context.container.name().successOrNull() ?: fallbackName,
                    mediaType = mediaType,
                    assetType = if (context.isExploded) AssetType.Directory else AssetType.Archive,
                    container = context.container
                )
            is ResourceSnifferContext ->
                Asset.Resource(
                    name = context.resource.name().successOrNull() ?: fallbackName,
                    mediaType = mediaType,
                    resource = context.resource
                )
        }
    }
}
