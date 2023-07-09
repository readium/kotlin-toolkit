/*
 * Copyright 2023 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.streamer

import java.io.File
import java.nio.charset.Charset
import org.json.JSONObject
import org.readium.r2.shared.asset.Asset
import org.readium.r2.shared.error.*
import org.readium.r2.shared.fetcher.ContainerFetcher
import org.readium.r2.shared.fetcher.ResourceFetcher
import org.readium.r2.shared.fetcher.RoutingFetcher
import org.readium.r2.shared.publication.Link
import org.readium.r2.shared.publication.Manifest
import org.readium.r2.shared.publication.Publication
import org.readium.r2.shared.resource.Container
import org.readium.r2.shared.resource.Resource
import org.readium.r2.shared.util.http.HttpClient
import org.readium.r2.shared.util.http.HttpFetcher
import org.readium.r2.shared.util.mediatype.MediaType
import org.readium.r2.shared.util.mediatype.MediaTypeRetriever
import org.readium.r2.streamer.parser.PublicationParser

internal class ParserAssetFactory(
    private val httpClient: HttpClient,
    private val mediaTypeRetriever: MediaTypeRetriever,
) {

    suspend fun createParserAsset(
        asset: Asset,
    ): Try<PublicationParser.Asset, Publication.OpeningException> {
        return when (asset) {
            is Asset.Container ->
                Try.success(createFetcherForContainer(asset.container, asset.mediaType, asset.name))
            is Asset.Resource ->
                createFetcherForResource(asset.resource, asset.mediaType, asset.name)
        }
    }

    private fun createFetcherForContainer(
        container: Container,
        mediaType: MediaType,
        assetName: String
    ): PublicationParser.Asset {
        val fetcher = ContainerFetcher(container, mediaTypeRetriever)
        return PublicationParser.Asset(assetName, mediaType, fetcher)
    }

    private suspend fun createFetcherForResource(
        resource: Resource,
        mediaType: MediaType,
        assetName: String
    ): Try<PublicationParser.Asset, Publication.OpeningException> =
        if (mediaType.isRwpm) {
            createFetcherForManifest(resource, assetName)
        } else {
            createFetcherForContent(resource, mediaType, assetName)
        }

    private suspend fun createFetcherForManifest(
        resource: Resource,
        assetName: String
    ): Try<PublicationParser.Asset, Publication.OpeningException> {
        val manifest = resource.readAsRwpm(packaged = false)
            .mapFailure { Publication.OpeningException.ParsingFailed(ThrowableError(it)) }
            .getOrElse { return Try.failure(it) }

        val baseUrl =
            manifest.linkWithRel("self")?.let { File(it.href).parent }
                ?: return Try.failure(
                    Publication.OpeningException.ParsingFailed(
                        SimpleError("No self link in the manifest.")
                    )
                )

        if (!baseUrl.startsWith("http")) {
            return Try.failure(
                Publication.OpeningException.UnsupportedAsset("Self link doesn't use the HTTP(S) scheme.")
            )
        }

        val link = Link(
            href = "/manifest.json",
            type = MediaType.READIUM_WEBPUB_MANIFEST.toString()
        )

        val fetcher =
            RoutingFetcher(
                local = ResourceFetcher(link, resource),
                remote = HttpFetcher(httpClient, baseUrl)
            )

        return Try.success(
            PublicationParser.Asset(assetName, MediaType.READIUM_WEBPUB, fetcher)
        )
    }

    private fun createFetcherForContent(
        resource: Resource,
        mediaType: MediaType,
        assetName: String
    ): Try<PublicationParser.Asset, Publication.OpeningException> {
        val link = Link(href = "/$assetName", type = mediaType.toString())
        val fetcher = ResourceFetcher(link, resource)

        return Try.success(
            PublicationParser.Asset(assetName, mediaType, fetcher)
        )
    }

    private suspend fun Resource.readAsRwpm(packaged: Boolean): Try<Manifest, Exception> =
        try {
            val bytes = read().getOrThrow()
            val string = String(bytes, Charset.defaultCharset())
            val json = JSONObject(string)
            val manifest = Manifest.fromJSON(json, packaged = packaged)
                ?: throw Exception("Failed to parse the RWPM Manifest")
            Try.success(manifest)
        } catch (e: Exception) {
            Try.failure(e)
        }
}
