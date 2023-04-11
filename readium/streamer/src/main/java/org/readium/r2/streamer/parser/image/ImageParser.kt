/*
 * Copyright 2023 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.streamer.parser.image

import java.io.File
import org.readium.r2.shared.fetcher.Fetcher
import org.readium.r2.shared.publication.*
import org.readium.r2.shared.publication.asset.PublicationAsset
import org.readium.r2.shared.publication.services.PerResourcePositionsService
import org.readium.r2.shared.util.logging.WarningLogger
import org.readium.r2.shared.util.mediatype.MediaType
import org.readium.r2.streamer.extensions.guessTitle
import org.readium.r2.streamer.extensions.isHiddenOrThumbs
import org.readium.r2.streamer.extensions.lowercasedExtension
import org.readium.r2.streamer.parser.PublicationParser

/**
 * Parses an image–based Publication from an unstructured archive format containing bitmap files,
 * such as CBZ or a simple ZIP.
 *
 * It can also work for a standalone bitmap file.
 */
class ImageParser : PublicationParser {

    override suspend fun parse(asset: PublicationAsset, warnings: WarningLogger?): Publication.Builder? {

        if (!accepts(asset.mediaType, asset.fetcher))
            return null

        val readingOrder = asset.fetcher.links()
            .filter { !File(it.href).isHiddenOrThumbs && it.mediaType.isBitmap }
            .sortedBy(Link::href)
            .toMutableList()

        if (readingOrder.isEmpty())
            throw Exception("No bitmap found in the publication.")

        val title = asset.fetcher.guessTitle() ?: asset.name

        // First valid resource is the cover.
        readingOrder[0] = readingOrder[0].copy(rels = setOf("cover"))

        val manifest = Manifest(
            metadata = Metadata(
                conformsTo = setOf(Publication.Profile.DIVINA),
                localizedTitle = LocalizedString(title)
            ),
            readingOrder = readingOrder
        )

        return Publication.Builder(
            manifest = manifest,
            fetcher = asset.fetcher,
            servicesBuilder = Publication.ServicesBuilder(
                positions = PerResourcePositionsService.createFactory(fallbackMediaType = "image/*")
            )
        )
    }

    private suspend fun accepts(mediaType: MediaType, fetcher: Fetcher): Boolean {
        if (mediaType == MediaType.CBZ)
            return true

        val allowedExtensions = listOf("acbf", "txt", "xml")

        if (fetcher.links()
            .filterNot { File(it.href).isHiddenOrThumbs }
            .all { it.mediaType.isBitmap || File(it.href).lowercasedExtension in allowedExtensions }
        )
            return true

        return false
    }
}
