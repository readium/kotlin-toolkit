/*
 * Copyright 2023 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.streamer.parser.readium

import android.content.Context
import org.readium.r2.shared.publication.Publication
import org.readium.r2.shared.publication.services.InMemoryCacheService
import org.readium.r2.shared.publication.services.PerResourcePositionsService
import org.readium.r2.shared.publication.services.WebPositionsService
import org.readium.r2.shared.publication.services.cacheServiceFactory
import org.readium.r2.shared.publication.services.locatorServiceFactory
import org.readium.r2.shared.publication.services.positionsServiceFactory
import org.readium.r2.shared.util.AbsoluteUrl
import org.readium.r2.shared.util.DebugError
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
import org.readium.r2.shared.util.getOrElse
import org.readium.r2.shared.util.http.HttpClient
import org.readium.r2.shared.util.http.HttpContainer
import org.readium.r2.shared.util.logging.WarningLogger
import org.readium.r2.shared.util.mediatype.MediaType
import org.readium.r2.shared.util.pdf.PdfDocumentFactory
import org.readium.r2.shared.util.resource.SingleResourceContainer
import org.readium.r2.streamer.parser.PublicationParser
import org.readium.r2.streamer.parser.audio.AudioLocatorService
import timber.log.Timber

/**
 * Parses any Readium Web Publication package or manifest, e.g. WebPub, Audiobook, DiViNa, LCPDF...
 */
public class ReadiumWebPubParser(
    private val context: Context? = null,
    private val httpClient: HttpClient,
    private val pdfFactory: PdfDocumentFactory<*>?
) : PublicationParser {

    override suspend fun parse(
        asset: Asset,
        warnings: WarningLogger?
    ): Try<Publication.Builder, PublicationParser.Error> {
        if (asset is ResourceAsset && asset.format.conformsTo(Format.RWPM)) {
            val packageAsset = createPackage(asset)
                .getOrElse { return Try.failure(it) }
            return parse(packageAsset, warnings)
        }

        if (asset !is ContainerAsset || !asset.format.conformsTo(Format.RPF)) {
            return Try.failure(PublicationParser.Error.FormatNotSupported())
        }

        val manifestResource = asset.container[Url("manifest.json")!!]
            ?: return Try.failure(
                PublicationParser.Error.Reading(
                    ReadError.Decoding(
                        DebugError("Missing manifest.")
                    )
                )
            )

        val manifest = manifestResource
            .readDecodeOrElse(
                decode = { it.decodeRwpm() },
                recover = { return Try.failure(PublicationParser.Error.Reading(it)) }
            )

        // Checks the requirements from the LCPDF specification.
        // https://readium.org/lcp-specs/notes/lcp-for-pdf.html
        val readingOrder = manifest.readingOrder
        if (asset.format.conformsTo(Format.RPF_PDF_LCP) &&
            (readingOrder.isEmpty() || !readingOrder.all { MediaType.PDF.matches(it.mediaType) })
        ) {
            return Try.failure(
                PublicationParser.Error.Reading(
                    ReadError.Decoding("Invalid LCP Protected PDF.")
                )
            )
        }

        val servicesBuilder = Publication.ServicesBuilder().apply {
            cacheServiceFactory = InMemoryCacheService.createFactory(context)

            positionsServiceFactory = when (asset.format) {
                Format.RPF_PDF_LCP ->
                    pdfFactory?.let { LcpdfPositionsService.create(it) }
                Format.RPF_IMAGE ->
                    PerResourcePositionsService.createFactory(MediaType("image/*")!!)
                else ->
                    WebPositionsService.createFactory(httpClient)
            }

            locatorServiceFactory = when {
                asset.format.conformsTo(Format.RPF_AUDIO) ->
                    AudioLocatorService.createFactory()
                else ->
                    null
            }
        }

        val publicationBuilder = Publication.Builder(manifest, asset.container, servicesBuilder)
        return Try.success(publicationBuilder)
    }

    private suspend fun createPackage(asset: ResourceAsset): Try<ContainerAsset, PublicationParser.Error> {
        val manifest = asset.resource
            .readDecodeOrElse(
                decode = { it.decodeRwpm() },
                recover = { return Try.failure(PublicationParser.Error.Reading(it)) }
            )

        val baseUrl = manifest.linkWithRel("self")?.href?.resolve()
        if (baseUrl == null) {
            Timber.w("No self link found in the manifest at ${asset.resource.sourceUrl}")
        } else {
            if (baseUrl !is AbsoluteUrl) {
                return Try.failure(
                    PublicationParser.Error.Reading(
                        ReadError.Decoding("Self link is not absolute.")
                    )
                )
            }
            if (!baseUrl.isHttp) {
                return Try.failure(
                    PublicationParser.Error.Reading(
                        ReadError.Decoding("Self link doesn't use the HTTP(S) scheme.")
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
            ContainerAsset(
                format = Format.RPF,
                container = container
            )
        )
    }
}
