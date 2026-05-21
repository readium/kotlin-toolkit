/*
 * Copyright 2023 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

@file:OptIn(InternalReadiumApi::class)

package org.readium.r2.lcp.util

import java.io.File
import java.security.MessageDigest
import org.readium.r2.shared.InternalReadiumApi
import org.readium.r2.shared.extensions.tryOrNull

/**
 * Returns the SHA-256 sum of file content or null if computation failed.
 */
internal fun File.sha256(): ByteArray? =
    tryOrNull<ByteArray> {
        val md = MessageDigest.getInstance("SHA-256")
        val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
        inputStream().use {
            var bytes = it.read(buffer)
            while (bytes >= 0) {
                md.update(buffer, 0, bytes)
                bytes = it.read(buffer)
            }
        }
        return md.digest()
    }

/**
 * Returns the SHA-256 sum of the byte array.
 */
internal fun ByteArray.sha256(): ByteArray {
    val md = MessageDigest.getInstance("SHA-256")
    return md.digest(this)
}

/**
 * Returns the SHA-256 sum of the string encoded as a lowercase hex string.
 */
internal fun String.sha256Hex(): String =
    toByteArray().sha256().toHexString()
