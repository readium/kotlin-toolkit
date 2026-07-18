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
// (Java source: `util/zip/compress/archivers/zip/ZipConstants.java`, phase 05b).

package org.readium.r2.shared.util.zip.compress.archivers.zip

/**
 * Various constants used throughout the zip package.
 */
internal object ZipConstants {

    /** Masks last eight bits. */
    const val BYTE_MASK: Int = 0xFF

    /** Length of a ZipShort in bytes. */
    const val SHORT: Int = 2

    /** Length of a ZipLong in bytes. */
    const val WORD: Int = 4

    /** Length of a ZipEightByteInteger in bytes. */
    const val DWORD: Int = 8

    /** Magic short used by zip64 in the "version needed to extract" field. */
    const val ZIP64_MAGIC_SHORT: Int = 0xFFFF

    /**
     * The magic value used by zip64 (as a long) in the "size" and "offset" fields, indicating the
     * real value is stored in the zip64 extended information extra field.
     */
    const val ZIP64_MAGIC: Long = 0xFFFFFFFFL
}
