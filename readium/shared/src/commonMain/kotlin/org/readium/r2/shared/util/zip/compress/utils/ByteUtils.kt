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
// (Java source: `util/zip/compress/utils/ByteUtils.java`, phase 05b of the KMP migration).
// The stream/supplier/consumer variants were dropped: only the array-based helpers are used by
// the read-only zip stack.

package org.readium.r2.shared.util.zip.compress.utils

/**
 * Utility methods for reading and writing bytes.
 */
internal object ByteUtils {

    /** Empty array. */
    val EMPTY_BYTE_ARRAY: ByteArray = ByteArray(0)

    private fun checkReadLength(length: Int) {
        require(length <= 8) { "Can't read more than eight bytes into a long value" }
    }

    /**
     * Reads the given byte array as a little endian long.
     *
     * @param off the offset into the array that starts the value
     * @param length the number of bytes representing the value
     */
    fun fromLittleEndian(bytes: ByteArray, off: Int = 0, length: Int = bytes.size): Long {
        checkReadLength(length)
        var l = 0L
        for (i in 0 until length) {
            l = l or ((bytes[off + i].toLong() and 0xff) shl (8 * i))
        }
        return l
    }

    /**
     * Inserts the given value into the array as a little endian sequence of the given length
     * starting at the given offset.
     */
    fun toLittleEndian(b: ByteArray, value: Long, off: Int, length: Int) {
        var num = value
        for (i in 0 until length) {
            b[off + i] = (num and 0xff).toByte()
            num = num shr 8
        }
    }
}
