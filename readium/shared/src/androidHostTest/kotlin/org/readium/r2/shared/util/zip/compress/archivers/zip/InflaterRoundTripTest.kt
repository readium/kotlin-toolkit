/*
 * Copyright 2026 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.shared.util.zip.compress.archivers.zip

import java.util.zip.Deflater
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertTrue

/**
 * Round-trip test for the common [Inflater]: deflate on the JVM, inflate with the ported common
 * code (phase 05b of the KMP plan).
 */
class InflaterRoundTripTest {

    private fun deflateRaw(data: ByteArray): ByteArray {
        val deflater = Deflater(Deflater.BEST_COMPRESSION, true)
        try {
            deflater.setInput(data)
            deflater.finish()
            val output = ByteArray(data.size * 2 + 64)
            var produced = 0
            while (!deflater.finished()) {
                produced += deflater.deflate(output, produced, output.size - produced)
            }
            return output.copyOf(produced)
        } finally {
            deflater.end()
        }
    }

    private fun inflateRaw(compressed: ByteArray, expectedSize: Int): ByteArray {
        val inflater = Inflater(true)
        try {
            inflater.setInput(compressed, 0, compressed.size)
            val output = ByteArray(expectedSize)
            var produced = 0
            while (!inflater.finished()) {
                val n = inflater.inflate(output, produced, output.size - produced)
                if (n == 0 && inflater.needsInput()) {
                    break
                }
                produced += n
            }
            assertTrue(inflater.finished())
            return output.copyOf(produced)
        } finally {
            inflater.end()
        }
    }

    @Test
    fun roundTripText() {
        val data = "The quick brown fox jumps over the lazy dog. ".repeat(100).encodeToByteArray()
        assertContentEquals(data, inflateRaw(deflateRaw(data), data.size))
    }

    @Test
    fun roundTripRandomBinary() {
        val data = Random(42).nextBytes(100_000)
        assertContentEquals(data, inflateRaw(deflateRaw(data), data.size))
    }

    @Test
    fun roundTripEmpty() {
        val data = ByteArray(0)
        assertContentEquals(data, inflateRaw(deflateRaw(data), 0))
    }
}
