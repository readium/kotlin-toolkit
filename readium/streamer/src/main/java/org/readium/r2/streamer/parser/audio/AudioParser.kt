/*
 * Copyright 2023 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.streamer.parser.audio

import org.readium.r2.shared.publication.Link
import org.readium.r2.shared.publication.LocalizedString
import org.readium.r2.shared.publication.Manifest
import org.readium.r2.shared.publication.Metadata
import org.readium.r2.shared.publication.Publication
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
import org.readium.r2.shared.util.resource.Resource
import org.readium.r2.streamer.extensions.guessTitle
import org.readium.r2.streamer.extensions.isHiddenOrThumbs
import org.readium.r2.streamer.extensions.sniffContainerEntries
import org.readium.r2.streamer.extensions.toContainer
import org.readium.r2.streamer.parser.PublicationParser

/**
 * Parses an audiobook Publication from an unstructured archive format containing audio files,
 * such as ZAB (Zipped Audio Book) or a simple ZIP.
 *
 * It can also work for a standalone audio file.
 */
public class AudioParser(
    private val assetSniffer: AssetRetriever,
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
        if (!asset.format.conformsToAny(audioSpecifications)) {
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
        if (!asset.format.conformsTo(Specification.InformalAudiobook)) {
            return Try.failure(PublicationParser.ParseError.FormatNotSupported())
        }

        val entryFormats: Map<Url, Format> = assetSniffer
            .sniffContainerEntries(asset.container) { !it.isHiddenOrThumbs }
            .getOrElse { return Try.failure(PublicationParser.ParseError.Reading(it)) }

        val readingOrderWithFormat =
            asset.container
                .mapNotNull { url -> entryFormats.getEquivalent(url)?.let { url to it } }
                .filter { (_, format) -> format.specification.specifications.any { it in audioSpecifications } }
                .sortedBy { it.first.toString() }

        if (readingOrderWithFormat.isEmpty()) {
            return Try.failure(
                PublicationParser.ParseError.Reading(
                    ReadError.Decoding(
                        DebugError("No audio file found in the publication.")
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
        }

        val manifest = Manifest(
            metadata = Metadata(
                conformsTo = setOf(Publication.Profile.AUDIOBOOK),
                localizedTitle = title?.let { LocalizedString(it) }
            ),
            readingOrder = readingOrder
        )

        val publicationBuilder = Publication.Builder(
            manifest = manifest,
            container = container,
            servicesBuilder = Publication.ServicesBuilder(
                locator = AudioLocatorService.createFactory()
            )
        )

        return Try.success(publicationBuilder)
    }

    private val audioSpecifications: Set<Specification> =
        setOf(
            Specification.Aac,
            Specification.Aiff,
            Specification.Flac,
            Specification.Mp4,
            Specification.Mp3,
            Specification.Ogg,
            Specification.Opus,
            Specification.Wav,
            Specification.Webm
        )
}
