/*
 * Copyright 2023 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.streamer.parser.audio

import java.io.File
import org.readium.r2.shared.fetcher.Fetcher
import org.readium.r2.shared.publication.*
import org.readium.r2.shared.publication.asset.PublicationAsset
import org.readium.r2.shared.util.Try
import org.readium.r2.shared.util.logging.WarningLogger
import org.readium.r2.shared.util.mediatype.MediaType
import org.readium.r2.streamer.extensions.guessTitle
import org.readium.r2.streamer.extensions.isHiddenOrThumbs
import org.readium.r2.streamer.extensions.lowercasedExtension
import org.readium.r2.streamer.parser.PublicationParser

/**
 * Parses an audiobook Publication from an unstructured archive format containing audio files,
 * such as ZAB (Zipped Audio Book) or a simple ZIP.
 *
 * It can also work for a standalone audio file.
 */
class AudioParser : PublicationParser {

    override suspend fun parse(
        asset: PublicationAsset,
        warnings: WarningLogger?
    ): Try<Publication.Builder, PublicationParser.Error> {

        if (!accepts(asset.mediaType, asset.fetcher))
            return Try.failure(PublicationParser.Error.FormatNotSupported)

        val readingOrder = asset.fetcher.links()
            .filter { link -> with(File(link.href)) { lowercasedExtension in audioExtensions && !isHiddenOrThumbs } }
            .sortedBy(Link::href)
            .toMutableList()

        if (readingOrder.isEmpty())
            throw Exception("No audio file found in the publication.")

        val title = asset.fetcher.guessTitle() ?: asset.name

        val manifest = Manifest(
            metadata = Metadata(
                conformsTo = setOf(Publication.Profile.AUDIOBOOK),
                localizedTitle = LocalizedString(title)
            ),
            readingOrder = readingOrder
        )

        val publicationBuilder = Publication.Builder(
            manifest = manifest,
            fetcher = asset.fetcher,
            servicesBuilder = Publication.ServicesBuilder(
                locator = AudioLocatorService.createFactory()
            )
        )

        return Try.success(publicationBuilder)
    }

    private suspend fun accepts(mediaType: MediaType, fetcher: Fetcher): Boolean {
        if (mediaType == MediaType.ZAB)
            return true

        val allowedExtensions = audioExtensions +
            listOf("asx", "bio", "m3u", "m3u8", "pla", "pls", "smil", "txt", "vlc", "wpl", "xspf", "zpl")

        if (fetcher.links().filterNot { File(it.href).isHiddenOrThumbs }
            .all { File(it.href).lowercasedExtension in allowedExtensions }
        )
            return true

        return false
    }

    private val audioExtensions = listOf(
        "aac", "aiff", "alac", "flac", "m4a", "m4b", "mp3",
        "ogg", "oga", "mogg", "opus", "wav", "webm"
    )
}
