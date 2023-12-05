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
import org.readium.r2.shared.util.data.ReadError
import org.readium.r2.shared.util.getOrElse
import org.readium.r2.shared.util.logging.WarningLogger
import org.readium.r2.shared.util.mediatype.MediaType
import org.readium.r2.shared.util.mediatype.MediaTypeRetriever
import org.readium.r2.shared.util.mediatype.MediaTypeSnifferError
import org.readium.r2.shared.util.use
import org.readium.r2.streamer.extensions.guessTitle
import org.readium.r2.streamer.extensions.isHiddenOrThumbs
import org.readium.r2.streamer.parser.PublicationParser

/**
 * Parses an audiobook Publication from an unstructured archive format containing audio files,
 * such as ZAB (Zipped Audio Book) or a simple ZIP.
 *
 * It can also work for a standalone audio file.
 */
public class AudioParser(
    private val mediaTypeRetriever: MediaTypeRetriever
) : PublicationParser {

    override suspend fun parse(
        asset: PublicationParser.Asset,
        warnings: WarningLogger?
    ): Try<Publication.Builder, PublicationParser.Error> {
        if (!asset.mediaType.matches(MediaType.ZAB) && !asset.mediaType.isAudio) {
            return Try.failure(PublicationParser.Error.FormatNotSupported())
        }

        val readingOrder =
            if (asset.mediaType.matches(MediaType.ZAB)) {
                asset.container
                    .filter { zabCanContain(it) }
                    .sortedBy { it.toString() }
            } else {
                listOfNotNull(
                    asset.container.entries.firstOrNull()
                )
            }

        if (readingOrder.isEmpty()) {
            return Try.failure(
                PublicationParser.Error.Reading(
                    ReadError.Decoding(
                        DebugError("No audio file found in the publication.")
                    )
                )
            )
        }

        val readingOrderLinks = readingOrder.map { url ->
            val mediaType = asset.container[url]!!.use { resource ->
                mediaTypeRetriever.retrieve(resource)
                    .getOrElse { error ->
                        when (error) {
                            MediaTypeSnifferError.NotRecognized ->
                                null
                            is MediaTypeSnifferError.Reading ->
                                return Try.failure(PublicationParser.Error.Reading(error.cause))
                        }
                    }
            }
            Link(href = url, mediaType = mediaType)
        }

        val manifest = Manifest(
            metadata = Metadata(
                conformsTo = setOf(Publication.Profile.AUDIOBOOK),
                localizedTitle = asset.container.entries.guessTitle()?.let { LocalizedString(it) }
            ),
            readingOrder = readingOrderLinks
        )

        val publicationBuilder = Publication.Builder(
            manifest = manifest,
            container = asset.container,
            servicesBuilder = Publication.ServicesBuilder(
                locator = AudioLocatorService.createFactory()
            )
        )

        return Try.success(publicationBuilder)
    }

    private fun zabCanContain(url: Url): Boolean =
        url.extension?.lowercase() in audioExtensions && !url.isHiddenOrThumbs

    private val audioExtensions = listOf(
        "aac", "aiff", "alac", "flac", "m4a", "m4b", "mp3",
        "ogg", "oga", "mogg", "opus", "wav", "webm"
    )
}
