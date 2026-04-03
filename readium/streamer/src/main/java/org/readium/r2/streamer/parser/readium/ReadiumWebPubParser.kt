/*
 * Copyright 2023 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

@file:OptIn(InternalReadiumApi::class)

package org.readium.r2.streamer.parser.readium

import android.content.Context
import org.readium.r2.shared.DelicateReadiumApi
import org.readium.r2.shared.ExperimentalReadiumApi
import org.readium.r2.shared.InternalReadiumApi
import org.readium.r2.shared.publication.Manifest
import org.readium.r2.shared.publication.Publication
import org.readium.r2.shared.publication.services.InMemoryCacheService
import org.readium.r2.shared.publication.services.PerResourcePositionsService
import org.readium.r2.shared.publication.services.WebPositionsService
import org.readium.r2.shared.publication.services.cacheServiceFactory
import org.readium.r2.shared.publication.services.content.DefaultContentService
import org.readium.r2.shared.publication.services.content.contentServiceFactory
import org.readium.r2.shared.publication.services.content.iterators.HtmlResourceContentIterator
import org.readium.r2.shared.publication.services.locatorServiceFactory
import org.readium.r2.shared.publication.services.positionsServiceFactory
import org.readium.r2.shared.publication.services.search.StringSearchService
import org.readium.r2.shared.publication.services.search.searchServiceFactory
import org.readium.r2.shared.util.AbsoluteUrl
import org.readium.r2.shared.util.DebugError
import org.readium.r2.shared.util.Try
import org.readium.r2.shared.util.Url
import org.readium.r2.shared.util.asset.Asset
import org.readium.r2.shared.util.asset.ContainerAsset
import org.readium.r2.shared.util.asset.ResourceAsset
import org.readium.r2.shared.util.data.CompositeContainer
import org.readium.r2.shared.util.data.Container
import org.readium.r2.shared.util.data.ReadError
import org.readium.r2.shared.util.data.decodeRwpm
import org.readium.r2.shared.util.data.readDecodeOrElse
import org.readium.r2.shared.util.format.FormatSpecification
import org.readium.r2.shared.util.format.Specification
import org.readium.r2.shared.util.http.HttpClient
import org.readium.r2.shared.util.http.HttpContainer
import org.readium.r2.shared.util.logging.WarningLogger
import org.readium.r2.shared.util.mediatype.MediaType
import org.readium.r2.shared.util.pdf.PdfDocumentFactory
import org.readium.r2.shared.util.resource.Resource
import org.readium.r2.shared.util.resource.SingleResourceContainer
import org.readium.r2.streamer.parser.PublicationParser
import org.readium.r2.streamer.parser.audio.AudioLocatorService
import org.readium.r2.streamer.parser.epub.EpubPositionsService
import timber.log.Timber

/**
 * Parses any Readium Web Publication package or manifest, e.g. WebPub, Audiobook, DiViNa, LCPDF...
 *
 * @param epubReflowablePositionsStrategy Strategy used to calculate the number
 * of positions in a reflowable resource of a web publication conforming to the
 * EPUB profile.
 */
@OptIn(ExperimentalReadiumApi::class)
public class ReadiumWebPubParser(
    private val context: Context? = null,
    private val httpClient: HttpClient,
    private val pdfFactory: PdfDocumentFactory<*>?,
    private val epubReflowablePositionsStrategy: EpubPositionsService.ReflowableStrategy = EpubPositionsService.ReflowableStrategy.recommended,
) : PublicationParser {

    override suspend fun parse(
        asset: Asset,
        warnings: WarningLogger?,
    ): Try<Publication.Builder, PublicationParser.ParseError> = when (asset) {
        is ResourceAsset -> parseResourceAsset(asset.resource, asset.format.specification)
        is ContainerAsset -> parseContainerAsset(asset.container, asset.format.specification)
    }

    private suspend fun parseContainerAsset(
        container: Container<Resource>,
        formatSpecification: FormatSpecification,
    ): Try<Publication.Builder, PublicationParser.ParseError> {
        if (!formatSpecification.conformsTo(Specification.Rpf)) {
            return Try.failure(PublicationParser.ParseError.FormatNotSupported())
        }

        val manifestResource = container[Url("manifest.json")!!]
            ?: return Try.failure(
                PublicationParser.ParseError.Reading(
                    ReadError.Decoding(
                        DebugError("Missing manifest.")
                    )
                )
            )

        val manifest = manifestResource
            .readDecodeOrElse(
                decode = { it.decodeRwpm() },
                recover = { return Try.failure(PublicationParser.ParseError.Reading(it)) }
            )

        checkProfileRequirements(manifest)
            ?.let { return Try.failure(it) }

        val servicesBuilder = Publication.ServicesBuilder().apply {
            cacheServiceFactory = InMemoryCacheService.createFactory(context)

            positionsServiceFactory = when {
                manifest.conformsTo(Publication.Profile.PDF) && formatSpecification.conformsTo(
                    Specification.Lcp
                ) ->
                    pdfFactory?.let { LcpdfPositionsService.create(it) }
                manifest.conformsTo(Publication.Profile.DIVINA) ->
                    PerResourcePositionsService.createFactory(MediaType("image/*")!!)
                manifest.conformsTo(Publication.Profile.EPUB) ->
                    EpubPositionsService.createFactory(epubReflowablePositionsStrategy)
                else ->
                    WebPositionsService.createFactory(httpClient)
            }

            locatorServiceFactory = when {
                manifest.conformsTo(Publication.Profile.AUDIOBOOK) ->
                    AudioLocatorService.createFactory()
                else ->
                    null
            }

            // Add content- and search-service for WebPubs with HTML contents.
            if (manifest.readingOrder.any { it.mediaType?.isHtml == true }) {
                contentServiceFactory = DefaultContentService.createFactory(
                    resourceContentIteratorFactories = listOf(
                        HtmlResourceContentIterator.Factory()
                    )
                )
                searchServiceFactory = StringSearchService.createDefaultFactory()
            }
        }

        val publicationBuilder = Publication.Builder(manifest, container, servicesBuilder)
        return Try.success(publicationBuilder)
    }

    private fun checkProfileRequirements(manifest: Manifest): PublicationParser.ParseError? =
        when {
            manifest.conformsTo(Publication.Profile.PDF) -> {
                if (manifest.readingOrder.isEmpty() ||
                    manifest.readingOrder.any { !MediaType.PDF.matches(it.mediaType) }
                ) {
                    PublicationParser.ParseError.Reading(
                        ReadError.Decoding(
                            "Publication does not conform to the PDF profile specification."
                        )
                    )
                } else {
                    null
                }
            }
            manifest.conformsTo(Publication.Profile.AUDIOBOOK) -> {
                if (manifest.readingOrder.isEmpty()) {
                    PublicationParser.ParseError.Reading(
                        ReadError.Decoding(
                            "Publication does not conform to the Audiobook profile specification."
                        )
                    )
                } else {
                    null
                }
            }
            else -> {
                null
            }
        }

    @OptIn(DelicateReadiumApi::class)
    private suspend fun parseResourceAsset(
        resource: Resource,
        formatSpecification: FormatSpecification,
    ): Try<Publication.Builder, PublicationParser.ParseError> {
        if (!formatSpecification.conformsTo(Specification.Rwpm)) {
            return Try.failure(PublicationParser.ParseError.FormatNotSupported())
        }

        val manifest = resource
            .readDecodeOrElse(
                decode = { it.decodeRwpm() },
                recover = { return Try.failure(PublicationParser.ParseError.Reading(it)) }
            )

        val baseUrl = manifest.linkWithRel("self")?.href?.resolve()
        if (baseUrl == null) {
            Timber.w("No self link found in the manifest at ${resource.sourceUrl}")
        } else {
            if (baseUrl !is AbsoluteUrl) {
                return Try.failure(
                    PublicationParser.ParseError.Reading(
                        ReadError.Decoding("Self link is not absolute.")
                    )
                )
            }
            if (!baseUrl.isHttp) {
                return Try.failure(
                    PublicationParser.ParseError.Reading(
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
                    resource
                ),
                HttpContainer(baseUrl, resources, httpClient)
            )

        return parseContainerAsset(container, FormatSpecification(Specification.Rpf))
    }
}
