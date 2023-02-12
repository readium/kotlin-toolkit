/*
 * Copyright 2023 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.shared.publication.asset

import java.net.URL
import org.readium.r2.shared.extensions.extension
import org.readium.r2.shared.fetcher.ArchiveFetcher
import org.readium.r2.shared.fetcher.Fetcher
import org.readium.r2.shared.fetcher.HttpFetcher
import org.readium.r2.shared.publication.Link
import org.readium.r2.shared.publication.Publication
import org.readium.r2.shared.util.Try
import org.readium.r2.shared.util.mediatype.MediaType

/**
 * Represents a publication accessible remotely.
 */
class RemoteAsset(
    val url: URL,
    private val knownMediaType: MediaType?,
    private val mediaTypeHint: String?
) : PublicationAsset {

    /**
     * Creates a [RemoteAsset] from a [URL] and an optional media type, when known.
     */
    constructor(url: URL, mediaType: MediaType? = null) :
        this(url, knownMediaType = mediaType, mediaTypeHint = null)

    /**
     * Creates a [RemoteAsset] from a [URL] and an optional media type hint.
     *
     * Providing a media type hint will improve performances when sniffing the media type.
     */
    constructor(url: URL, mediaTypeHint: String?) :
        this(url, knownMediaType = null, mediaTypeHint = mediaTypeHint)

    override suspend fun mediaType(): MediaType {
        if (!::_mediaType.isInitialized) {
            val bytes = { url.openConnection().getInputStream().use { it.readBytes() } }
            _mediaType = knownMediaType
                ?: MediaType.ofBytes(bytes, mediaType = mediaTypeHint, fileExtension = url.extension)
                ?: MediaType.BINARY
        }

        return _mediaType
    }

    private lateinit var _mediaType: MediaType

    override val name: String =
        url.file

    override suspend fun createFetcher(
        dependencies: PublicationAsset.Dependencies,
        credentials: String?
    ): Try<Fetcher, Publication.OpeningException> {
        ArchiveFetcher.fromUrl(url, dependencies.archiveFactory)
            ?.let { return Try.success(it) }

        val httpFetcher = HttpFetcher(
            client = dependencies.httpClient,
            baseUrl = url.toString(),
            links = listOf(
                Link(href = url.toString(), type = mediaType().toString())
            )
        )

        return Try.success(httpFetcher)
    }

    override fun toString(): String = "RemoteAsset($url)"
}
