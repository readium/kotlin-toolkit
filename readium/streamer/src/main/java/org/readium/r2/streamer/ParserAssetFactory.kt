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
import org.readium.r2.shared.error.MessageError
import org.readium.r2.shared.error.ThrowableError
import org.readium.r2.shared.error.Try
import org.readium.r2.shared.error.getOrElse
import org.readium.r2.shared.error.getOrThrow
import org.readium.r2.shared.publication.Manifest
import org.readium.r2.shared.publication.Publication
import org.readium.r2.shared.resource.Resource
import org.readium.r2.shared.resource.ResourceContainer
import org.readium.r2.shared.resource.RoutingContainer
import org.readium.r2.shared.resource.StringResource
import org.readium.r2.shared.util.http.HttpClient
import org.readium.r2.shared.util.http.HttpContainer
import org.readium.r2.shared.util.mediatype.MediaType
import org.readium.r2.shared.util.mediatype.MediaTypeRetriever
import org.readium.r2.streamer.parser.PublicationParser

internal class ParserAssetFactory(
    private val httpClient: HttpClient,
    private val mediaTypeRetriever: MediaTypeRetriever
) {

    suspend fun createParserAsset(
        asset: Asset
    ): Try<PublicationParser.Asset, Publication.OpeningException> {
        return when (asset) {
            is Asset.Container ->
                createParserAssetForContainer(asset)
            is Asset.Resource ->
                createParserAssetForResource(asset)
        }
    }

    private fun createParserAssetForContainer(
        asset: Asset.Container
    ): Try<PublicationParser.Asset, Publication.OpeningException> =
        Try.success(
            PublicationParser.Asset(
                mediaType = asset.mediaType,
                container = asset.container
            )
        )

    private suspend fun createParserAssetForResource(
        asset: Asset.Resource
    ): Try<PublicationParser.Asset, Publication.OpeningException> =
        if (asset.mediaType.isRwpm) {
            createParserAssetForManifest(asset)
        } else {
            createParserAssetForContent(asset)
        }

    private suspend fun createParserAssetForManifest(
        asset: Asset.Resource
    ): Try<PublicationParser.Asset, Publication.OpeningException> {
        val manifest = asset.resource.readAsRwpm(packaged = false)
            .mapFailure { Publication.OpeningException.ParsingFailed(ThrowableError(it)) }
            .getOrElse { return Try.failure(it) }

        val baseUrl =
            manifest.linkWithRel("self")?.let { File(it.href).parent }
                ?: return Try.failure(
                    Publication.OpeningException.ParsingFailed(
                        MessageError("No self link in the manifest.")
                    )
                )

        if (!baseUrl.startsWith("http")) {
            return Try.failure(
                Publication.OpeningException.UnsupportedAsset(
                    "Self link doesn't use the HTTP(S) scheme."
                )
            )
        }

        val container =
            RoutingContainer(
                local = ResourceContainer(
                    path = "/manifest.json",
                    resource = StringResource(manifest.toJSON().toString(), asset.mediaType)
                ),
                remote = HttpContainer(httpClient, baseUrl)
            )

        return Try.success(
            PublicationParser.Asset(
                mediaType = MediaType.READIUM_WEBPUB,
                container = container
            )
        )
    }

    private fun createParserAssetForContent(
        asset: Asset.Resource
    ): Try<PublicationParser.Asset, Publication.OpeningException> {
        // Historically, the reading order of a standalone file contained a single link with the
        // HREF "/$assetName". This was fragile if the asset named changed, or was different on
        // other devices. To avoid this, we now use a single link with the HREF ".".
        val container = ResourceContainer(".", asset.resource)

        return Try.success(
            PublicationParser.Asset(
                mediaType = asset.mediaType,
                container = container
            )
        )
    }

    private suspend fun Resource.readAsRwpm(packaged: Boolean): Try<Manifest, Exception> =
        try {
            val bytes = read().getOrThrow()
            val string = String(bytes, Charset.defaultCharset())
            val json = JSONObject(string)
            val manifest = Manifest.fromJSON(
                json,
                packaged = packaged,
                mediaTypeRetriever = mediaTypeRetriever
            )
                ?: throw Exception("Failed to parse the RWPM Manifest")
            Try.success(manifest)
        } catch (e: Exception) {
            Try.failure(e)
        }
}
