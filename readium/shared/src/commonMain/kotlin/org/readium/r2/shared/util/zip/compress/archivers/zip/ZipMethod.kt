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
// (Java source: `util/zip/compress/archivers/zip/ZipMethod.java`, phase 05b).

package org.readium.r2.shared.util.zip.compress.archivers.zip

/**
 * List of known compression methods.
 */
internal enum class ZipMethod(val code: Int) {

    /** Compression method 0 for uncompressed entries. */
    STORED(0),

    /** UnShrinking. dynamic Lempel-Ziv-Welch-Algorithm. */
    UNSHRINKING(1),

    /** Reduced with compression factor 1. */
    EXPANDING_LEVEL_1(2),

    /** Reduced with compression factor 2. */
    EXPANDING_LEVEL_2(3),

    /** Reduced with compression factor 3. */
    EXPANDING_LEVEL_3(4),

    /** Reduced with compression factor 4. */
    EXPANDING_LEVEL_4(5),

    /** Imploding. */
    IMPLODING(6),

    /** Tokenization. */
    TOKENIZATION(7),

    /** Compression method 8 for compressed (deflated) entries. */
    DEFLATED(8),

    /** Compression Method 9 for enhanced deflate. */
    ENHANCED_DEFLATED(9),

    /** PKWARE Data Compression Library Imploding. */
    PKWARE_IMPLODING(10),

    /** Compression Method 12 for bzip2. */
    BZIP2(12),

    /** Compression Method 14 for LZMA. */
    LZMA(14),

    /** Compression Method 95 for XZ. */
    XZ(95),

    /** Compression Method 96 for Jpeg compression. */
    JPEG(96),

    /** Compression Method 97 for WavPack. */
    WAVPACK(97),

    /** Compression Method 98 for PPMd. */
    PPMD(98),

    /** Compression Method 99 for AES encryption. */
    AES_ENCRYPTED(99),

    /** Unknown compression method. */
    UNKNOWN(-1),
    ;

    companion object {

        const val UNKNOWN_CODE: Int = -1

        private val codeToEnum: Map<Int, ZipMethod> =
            entries.associateBy { it.code }

        /**
         * Returns the [ZipMethod] for the given code or null if the method is not known.
         */
        fun getMethodByCode(code: Int): ZipMethod? =
            codeToEnum[code]
    }
}
