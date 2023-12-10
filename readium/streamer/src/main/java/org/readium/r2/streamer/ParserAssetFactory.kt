/*
 * Copyright 2023 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.streamer

import org.readium.r2.shared.extensions.addPrefix
import org.readium.r2.shared.util.AbsoluteUrl
import org.readium.r2.shared.util.DebugError
import org.readium.r2.shared.util.Error
import org.readium.r2.shared.util.Try
import org.readium.r2.shared.util.Url
import org.readium.r2.shared.util.asset.Asset
import org.readium.r2.shared.util.asset.ContainerAsset
import org.readium.r2.shared.util.asset.ResourceAsset
import org.readium.r2.shared.util.data.CompositeContainer
import org.readium.r2.shared.util.data.ReadError
import org.readium.r2.shared.util.data.decodeRwpm
import org.readium.r2.shared.util.data.readDecodeOrElse
import org.readium.r2.shared.util.format.Format
import org.readium.r2.shared.util.format.FormatRegistry
import org.readium.r2.shared.util.http.HttpClient
import org.readium.r2.shared.util.http.HttpContainer
import org.readium.r2.shared.util.resource.SingleResourceContainer
import org.readium.r2.streamer.parser.PublicationParser
import timber.log.Timber

internal class ParserAssetFactory(
    private val httpClient: HttpClient,
    private val formatRegistry: FormatRegistry
) {

    sealed class CreateError(
        override val message: String,
        override val cause: Error?
    ) : Error {

        class Reading(
            override val cause: ReadError
        ) : CreateError("An error occurred while trying to read asset.", cause)

        class FormatNotSupported(
            override val cause: Error?
        ) : CreateError("Asset is not supported.", cause)
    }

    suspend fun createParserAsset(
        asset: Asset
    ): Try<PublicationParser.Asset, CreateError> {
        return when (asset) {
            is ContainerAsset ->
                createParserAssetForContainer(asset)
            is ResourceAsset ->
                createParserAssetForResource(asset)
        }
    }

    private fun createParserAssetForContainer(
        asset: ContainerAsset
    ): Try<PublicationParser.Asset, CreateError> =
        Try.success(
            PublicationParser.Asset(
                format = asset.format,
                container = asset.container
            )
        )

    private suspend fun createParserAssetForResource(
        asset: ResourceAsset
    ): Try<PublicationParser.Asset, CreateError> =
        if (asset.format.conformsTo(Format.RWPM)) {
            createParserAssetForManifest(asset)
        } else {
            createParserAssetForContent(asset)
        }

    private suspend fun createParserAssetForManifest(
        asset: ResourceAsset
    ): Try<PublicationParser.Asset, CreateError> {
        val manifest = asset.resource
            .readDecodeOrElse(
                decode = { it.decodeRwpm() },
                recover = { return Try.failure(CreateError.Reading(it)) }
            )

        val baseUrl = manifest.linkWithRel("self")?.href?.resolve()
        if (baseUrl == null) {
            Timber.w("No self link found in the manifest at ${asset.resource.sourceUrl}")
        } else {
            if (baseUrl !is AbsoluteUrl) {
                return Try.failure(
                    CreateError.Reading(
                        ReadError.Decoding("Self link is not absolute.")
                    )
                )
            }
            if (!baseUrl.isHttp) {
                return Try.failure(
                    CreateError.FormatNotSupported(
                        DebugError("Self link doesn't use the HTTP(S) scheme.")
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
                HttpContainer(baseUrl, resources, httpClient)
            )

        return Try.success(
            PublicationParser.Asset(
                format = Format.RPF,
                container = container
            )
        )
    }

    private fun createParserAssetForContent(
        asset: ResourceAsset
    ): Try<PublicationParser.Asset, CreateError> {
        // Historically, the reading order of a standalone file contained a single link with the
        // HREF "/$assetName". This was fragile if the asset named changed, or was different on
        // other devices. To avoid this, we now use a single link with the HREF
        // "publication.extension".
        val extension = formatRegistry[asset.format]
            ?.fileExtension?.value?.addPrefix(".")
            ?: ""
        val container = SingleResourceContainer(
            Url("publication$extension")!!,
            asset.resource
        )

        return Try.success(
            PublicationParser.Asset(
                format = asset.format,
                container = container
            )
        )
    }
}
