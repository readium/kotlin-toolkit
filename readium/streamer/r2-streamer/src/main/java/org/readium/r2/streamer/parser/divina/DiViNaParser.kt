/*
 * Module: r2-streamer-kotlin
 * Developers: Aferdita Muriqi
 *
 * Copyright (c) 2018. Readium Foundation. All rights reserved.
 * Use of this source code is governed by a BSD-style license which is detailed in the
 * LICENSE file present in the project repository where this source code is maintained.
 */

package org.readium.r2.streamer.parser.divina

import org.json.JSONObject
import org.readium.r2.shared.Publication
import org.readium.r2.shared.parsePublication
import org.readium.r2.streamer.container.ContainerError
import org.readium.r2.streamer.parser.PubBox
import org.readium.r2.streamer.parser.PublicationParser
import timber.log.Timber
import java.io.ByteArrayInputStream
import java.io.File
import java.io.FileNotFoundException
import java.io.InputStream
import java.nio.charset.Charset

class DiViNaConstant {
    companion object {
        // Some constants useful to parse an DiViNa document
        const val mimetype = "application/divina+json"
        const val manifestPath = "manifest.json"
        const val publicationPath = "publication.json"
    }
}

/**
 *      DiViNaParser : Handle any DiViNa file. Opening, listing files
 *                  get name of the resource, creating the Publication
 *                  for rendering
 */
class DiViNaParser : PublicationParser {


    /**
     * Check if path exist, generate a container for DiViNa file
     *                   then check if creation was a success
     */
    private fun generateContainerFrom(path: String): ContainerDiViNa {
        val container: ContainerDiViNa?

        if (!File(path).exists())
            throw ContainerError.missingFile(path)
        container = ContainerDiViNa(path)
        return container
    }

    /**
     *
     */
    override fun parse(fileAtPath: String, title: String): PubBox? {

        val container = try {
            generateContainerFrom(fileAtPath)
        } catch (e: Exception) {
            Timber.e(e,"Could not generate container")
            return null
        }

        val data = try {
            container.data(DiViNaConstant.manifestPath)
        } catch (e: Exception) {
            Timber.e(e, "Missing File : ${DiViNaConstant.manifestPath}")
            try {
                val publication = container.data(DiViNaConstant.publicationPath)
                container.rootFile
                val inputStream = ByteArrayInputStream(publication)
                inputStream.toFile("${container.rootFile.rootPath}/${DiViNaConstant.manifestPath}")
                publication
            } catch (e: FileNotFoundException) {
                Timber.e(e, "Missing File : ${DiViNaConstant.publicationPath}")
                return null
            } catch (e: Exception) {
                Timber.e(e, "${container.rootFile.rootPath}/${DiViNaConstant.manifestPath}")
                return null
            }
        }

        //Building publication object from manifest.json
        //Getting manifest.json
        val stringManifest = data.toString(Charset.defaultCharset())
        val json = JSONObject(stringManifest)

        //Parsing manifest.json & building publication object
        val publication = parsePublication(json)
        publication.type = Publication.TYPE.DiViNa

        // Add href as title if title is missing (this is used to display the TOC)
        for ((index, link) in publication.readingOrder.withIndex()) {
            if (link.title == null || link.title!!.isEmpty()) {
                link.title = link.href
            }
        }

        return PubBox(publication, container)
    }

    private fun InputStream.toFile(path: String) {
        use { input ->
            File(path).outputStream().use { input.copyTo(it) }
        }
    }

}