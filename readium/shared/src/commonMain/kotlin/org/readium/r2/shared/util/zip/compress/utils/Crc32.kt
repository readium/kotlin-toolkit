/*
 * Copyright 2026 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.shared.util.zip.compress.utils

/**
 * A common implementation of the standard CRC-32 checksum (as used by zip), replacing
 * `java.util.zip.CRC32` for the ported zip stack.
 */
internal class Crc32 {

    private var crc: Int = -1 // 0xFFFFFFFF

    /** The current checksum value. */
    val value: Long
        get() = crc.inv().toLong() and 0xffffffffL

    /** Updates the checksum with [len] bytes of [b] starting at [off]. */
    fun update(b: ByteArray, off: Int = 0, len: Int = b.size) {
        var c = crc
        for (i in off until off + len) {
            c = (c ushr 8) xor TABLE[(c xor b[i].toInt()) and 0xff]
        }
        crc = c
    }

    /** Resets the checksum to its initial value. */
    fun reset() {
        crc = -1
    }

    private companion object {

        private val TABLE: IntArray = IntArray(256).also { table ->
            for (n in 0 until 256) {
                var c = n
                repeat(8) {
                    c = if (c and 1 != 0) {
                        (c ushr 1) xor 0xEDB88320.toInt()
                    } else {
                        c ushr 1
                    }
                }
                table[n] = c
            }
        }
    }
}
