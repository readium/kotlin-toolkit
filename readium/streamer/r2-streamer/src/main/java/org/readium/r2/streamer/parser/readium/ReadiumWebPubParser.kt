/*
 * Module: r2-streamer-kotlin
 * Developers: Mickaël Menu
 *
 * Copyright (c) 2020. Readium Foundation. All rights reserved.
 * Use of this source code is governed by a BSD-style license which is detailed in the
 * LICENSE file present in the project repository where this source code is maintained.
 */

package org.readium.r2.streamer.parser.readium

import android.content.Context
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import org.readium.r2.shared.drm.DRM
import org.readium.r2.shared.fetcher.Fetcher
import org.readium.r2.shared.fetcher.TransformingFetcher
import org.readium.r2.shared.format.Format
import org.readium.r2.shared.format.MediaType
import org.readium.r2.shared.publication.Manifest
import org.readium.r2.shared.publication.Publication
import org.readium.r2.shared.publication.services.PerResourcePositionsService
import org.readium.r2.shared.util.File

import org.readium.r2.shared.util.logging.WarningLogger
import org.readium.r2.streamer.PublicationParser
import org.readium.r2.streamer.container.ContainerError
import org.readium.r2.streamer.container.PublicationContainer
import org.readium.r2.streamer.extensions.fromFile
import org.readium.r2.streamer.fetcher.LcpDecryptor
import org.readium.r2.streamer.parser.PubBox
import org.readium.r2.streamer.toPublicationType
import java.io.FileNotFoundException

/**
 * Parses any Readium Web Publication package or manifest, e.g. WebPub, Audiobook, DiViNa, LCPDF...
 */
class ReadiumWebPubParser(private val context: Context) : PublicationParser, org.readium.r2.streamer.parser.PublicationParser {

    override suspend fun parse(
        file: File,
        fetcher: Fetcher,
        fallbackTitle: String,
        warnings: WarningLogger?
    ): PublicationParser.PublicationBuilder? {

        if (file.format()?.mediaType?.isReadiumWebPubProfile in listOf(false, null))
            return null

        val manifest =
            if (file.format()?.mediaType?.isRwpm == true) {
                val manifestLink = fetcher.links().firstOrNull()
                    ?: error("Empty fetcher.")
                val manifestJson = fetcher.get(manifestLink).use {
                    it.readAsString()
                    .getOrThrow()
                }
                Manifest.fromJSON(JSONObject(manifestJson))
            } else {
                val manifestLink = fetcher.links()
                    .firstOrNull { it.href == "/manifest.json" }
                    ?: error("Unable to find a manifest link.")
                val manifestJson = fetcher.get(manifestLink).use {
                    it.readAsString()
                    .getOrThrow()
                }
                Manifest.fromJSON(JSONObject(manifestJson), packaged = true)
            }
                ?: throw Exception("Failed to parse RWPM.")


        // Checks the requirements from the LCPDF specification.
        // https://readium.org/lcp-specs/notes/lcp-for-pdf.html
        val readingOrder = manifest.readingOrder
        if (file.format() == Format.LCP_PROTECTED_PDF && (readingOrder.isEmpty() || !readingOrder.all { it.mediaType?.matches(MediaType.PDF) == true })) {
            throw Exception("Invalid LCP Protected PDF.")
        }

        val positionsService = when(file.format()) {
            Format.LCP_PROTECTED_PDF ->
                LcpdfPositionsService.create(context.applicationContext)
            Format.READIUM_AUDIOBOOK_MANIFEST, Format.READIUM_AUDIOBOOK, Format.LCP_PROTECTED_AUDIOBOOK ->
                PerResourcePositionsService.createFactory(fallbackMediaType = "audio/*")
            Format.DIVINA_MANIFEST, Format.DIVINA ->
                PerResourcePositionsService.createFactory("image/*")
            else -> null
        }
        val servicesBuilder = Publication.ServicesBuilder(positions = positionsService)

        return PublicationParser.PublicationBuilder(manifest, fetcher, servicesBuilder)
    }

    override fun parse(fileAtPath: String, fallbackTitle: String): PubBox? = runBlocking {

        val file = File(fileAtPath)
        val format = file.format() ?: return@runBlocking null
        var baseFetcher = try {
            Fetcher.fromFile(file.file)
        } catch (e: SecurityException) {
            return@runBlocking null
        } catch (e: FileNotFoundException) {
            throw ContainerError.missingFile(fileAtPath)
        }

        val drm = if (baseFetcher.isProtectedWithLcp()) DRM(DRM.Brand.lcp) else null
        if (drm?.brand == DRM.Brand.lcp) {
            baseFetcher = TransformingFetcher(baseFetcher, LcpDecryptor(drm)::transform)
        }

        val builder = try {
            parse(file, baseFetcher, fallbackTitle)
        } catch (e: Exception) {
            return@runBlocking null
        } ?: return@runBlocking null

        val publication = builder.build()
            .apply { type = format.toPublicationType() }

        val container = PublicationContainer(
            publication = publication,
            path = file.file.canonicalPath,
            mediaType = format.mediaType,
            drm = drm
        ).apply {
            if (!format.mediaType.isRwpm) {
                rootFile.rootFilePath = "manifest.json"
            }
        }

        PubBox(publication, container)
    }
}

private suspend fun Fetcher.isProtectedWithLcp(): Boolean =
    get("license.lcpl").use { it.length().isSuccess }
