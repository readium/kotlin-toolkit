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
import org.readium.r2.shared.util.asset.AssetSniffer
import org.readium.r2.shared.util.asset.ContainerAsset
import org.readium.r2.shared.util.asset.ResourceAsset
import org.readium.r2.shared.util.data.ReadError
import org.readium.r2.shared.util.format.Format
import org.readium.r2.shared.util.format.FormatRegistry
import org.readium.r2.shared.util.format.Trait
import org.readium.r2.shared.util.getOrElse
import org.readium.r2.shared.util.logging.WarningLogger
import org.readium.r2.shared.util.mediatype.MediaType
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
    private val assetSniffer: AssetSniffer,
    private val formatRegistry: FormatRegistry
) : PublicationParser {

    override suspend fun parse(
        asset: Asset,
        warnings: WarningLogger?
    ): Try<Publication.Builder, PublicationParser.ParseError> {
        if (!asset.format.conformsTo(Trait.COMICS) && !asset.format.conformsTo(Trait.BITMAP)) {
            return Try.failure(PublicationParser.ParseError.FormatNotSupported())
        }

        val container = when (asset) {
            is ResourceAsset ->
                asset.resource.toContainer()
            is ContainerAsset ->
                asset.container
        }

        val entryFormats: Map<Url, Format> = assetSniffer
            .sniffContainerEntries(container) { !it.isHiddenOrThumbs }
            .getOrElse { return Try.failure(PublicationParser.ParseError.Reading(it)) }

        val readingOrderWithFormat =
            if (asset.format.conformsTo(Trait.COMICS)) {
                container
                    .mapNotNull { url -> entryFormats[url]?.let { url to it } }
                    .filter { it.second.conformsTo(Trait.BITMAP) }
                    .sortedBy { it.first.toString() }
            } else {
                listOfNotNull(container.first() to asset.format)
            }

        if (readingOrderWithFormat.isEmpty()) {
            return Try.failure(
                PublicationParser.ParseError.Reading(
                    ReadError.Decoding(
                        DebugError("No bitmap found in the publication.")
                    )
                )
            )
        }

        val readingOrderLinks = readingOrderWithFormat.map { (url, format) ->
            val mediaType = formatRegistry[format]?.mediaType
            Link(href = url, mediaType = mediaType)
        }.toMutableList()

        // First valid resource is the cover.
        readingOrderLinks[0] = readingOrderLinks[0].copy(rels = setOf("cover"))

        val manifest = Manifest(
            metadata = Metadata(
                conformsTo = setOf(Publication.Profile.DIVINA),
                localizedTitle = container.guessTitle()?.let { LocalizedString(it) }
            ),
            readingOrder = readingOrderLinks
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
}
