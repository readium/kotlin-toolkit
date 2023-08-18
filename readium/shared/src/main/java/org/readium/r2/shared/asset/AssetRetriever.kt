/*
 * Copyright 2023 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.shared.asset

import android.net.Uri
import java.io.File
import org.readium.r2.shared.error.ThrowableError
import org.readium.r2.shared.error.Try
import org.readium.r2.shared.error.flatMap
import org.readium.r2.shared.error.getOrElse
import org.readium.r2.shared.format.Format
import org.readium.r2.shared.format.FormatHints
import org.readium.r2.shared.format.FormatRegistry
import org.readium.r2.shared.resource.ArchiveFactory
import org.readium.r2.shared.resource.Container
import org.readium.r2.shared.resource.ContainerFactory
import org.readium.r2.shared.resource.ContainerMediaTypeSnifferContext
import org.readium.r2.shared.resource.DefaultArchiveFactory
import org.readium.r2.shared.resource.DirectoryContainerFactory
import org.readium.r2.shared.resource.FileResourceFactory
import org.readium.r2.shared.resource.Resource
import org.readium.r2.shared.resource.ResourceFactory
import org.readium.r2.shared.resource.ResourceMediaTypeSnifferContext
import org.readium.r2.shared.util.Url
import org.readium.r2.shared.util.mediatype.MediaType
import org.readium.r2.shared.util.toUrl

public class AssetRetriever(
    private val formatRegistry: FormatRegistry,
    private val resourceFactory: ResourceFactory,
    private val containerFactory: ContainerFactory,
    private val archiveFactory: ArchiveFactory
) {

    public companion object {
        public operator fun invoke(): AssetRetriever {
            val formatRegistry = FormatRegistry()
            return AssetRetriever(
                formatRegistry = formatRegistry,
                resourceFactory = FileResourceFactory(formatRegistry),
                containerFactory = DirectoryContainerFactory(formatRegistry),
                archiveFactory = DefaultArchiveFactory()
            )
        }
    }

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
    ): Try<Asset, Error> {
        val format = formatRegistry.retrieve(mediaType)

        return when (assetType) {
            AssetType.Archive ->
                retrieveArchiveAsset(url, format)

            AssetType.Directory ->
                retrieveDirectoryAsset(url, format)

            AssetType.Resource ->
                retrieveResourceAsset(url, format)
        }
    }

    private suspend fun retrieveArchiveAsset(
        url: Url,
        format: Format
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
            .map { container -> Asset.Container(format, exploded = false, container) }
    }

    private suspend fun retrieveDirectoryAsset(
        url: Url,
        format: Format
    ): Try<Asset.Container, Error> {
        return containerFactory.create(url)
            .map { container ->
                Asset.Container(format, exploded = true, container)
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
        format: Format
    ): Try<Asset.Resource, Error> {
        return retrieveResource(url)
            .map { resource -> Asset.Resource(format, resource) }
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

    /**
     * Retrieves an asset from a local file.
     */
    public suspend fun retrieve(
        file: File,
        hints: FormatHints = FormatHints()
    ): Asset? =
        retrieve(file.toUrl(), hints)

    /**
     * Retrieves an asset from a Uri.
     */
    public suspend fun retrieve(
        uri: Uri,
        hints: FormatHints = FormatHints()
    ): Asset? {
        val url = uri.toUrl()
            ?: return null

        return retrieve(url, hints)
    }

    /**
     * Retrieves an asset from a Url.
     */
    public suspend fun retrieve(
        url: Url,
        hints: FormatHints = FormatHints()
    ): Asset? {
        @Suppress("NAME_SHADOWING")
        val hints =
            hints + FormatHints(fileExtension = url.extension)

        val resource = resourceFactory
            .create(url)
            .getOrElse { error ->
                when (error) {
                    is ResourceFactory.Error.NotAResource ->
                        return containerFactory.create(url).getOrNull()
                            ?.let { retrieve(it, exploded = true, hints) }
                    else -> return null
                }
            }

        return archiveFactory.create(resource, password = null)
            .fold(
                { retrieve(container = it, exploded = false, hints) },
                { retrieve(resource, hints) }
            )
    }

    private suspend fun retrieve(container: Container, exploded: Boolean, hints: FormatHints): Asset? {
        val format = formatRegistry.retrieve(ContainerMediaTypeSnifferContext(container, hints))
            ?: return null
        return Asset.Container(format, exploded = exploded, container = container)
    }

    private suspend fun retrieve(resource: Resource, hints: FormatHints): Asset? {
        val format = formatRegistry.retrieve(ResourceMediaTypeSnifferContext(resource, hints))
            ?: return null
        return Asset.Resource(format, resource = resource)
    }
}
