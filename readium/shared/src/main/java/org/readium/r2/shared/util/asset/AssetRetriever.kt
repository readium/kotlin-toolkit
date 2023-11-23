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
import org.readium.r2.shared.util.getOrElse
import org.readium.r2.shared.util.mediatype.FormatRegistry
import org.readium.r2.shared.util.mediatype.MediaType
import org.readium.r2.shared.util.mediatype.MediaTypeHints
import org.readium.r2.shared.util.resource.ArchiveFactory
import org.readium.r2.shared.util.resource.MediaTypeRetriever
import org.readium.r2.shared.util.resource.Resource
import org.readium.r2.shared.util.resource.SmartArchiveFactory
import org.readium.r2.shared.util.resource.invoke
import org.readium.r2.shared.util.toUrl

/**
 * Retrieves an [Asset] instance providing reading access to the resource(s) of an asset stored at a
 * given [Url].
 */
public class AssetRetriever(
    private val mediaTypeRetriever: MediaTypeRetriever,
    private val resourceFactory: ResourceFactory,
    archiveFactory: ArchiveFactory,
    formatRegistry: FormatRegistry = FormatRegistry()
) {

    public sealed class Error(
        override val message: String,
        override val cause: org.readium.r2.shared.util.Error?
    ) : org.readium.r2.shared.util.Error {

        public class SchemeNotSupported(
            public val scheme: Url.Scheme,
            cause: org.readium.r2.shared.util.Error? = null
        ) : Error("Scheme $scheme is not supported.", cause)

        public class FormatNotSupported(cause: org.readium.r2.shared.util.Error) :
            Error("Archive providers do not support this kind of archive.", cause)

        public class ReadError(override val cause: org.readium.r2.shared.util.data.ReadError) :
            Error("An error occurred when trying to read asset.", cause)
    }

    private val archiveFactory: ArchiveFactory =
        SmartArchiveFactory(archiveFactory, formatRegistry)

    /**
     * Retrieves an asset from a known media and asset type.
     */
    public suspend fun retrieve(
        url: AbsoluteUrl,
        mediaType: MediaType
    ): Try<Asset, Error> {
        val resource = retrieveResource(url, mediaType)
            .getOrElse { return Try.failure(it) }

        val archive = archiveFactory.create(mediaType, resource)
            .getOrElse {
                return when (it) {
                    is ArchiveFactory.Error.ReadError ->
                        Try.failure(Error.ReadError(it.cause))
                    is ArchiveFactory.Error.FormatNotSupported ->
                        Try.success(Asset.Resource(mediaType, resource))
                    is ArchiveFactory.Error.PasswordsNotSupported ->
                        Try.failure(Error.FormatNotSupported(it))
                }
            }

        return Try.success(Asset.Container(mediaType, archive))
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

        val mediaType = mediaTypeRetriever.retrieve(
            MediaTypeHints(properties),
            resource
        ).getOrElse {
            return Try.failure(
                Error.FormatNotSupported(
                    MessageError("Cannot determine asset media type.")
                )
            )
        }

        val container = archiveFactory.create(mediaType, resource)
            .getOrElse {
                when (it) {
                    is ArchiveFactory.Error.ReadError ->
                        return Try.failure(Error.ReadError(it.cause))
                    else ->
                        return Try.success(Asset.Resource(mediaType, resource))
                }
            }

        return Try.success(Asset.Container(mediaType, container))
    }
}
