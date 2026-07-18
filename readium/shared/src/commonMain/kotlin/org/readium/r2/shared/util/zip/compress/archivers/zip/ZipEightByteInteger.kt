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
// (Java source: `util/zip/compress/archivers/zip/ZipEightByteInteger.java`, phase 05b).
// The `java.math.BigInteger` backing was replaced with a plain [Long] carrying the full eight
// bytes in two's complement: the bytes <-> value round-trip is lossless, only the textual
// representation of values >= 2^63 differs (rendered unsigned through [ULong] here).

package org.readium.r2.shared.util.zip.compress.archivers.zip

import org.readium.r2.shared.util.zip.compress.utils.ByteUtils

/**
 * Utility class that represents an eight byte integer with conversion rules for the little endian
 * byte order of ZIP files.
 */
internal class ZipEightByteInteger(val longValue: Long) {

    /** Creates instance from the eight bytes starting at [offset]. */
    constructor(bytes: ByteArray, offset: Int = 0) : this(getLongValue(bytes, offset))

    /** The value as an eight bytes array in big endian byte order. */
    val bytes: ByteArray
        get() = getBytes(longValue)

    override fun equals(other: Any?): Boolean =
        other is ZipEightByteInteger && longValue == other.longValue

    override fun hashCode(): Int = longValue.hashCode()

    override fun toString(): String = "ZipEightByteInteger value: ${longValue.toULong()}"

    companion object {

        val ZERO: ZipEightByteInteger = ZipEightByteInteger(0)

        /** Gets the value as an eight bytes array in big endian byte order. */
        fun getBytes(value: Long): ByteArray {
            val result = ByteArray(ZipConstants.DWORD)
            ByteUtils.toLittleEndian(result, value, 0, ZipConstants.DWORD)
            return result
        }

        /** Helper method to get the value as a Java long from eight bytes starting at [offset]. */
        fun getLongValue(bytes: ByteArray, offset: Int = 0): Long =
            ByteUtils.fromLittleEndian(bytes, offset, ZipConstants.DWORD)
    }
}
