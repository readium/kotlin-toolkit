/*
 * Module: r2-streamer-kotlin
 * Developers: Aferdita Muriqi, Clément Baumann
 *
 * Copyright (c) 2018. Readium Foundation. All rights reserved.
 * Use of this source code is governed by a BSD-style license which is detailed in the
 * LICENSE file present in the project repository where this source code is maintained.
 */

package org.readium.r2.streamer.parser.cbz

import kotlinx.coroutines.runBlocking
import org.readium.r2.shared.fetcher.Fetcher
import org.readium.r2.shared.util.File
import org.readium.r2.shared.format.MediaType
import org.readium.r2.shared.publication.*
import org.readium.r2.streamer.container.ContainerError
import org.readium.r2.streamer.container.PublicationContainer
import org.readium.r2.streamer.extensions.fromArchiveOrDirectory
import org.readium.r2.streamer.parser.PubBox
import org.readium.r2.streamer.parser.PublicationParser
import org.readium.r2.streamer.parser.image.ImageParser

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

    private val imageParser = ImageParser()

    override fun parse(fileAtPath: String, fallbackTitle: String): PubBox? = runBlocking {
        makePubBox(fileAtPath, fallbackTitle)
    }

    private suspend fun makePubBox(fileAtPath: String, fallbackTitle: String): PubBox? {

        val file = File(fileAtPath)

        val fetcher = Fetcher.fromArchiveOrDirectory(fileAtPath)
            ?: throw ContainerError.missingFile(fileAtPath)

        val publication = imageParser.parse(file, fetcher, fallbackTitle)
            ?.build()
            ?.apply { type = Publication.TYPE.CBZ }
            ?: return null

        val container = PublicationContainer(
            publication = publication,
            path = file.file.canonicalPath,
            mediaType = MediaType.CBZ
        )

        return PubBox(publication, container)
    }

}
