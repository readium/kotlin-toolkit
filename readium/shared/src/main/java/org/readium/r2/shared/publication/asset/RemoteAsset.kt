/*
 * Copyright 2023 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.shared.publication.asset

import org.readium.r2.shared.InternalReadiumApi
import org.readium.r2.shared.fetcher.ArchiveFetcher
import org.readium.r2.shared.fetcher.Fetcher
import org.readium.r2.shared.fetcher.HttpFetcher
import org.readium.r2.shared.publication.Link
import org.readium.r2.shared.publication.Publication
import org.readium.r2.shared.util.Try
import org.readium.r2.shared.util.Url
import org.readium.r2.shared.util.archive.ArchiveFactory
import org.readium.r2.shared.util.http.HttpClient
import org.readium.r2.shared.util.mediatype.MediaType

/**
 * A [PublicationAsset] built for a remote publication.
 */
data class RemoteAsset(
    val url: Url,
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

        suspend fun createAssetForPackagedPublication(
            url: Url,
            mediaType: MediaType
        ) : Try<PublicationAsset, Publication.OpeningException> {

            return ArchiveFetcher.create(url, archiveFactory)
                .map { fetcher ->
                    RemoteAsset(
                        url = url,
                        mediaType = mediaType,
                        fetcher = fetcher
                    )
                }.mapFailure { error ->
                    when (error) {
                        else -> Publication.OpeningException.Unavailable()
                    }
                }
        }

        suspend fun createAssetForExplodedPublication(
            url: Url,
            mediaType: MediaType
        ) : Try<PublicationAsset, Publication.OpeningException> {
            val fetcher =
                HttpFetcher(
                    client = httpClient,
                    baseUrl = url.toString(),
                    links = emptyList()
                )

            val asset =
                RemoteAsset(
                    url = url,
                    mediaType = mediaType,
                    fetcher = fetcher
                )

            return Try.success(asset)
        }

        suspend fun createAssetForWebpubManifest(
            url: Url,
            mediaType: MediaType
        ) : Try<PublicationAsset, Publication.OpeningException> {
            val fetcher = HttpFetcher(
                client = httpClient,
                baseUrl = url.toString(),
                links = listOf(
                    Link(href = url.toString(), type = mediaType.toString())
                )
            )

            val asset =
                RemoteAsset(
                    url = url,
                    mediaType = mediaType,
                    fetcher = fetcher
                )

            return Try.success(asset)
        }

        suspend fun createAssetForPublicationFile(
            url: Url,
            mediaType: MediaType
        ) : Try<PublicationAsset, Publication.OpeningException> {
            val fetcher = HttpFetcher(
                client = httpClient,
                baseUrl = null,
                links = listOf(
                    Link(href = url.toString(), type = mediaType.toString())
                )
            )

            val asset =
                RemoteAsset(
                    url = url,
                    mediaType = mediaType,
                    fetcher = fetcher
                )

            return Try.success(asset)
        }
    }
}
