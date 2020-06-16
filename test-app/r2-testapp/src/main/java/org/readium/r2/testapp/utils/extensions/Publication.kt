/*
 * Module: r2-testapp-kotlin
 * Developers: Mickaël Menu
 *
 * Copyright (c) 2020. Readium Foundation. All rights reserved.
 * Use of this source code is governed by a BSD-style license which is detailed in the
 * LICENSE file present in the project repository where this source code is maintained.
 */

package org.readium.r2.testapp.utils.extensions

import kotlinx.coroutines.runBlocking
import org.readium.r2.shared.format.Format
import org.readium.r2.shared.publication.Publication
import org.readium.r2.streamer.parser.PubBox
import org.readium.r2.streamer.parser.audio.AudioBookParser
import org.readium.r2.streamer.parser.cbz.CBZParser
import org.readium.r2.streamer.parser.divina.DiViNaParser
import org.readium.r2.streamer.parser.epub.EpubParser
import timber.log.Timber

fun Publication.Companion.parse(path: String, format: Format): PubBox? =
    try {
        when (format) {
            Format.EPUB -> EpubParser()
            Format.CBZ -> CBZParser()
            Format.DIVINA -> DiViNaParser()
            Format.READIUM_AUDIOBOOK -> AudioBookParser()
            else -> null

        }?.parse(path)

    } catch (e: Exception) {
        Timber.e(e)
        null
    }

fun Publication.Companion.parse(path: String, mediaType: String? = null, fileExtension: String? = null): PubBox? =
    runBlocking { Format.ofFile(path, mediaType = mediaType, fileExtension = fileExtension) }
        ?.let { parse(path, it) }

val Publication.TYPE.format: Format? get() = when (this) {
    Publication.TYPE.EPUB -> Format.EPUB
    Publication.TYPE.CBZ -> Format.CBZ
    Publication.TYPE.FXL -> Format.EPUB
    Publication.TYPE.WEBPUB -> Format.READIUM_WEBPUB
    Publication.TYPE.AUDIO -> Format.READIUM_AUDIOBOOK
    Publication.TYPE.DiViNa -> Format.DIVINA
}
