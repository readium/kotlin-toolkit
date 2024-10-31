/*
 * Copyright 2021 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.shared.util.io

import java.io.FilterInputStream
import java.io.IOException
import java.io.InputStream
import org.readium.r2.shared.InternalReadiumApi
import org.readium.r2.shared.extensions.coerceFirstNonNegative
import org.readium.r2.shared.extensions.read
import org.readium.r2.shared.extensions.requireLengthFitInt

/**
 * An [InputStream] counting the number of bytes read from a wrapped [inputStream].
 */
@InternalReadiumApi
public class CountingInputStream(
    inputStream: InputStream,
    initialCount: Long = 0
) : FilterInputStream(inputStream) {

    public var count: Long = initialCount
        private set

    private var mark: Long = -1

    override fun read(): Int =
        super.read()
            .also {
                if (it != -1) {
                    count++
                }
            }

    override fun read(b: ByteArray?, off: Int, len: Int): Int =
        super.read(b, off, len)
            .also { readLen ->
                if (readLen != -1) {
                    count += readLen.toLong()
                }
            }

    override fun skip(n: Long): Long =
        super.skip(n)
            .also { count += it }

    override fun mark(readlimit: Int) {
        super.mark(readlimit)
        mark = count
    }

    override fun reset() {
        if (!`in`.markSupported()) {
            throw IOException("Mark not supported")
        }
        if (mark == -1L) {
            throw IOException("Mark not set")
        }

        super.reset()
        count = mark.coerceAtLeast(0)
    }

    public fun readRange(range: LongRange): ByteArray {
        @Suppress("NAME_SHADOWING")
        val range = range
            .coerceFirstNonNegative()
            .requireLengthFitInt()

        if (range.isEmpty()) {
            return ByteArray(0)
        }

        val toSkip = range.first - count
        var skipped: Long = 0

        while (skipped != toSkip) {
            skipped += skip(toSkip - skipped)
            if (skipped == 0L) {
                throw IOException("Could not skip InputStream to read ranges.")
            }
        }

        val length = range.last - range.first + 1
        return read(length)
    }
}
