/*
 * Module: r2-streamer-kotlin
 * Developers: Aferdita Muriqi, Clément Baumann
 *
 * Copyright (c) 2018. Readium Foundation. All rights reserved.
 * Use of this source code is governed by a BSD-style license which is detailed in the
 * LICENSE file present in the project repository where this source code is maintained.
 */

package org.readium.r2.streamer.parser.cbz

import org.readium.r2.shared.fetcher.ArchiveFetcher
import org.readium.r2.shared.format.Format
import org.readium.r2.shared.format.MediaType
import org.readium.r2.shared.publication.*
import org.readium.r2.shared.publication.services.positionsServiceFactory
import org.readium.r2.streamer.BuildConfig.DEBUG
import org.readium.r2.streamer.container.ContainerError
import org.readium.r2.streamer.parser.PerResourcePositionsService
import org.readium.r2.streamer.parser.PubBox
import org.readium.r2.streamer.parser.PublicationParser
import timber.log.Timber
import java.io.File
import java.io.FileInputStream
import java.io.InputStream
import java.security.MessageDigest
import kotlin.experimental.and


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

        val container = try {
            generateContainerFrom(fileAtPath)
        } catch (e: Exception) {
            if (DEBUG) Timber.e(e, "Could not generate container")
            return null
        }
        val listFiles = try {
            container.files
                .filterNot { it.startsWith(".") }
                .sorted()

        } catch (e: Exception) {
            if (DEBUG) Timber.e(e, "Missing File : META-INF/container.xml")
            return null
        }

        val hash = fileToMD5(fileAtPath)
        val metadata = Metadata(identifier = hash, localizedTitle = LocalizedString(fallbackTitle))

        val readingOrder = listFiles.mapIndexed { index, path ->
            Link(
                href = path,
                type = Format.of(fileExtension = File(path).extension)?.mediaType.toString(),
                rels = if (index == 0) setOf("cover") else emptySet()
            )
        }
        val manifest = Manifest(
            metadata = metadata,
            readingOrder = readingOrder,
            otherCollections = listOf(
                PublicationCollection(role = "images", links = readingOrder)
            )
        )

        val publication = Publication(
            manifest = manifest,
            fetcher = fetcher,
            servicesBuilder = Publication.ServicesBuilder().apply {
                positionsServiceFactory = PerResourcePositionsService.createFactory(fallbackMediaType = "image/*")
            }
        ).apply {
            type =  Publication.TYPE.CBZ
        }

        return PubBox(publication, container)
    }

    private fun fileToMD5(filePath: String): String? {
        var inputStream: InputStream? = null
        try {
            inputStream = FileInputStream(filePath)
            val buffer = ByteArray(1024)
            val digest = MessageDigest.getInstance("MD5")
            var numRead = 0
            while (numRead != -1) {
                numRead = inputStream.read(buffer)
                if (numRead > 0)
                    digest.update(buffer, 0, numRead)
            }
            val md5Bytes = digest.digest()
            return convertHashToString(md5Bytes)
        } catch (e: Exception) {
            return null
        } finally {
            if (inputStream != null) {
                try {
                    inputStream.close()
                } catch (e: Exception) {
                }

            }
        }
    }

    private fun convertHashToString(md5Bytes: ByteArray): String {
        var returnVal = ""
        for (i in md5Bytes.indices) {
            returnVal += ((md5Bytes[i] and 0xff.toByte()) + 0x100).toString(16).substring(1)
        }
        return returnVal
    }
}
