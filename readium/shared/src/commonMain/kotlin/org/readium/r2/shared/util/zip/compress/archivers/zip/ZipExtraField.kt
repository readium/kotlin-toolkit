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
// (Java source: `util/zip/compress/archivers/zip/ZipExtraField.java`, phase 05b).

package org.readium.r2.shared.util.zip.compress.archivers.zip

/**
 * General format of extra field data.
 *
 * Extra fields usually appear twice per file, once in the local file data and once in the central
 * directory. Usually they are the same, but they don't have to be.
 */
internal interface ZipExtraField {

    /** The Header-ID. */
    val headerId: ZipShort

    /** The length of the local file data - without Header-ID or length specifier. */
    val localFileDataLength: ZipShort

    /** The length of the central directory data - without Header-ID or length specifier. */
    val centralDirectoryLength: ZipShort

    /** The actual data to put into local file data - without Header-ID or length specifier. */
    val localFileDataData: ByteArray?

    /** The actual data to put into central directory - without Header-ID or length specifier. */
    val centralDirectoryData: ByteArray?

    /**
     * Populates data from this array as if it was in local file data.
     *
     * @throws ZipException on error.
     */
    fun parseFromLocalFileData(buffer: ByteArray, offset: Int, length: Int)

    /**
     * Populates data from this array as if it was in central directory data.
     *
     * @throws ZipException on error.
     */
    fun parseFromCentralDirectoryData(buffer: ByteArray, offset: Int, length: Int)

    companion object {
        /** Size of an extra field header (id + length). */
        const val EXTRAFIELD_HEADER_SIZE: Int = 4
    }
}
