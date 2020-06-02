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
import org.json.JSONObject
import org.readium.r2.shared.drm.DRM
import org.readium.r2.shared.format.Format
import org.readium.r2.shared.format.MediaType
import org.readium.r2.shared.normalize
import org.readium.r2.shared.publication.Manifest
import org.readium.r2.shared.publication.Publication
import org.readium.r2.streamer.container.ArchiveContainer
import org.readium.r2.streamer.container.Container
import org.readium.r2.streamer.container.EmptyContainer
import org.readium.r2.streamer.parser.PubBox
import org.readium.r2.streamer.parser.PublicationParser
import timber.log.Timber
import java.io.File

/**
 * Parses any Readium Web Publication package or manifest, e.g. WebPub, Audiobook, DiViNa, LCPDF...
 */
class ReadiumWebPubParser(private val context: Context) : PublicationParser {

    override fun parse(fileAtPath: String, fallbackTitle: String): PubBox? {
        val file = File(fileAtPath)
        val format = Format.of(file) ?: return null

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

    private fun parseManifest(file: File, format: Format): PubBox? {
        return try {
            val container = EmptyContainer(file.path, mimetype = format.mediaType.toString())
            val manifestJson = file.readText()
            parsePublication(manifestJson, container, format, isPackage = false)
                ?.let { PubBox(it, container) }

        } catch(e: Exception) {
            Timber.e(e, "Failed to parse RWPM")
            null
        }
    }

    private fun parsePackage(file: File, format: Format): PubBox? {
        return try {
            val manifestPath = "manifest.json"
            val container = ArchiveContainer(file.path, mimetype = format.mediaType.toString()).apply {
                rootFile.rootFilePath = manifestPath
            }
            val manifestJson = String(container.data(manifestPath))

            parsePublication(manifestJson, container, format, isPackage = true)
                ?.let { PubBox(it, container) }

        } catch (e: Exception) {
            Timber.e(e, "Failed to parse Readium WebPub package")
            null
        }
    }

    private fun parsePublication(manifestJson: String, container: Container, format: Format, isPackage: Boolean): Publication? {
        try {
            val lcpProtected = (isPackage && container.isProtectedWithLcp)
            if (lcpProtected) {
                container.drm = DRM(DRM.Brand.lcp)
            }

            val manifest = Manifest.fromJSON(JSONObject(manifestJson)) { normalize(base = "/", href = it) }
                ?: return null

            var positionsFactory: Publication.PositionListFactory? = null

            if (format == Format.LCP_PROTECTED_PDF) {
                positionsFactory = LcpdfPositionListFactory(
                    context = this@ReadiumWebPubParser.context.applicationContext,
                    container = container,
                    readingOrder = manifest.readingOrder
                )
            }

            val publication = Publication(
                manifest = manifest,
                positionsFactory = positionsFactory
            )
            publication.type = format.toPublicationType()

            return publication

        } catch (e: Exception) {
            Timber.e(e, "Failed to parse RWPM")
            return null
        }
    }

}

private val Container.isProtectedWithLcp: Boolean get() =
    try { dataLength("license.lcpl") > 0 }
    catch (e: Exception) { false }

private fun Format.toPublicationType(): Publication.TYPE =
    when (this) {
        Format.AUDIOBOOK, Format.AUDIOBOOK_MANIFEST -> Publication.TYPE.AUDIO
        Format.DIVINA, Format.DIVINA_MANIFEST -> Publication.TYPE.DiViNa
        else -> Publication.TYPE.WEBPUB
    }
