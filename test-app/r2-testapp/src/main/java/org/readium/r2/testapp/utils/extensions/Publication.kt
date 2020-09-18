/*
 * Module: r2-testapp-kotlin
 * Developers: Mickaël Menu
 *
 * Copyright (c) 2020. Readium Foundation. All rights reserved.
 * Use of this source code is governed by a BSD-style license which is detailed in the
 * LICENSE file present in the project repository where this source code is maintained.
 */

package org.readium.r2.testapp.utils.extensions

import android.content.Context
import kotlinx.coroutines.runBlocking
import org.readium.r2.shared.PdfSupport
import org.readium.r2.shared.format.Format
import org.readium.r2.shared.publication.Publication
import org.readium.r2.streamer.parser.PubBox
import org.readium.r2.streamer.parser.audio.AudioBookParser
import org.readium.r2.streamer.parser.cbz.CBZParser
import org.readium.r2.streamer.parser.divina.DiViNaParser
import org.readium.r2.streamer.parser.epub.EpubParser
import org.readium.r2.streamer.parser.pdf.PdfParser
import org.readium.r2.streamer.parser.readium.ReadiumWebPubParser
import timber.log.Timber

@OptIn(PdfSupport::class)
fun Publication.Companion.parse(context: Context, path: String, format: Format): PubBox? =
    try {
        when (format) {
            Format.EPUB -> EpubParser()
            Format.CBZ -> CBZParser()
            Format.DIVINA -> DiViNaParser()
            Format.PDF -> PdfParser(context)
            Format.READIUM_AUDIOBOOK -> AudioBookParser()
            Format.LCP_PROTECTED_PDF -> ReadiumWebPubParser(context)
            else -> null

        }?.parse(path)

    } catch (e: Exception) {
        Timber.e(e)
        null
    }

fun Publication.Companion.parse(context: Context, path: String, mediaType: String? = null, fileExtension: String? = null): PubBox? =
    runBlocking { Format.ofFile(path, mediaType = mediaType, fileExtension = fileExtension) }
        ?.let { parse(context, path, it) }

val Publication.TYPE.format: Format? get() = when (this) {
    Publication.TYPE.EPUB -> Format.EPUB
    Publication.TYPE.CBZ -> Format.CBZ
    Publication.TYPE.FXL -> Format.EPUB
    Publication.TYPE.WEBPUB -> Format.READIUM_WEBPUB
    Publication.TYPE.AUDIO -> Format.READIUM_AUDIOBOOK
    Publication.TYPE.DiViNa -> Format.DIVINA
}
