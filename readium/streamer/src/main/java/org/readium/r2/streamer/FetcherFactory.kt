package org.readium.r2.streamer

import java.io.File
import java.nio.charset.Charset
import org.json.JSONObject
import org.readium.r2.shared.asset.Asset
import org.readium.r2.shared.fetcher.ContainerFetcher
import org.readium.r2.shared.fetcher.Fetcher
import org.readium.r2.shared.fetcher.RoutingFetcher
import org.readium.r2.shared.fetcher.SingleResourceFetcher
import org.readium.r2.shared.fetcher.withLink
import org.readium.r2.shared.publication.Link
import org.readium.r2.shared.publication.Manifest
import org.readium.r2.shared.publication.Publication
import org.readium.r2.shared.resource.Container
import org.readium.r2.shared.resource.Resource
import org.readium.r2.shared.util.Try
import org.readium.r2.shared.util.getOrElse
import org.readium.r2.shared.util.http.HttpClient
import org.readium.r2.shared.util.http.HttpFetcher
import org.readium.r2.shared.util.mediatype.MediaType
import org.readium.r2.shared.util.mediatype.MediaTypeRetriever

class FetcherFactory(
    private val httpClient: HttpClient,
    private val mediaTypeRetriever: MediaTypeRetriever,
) {

    suspend fun createFetcher(
        asset: Asset,
        mediaType: MediaType,
    ): Try<Fetcher, Publication.OpeningException> {
        return when (asset) {
            is Asset.Container ->
                Try.success(createFetcherForContainer(asset.container))
            is Asset.Resource ->
                createFetcherForResource(asset.resource, mediaType, asset.name)
        }
    }

    private fun createFetcherForContainer(
        container: Container
    ): Fetcher {
        return ContainerFetcher(container, mediaTypeRetriever)
    }

    private suspend fun createFetcherForResource(
        resource: Resource,
        mediaType: MediaType,
        assetName: String
    ): Try<Fetcher, Publication.OpeningException> =
        if (mediaType.isRwpm) {
            createFetcherForManifest(resource)
        } else {
            createFetcherForContent(resource, mediaType, assetName)
        }

    private suspend fun createFetcherForManifest(
        resource: Resource
    ): Try<Fetcher, Publication.OpeningException> {
        val manifest = resource.readAsRwpm(packaged = false)
            .mapFailure { Publication.OpeningException.ParsingFailed(it) }
            .getOrElse { return Try.failure(it) }

        val baseUrl =
            manifest.linkWithRel("self")?.let { File(it.href).parent }

        val link = Link(
            href = "/manifest.json",
            type = MediaType.READIUM_WEBPUB_MANIFEST.toString()
        )

        val fetcher =
            RoutingFetcher(
                local = SingleResourceFetcher(resource.withLink(link)),
                remote = HttpFetcher(httpClient, baseUrl)
            )

        return Try.success(fetcher)
    }

    private suspend fun createFetcherForContent(
        resource: Resource,
        mediaType: MediaType,
        assetName: String
    ): Try<Fetcher, Publication.OpeningException> {
        val link = Link(href = "/$assetName", type = mediaType.toString())

        return Try.success(
            SingleResourceFetcher(resource.withLink(link))
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