/*
 * Copyright 2026 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.shared.util.zip.compress.archivers.zip

/**
 * Android actual of the zip [Inflater], delegating to `java.util.zip.Inflater`.
 */
internal actual class Inflater actual constructor(nowrap: Boolean) {

    private val inflater = java.util.zip.Inflater(nowrap)

    actual fun setInput(b: ByteArray, off: Int, len: Int) {
        inflater.setInput(b, off, len)
    }

    actual fun needsInput(): Boolean =
        inflater.needsInput()

    actual fun needsDictionary(): Boolean =
        inflater.needsDictionary()

    actual fun finished(): Boolean =
        inflater.finished()

    actual fun inflate(b: ByteArray, off: Int, len: Int): Int =
        try {
            inflater.inflate(b, off, len)
        } catch (e: java.util.zip.DataFormatException) {
            throw ZipException(e.message ?: "Invalid deflate data", e)
        }

    actual fun getRemaining(): Int =
        inflater.remaining

    actual fun getBytesRead(): Long =
        inflater.bytesRead

    actual fun getBytesWritten(): Long =
        inflater.bytesWritten

    actual fun end() {
        inflater.end()
    }
}
