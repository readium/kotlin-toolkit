/*
 * Module: r2-streamer-kotlin
 * Developers: Quentin Gliosca
 *
 * Copyright (c) 2020. Readium Foundation. All rights reserved.
 * Use of this source code is governed by a BSD-style license which is detailed in the
 * LICENSE file present in the project repository where this source code is maintained.
 */

package org.readium.r2.streamer.parser.audio

import org.readium.r2.shared.extensions.md5
import org.readium.r2.shared.fetcher.Fetcher
import org.readium.r2.shared.publication.Link
import org.readium.r2.shared.publication.LocalizedString
import org.readium.r2.shared.publication.Manifest
import org.readium.r2.shared.publication.Metadata
import org.readium.r2.shared.publication.Publication
import org.readium.r2.shared.util.File
import org.readium.r2.shared.util.logging.WarningLogger
import org.readium.r2.shared.util.mediatype.MediaType
import org.readium.r2.streamer.PublicationParser
import org.readium.r2.streamer.extensions.guessTitle
import org.readium.r2.streamer.extensions.isHiddenOrThumbs
import org.readium.r2.streamer.extensions.lowercasedExtension
import org.readium.r2.streamer.extensions.toTitle

/**
 * Parses an audiobook Publication from an unstructured archive format containing audio files,
 * such as ZAB (Zipped Audio Book) or a simple ZIP.
 *
 * It can also work for a standalone audio file.
 */
class AudioParser :  PublicationParser {

    override suspend fun parse(file: File, fetcher: Fetcher, warnings: WarningLogger?): Publication.Builder? {

        if (!accepts(file, fetcher))
            return null

        val readingOrder = fetcher.links()
            .filter { link -> with(File(link.href)) { lowercasedExtension in audioExtensions && !isHiddenOrThumbs } }
            .sortedBy(Link::href)
            .toMutableList()

        if (readingOrder.isEmpty())
            throw Exception("No audio file found in the publication.")

        val title = fetcher.guessTitle()
            ?: file.toTitle()

        val manifest = Manifest(
            metadata = Metadata(
                identifier = file.file.md5(),
                localizedTitle = LocalizedString(title)
            ),
            readingOrder = readingOrder
        )

        return Publication.Builder(
            manifest = manifest,
            fetcher = fetcher,
            servicesBuilder = Publication.ServicesBuilder(
                locator = AudioLocatorService.createFactory()
            )
        )
    }

    private suspend fun accepts(file: File, fetcher: Fetcher): Boolean {
        if (file.mediaType() == MediaType.ZAB)
            return true

        val allowedExtensions = audioExtensions +
                listOf("asx", "bio", "m3u", "m3u8", "pla", "pls", "smil", "txt", "vlc", "wpl", "xspf", "zpl")

        if (fetcher.links().filterNot { File(it.href).isHiddenOrThumbs }
                .all { File(it.href).lowercasedExtension in allowedExtensions })
            return true

        return false
    }

    private val audioExtensions = listOf(
        "aac", "aiff", "alac", "flac", "m4a", "m4b", "mp3",
        "ogg", "oga", "mogg", "opus", "wav", "webm"
    )
}
