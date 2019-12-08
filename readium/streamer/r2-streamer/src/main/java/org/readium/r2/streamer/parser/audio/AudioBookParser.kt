/*
 * Module: r2-streamer-kotlin
 * Developers: Aferdita Muriqi, Irteza Sheikh
 *
 * Copyright (c) 2018. Readium Foundation. All rights reserved.
 * Use of this source code is governed by a BSD-style license which is detailed in the
 * LICENSE file present in the project repository where this source code is maintained.
 */

package org.readium.r2.streamer.parser.audio

import org.json.JSONObject
import org.readium.r2.shared.Publication
import org.readium.r2.shared.parsePublication
import org.readium.r2.streamer.container.ContainerError
import org.readium.r2.streamer.parser.PubBox
import org.readium.r2.streamer.parser.PublicationParser
import timber.log.Timber
import java.io.File
import java.net.URI
import java.nio.charset.Charset



class AudioBookConstant {
    companion object {
        // Some constants useful to parse an DiViNa document
        const val mimetype = "application/audiobook+zip"
        const val manifestPath = "manifest.json"
    }
}

/**
 *      AudiobookParser : Handle any Audiobook Package file. Opening, listing files
 *                  get name of the resource, creating the Publication
 *                  for rendering
 */
class AudioBookParser : PublicationParser {


    /**
     * Check if path exist, generate a container for CBZ file
     *                   then check if creation was a success
     */
    private fun generateContainerFrom(path: String): AudioBookDirectoryContainer {
        val container: AudioBookDirectoryContainer?

        if (!File(path).exists())
            throw ContainerError.missingFile(path)
        container = AudioBookDirectoryContainer(path)
        return container
    }

    /**
     * This functions parse a manifest.json and build PubBox object from it
     */
    override fun parse(fileAtPath: String, title: String): PubBox? {

        //Building container
        val container = try {
            generateContainerFrom(fileAtPath)
        } catch (e: Exception) {
            Timber.e(e, "Could not generate container")
            return null
        }
        val data = try {
            container.data(AudioBookConstant.manifestPath)
        } catch (e: Exception) {
            Timber.e(e, "Missing File : ${AudioBookConstant.manifestPath}")
            return null
        }

        //Building publication object from manifest.json
        //Getting manifest.json
        val stringManifest = data.toString(Charset.defaultCharset())
        val json = JSONObject(stringManifest)

        //Parsing manifest.json & building publication object
        val publication = parsePublication(json)

        //Modifying path of links
        for ((index, link) in publication.readingOrder.withIndex()) {
            val uri: String = if (URI(link.href).isAbsolute) {
                link.href!!
            } else {
                fileAtPath + "/" + link.href
            }
            publication.readingOrder[index].href = uri
        }

        publication.type = Publication.TYPE.AUDIO

        return PubBox(publication, container)

    }
}