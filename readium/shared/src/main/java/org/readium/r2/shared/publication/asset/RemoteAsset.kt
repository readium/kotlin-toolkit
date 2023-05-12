/*
 * Copyright 2023 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.shared.publication.asset

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
    override val assetType: PublicationAsset.Type
) : PublicationAsset {

    override val name: String =
        url.file
}

class RemoteFetcherFactory(
    private val archiveFactory: ArchiveFactory,
    private val httpClient: HttpClient
) {

    suspend fun createFetcher(
        asset: RemoteAsset,
    ) : Try<Fetcher, Publication.OpeningException> =
        when (asset.assetType) {
            PublicationAsset.Type.Manifest ->
                createFetcherForManifest(asset.url, asset.mediaType)
            PublicationAsset.Type.PackagedPublication ->
                createFetcherForPackagedPublication(asset.url, asset.mediaType)
            PublicationAsset.Type.ExplodedPublication ->
                createFetcherForExplodedPublication(asset.url)
            PublicationAsset.Type.Content ->
                createFetcherForContentFile(asset.url, asset.mediaType)
        }

    private suspend fun createFetcherForPackagedPublication(
        url: Url,
        mediaType: MediaType,
    ) : Try<Fetcher, Publication.OpeningException> {
        val resource = HttpFetcher.HttpResource(
            httpClient,
            Link(href = url.toString(), type = mediaType.toString()),
            url.toString()
        )
        return archiveFactory.open(resource, password = null)
            .mapFailure { Publication.OpeningException.ParsingFailed(it) }
            .map { ArchiveFetcher(it) }
    }

    private fun createFetcherForExplodedPublication(
        url: Url
    ) : Try<Fetcher, Publication.OpeningException> {
        val fetcher =
            HttpFetcher(
                client = httpClient,
                baseUrl = url.toString(),
                links = emptyList()
            )
        return Try.success(fetcher)
    }

    private fun createFetcherForManifest(
        url: Url,
        mediaType: MediaType
    ) : Try<Fetcher, Publication.OpeningException> {
        val fetcher = HttpFetcher(
            client = httpClient,
            baseUrl = url.toString(),
            links = listOf(
                Link(href = url.toString(), type = mediaType.toString())
            )
        )

        return Try.success(fetcher)
    }

    private fun createFetcherForContentFile(
        url: Url,
        mediaType: MediaType
    ) : Try<Fetcher, Publication.OpeningException> {
        val fetcher = HttpFetcher(
            client = httpClient,
            baseUrl = null,
            links = listOf(
                Link(href = url.toString(), type = mediaType.toString())
            )
        )

        return Try.success(fetcher)
    }
}