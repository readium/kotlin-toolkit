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
// (Java source: `util/zip/compress/archivers/zip/ZipShort.java`, phase 05b).

package org.readium.r2.shared.util.zip.compress.archivers.zip

import org.readium.r2.shared.util.zip.compress.utils.ByteUtils

/**
 * Utility class that represents a two byte integer with conversion rules for the little endian
 * byte order of ZIP files.
 */
internal class ZipShort(val value: Int) {

    /** Creates instance from the two bytes starting at [offset]. */
    constructor(bytes: ByteArray, offset: Int = 0) : this(getValue(bytes, offset))

    /** The value as a two bytes array in big endian byte order. */
    val bytes: ByteArray
        get() = getBytes(value)

    override fun equals(other: Any?): Boolean =
        other is ZipShort && value == other.value

    override fun hashCode(): Int = value

    override fun toString(): String = "ZipShort value: $value"

    companion object {

        val ZERO: ZipShort = ZipShort(0)

        /** Gets the value as a two bytes array in big endian byte order. */
        fun getBytes(value: Int): ByteArray {
            val result = ByteArray(2)
            putShort(value, result, 0)
            return result
        }

        /** Helper method to get the value as a Java int from two bytes starting at [offset]. */
        fun getValue(bytes: ByteArray, offset: Int = 0): Int =
            ByteUtils.fromLittleEndian(bytes, offset, 2).toInt()

        /** Puts the value into the [buf] as a little endian array of 2 bytes. */
        fun putShort(value: Int, buf: ByteArray, offset: Int) {
            ByteUtils.toLittleEndian(buf, value.toLong(), offset, 2)
        }
    }
}
