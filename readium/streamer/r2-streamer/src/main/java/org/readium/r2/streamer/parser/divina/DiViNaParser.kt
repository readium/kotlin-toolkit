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
import org.readium.r2.shared.format.MediaType
import org.readium.r2.shared.publication.Manifest
import org.readium.r2.shared.publication.Publication
import org.readium.r2.shared.publication.services.PerResourcePositionsService
import org.readium.r2.streamer.BuildConfig.DEBUG
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
        @Deprecated("Use [MediaType.DIVINA_MANIFEST.toString()] instead", replaceWith = ReplaceWith("MediaType.DIVINA_MANIFEST.toString()"))
        val mimetype get() = MediaType.DIVINA_MANIFEST.toString()

        internal const val manifestPath = "manifest.json"
        internal const val publicationPath = "publication.json"
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

    override fun parse(fileAtPath: String, fallbackTitle: String): PubBox? {

        val container = try {
            generateContainerFrom(fileAtPath)
        } catch (e: Exception) {
            if (DEBUG) Timber.e(e,"Could not generate container")
            return null
        }

        val data = try {
            container.data(DiViNaConstant.manifestPath)
        } catch (e: Exception) {
            if (DEBUG) Timber.e(e, "Missing File : ${DiViNaConstant.manifestPath}")
            try {
                val publication = container.data(DiViNaConstant.publicationPath)
                container.rootFile
                val inputStream = ByteArrayInputStream(publication)
                inputStream.toFile("${container.rootFile.rootPath}/${DiViNaConstant.manifestPath}")
                publication
            } catch (e: FileNotFoundException) {
                if (DEBUG) Timber.e(e, "Missing File : ${DiViNaConstant.publicationPath}")
                return null
            } catch (e: Exception) {
                if (DEBUG) Timber.e(e, "${container.rootFile.rootPath}/${DiViNaConstant.manifestPath}")
                return null
            }
        }

        //Building publication object from manifest.json
        //Getting manifest.json
        val stringManifest = data.toString(Charset.defaultCharset())
        val json = JSONObject(stringManifest)

        //Parsing manifest.json & building publication object
        val manifest = Manifest.fromJSON(json)
            ?: return null

        val publication = Publication(
            manifest = manifest,
            servicesBuilder = Publication.ServicesBuilder(
                positions = PerResourcePositionsService.createFactory(fallbackMediaType = "image/*")
            )
        ).apply {
            Publication.TYPE.DiViNa
        }

        return PubBox(publication, container)
    }

    private fun InputStream.toFile(path: String) {
        use { input ->
            File(path).outputStream().use { input.copyTo(it) }
        }
    }

}