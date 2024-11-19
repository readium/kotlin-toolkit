/*
 * Copyright 2023 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.streamer.parser.image

import org.readium.r2.shared.publication.Link
import org.readium.r2.shared.publication.LocalizedString
import org.readium.r2.shared.publication.Manifest
import org.readium.r2.shared.publication.Metadata
import org.readium.r2.shared.publication.Publication
import org.readium.r2.shared.publication.services.PerResourcePositionsService
import org.readium.r2.shared.util.DebugError
import org.readium.r2.shared.util.Try
import org.readium.r2.shared.util.Url
import org.readium.r2.shared.util.asset.Asset
import org.readium.r2.shared.util.asset.AssetRetriever
import org.readium.r2.shared.util.asset.ContainerAsset
import org.readium.r2.shared.util.asset.ResourceAsset
import org.readium.r2.shared.util.data.Container
import org.readium.r2.shared.util.data.ReadError
import org.readium.r2.shared.util.format.Format
import org.readium.r2.shared.util.format.Specification
import org.readium.r2.shared.util.getEquivalent
import org.readium.r2.shared.util.getOrElse
import org.readium.r2.shared.util.logging.WarningLogger
import org.readium.r2.shared.util.mediatype.MediaType
import org.readium.r2.shared.util.resource.Resource
import org.readium.r2.streamer.extensions.guessTitle
import org.readium.r2.streamer.extensions.isHiddenOrThumbs
import org.readium.r2.streamer.extensions.sniffContainerEntries
import org.readium.r2.streamer.extensions.toContainer
import org.readium.r2.streamer.parser.PublicationParser

/**
 * Parses an image–based Publication from an unstructured archive format containing bitmap files,
 * such as CBZ or a simple ZIP.
 *
 * It can also work for a standalone bitmap file.
 */
public class ImageParser(
    private val assetRetriever: AssetRetriever,
) : PublicationParser {

    override suspend fun parse(
        asset: Asset,
        warnings: WarningLogger?,
    ): Try<Publication.Builder, PublicationParser.ParseError> =
        when (asset) {
            is ResourceAsset -> parseResourceAsset(asset)
            is ContainerAsset -> parseContainerAsset(asset)
        }

    private fun parseResourceAsset(
        asset: ResourceAsset,
    ): Try<Publication.Builder, PublicationParser.ParseError> {
        if (!asset.format.conformsToAny(bitmapSpecifications)) {
            return Try.failure(PublicationParser.ParseError.FormatNotSupported())
        }

        val container =
            asset.toContainer()

        val readingOrderWithFormat =
            listOfNotNull(container.first() to asset.format)

        return finalizeParsing(container, readingOrderWithFormat, null)
    }

    private suspend fun parseContainerAsset(
        asset: ContainerAsset,
    ): Try<Publication.Builder, PublicationParser.ParseError> {
        if (!asset.format.conformsTo(Specification.InformalComic)) {
            return Try.failure(PublicationParser.ParseError.FormatNotSupported())
        }

        val entryFormats: Map<Url, Format> = assetRetriever
            .sniffContainerEntries(asset.container) { !it.isHiddenOrThumbs }
            .getOrElse { return Try.failure(PublicationParser.ParseError.Reading(it)) }

        val readingOrderWithFormat =
            asset.container
                .mapNotNull { url -> entryFormats.getEquivalent(url)?.let { url to it } }
                .filter { (_, format) -> format.specification.specifications.any { it in bitmapSpecifications } }
                .sortedBy { it.first.toString() }

        if (readingOrderWithFormat.isEmpty()) {
            return Try.failure(
                PublicationParser.ParseError.Reading(
                    ReadError.Decoding(
                        DebugError("No bitmap found in the publication.")
                    )
                )
            )
        }

        val title = asset
            .container
            .entries
            .guessTitle()

        return finalizeParsing(asset.container, readingOrderWithFormat, title)
    }

    private fun finalizeParsing(
        container: Container<Resource>,
        readingOrderWithFormat: List<Pair<Url, Format>>,
        title: String?,
    ): Try<Publication.Builder, PublicationParser.ParseError> {
        val readingOrder = readingOrderWithFormat.map { (url, format) ->
            Link(href = url, mediaType = format.mediaType)
        }.toMutableList()

        // First valid resource is the cover.
        readingOrder[0] = readingOrder[0].copy(rels = setOf("cover"))

        val manifest = Manifest(
            metadata = Metadata(
                conformsTo = setOf(Publication.Profile.DIVINA),
                localizedTitle = title?.let { LocalizedString(it) }
            ),
            readingOrder = readingOrder
        )

        val publicationBuilder = Publication.Builder(
            manifest = manifest,
            container = container,
            servicesBuilder = Publication.ServicesBuilder(
                positions = PerResourcePositionsService.createFactory(
                    fallbackMediaType = MediaType("image/*")!!
                )
            )
        )

        return Try.success(publicationBuilder)
    }

    private val bitmapSpecifications: Set<Specification> =
        setOf(
            Specification.Avif,
            Specification.Bmp,
            Specification.Gif,
            Specification.Jpeg,
            Specification.Jxl,
            Specification.Png,
            Specification.Tiff,
            Specification.Webp
        )
}
