/*
 * Module: r2-shared-kotlin
 * Developers: Quentin Gliosca
 *
 * Copyright (c) 2020. Readium Foundation. All rights reserved.
 * Use of this source code is governed by a BSD-style license which is detailed in the
 * LICENSE file present in the project repository where this source code is maintained.
 *
 * This piece of code is adapted from
 * https://github.com/JetBrains/kotlin/blob/deb416484c5128a6f4bc76c39a3d9878b38cec8c/libraries/stdlib/jvm/src/kotlin/io/IOStreams.kt
 *
 */

package org.readium.r2.shared.extensions

import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.io.OutputStream

/**
 * Copies this stream to the given output stream, returning the number of bytes copied
 *
 * **Note** It is the caller's responsibility to close both of these resources.
 */
internal fun InputStream.copyTo(out: OutputStream, limit: Long, bufferSize: Int = DEFAULT_BUFFER_SIZE): Long {
    var bytesCopied: Long = 0
    val buffer = ByteArray(bufferSize)
    var bytes = read(buffer, 0, limit.toInt())
    while (bytes >= 0) {
        out.write(buffer, 0, bytes)
        bytesCopied += bytes
        bytes = read(buffer, 0, (limit - bytesCopied).toInt())
    }
    return bytesCopied
}

/**
 * Reads this stream completely into a byte array.
 *
 * **Note**: It is the caller's responsibility to close this stream.
 */
internal fun InputStream.read(limit: Long): ByteArray {
        val buffer = ByteArrayOutputStream(maxOf(DEFAULT_BUFFER_SIZE, this.available(), limit.toInt()))
        copyTo (buffer, limit)
        return buffer.toByteArray()
    }