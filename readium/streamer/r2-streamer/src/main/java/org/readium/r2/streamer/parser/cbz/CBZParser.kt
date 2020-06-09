/*
 * Module: r2-streamer-kotlin
 * Developers: Aferdita Muriqi, Clément Baumann
 *
 * Copyright (c) 2018. Readium Foundation. All rights reserved.
 * Use of this source code is governed by a BSD-style license which is detailed in the
 * LICENSE file present in the project repository where this source code is maintained.
 */

package org.readium.r2.streamer.parser.cbz

import org.readium.r2.shared.extensions.md5
import org.readium.r2.shared.fetcher.ArchiveFetcher
import org.readium.r2.shared.format.MediaType
import org.readium.r2.shared.publication.*
import org.readium.r2.streamer.container.ContainerError
import org.readium.r2.streamer.container.PublicationContainer
import org.readium.r2.streamer.parser.PerResourcePositionsService
import org.readium.r2.streamer.parser.PubBox
import org.readium.r2.streamer.parser.PublicationParser
import java.io.File

@Deprecated("Use [MediaType] instead")
class CBZConstant {
    companion object {
        @Deprecated("Use [MediaType.CBZ.toString()] instead", replaceWith = ReplaceWith("MediaType.CBZ.toString()"))
        val mimetypeCBZ get() = MediaType.CBZ.toString()
        @Deprecated("RAR archives are not supported in Readium, don't use this constant", level = DeprecationLevel.ERROR)
        const val mimetypeCBR = "application/x-cbr"
        @Deprecated("Use [MediaType.JPEG.toString()] instead", replaceWith = ReplaceWith("MediaType.JPEG.toString()"))
        val mimetypeJPEG get() = MediaType.JPEG.toString()
        @Deprecated("Use [MediaType.PNG.toString()] instead", replaceWith = ReplaceWith("MediaType.PNG.toString()"))
        val mimetypePNG = MediaType.PNG.toString()
    }
}

/**
 *      CBZParser : Handle any CBZ file. Opening, listing files
 *                  get name of the resource, creating the Publication
 *                  for rendering
 */
class CBZParser : PublicationParser {


    /**
     * Check if path exist, generate a container for CBZ file
     *                   then check if creation was a success
     */
    private fun generateContainerFrom(path: String): CBZArchiveContainer {
        val container: CBZArchiveContainer?
        if (!File(path).exists())
            throw ContainerError.missingFile(path)
        container = CBZArchiveContainer(path)
        return container
    }

    override fun parse(fileAtPath: String, fallbackTitle: String): PubBox? {
        val fetcher = ArchiveFetcher.fromPath(fileAtPath)
            ?: return null

        val readingOrder = fetcher.links
            .filter { it.mediaType?.isBitmap == true && !it.href.startsWith(".") }
            .sortedBy { it.href }
            .toMutableList()

        if (readingOrder.isEmpty()) {
            return null
        }

        // First valid resource is the cover.
        readingOrder[0] = readingOrder[0].copy(rels = setOf("cover"))

        val manifest = Manifest(
            metadata = Metadata(
                identifier = File(fileAtPath).md5(),
                localizedTitle = LocalizedString(fallbackTitle)
            ),
            readingOrder = readingOrder,
            subcollections = mapOf(
                "images" to listOf(PublicationCollection(links = readingOrder))
            )
        )

        val publication = Publication(
            manifest = manifest,
            fetcher = fetcher,
            servicesBuilder = Publication.ServicesBuilder(
                positions = PerResourcePositionsService.createFactory(fallbackMediaType = "image/*")
            )
        ).apply {
            type =  Publication.TYPE.CBZ
        }

        val container = PublicationContainer(publication = publication, path = fileAtPath, mediaType = MediaType.CBZ)
        return PubBox(publication, container)
    }

}
