/*
 * Copyright 2023 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.shared.asset

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import java.io.File
import org.readium.r2.shared.error.ThrowableError
import org.readium.r2.shared.error.Try
import org.readium.r2.shared.error.flatMap
import org.readium.r2.shared.resource.*
import org.readium.r2.shared.util.Url
import org.readium.r2.shared.util.mediatype.*
import org.readium.r2.shared.util.toUrl

public class AssetRetriever(
    private val resourceFactory: ResourceFactory,
    private val containerFactory: ContainerFactory,
    private val archiveFactory: ArchiveFactory,
    contentResolver: ContentResolver,
    sniffers: List<Sniffer>
) {

    public constructor(context: Context) : this(
        resourceFactory = FileResourceFactory(),
        containerFactory = DirectoryContainerFactory(),
        archiveFactory = DefaultArchiveFactory(),
        contentResolver = context.contentResolver,
        sniffers = MediaType.sniffers
    )

    public sealed class Error : org.readium.r2.shared.error.Error {

        public class SchemeNotSupported(
            public val scheme: String,
            override val cause: org.readium.r2.shared.error.Error?
        ) : Error() {

            public constructor(scheme: String, exception: Exception) :
                this(scheme, ThrowableError(exception))

            override val message: String =
                "Scheme $scheme is not supported."
        }

        public class NotFound(
            public val url: Url,
            override val cause: org.readium.r2.shared.error.Error?
        ) : Error() {

            public constructor(url: Url, exception: Exception) :
                this(url, ThrowableError(exception))

            override val message: String =
                "Asset could not be found at $url."
        }

        public class InvalidAsset(
            override val cause: org.readium.r2.shared.error.Error?
        ) : Error() {

            public constructor(exception: Exception) :
                this(ThrowableError(exception))

            override val message: String =
                "Asset looks corrupted."
        }

        public class ArchiveFormatNotSupported(
            override val cause: org.readium.r2.shared.error.Error?
        ) : Error() {

            public constructor(exception: Exception) :
                this(ThrowableError(exception))

            override val message: String =
                "Archive factory does not support this kind of archive."
        }

        public class Forbidden(
            public val url: Url,
            override val cause: org.readium.r2.shared.error.Error
        ) : Error() {

            public constructor(url: Url, exception: Exception) :
                this(url, ThrowableError(exception))

            override val message: String =
                "Access to asset at url $url is forbidden."
        }

        public class Unavailable(
            override val cause: org.readium.r2.shared.error.Error
        ) : Error() {

            public constructor(exception: Exception) :
                this(ThrowableError(exception))

            override val message: String =
                "Asset seems not to be available at the moment."
        }

        public class OutOfMemory(
            error: OutOfMemoryError
        ) : Error() {

            override val message: String =
                "There is not enough memory on the device to load the asset."

            override val cause: org.readium.r2.shared.error.Error =
                ThrowableError(error)
        }

        public class Unknown(
            private val exception: Exception
        ) : Error() {

            override val message: String =
                exception.message ?: "Something unexpected happened."

            override val cause: org.readium.r2.shared.error.Error =
                ThrowableError(exception)
        }
    }

    /**
     * Retrieves an asset from a known media and asset type again.
     */
    public suspend fun retrieve(
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
        return retrieveResource(url)
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
            .map { container -> Asset.Container(mediaType, exploded = false, container) }
    }

    private suspend fun retrieveDirectoryAsset(
        url: Url,
        mediaType: MediaType
    ): Try<Asset.Container, Error> {
        return containerFactory.create(url)
            .map { container ->
                Asset.Container(mediaType, exploded = true, container)
            }
            .mapFailure { error ->
                when (error) {
                    is ContainerFactory.Error.NotAContainer ->
                        Error.NotFound(url, error)
                    is ContainerFactory.Error.Forbidden ->
                        Error.Forbidden(url, error)
                    is ContainerFactory.Error.SchemeNotSupported ->
                        Error.SchemeNotSupported(error.scheme, error)
                }
            }
    }

    private suspend fun retrieveResourceAsset(
        url: Url,
        mediaType: MediaType
    ): Try<Asset.Resource, Error> {
        return retrieveResource(url)
            .map { resource -> Asset.Resource(mediaType, resource) }
    }

    private suspend fun retrieveResource(
        url: Url
    ): Try<Resource, Error> {
        return resourceFactory.create(url)
            .mapFailure { error ->
                when (error) {
                    is ResourceFactory.Error.NotAResource ->
                        Error.NotFound(url, error)
                    is ResourceFactory.Error.Forbidden ->
                        Error.Forbidden(url, error)
                    is ResourceFactory.Error.SchemeNotSupported ->
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
        UrlSnifferContextFactory(resourceFactory, containerFactory, archiveFactory)

    private val mediaTypeRetriever: MediaTypeRetriever =
        MediaTypeRetriever(
            resourceFactory,
            containerFactory,
            archiveFactory,
            contentResolver,
            sniffers
        )

    /**
     * Retrieves an asset from a local file.
     */
    public suspend fun retrieve(
        file: File,
        mediaType: String? = null,
        fileExtension: String? = null
    ): Asset? =
        retrieve(
            file,
            mediaTypes = listOfNotNull(mediaType),
            fileExtensions = listOfNotNull(fileExtension)
        )

    /**
     * Retrieves an asset from a local file.
     */
    public suspend fun retrieve(
        file: File,
        mediaTypes: List<String>,
        fileExtensions: List<String>
    ): Asset? {
        val context = snifferContextFactory
            .createContext(
                file.toUrl(),
                mediaTypes = mediaTypes,
                fileExtensions = listOf(file.extension) + fileExtensions
            ) ?: return null

        return retrieve(context)
    }

    /**
     * Retrieves an asset from an Uri.
     */
    public suspend fun retrieve(
        uri: Uri,
        mediaType: String? = null,
        fileExtension: String? = null
    ): Asset? =
        retrieve(
            uri,
            mediaTypes = listOfNotNull(mediaType),
            fileExtensions = listOfNotNull(fileExtension)
        )

    /**
     * Retrieves an asset from a Uri.
     */
    public suspend fun retrieve(
        uri: Uri,
        mediaTypes: List<String>,
        fileExtensions: List<String>
    ): Asset? {
        val url = uri.toUrl()
            ?: return null

        return retrieve(url, mediaTypes, fileExtensions)
    }

    /**
     * Retrieves an asset from a Url.
     */
    public suspend fun retrieve(
        url: Url,
        mediaType: String? = null,
        fileExtension: String? = null
    ): Asset? {
        return retrieve(url, listOfNotNull(mediaType), listOfNotNull(fileExtension))
    }

    /**
     * Retrieves an asset from a Url.
     */
    public suspend fun retrieve(
        url: Url,
        mediaTypes: List<String>,
        fileExtensions: List<String>
    ): Asset? {
        val context = snifferContextFactory
            .createContext(
                url,
                mediaTypes = mediaTypes,
                fileExtensions = buildList {
                    addAll(fileExtensions)
                    url.extension?.let { add(it) }
                }
            )
            ?: return null

        return retrieve(context)
    }

    private suspend fun retrieve(context: ContentAwareSnifferContext): Asset? {
        val mediaType = mediaTypeRetriever.doRetrieve(
            fullContext = { context },
            mediaTypes = context.mediaTypes.map(MediaType::toString),
            fileExtensions = context.fileExtensions
        ) ?: return null

        return when (context) {
            is ContainerSnifferContext ->
                Asset.Container(
                    mediaType = mediaType,
                    exploded = context.isExploded,
                    container = context.container
                )
            is ResourceSnifferContext ->
                Asset.Resource(
                    mediaType = mediaType,
                    resource = context.resource
                )
        }
    }
}
