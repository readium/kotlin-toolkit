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
import org.readium.r2.shared.fetcher.FileFetcher
import org.readium.r2.shared.fetcher.TransformingFetcher
import org.readium.r2.shared.format.Format
import org.readium.r2.shared.format.MediaType
import org.readium.r2.shared.normalize
import org.readium.r2.shared.publication.Manifest
import org.readium.r2.shared.publication.Publication
import org.readium.r2.shared.publication.services.positionsServiceFactory
import org.readium.r2.streamer.container.PublicationContainer
import org.readium.r2.streamer.extensions.fromArchiveOrDirectory
import org.readium.r2.streamer.fetcher.LcpDecryptor
import org.readium.r2.streamer.parser.PubBox
import org.readium.r2.streamer.parser.PublicationParser
import timber.log.Timber
import java.io.File

/**
 * Parses any Readium Web Publication package or manifest, e.g. WebPub, Audiobook, DiViNa, LCPDF...
 */
class ReadiumWebPubParser(private val context: Context) : PublicationParser {
    override fun parse(fileAtPath: String, fallbackTitle: String): PubBox? = runBlocking {
        _parse(fileAtPath, fallbackTitle)
    }

    private suspend fun _parse(fileAtPath: String, fallbackTitle: String): PubBox? {
        val file = File(fileAtPath)
        val format = runBlocking { Format.ofFile(file) } ?: return null

        val pubBox = if (format.mediaType.isRwpm) {
            parseManifest(file, format)
        } else {
            parsePackage(file, format)
        }

        if (pubBox != null) {
            val readingOrder = pubBox.publication.readingOrder

            // Checks the requirements from the LCPDF specification.
            // https://readium.org/lcp-specs/notes/lcp-for-pdf.html
            if (format == Format.LCP_PROTECTED_PDF && (readingOrder.isEmpty() || !readingOrder.all { it.mediaType?.matches(MediaType.PDF) == true })) {
                Timber.e("Invalid LCP Protected PDF")
                return null
            }
        }

        return pubBox
    }

    private suspend fun parseManifest(file: File, format: Format): PubBox? {
        return try {
            val fetcher = FileFetcher(href = "/manifest.json", file = file)
            val manifestJson = file.readText()
            parsePublication(manifestJson, file, format, fetcher, isPackage = false)

        } catch(e: Exception) {
            Timber.e(e, "Failed to parse RWPM")
            null
        }
    }

    private suspend fun parsePackage(file: File, format: Format): PubBox? {
        return try {
            val fetcher = Fetcher.fromArchiveOrDirectory(file.path) ?: return null
            val manifestJson = fetcher.get("/manifest.json").readAsString().getOrNull() ?: return null

            parsePublication(manifestJson, file, format, fetcher, isPackage = true)

        } catch (e: Exception) {
            Timber.e(e, "Failed to parse Readium WebPub package")
            null
        }
    }

    private suspend fun parsePublication(manifestJson: String, file: File, format: Format, fetcher: Fetcher, isPackage: Boolean): PubBox? {
        try {
            val drm =
                if (fetcher.isProtectedWithLcp()) DRM(DRM.Brand.lcp)
                else null

            @Suppress("NAME_SHADOWING")
            var fetcher = fetcher

            if (drm?.brand == DRM.Brand.lcp) {
                fetcher = TransformingFetcher(fetcher, listOfNotNull(
                    LcpDecryptor(drm)::transform
                ))
            }

            val manifest = Manifest.fromJSON(JSONObject(manifestJson)) { normalize(base = "/", href = it) }
                ?: return null

            val publication = Publication(
                manifest = manifest,
                fetcher = fetcher,
                servicesBuilder = Publication.ServicesBuilder().apply {
                    if (format == Format.LCP_PROTECTED_PDF) {
                        positionsServiceFactory = LcpdfPositionsService.create(context.applicationContext)
                    }
                }
            ).apply {
                type = format.toPublicationType()
            }

            val container = PublicationContainer(
                publication = publication,
                path = file.canonicalPath,
                mediaType = format.mediaType,
                drm = drm
            ).apply {
                if (isPackage) {
                    rootFile.rootFilePath = "manifest.json"
                }
            }

            return PubBox(publication, container)

        } catch (e: Exception) {
            Timber.e(e, "Failed to parse RWPM")
            return null
        }
    }

}

private suspend fun Fetcher.isProtectedWithLcp(): Boolean =
    get("license.lcpl").length().isSuccess

private fun Format.toPublicationType(): Publication.TYPE =
    when (this) {
        Format.READIUM_AUDIOBOOK, Format.READIUM_AUDIOBOOK_MANIFEST -> Publication.TYPE.AUDIO
        Format.DIVINA, Format.DIVINA_MANIFEST -> Publication.TYPE.DiViNa
        else -> Publication.TYPE.WEBPUB
    }
