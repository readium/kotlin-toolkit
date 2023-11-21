/*
 * Copyright 2023 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.streamer

import org.readium.r2.shared.extensions.addPrefix
import org.readium.r2.shared.util.AbsoluteUrl
import org.readium.r2.shared.util.MessageError
import org.readium.r2.shared.util.Try
import org.readium.r2.shared.util.Url
import org.readium.r2.shared.util.asset.Asset
import org.readium.r2.shared.util.data.CompositeContainer
import org.readium.r2.shared.util.data.DecoderError
import org.readium.r2.shared.util.data.ReadError
import org.readium.r2.shared.util.data.readAsRwpm
import org.readium.r2.shared.util.getOrElse
import org.readium.r2.shared.util.http.HttpClient
import org.readium.r2.shared.util.http.HttpContainer
import org.readium.r2.shared.util.mediatype.FormatRegistry
import org.readium.r2.shared.util.mediatype.MediaType
import org.readium.r2.shared.util.mediatype.MediaTypeRetriever
import org.readium.r2.shared.util.resource.SingleResourceContainer
import org.readium.r2.streamer.parser.PublicationParser
import timber.log.Timber

internal class ParserAssetFactory(
    private val httpClient: HttpClient,
    private val formatRegistry: FormatRegistry,
    private val mediaTypeRetriever: MediaTypeRetriever
) {

    sealed class Error(
        override val message: String,
        override val cause: org.readium.r2.shared.util.Error?
    ) : org.readium.r2.shared.util.Error {

        class ReadError(
            override val cause: org.readium.r2.shared.util.data.ReadError
        ) : Error("An error occurred while trying to read asset.", cause)

        class UnsupportedAsset(
            override val cause: org.readium.r2.shared.util.Error?
        ) : Error("Asset is not supported.", cause)
    }

    suspend fun createParserAsset(
        asset: Asset
    ): Try<PublicationParser.Asset, Error> {
        return when (asset) {
            is Asset.Container ->
                createParserAssetForContainer(asset)
            is Asset.Resource ->
                createParserAssetForResource(asset)
        }
    }

    private fun createParserAssetForContainer(
        asset: Asset.Container
    ): Try<PublicationParser.Asset, Error> =
        Try.success(
            PublicationParser.Asset(
                mediaType = asset.mediaType,
                container = asset.container
            )
        )

    private suspend fun createParserAssetForResource(
        asset: Asset.Resource
    ): Try<PublicationParser.Asset, Error> =
        if (asset.mediaType.isRwpm) {
            createParserAssetForManifest(asset)
        } else {
            createParserAssetForContent(asset)
        }

    private suspend fun createParserAssetForManifest(
        asset: Asset.Resource
    ): Try<PublicationParser.Asset, Error> {
        val manifest = asset.resource.readAsRwpm()
            .mapFailure {
                when (it) {
                    is DecoderError.Decoding -> ReadError.Decoding(it.cause)
                    is DecoderError.Read -> it.cause
                }
            }
            .getOrElse { return Try.failure(Error.ReadError(it)) }

        val baseUrl = manifest.linkWithRel("self")?.href?.resolve()
        if (baseUrl == null) {
            Timber.w("No self link found in the manifest at ${asset.resource.source}")
        } else {
            if (baseUrl !is AbsoluteUrl) {
                return Try.failure(
                    Error.ReadError(
                        ReadError.Decoding("Self link is not absolute.")
                    )
                )
            }
            if (!baseUrl.isHttp) {
                return Try.failure(
                    Error.UnsupportedAsset(
                        MessageError("Self link doesn't use the HTTP(S) scheme.")
                    )
                )
            }
        }

        val resources = (manifest.readingOrder + manifest.resources)
            .map { it.url() }
            .toSet()

        val container =
            CompositeContainer(
                SingleResourceContainer(
                    Url("manifest.json")!!,
                    asset.resource
                ),
                HttpContainer(baseUrl, resources, httpClient, mediaTypeRetriever)
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
    ): Try<PublicationParser.Asset, Error> {
        // Historically, the reading order of a standalone file contained a single link with the
        // HREF "/$assetName". This was fragile if the asset named changed, or was different on
        // other devices. To avoid this, we now use a single link with the HREF
        // "publication.extension".
        val extension = formatRegistry.fileExtension(asset.mediaType)?.addPrefix(".") ?: ""
        val container = SingleResourceContainer(
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
}
