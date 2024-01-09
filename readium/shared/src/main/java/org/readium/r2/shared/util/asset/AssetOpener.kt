/*
 * Copyright 2023 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.shared.util.asset

import android.content.ContentResolver
import java.io.File
import org.readium.r2.shared.InternalReadiumApi
import org.readium.r2.shared.util.AbsoluteUrl
import org.readium.r2.shared.util.Error
import org.readium.r2.shared.util.Try
import org.readium.r2.shared.util.Url
import org.readium.r2.shared.util.format.Format
import org.readium.r2.shared.util.format.FormatHints
import org.readium.r2.shared.util.format.FormatSniffer
import org.readium.r2.shared.util.getOrElse
import org.readium.r2.shared.util.http.HttpClient
import org.readium.r2.shared.util.mediatype.MediaType
import org.readium.r2.shared.util.resource.Resource
import org.readium.r2.shared.util.resource.ResourceFactory
import org.readium.r2.shared.util.toUrl

/**
 * Retrieves an [Asset] instance providing reading access to the resource(s) of an asset stored at
 * a given [Url] as well as a canonical media type.
 */
public class AssetOpener(
    @InternalReadiumApi public val assetSniffer: AssetSniffer,
    private val resourceFactory: ResourceFactory,
    private val archiveOpener: ArchiveOpener
) {
    public constructor(
        resourceFactory: ResourceFactory,
        archiveOpener: ArchiveOpener,
        formatSniffer: FormatSniffer
    ) : this(AssetSniffer(formatSniffer, archiveOpener), resourceFactory, archiveOpener)

    public constructor(
        contentResolver: ContentResolver,
        httpClient: HttpClient
    ) : this(
        DefaultResourceFactory(contentResolver, httpClient),
        DefaultArchiveOpener(),
        DefaultFormatSniffer()
    )

    public sealed class OpenError(
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
        ) : OpenError("Url scheme $scheme is not supported.", cause)

        /**
         * The format of the resource at the requested [Url] is not recognized by the
         * [assetSniffer].
         */
        public class FormatNotSupported(
            cause: Error? = null
        ) : OpenError("Asset format is not supported.", cause)

        /**
         * An error occurred when trying to read the asset.
         */
        public class Reading(override val cause: org.readium.r2.shared.util.data.ReadError) :
            OpenError("An error occurred when trying to read asset.", cause)
    }

    /**
     * Retrieves an asset from an url and a known format.
     */
    public suspend fun open(
        url: AbsoluteUrl,
        format: Format
    ): Try<Asset, OpenError> {
        val resource = retrieveResource(url)
            .getOrElse { return Try.failure(it) }

        val asset = archiveOpener
            .open(format, resource)
            .getOrElse {
                return when (it) {
                    is ArchiveOpener.OpenError.Reading ->
                        Try.failure(OpenError.Reading(it.cause))
                    is ArchiveOpener.OpenError.FormatNotSupported ->
                        Try.success(ResourceAsset(format, resource))
                }
            }

        return Try.success(asset)
    }

    private suspend fun retrieveResource(
        url: AbsoluteUrl
    ): Try<Resource, OpenError> {
        return resourceFactory.create(url)
            .mapFailure { error ->
                when (error) {
                    is ResourceFactory.Error.SchemeNotSupported ->
                        OpenError.SchemeNotSupported(error.scheme, error)
                }
            }
    }

    /* Sniff unknown assets */

    /**
     * Retrieves an asset from an unknown local file.
     */
    public suspend fun open(file: File, mediaType: MediaType? = null): Try<Asset, OpenError> =
        open(file.toUrl(), mediaType)

    /**
     * Retrieves an asset from an unknown [AbsoluteUrl].
     */
    public suspend fun open(url: AbsoluteUrl, mediaType: MediaType? = null): Try<Asset, OpenError> {
        val resource = resourceFactory.create(url)
            .getOrElse {
                return Try.failure(
                    when (it) {
                        is ResourceFactory.Error.SchemeNotSupported ->
                            OpenError.SchemeNotSupported(it.scheme)
                    }
                )
            }

        return assetSniffer.sniffOpen(resource, FormatHints(mediaType = mediaType))
            .mapFailure {
                when (it) {
                    SniffError.NotRecognized -> OpenError.FormatNotSupported()
                    is SniffError.Reading -> OpenError.Reading(it.cause)
                }
            }
    }
}
