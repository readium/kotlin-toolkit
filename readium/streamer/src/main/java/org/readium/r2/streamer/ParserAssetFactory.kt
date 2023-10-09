/*
 * Copyright 2023 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.streamer

import java.nio.charset.Charset
import org.json.JSONObject
import org.readium.r2.shared.extensions.addPrefix
import org.readium.r2.shared.publication.Manifest
import org.readium.r2.shared.publication.Publication
import org.readium.r2.shared.util.AbsoluteUrl
import org.readium.r2.shared.util.MessageError
import org.readium.r2.shared.util.ThrowableError
import org.readium.r2.shared.util.Try
import org.readium.r2.shared.util.Url
import org.readium.r2.shared.util.asset.Asset
import org.readium.r2.shared.util.getOrElse
import org.readium.r2.shared.util.getOrThrow
import org.readium.r2.shared.util.http.HttpClient
import org.readium.r2.shared.util.http.HttpContainer
import org.readium.r2.shared.util.mediatype.FormatRegistry
import org.readium.r2.shared.util.mediatype.MediaType
import org.readium.r2.shared.util.mediatype.MediaTypeRetriever
import org.readium.r2.shared.util.resource.Resource
import org.readium.r2.shared.util.resource.ResourceContainer
import org.readium.r2.shared.util.resource.RoutingContainer
import org.readium.r2.streamer.parser.PublicationParser

internal class ParserAssetFactory(
    private val httpClient: HttpClient,
    private val mediaTypeRetriever: MediaTypeRetriever,
    private val formatRegistry: FormatRegistry
) {

    suspend fun createParserAsset(
        asset: org.readium.r2.shared.util.asset.Asset
    ): Try<PublicationParser.Asset, Publication.OpenError> {
        return when (asset) {
            is org.readium.r2.shared.util.asset.Asset.Container ->
                createParserAssetForContainer(asset)
            is org.readium.r2.shared.util.asset.Asset.Resource ->
                createParserAssetForResource(asset)
        }
    }

    private fun createParserAssetForContainer(
        asset: org.readium.r2.shared.util.asset.Asset.Container
    ): Try<PublicationParser.Asset, Publication.OpenError> =
        Try.success(
            PublicationParser.Asset(
                mediaType = asset.mediaType,
                container = asset.container
            )
        )

    private suspend fun createParserAssetForResource(
        asset: org.readium.r2.shared.util.asset.Asset.Resource
    ): Try<PublicationParser.Asset, Publication.OpenError> =
        if (asset.mediaType.isRwpm) {
            createParserAssetForManifest(asset)
        } else {
            createParserAssetForContent(asset)
        }

    private suspend fun createParserAssetForManifest(
        asset: org.readium.r2.shared.util.asset.Asset.Resource
    ): Try<PublicationParser.Asset, Publication.OpenError> {
        val manifest = asset.resource.readAsRwpm()
            .mapFailure { Publication.OpenError.InvalidAsset(ThrowableError(it)) }
            .getOrElse { return Try.failure(it) }

        val baseUrl =
            manifest.linkWithRel("self")?.href?.resolve()
                ?: return Try.failure(
                    Publication.OpenError.InvalidAsset(
                        MessageError("No self link in the manifest.")
                    )
                )

        if (baseUrl !is AbsoluteUrl) {
            return Try.failure(
                Publication.OpenError.InvalidAsset(
                    MessageError("Self link is not absolute.")
                )
            )
        }

        if (!baseUrl.isHttp) {
            return Try.failure(
                Publication.OpenError.UnsupportedAsset(
                    "Self link doesn't use the HTTP(S) scheme."
                )
            )
        }

        val container =
            RoutingContainer(
                local = ResourceContainer(
                    url = Url("manifest.json")!!,
                    asset.resource
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
        asset: org.readium.r2.shared.util.asset.Asset.Resource
    ): Try<PublicationParser.Asset, Publication.OpenError> {
        // Historically, the reading order of a standalone file contained a single link with the
        // HREF "/$assetName". This was fragile if the asset named changed, or was different on
        // other devices. To avoid this, we now use a single link with the HREF
        // "publication.extension".
        val extension = formatRegistry.fileExtension(asset.mediaType)?.addPrefix(".") ?: ""
        val container = ResourceContainer(
            Url("publication$extension")!!,
            asset.resource
        )

        return Try.success(
            PublicationParser.Asset(
                mediaType = asset.mediaType,
                container = container
            )
        )
    }

    private suspend fun Resource.readAsRwpm(): Try<Manifest, Exception> =
        try {
            val bytes = read().getOrThrow()
            val string = String(bytes, Charset.defaultCharset())
            val json = JSONObject(string)
            val manifest = Manifest.fromJSON(
                json,
                mediaTypeRetriever = mediaTypeRetriever
            )
                ?: throw Exception("Failed to parse the RWPM Manifest")
            Try.success(manifest)
        } catch (e: Exception) {
            Try.failure(e)
        }
}
