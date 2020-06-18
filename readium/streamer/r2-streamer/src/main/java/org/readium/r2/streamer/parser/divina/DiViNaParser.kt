/*
 * Module: r2-streamer-kotlin
 * Developers: Aferdita Muriqi
 *
 * Copyright (c) 2018. Readium Foundation. All rights reserved.
 * Use of this source code is governed by a BSD-style license which is detailed in the
 * LICENSE file present in the project repository where this source code is maintained.
 */

package org.readium.r2.streamer.parser.divina

import kotlinx.coroutines.runBlocking
import org.readium.r2.shared.fetcher.Fetcher
import org.readium.r2.shared.format.MediaType
import org.readium.r2.shared.publication.Manifest
import org.readium.r2.shared.publication.Publication
import org.readium.r2.shared.publication.services.PerResourcePositionsService
import org.readium.r2.streamer.container.ContainerError
import org.readium.r2.streamer.container.PublicationContainer
import org.readium.r2.streamer.extensions.fromArchiveOrDirectory
import org.readium.r2.streamer.extensions.readAsJsonOrNull
import org.readium.r2.streamer.parser.PubBox
import org.readium.r2.streamer.parser.PublicationParser

class DiViNaConstant {
    companion object {
        @Deprecated("Use [MediaType.DIVINA_MANIFEST.toString()] instead", replaceWith = ReplaceWith("MediaType.DIVINA_MANIFEST.toString()"), level = DeprecationLevel.ERROR)
        val mimetype get() = MediaType.DIVINA_MANIFEST.toString()
    }
}

/**
 *      DiViNaParser : Handle any DiViNa file. Opening, listing files
 *                  get name of the resource, creating the Publication
 *                  for rendering
 */
class DiViNaParser : PublicationParser {
    override fun parse(fileAtPath: String, fallbackTitle: String): PubBox? = runBlocking {
        _parse(fileAtPath, fallbackTitle)
    }

    private suspend fun _parse(fileAtPath: String, fallbackTitle: String): PubBox? {
        val fetcher = Fetcher.fromArchiveOrDirectory(fileAtPath)
            ?: throw ContainerError.missingFile(fileAtPath)

        val manifest = fetcher.readAsJsonOrNull("manifest.json")
            .let { Manifest.fromJSON(it) }
            ?: return null

        val publication = Publication(
            manifest = manifest,
            servicesBuilder = Publication.ServicesBuilder(
                positions = PerResourcePositionsService.createFactory(fallbackMediaType = "image/*")
            )
        ).apply {
            type = Publication.TYPE.DiViNa
        }

        val container = PublicationContainer(
            fetcher = fetcher,
            path = fileAtPath,
            mediaType = MediaType.DIVINA
        )
        return PubBox(publication, container)
    }

}