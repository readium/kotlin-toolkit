/*
 * Copyright 2023 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.shared.util.asset

import java.io.File
import org.readium.r2.shared.util.AbsoluteUrl
import org.readium.r2.shared.util.DebugError
import org.readium.r2.shared.util.Error
import org.readium.r2.shared.util.Try
import org.readium.r2.shared.util.Url
import org.readium.r2.shared.util.archive.ArchiveFactory
import org.readium.r2.shared.util.archive.RecursiveArchiveFactory
import org.readium.r2.shared.util.getOrElse
import org.readium.r2.shared.util.mediatype.FormatRegistry
import org.readium.r2.shared.util.mediatype.MediaType
import org.readium.r2.shared.util.mediatype.MediaTypeRetriever
import org.readium.r2.shared.util.resource.Resource
import org.readium.r2.shared.util.resource.ResourceFactory
import org.readium.r2.shared.util.toUrl

/**
 * Retrieves an [Asset] instance providing reading access to the resource(s) of an asset stored at
 * a given [Url] as well as a canonical media type.
 */
public class AssetRetriever(
    private val mediaTypeRetriever: MediaTypeRetriever,
    private val resourceFactory: ResourceFactory,
    archiveFactory: ArchiveFactory,
    formatRegistry: FormatRegistry
) {

    public sealed class RetrieveError(
        override val message: String,
        override val cause: Error?
    ) : Error {

        /**
         * The scheme (e.g. http, file, content) for the requested [Url] is not supported by the
         * [resourceFactory].
         */
        public class SchemeNotSupported(
            public val scheme: Url.Scheme,
            cause: Error? = null
        ) : RetrieveError("Url scheme $scheme is not supported.", cause)

        /**
         * The format of the resource at the requested [Url] is not recognized by the
         * [mediaTypeRetriever] and [archiveFactory].
         */
        public class FormatNotSupported(cause: Error) :
            RetrieveError("Asset format is not supported.", cause)

        /**
         * An error occurred when trying to read the asset.
         */
        public class Reading(override val cause: org.readium.r2.shared.util.data.ReadError) :
            RetrieveError("An error occurred when trying to read asset.", cause)
    }

    private val archiveFactory: ArchiveFactory =
        RecursiveArchiveFactory(archiveFactory, formatRegistry)

    /**
     * Retrieves an asset from an url and a known media type.
     */
    public suspend fun retrieve(
        url: AbsoluteUrl,
        mediaType: MediaType
    ): Try<Asset, RetrieveError> {
        val resource = retrieveResource(url, mediaType)
            .getOrElse { return Try.failure(it) }

        val archive = archiveFactory.create(mediaType, resource)
            .getOrElse {
                return when (it) {
                    is ArchiveFactory.CreateError.Reading ->
                        Try.failure(RetrieveError.Reading(it.cause))
                    is ArchiveFactory.CreateError.FormatNotSupported ->
                        Try.success(ResourceAsset(mediaType, resource))
                }
            }

        return Try.success(ContainerAsset(mediaType, archive))
    }

    private suspend fun retrieveResource(
        url: AbsoluteUrl,
        mediaType: MediaType
    ): Try<Resource, RetrieveError> {
        return resourceFactory.create(url, mediaType)
            .mapFailure { error ->
                when (error) {
                    is ResourceFactory.Error.SchemeNotSupported ->
                        RetrieveError.SchemeNotSupported(error.scheme, error)
                }
            }
    }

    /* Sniff unknown assets */

    /**
     * Retrieves an asset from an unknown local file.
     */
    public suspend fun retrieve(file: File): Try<Asset, RetrieveError> =
        retrieve(file.toUrl())

    /**
     * Retrieves an asset from an unknown [AbsoluteUrl].
     */
    public suspend fun retrieve(url: AbsoluteUrl): Try<Asset, RetrieveError> {
        val resource = resourceFactory.create(url)
            .getOrElse {
                return Try.failure(
                    when (it) {
                        is ResourceFactory.Error.SchemeNotSupported ->
                            RetrieveError.SchemeNotSupported(it.scheme)
                    }
                )
            }

        val mediaType = mediaTypeRetriever.retrieve(resource)
            .getOrElse {
                return Try.failure(
                    RetrieveError.FormatNotSupported(
                        DebugError("Cannot determine asset media type.")
                    )
                )
            }

        val container = archiveFactory.create(mediaType, resource)
            .getOrElse {
                when (it) {
                    is ArchiveFactory.CreateError.Reading ->
                        return Try.failure(RetrieveError.Reading(it.cause))
                    is ArchiveFactory.CreateError.FormatNotSupported ->
                        return Try.success(ResourceAsset(mediaType, resource))
                }
            }

        return Try.success(ContainerAsset(mediaType, container))
    }
}
