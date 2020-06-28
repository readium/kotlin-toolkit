/*
 * Module: r2-streamer-kotlin
 * Developers: Aferdita Muriqi, Irteza Sheikh
 *
 * Copyright (c) 2018. Readium Foundation. All rights reserved.
 * Use of this source code is governed by a BSD-style license which is detailed in the
 * LICENSE file present in the project repository where this source code is maintained.
 */

package org.readium.r2.streamer.parser.audio

import kotlinx.coroutines.runBlocking
import org.readium.r2.shared.fetcher.Fetcher
import org.readium.r2.shared.format.MediaType
import org.readium.r2.shared.publication.Manifest
import org.readium.r2.shared.publication.Publication
import org.readium.r2.streamer.container.ContainerError
import org.readium.r2.streamer.container.PublicationContainer
import org.readium.r2.streamer.extensions.fromArchiveOrDirectory
import org.readium.r2.streamer.extensions.readAsJsonOrNull
import org.readium.r2.streamer.parser.PubBox
import org.readium.r2.streamer.parser.PublicationParser
import java.net.URI



class AudioBookConstant {
    companion object {
        @Deprecated("Use [MediaType.AUDIOBOOK.toString()] instead", replaceWith = ReplaceWith("MediaType.AUDIOBOOK.toString()"))
        val mimetype get() = MediaType.READIUM_AUDIOBOOK.toString()
    }
}

/**
 *      AudiobookParser : Handle any Audiobook Package file. Opening, listing files
 *                  get name of the resource, creating the Publication
 *                  for rendering
 */
class AudioBookParser : PublicationParser {

    /**
     * This functions parse a manifest.json and build PubBox object from it
     */
    override fun parse(fileAtPath: String, fallbackTitle: String): PubBox? = runBlocking {
        _parse(fileAtPath, fallbackTitle)
    }

    private suspend fun _parse(fileAtPath: String, fallbackTitle: String): PubBox? {
        val fetcher = Fetcher.fromArchiveOrDirectory(fileAtPath)
            ?: throw ContainerError.missingFile(fileAtPath)

        fun normalizeHref(href: String): String =
            if (URI(href).isAbsolute) {
                href
            } else {
                // FIXME: Why the HREF is absolute on the local file system??
                "$fileAtPath/$href"
            }

        val manifest = fetcher.readAsJsonOrNull("manifest.json")
            ?.let { Manifest.fromJSON(it, normalizeHref = ::normalizeHref) }
            ?: return null

        val publication = Publication(
            manifest = manifest
        ).apply {
            type = Publication.TYPE.AUDIO
        }

        val container = PublicationContainer(
            fetcher = fetcher,
            path = fileAtPath,
            mediaType = MediaType.READIUM_AUDIOBOOK
        )

        return PubBox(publication, container)
    }
}
