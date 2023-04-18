/*
 * Copyright 2023 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.shared.publication.asset

import java.net.URL
import org.readium.r2.shared.InternalReadiumApi
import org.readium.r2.shared.fetcher.ArchiveFetcher
import org.readium.r2.shared.fetcher.Fetcher
import org.readium.r2.shared.fetcher.HttpFetcher
import org.readium.r2.shared.publication.Link
import org.readium.r2.shared.publication.Publication
import org.readium.r2.shared.util.Try
import org.readium.r2.shared.util.archive.ArchiveFactory
import org.readium.r2.shared.util.http.HttpClient
import org.readium.r2.shared.util.mediatype.MediaType

/**
 * A [PublicationAsset] built for a remote publication.
 */
data class RemoteAsset(
    val url: URL,
    override val mediaType: MediaType,
    override val fetcher: Fetcher
) : PublicationAsset {

    override val name: String =
        url.file

    @InternalReadiumApi
    class Factory(
        private val archiveFactory: ArchiveFactory,
        private val httpClient: HttpClient
    ) {
        suspend fun createAsset(
            url: URL,
            mediaType: MediaType,
        ): Try<PublicationAsset, Publication.OpeningException> =
            createFetcher(url, mediaType)
                .map { fetcher -> RemoteAsset(url, mediaType, fetcher) }

        private suspend fun createFetcher(url: URL, mediaType: MediaType): Try<Fetcher, Publication.OpeningException> {
            ArchiveFetcher.fromUrl(url, archiveFactory)
                ?.let { return Try.success(it) }

            // This enables support for both exploded containers (thanks to baseUrl) and
            // single-file publications (thanks to links).
            val httpFetcher = HttpFetcher(
                client = httpClient,
                baseUrl = url.toString(),
                links = listOf(
                    Link(href = url.toString(), type = mediaType.toString())
                )
            )

            return Try.success(httpFetcher)
        }
    }
}
