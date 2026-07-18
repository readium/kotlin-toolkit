/*
 * Copyright 2026 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.shared.util.zip.compress.archivers.zip

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertSame

class ZipEncodingTest {

    @Test
    fun helperRecognizesUtf8Aliases() {
        assertSame(Utf8ZipEncoding, ZipEncodingHelper.getZipEncoding("UTF8"))
        assertSame(Utf8ZipEncoding, ZipEncodingHelper.getZipEncoding("utf-8"))
        assertSame(Utf8ZipEncoding, ZipEncodingHelper.getZipEncoding(null))
        assertSame(Utf8ZipEncoding, ZipEncodingHelper.getZipEncoding("whatever"))
        assertSame(Cp437ZipEncoding, ZipEncodingHelper.getZipEncoding("CP437"))
        assertSame(Cp437ZipEncoding, ZipEncodingHelper.getZipEncoding("ibm437"))
    }

    @Test
    fun isUtf8() {
        kotlin.test.assertTrue(ZipEncodingHelper.isUTF8("UTF-8"))
        kotlin.test.assertTrue(ZipEncodingHelper.isUTF8("utf8"))
        kotlin.test.assertTrue(ZipEncodingHelper.isUTF8(null))
        kotlin.test.assertFalse(ZipEncodingHelper.isUTF8("CP437"))
    }

    @Test
    fun utf8DecodesValidSequences() {
        val text = "café/日本語/𝄞 music.txt"
        assertEquals(text, Utf8ZipEncoding.decode(text.encodeToByteArray()))
    }

    @Test
    fun utf8ReplacesMalformedBytesWithQuestionMark() {
        // 0xC3 alone is a truncated two-byte sequence; 0xFF is never valid.
        val bytes = byteArrayOf(
            'a'.code.toByte(),
            0xC3.toByte(),
            'b'.code.toByte(),
            0xFF.toByte()
        )
        assertEquals("a?b?", Utf8ZipEncoding.decode(bytes))
    }

    @Test
    fun utf8RejectsSurrogateEncodings() {
        // CESU-8 encoded surrogate: ED A0 81.
        val bytes = byteArrayOf(0xED.toByte(), 0xA0.toByte(), 0x81.toByte())
        assertEquals("???", Utf8ZipEncoding.decode(bytes))
    }

    @Test
    fun cp437DecodesAscii() {
        assertEquals("readme.txt", Cp437ZipEncoding.decode("readme.txt".encodeToByteArray()))
    }

    @Test
    fun cp437DecodesHighBytes() {
        // 0x82 -> é, 0xA5 -> Ñ, 0xE1 -> ß
        val bytes = byteArrayOf(0x82.toByte(), 0xA5.toByte(), 0xE1.toByte())
        assertEquals("éÑß", Cp437ZipEncoding.decode(bytes))
    }
}
