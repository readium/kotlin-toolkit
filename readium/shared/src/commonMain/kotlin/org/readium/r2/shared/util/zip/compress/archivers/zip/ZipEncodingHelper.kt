/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

// Ported to Kotlin Multiplatform for Readium from the vendored Commons Compress subset
// (Java sources: `ZipEncodingHelper.java` and `NioZipEncoding.java`, phase 05b).
// `java.nio.charset` is not available in common code: per the 05a decisions, the charset-based
// `NioZipEncoding` was replaced with a common encoding table. Zip names are either CP437 or
// UTF-8; any other/unknown encoding name falls back to UTF-8 (where the Java original fell back
// to the platform default charset, which is UTF-8 on the supported targets).

package org.readium.r2.shared.util.zip.compress.archivers.zip

/**
 * Static helper functions for robustly encoding file names in zip files.
 */
internal object ZipEncodingHelper {

    /** Name of the encoding UTF-8. */
    const val UTF8: String = "UTF8"

    /** The UTF-8 zip encoding. */
    val UTF8_ZIP_ENCODING: ZipEncoding = Utf8ZipEncoding

    /**
     * Instantiates a zip encoding: CP437 if [name] is a CP437 alias, UTF-8 otherwise.
     */
    fun getZipEncoding(name: String?): ZipEncoding =
        if (name != null && isCp437Alias(name)) {
            Cp437ZipEncoding
        } else {
            Utf8ZipEncoding
        }

    /** Returns whether a given encoding name is UTF-8. */
    fun isUTF8(charsetName: String?): Boolean {
        val actual = charsetName ?: return true
        return UTF8_ALIASES.any { it.equals(actual, ignoreCase = true) }
    }

    private fun isCp437Alias(name: String): Boolean =
        CP437_ALIASES.any { it.equals(name, ignoreCase = true) }

    private val UTF8_ALIASES = listOf("UTF-8", "UTF8", "unicode-1-1-utf-8")

    private val CP437_ALIASES = listOf("CP437", "IBM437", "IBM-437", "437", "csPC8CodePage437")
}

/**
 * The UTF-8 [ZipEncoding], replacing malformed input with `?` like the original
 * `NioZipEncoding` configured with `CodingErrorAction.REPLACE` and a `?` replacement.
 */
internal object Utf8ZipEncoding : ZipEncoding {

    private const val REPLACEMENT = '?'

    override fun decode(data: ByteArray): String {
        val sb = StringBuilder(data.size)
        var i = 0
        val size = data.size

        fun continuation(index: Int): Int =
            if (index < size) {
                val b = data[index].toInt() and 0xff
                if (b in 0x80..0xBF) b else -1
            } else {
                -1
            }

        while (i < size) {
            val b0 = data[i].toInt() and 0xff
            when {
                b0 < 0x80 -> {
                    sb.append(b0.toChar())
                    i += 1
                }
                b0 in 0xC2..0xDF -> {
                    val b1 = continuation(i + 1)
                    if (b1 == -1) {
                        sb.append(REPLACEMENT)
                        i += 1
                    } else {
                        sb.append((((b0 and 0x1F) shl 6) or (b1 and 0x3F)).toChar())
                        i += 2
                    }
                }
                b0 in 0xE0..0xEF -> {
                    val b1 = continuation(i + 1)
                    val b2 = continuation(i + 2)
                    val validB1 = when (b0) {
                        0xE0 -> b1 in 0xA0..0xBF
                        0xED -> b1 in 0x80..0x9F
                        else -> b1 != -1
                    }
                    if (!validB1 || b2 == -1) {
                        sb.append(REPLACEMENT)
                        i += 1
                    } else {
                        sb.append(
                            (
                                ((b0 and 0x0F) shl 12)
                                    or ((b1 and 0x3F) shl 6)
                                    or (b2 and 0x3F)
                                ).toChar()
                        )
                        i += 3
                    }
                }
                b0 in 0xF0..0xF4 -> {
                    val b1 = continuation(i + 1)
                    val b2 = continuation(i + 2)
                    val b3 = continuation(i + 3)
                    val validB1 = when (b0) {
                        0xF0 -> b1 in 0x90..0xBF
                        0xF4 -> b1 in 0x80..0x8F
                        else -> b1 != -1
                    }
                    if (!validB1 || b2 == -1 || b3 == -1) {
                        sb.append(REPLACEMENT)
                        i += 1
                    } else {
                        val codePoint = ((b0 and 0x07) shl 18)
                            .or((b1 and 0x3F) shl 12)
                            .or((b2 and 0x3F) shl 6)
                            .or(b3 and 0x3F)
                        val offset = codePoint - 0x10000
                        sb.append(((offset shr 10) + 0xD800).toChar())
                        sb.append(((offset and 0x3FF) + 0xDC00).toChar())
                        i += 4
                    }
                }
                else -> {
                    sb.append(REPLACEMENT)
                    i += 1
                }
            }
        }
        return sb.toString()
    }
}

/**
 * The CP437 [ZipEncoding], the historical default encoding of zip file names.
 */
internal object Cp437ZipEncoding : ZipEncoding {

    // Characters mapped from the bytes 0x80 to 0xFF; bytes 0x00 to 0x7F map to US-ASCII.
    private const val HIGH_CHARS =
        "Çüéâäàåç" +
            "êëèïîìÄÅ" +
            "ÉæÆôöòûù" +
            "ÿÖÜ¢£¥₧ƒ" +
            "áíóúñÑªº" +
            "¿⌐¬½¼¡«»" +
            "░▒▓│┤╡╢╖" +
            "╕╣║╗╝╜╛┐" +
            "└┴┬├─┼╞╟" +
            "╚╔╩╦╠═╬╧" +
            "╨╤╥╙╘╒╓╫" +
            "╪┘┌█▄▌▐▀" +
            "αßΓπΣσµτ" +
            "ΦΘΩδ∞φε∩" +
            "≡±≥≤⌠⌡÷≈" +
            "°∙·√ⁿ²■ "

    override fun decode(data: ByteArray): String {
        val sb = StringBuilder(data.size)
        for (byte in data) {
            val b = byte.toInt() and 0xff
            sb.append(if (b < 0x80) b.toChar() else HIGH_CHARS[b - 0x80])
        }
        return sb.toString()
    }
}
