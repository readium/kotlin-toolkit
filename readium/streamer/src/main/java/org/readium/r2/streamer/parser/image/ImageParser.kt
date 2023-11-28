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
import org.readium.r2.shared.util.MessageError
import org.readium.r2.shared.util.Try
import org.readium.r2.shared.util.Url
import org.readium.r2.shared.util.asset.MediaTypeRetriever
import org.readium.r2.shared.util.data.ReadError
import org.readium.r2.shared.util.getOrElse
import org.readium.r2.shared.util.logging.WarningLogger
import org.readium.r2.shared.util.mediatype.MediaType
import org.readium.r2.shared.util.mediatype.MediaTypeSnifferError
import org.readium.r2.shared.util.use
import org.readium.r2.streamer.extensions.guessTitle
import org.readium.r2.streamer.extensions.isHiddenOrThumbs
import org.readium.r2.streamer.parser.PublicationParser

/**
 * Parses an image–based Publication from an unstructured archive format containing bitmap files,
 * such as CBZ or a simple ZIP.
 *
 * It can also work for a standalone bitmap file.
 */
public class ImageParser(
    private val mediaTypeRetriever: MediaTypeRetriever
) : PublicationParser {

    override suspend fun parse(
        asset: PublicationParser.Asset,
        warnings: WarningLogger?
    ): Try<Publication.Builder, PublicationParser.Error> {
        if (!asset.mediaType.matches(MediaType.CBZ) && !asset.mediaType.isBitmap) {
            return Try.failure(PublicationParser.Error.UnsupportedFormat())
        }

        val readingOrder =
            if (asset.mediaType.matches(MediaType.CBZ)) {
                (asset.container)
                    .filter { cbzCanContain(it) }
                    .sortedBy { it.toString() }
            } else {
                listOfNotNull(asset.container.firstOrNull())
            }

        if (readingOrder.isEmpty()) {
            return Try.failure(
                PublicationParser.Error.ReadError(
                    ReadError.Decoding(
                        MessageError("No bitmap found in the publication.")
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
                            is MediaTypeSnifferError.Read ->
                                return Try.failure(PublicationParser.Error.ReadError(error.cause))
                        }
                    }
            }
            Link(href = url, mediaType = mediaType)
        }.toMutableList()

        // First valid resource is the cover.
        readingOrderLinks[0] = readingOrderLinks[0].copy(rels = setOf("cover"))

        val manifest = Manifest(
            metadata = Metadata(
                conformsTo = setOf(Publication.Profile.DIVINA),
                localizedTitle = asset.container.guessTitle()?.let { LocalizedString(it) }
            ),
            readingOrder = readingOrderLinks
        )

        val publicationBuilder = Publication.Builder(
            manifest = manifest,
            container = asset.container,
            servicesBuilder = Publication.ServicesBuilder(
                positions = PerResourcePositionsService.createFactory(
                    fallbackMediaType = MediaType("image/*")!!
                )
            )
        )

        return Try.success(publicationBuilder)
    }

    private fun cbzCanContain(url: Url): Boolean =
        url.extension?.lowercase() in bitmapExtensions && !url.isHiddenOrThumbs

    private val bitmapExtensions = listOf(
        "bmp", "dib", "gif", "jif", "jfi", "jfif", "jpg", "jpeg",
        "png", "tif", "tiff", "webp"
    )
}
