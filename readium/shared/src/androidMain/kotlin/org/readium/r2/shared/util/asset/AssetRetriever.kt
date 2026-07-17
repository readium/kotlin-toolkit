/*
 * Copyright 2023 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.shared.util.asset

import android.content.ContentResolver
import java.io.File
import org.readium.r2.shared.util.AbsoluteUrl
import org.readium.r2.shared.util.Either
import org.readium.r2.shared.util.Error
import org.readium.r2.shared.util.FileExtension
import org.readium.r2.shared.util.Try
import org.readium.r2.shared.util.Url
import org.readium.r2.shared.util.archive.ArchiveOpener
import org.readium.r2.shared.util.data.Container
import org.readium.r2.shared.util.file.FileResource
import org.readium.r2.shared.util.format.Format
import org.readium.r2.shared.util.format.FormatHints
import org.readium.r2.shared.util.format.FormatSniffer
import org.readium.r2.shared.util.getOrElse
import org.readium.r2.shared.util.http.HttpClient
import org.readium.r2.shared.util.mediatype.MediaType
import org.readium.r2.shared.util.resource.Resource
import org.readium.r2.shared.util.resource.ResourceFactory
import org.readium.r2.shared.util.resource.borrow
import org.readium.r2.shared.util.resource.filename
import org.readium.r2.shared.util.resource.mediaType
import org.readium.r2.shared.util.use

/**
 * Retrieves an [Asset] instance providing reading access to the resource(s) of an asset stored at
 * a given [Url] as well as its [Format].
 */
public class AssetRetriever private constructor(
    private val assetSniffer: AssetSniffer,
    private val resourceFactory: ResourceFactory,
    private val archiveOpener: ArchiveOpener,
) {
    public constructor(
        resourceFactory: ResourceFactory,
        archiveOpener: ArchiveOpener,
        formatSniffer: FormatSniffer,
    ) : this(AssetSniffer(formatSniffer, archiveOpener), resourceFactory, archiveOpener)

    public constructor(
        contentResolver: ContentResolver,
        httpClient: HttpClient,
    ) : this(
        DefaultResourceFactory(contentResolver, httpClient),
        DefaultArchiveOpener(),
        DefaultFormatSniffer()
    )

    /**
     * Error while trying to retrieve an asset from an URL.
     */
    public sealed class RetrieveUrlError(
        override val message: String,
        override val cause: Error?,
    ) : Error {

        /**
         * The scheme (e.g. http, file, content) for the requested [Url] is not supported.
         */
        public class SchemeNotSupported(
            public val scheme: Url.Scheme,
            cause: Error? = null,
        ) : RetrieveUrlError("Url scheme $scheme is not supported.", cause)

        /**
         * The format of the resource at the requested [Url] is not recognized.
         */
        public class FormatNotSupported(
            cause: Error? = null,
        ) : RetrieveUrlError("Asset format is not supported.", cause)

        /**
         * An error occurred when trying to read the asset.
         */
        public class Reading(override val cause: org.readium.r2.shared.util.data.ReadError) :
            RetrieveUrlError("An error occurred when trying to read asset.", cause)
    }

    /**
     * Error while trying to retrieve an asset from a [Resource] or a [Container].
     */
    public sealed class RetrieveError(
        override val message: String,
        override val cause: Error?,
    ) : Error {

        /**
         * The format of the resource is not recognized.
         */
        public class FormatNotSupported(
            cause: Error? = null,
        ) : RetrieveError("Asset format is not supported.", cause)

        /**
         * An error occurred when trying to read the asset.
         */
        public class Reading(override val cause: org.readium.r2.shared.util.data.ReadError) :
            RetrieveError("An error occurred when trying to read asset.", cause)
    }

    /**
     * Retrieves an asset from an url and a known format.
     */
    public suspend fun retrieve(
        url: AbsoluteUrl,
        format: Format,
    ): Try<Asset, RetrieveUrlError> {
        val resource = resourceFactory.create(url)
            .getOrElse {
                when (it) {
                    is ResourceFactory.Error.SchemeNotSupported ->
                        return Try.failure(RetrieveUrlError.SchemeNotSupported(it.scheme, it))
                }
            }

        val asset = archiveOpener
            .open(format, resource)
            .getOrElse {
                return when (it) {
                    is ArchiveOpener.OpenError.Reading ->
                        Try.failure(RetrieveUrlError.Reading(it.cause))
                    is ArchiveOpener.OpenError.FormatNotSupported ->
                        Try.success(ResourceAsset(format, resource))
                }
            }

        return Try.success(asset)
    }

    /**
     * Retrieves an asset from a local file.
     */
    public suspend fun retrieve(
        file: File,
        formatHints: FormatHints = FormatHints(),
    ): Try<Asset, RetrieveError> =
        retrieve(FileResource(file), formatHints)

    /**
     * Retrieves an asset from an [AbsoluteUrl].
     */
    public suspend fun retrieve(
        url: AbsoluteUrl,
        formatHints: FormatHints = FormatHints(),
    ): Try<Asset, RetrieveUrlError> {
        val resource = resourceFactory.create(url)
            .getOrElse {
                return Try.failure(
                    when (it) {
                        is ResourceFactory.Error.SchemeNotSupported ->
                            RetrieveUrlError.SchemeNotSupported(it.scheme)
                    }
                )
            }

        return retrieve(resource, formatHints)
            .mapFailure {
                when (it) {
                    is RetrieveError.FormatNotSupported -> RetrieveUrlError.FormatNotSupported(
                        it.cause
                    )
                    is RetrieveError.Reading -> RetrieveUrlError.Reading(it.cause)
                }
            }
    }

    /**
     * Retrieves an asset from an [AbsoluteUrl].
     */
    public suspend fun retrieve(
        url: AbsoluteUrl,
        mediaType: MediaType,
    ): Try<Asset, RetrieveUrlError> =
        retrieve(url, FormatHints(mediaType = mediaType))

    /**
     * Retrieves an asset from a local file.
     */
    public suspend fun retrieve(
        file: File,
        mediaType: MediaType,
    ): Try<Asset, RetrieveError> =
        retrieve(file, FormatHints(mediaType = mediaType))

    /**
     * Retrieves an asset from an already opened resource.
     */
    public suspend fun retrieve(
        resource: Resource,
        hints: FormatHints = FormatHints(),
    ): Try<Asset, RetrieveError> {
        val properties = resource.properties()
            .getOrElse { return Try.failure(RetrieveError.Reading(it)) }

        val internalHints = FormatHints(
            mediaType = properties.mediaType,
            fileExtension = properties.filename
                ?.substringAfterLast(".")
                ?.let { FileExtension((it)) }
        )

        return assetSniffer
            .sniff(Either.Left(resource), hints + internalHints)
            .mapFailure {
                when (it) {
                    AssetSniffer.SniffError.NotRecognized -> RetrieveError.FormatNotSupported(it)
                    is AssetSniffer.SniffError.Reading -> RetrieveError.Reading(it.cause)
                }
            }
    }

    /**
     * Retrieves an asset from an already opened container.
     */
    public suspend fun retrieve(
        container: Container<Resource>,
        hints: FormatHints = FormatHints(),
    ): Try<Asset, RetrieveError> =
        assetSniffer
            .sniff(Either.Right(container), hints)
            .mapFailure {
                when (it) {
                    AssetSniffer.SniffError.NotRecognized -> RetrieveError.FormatNotSupported(it)
                    is AssetSniffer.SniffError.Reading -> RetrieveError.Reading(it.cause)
                }
            }

    /**
     * Retrieves an asset from an already opened resource.
     */
    public suspend fun retrieve(
        resource: Resource,
        mediaType: MediaType,
    ): Try<Asset, RetrieveError> =
        retrieve(resource, FormatHints(mediaType = mediaType))

    /**
     * Retrieves an asset from an already opened container.
     */
    public suspend fun retrieve(
        container: Container<Resource>,
        mediaType: MediaType,
    ): Try<Asset, RetrieveError> =
        retrieve(container, FormatHints(mediaType = mediaType))

    /**
     * Sniffs the format of a file content.
     */
    public suspend fun sniffFormat(
        file: File,
        hints: FormatHints = FormatHints(),
    ): Try<Format, RetrieveError> =
        FileResource(file).use { sniffFormat(it, hints) }

    /**
     * Sniffs the format of the content available at [url].
     */
    public suspend fun sniffFormat(
        url: AbsoluteUrl,
        hints: FormatHints = FormatHints(),
    ): Try<Format, RetrieveUrlError> =
        retrieve(url, hints)
            .map { asset -> asset.use { it.format } }

    /**
     * Sniffs the format of a resource content.
     */
    public suspend fun sniffFormat(
        resource: Resource,
        hints: FormatHints = FormatHints(),
    ): Try<Format, RetrieveError> =
        retrieve(resource.borrow(), hints)
            .map { asset -> asset.use { it.format } }

    /**
     * Sniffs the format of a container content.
     */
    public suspend fun sniffFormat(
        container: Container<Resource>,
        hints: FormatHints = FormatHints(),
    ): Try<Format, RetrieveError> =
        retrieve(container, hints)
            .map { asset -> asset.use { it.format } }
}
