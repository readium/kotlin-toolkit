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
// (Java source: `util/zip/compress/archivers/zip/ZipLong.java`, phase 05b).

package org.readium.r2.shared.util.zip.compress.archivers.zip

import org.readium.r2.shared.util.zip.compress.utils.ByteUtils

/**
 * Utility class that represents a four byte integer with conversion rules for the little endian
 * byte order of ZIP files.
 */
internal class ZipLong(val value: Long) {

    constructor(value: Int) : this(value.toLong())

    /** Creates instance from the four bytes starting at [offset]. */
    constructor(bytes: ByteArray, offset: Int = 0) : this(getValue(bytes, offset))

    /** The value as a four bytes array in big endian byte order. */
    val bytes: ByteArray
        get() = getBytes(value)

    val intValue: Int
        get() = value.toInt()

    override fun equals(other: Any?): Boolean =
        other is ZipLong && value == other.value

    override fun hashCode(): Int = value.toInt()

    override fun toString(): String = "ZipLong value: $value"

    companion object {

        /** Central File Header Signature. */
        val CFH_SIG: ZipLong = ZipLong(0X02014B50L)

        /** Local File Header Signature. */
        val LFH_SIG: ZipLong = ZipLong(0X04034B50L)

        /** Data Descriptor signature. */
        val DD_SIG: ZipLong = ZipLong(0X08074B50L)

        /** Value stored in size and similar fields if actual value exceeds their limit. */
        val ZIP64_MAGIC: ZipLong = ZipLong(ZipConstants.ZIP64_MAGIC)

        /**
         * Marks ZIP archives that were supposed to be split or spanned but only needed a single
         * segment in the end.
         */
        val SINGLE_SEGMENT_SPLIT_MARKER: ZipLong = ZipLong(0X30304B50L)

        /** Archive extra data record signature. */
        val AED_SIG: ZipLong = ZipLong(0X08064B50L)

        /** Gets the value as a four bytes array in big endian byte order. */
        fun getBytes(value: Long): ByteArray {
            val result = ByteArray(ZipConstants.WORD)
            putLong(value, result, 0)
            return result
        }

        /** Helper method to get the value as a Java long from four bytes starting at [offset]. */
        fun getValue(bytes: ByteArray, offset: Int = 0): Long =
            ByteUtils.fromLittleEndian(bytes, offset, 4)

        /** Puts the value into the [buf] as a little endian array of 4 bytes. */
        fun putLong(value: Long, buf: ByteArray, offset: Int) {
            ByteUtils.toLittleEndian(buf, value, offset, 4)
        }
    }
}
