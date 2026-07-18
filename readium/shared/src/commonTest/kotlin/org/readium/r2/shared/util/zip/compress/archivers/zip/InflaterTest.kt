/*
 * Copyright 2026 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.shared.util.zip.compress.archivers.zip

import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue
import org.readium.r2.shared.Fixtures

class InflaterTest {

    private val fixtures = Fixtures("zip")

    private val deflated: ByteArray get() = fixtures.read("lorem.deflate").toByteArray()

    private val expected: ByteArray get() = fixtures.read("lorem.txt").toByteArray()

    @Test
    fun inflateWholeInput() {
        val inflater = Inflater(true)
        try {
            val compressed = deflated
            inflater.setInput(compressed, 0, compressed.size)
            val output = ByteArray(expected.size)
            var produced = 0
            while (!inflater.finished()) {
                val n = inflater.inflate(output, produced, output.size - produced)
                if (n == 0 && inflater.needsInput()) {
                    break
                }
                produced += n
            }
            assertTrue(inflater.finished())
            assertEquals(expected.size, produced)
            assertContentEquals(expected, output)
            assertEquals(compressed.size.toLong(), inflater.getBytesRead())
            assertEquals(expected.size.toLong(), inflater.getBytesWritten())
            assertEquals(0, inflater.getRemaining())
        } finally {
            inflater.end()
        }
    }

    @Test
    fun inflateInChunks() {
        val inflater = Inflater(true)
        try {
            val compressed = deflated
            val output = ByteArray(expected.size)
            var produced = 0
            var fed = 0
            while (!inflater.finished()) {
                if (inflater.needsInput() && fed < compressed.size) {
                    val chunk = minOf(7, compressed.size - fed)
                    inflater.setInput(compressed, fed, chunk)
                    fed += chunk
                }
                // Tiny output buffer chunks too.
                val n = inflater.inflate(output, produced, minOf(13, output.size - produced))
                produced += n
                if (n == 0 && inflater.needsInput() && fed >= compressed.size) {
                    error("Premature end of deflate data")
                }
            }
            assertEquals(expected.size, produced)
            assertContentEquals(expected, output)
        } finally {
            inflater.end()
        }
    }

    @Test
    fun invalidDataThrowsZipException() {
        val inflater = Inflater(true)
        try {
            val garbage = ByteArray(32) { (it * 7 + 3).toByte() }
            inflater.setInput(garbage, 0, garbage.size)
            assertFailsWith<ZipException> {
                val out = ByteArray(128)
                var guard = 0
                while (!inflater.finished() && guard < 100) {
                    inflater.inflate(out, 0, out.size)
                    guard++
                }
            }
        } finally {
            inflater.end()
        }
    }
}
