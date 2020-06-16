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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.ByteArrayInputStream
import java.io.File
import java.io.InputStream

/** Provides an access to a file's content to sniff its format. */
internal interface FormatSnifferContent {

    /** Reads the whole content as raw bytes. */
    suspend fun read(): ByteArray?

    /**
     * Raw bytes stream of the content.
     *
     * A byte stream can be useful when sniffers only need to read a few bytes at the beginning of
     * the file.
     */
    suspend fun stream(): InputStream?
}

/** Used to sniff a local file. */
internal class FormatSnifferFileContent(val file: File) : FormatSnifferContent {

    override suspend fun read(): ByteArray? = withContext(Dispatchers.IO) {
        try {
            // We only read files smaller than 100KB to avoid an [OutOfMemoryError].
            if (file.length() > 100000) {
                null
            } else {
                file.readBytes()
            }
        } catch (e: Exception) {
            Timber.e(e)
            null
        }
    }

    override suspend fun stream(): InputStream? =
        try {
            file.inputStream().buffered()
        } catch (e: Exception) {
            Timber.e(e)
            null
        }

}

/** Used to sniff a bytes array. */
internal class FormatSnifferBytesContent(val getBytes: () -> ByteArray) : FormatSnifferContent {

    private lateinit var _bytes: ByteArray

    private suspend fun bytes(): ByteArray {
        if (!this::_bytes.isInitialized) {
            _bytes = withContext(Dispatchers.IO) { getBytes() }
        }
        return _bytes
    }

    override suspend fun read(): ByteArray? = bytes()

    override suspend fun stream(): InputStream? =
        ByteArrayInputStream(bytes())

}

/** Used to sniff a content URI. */
internal class FormatSnifferUriContent(val uri: Uri, val contentResolver: ContentResolver) : FormatSnifferContent {

    override suspend fun read(): ByteArray? =
        stream()?.readBytes()

    override suspend fun stream(): InputStream? = withContext(Dispatchers.IO) {
        try {
            contentResolver.openInputStream(uri)
        } catch (e: Exception) {
            Timber.e(e)
            null
        }
    }

}