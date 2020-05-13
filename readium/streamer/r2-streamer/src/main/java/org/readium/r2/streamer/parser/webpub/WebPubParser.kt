/*
 * Module: r2-streamer-kotlin
 * Developers: Mickaël Menu
 *
 * Copyright (c) 2020. Readium Foundation. All rights reserved.
 * Use of this source code is governed by a BSD-style license which is detailed in the
 * LICENSE file present in the project repository where this source code is maintained.
 */

package org.readium.r2.streamer.parser.webpub

import org.json.JSONObject
import org.readium.r2.shared.drm.DRM
import org.readium.r2.shared.format.Format
import org.readium.r2.shared.normalize
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
class WebPubParser : PublicationParser {

    override fun parse(fileAtPath: String, fallbackTitle: String): PubBox? {
        val file = File(fileAtPath)
        val format = Format.of(file) ?: return null

        return if (format.mediaType.isRwpm) {
            parseManifest(file, format)
        } else {
            parsePackage(file, format)
        }
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
        return try {
            val lcpProtected = (isPackage && container.isProtectedWithLcp)
            if (lcpProtected) {
                container.drm = DRM(DRM.Brand.lcp)
            }

            Publication
                .fromJSON(JSONObject(manifestJson)) { normalize(base = "/", href = it) }
                ?.apply { type = format.toPublicationType() }

        } catch (e: Exception) {
            Timber.e(e, "Failed to parse RWPM")
            null
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
