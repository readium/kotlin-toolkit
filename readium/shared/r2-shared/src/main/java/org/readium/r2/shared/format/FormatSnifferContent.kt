/*
 * Module: r2-shared-kotlin
 * Developers: Mickaël Menu
 *
 * Copyright (c) 2020. Readium Foundation. All rights reserved.
 * Use of this source code is governed by a BSD-style license which is detailed in the
 * LICENSE file present in the project repository where this source code is maintained.
 */

package org.readium.r2.shared.format

import android.content.ContentResolver
import android.net.Uri
import timber.log.Timber
import java.io.ByteArrayInputStream
import java.io.File
import java.io.InputStream

/** Provides an access to a file's content to sniff its format. */
internal interface FormatSnifferContent {

    /** Reads the whole content as raw bytes. */
    fun read(): ByteArray?

    /**
     * Raw bytes stream of the content.
     *
     * A byte stream can be useful when sniffers only need to read a few bytes at the beginning of
     * the file.
     */
    fun stream(): InputStream?
}

/** Used to sniff a local file. */
internal class FormatSnifferFileContent(val file: File) : FormatSnifferContent {

    override fun read(): ByteArray? =
        try {
            file.readBytes()
        } catch (e: Exception) {
            Timber.e(e)
            null
        }

    override fun stream(): InputStream? =
        try {
            file.inputStream().buffered()
        } catch (e: Exception) {
            Timber.e(e)
            null
        }

}

/** Used to sniff a bytes array. */
internal class FormatSnifferBytesContent(val getBytes: () -> ByteArray) : FormatSnifferContent {

    private val bytes by lazy { getBytes() }

    override fun read(): ByteArray? = bytes

    override fun stream(): InputStream? =
        ByteArrayInputStream(bytes)

}

/** Used to sniff a content URI. */
internal class FormatSnifferUriContent(val uri: Uri, val contentResolver: ContentResolver) : FormatSnifferContent {

    override fun read(): ByteArray? =
        stream()?.readBytes()

    override fun stream(): InputStream? =
        try {
            contentResolver.openInputStream(uri)
        } catch (e: Exception) {
            Timber.e(e)
            null
        }

}