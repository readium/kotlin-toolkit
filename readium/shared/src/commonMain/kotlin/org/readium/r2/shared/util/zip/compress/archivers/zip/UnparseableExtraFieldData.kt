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
// (Java source: `util/zip/compress/archivers/zip/UnparseableExtraFieldData.java`, phase 05b).

package org.readium.r2.shared.util.zip.compress.archivers.zip

/**
 * Wrapper for extra field data that doesn't conform to the recommended format of header-tag +
 * size + data.
 */
internal class UnparseableExtraFieldData : ZipExtraField {

    private var localData: ByteArray? = null

    private var centralData: ByteArray? = null

    override val headerId: ZipShort
        get() = HEADER_ID

    override val localFileDataLength: ZipShort
        get() = ZipShort(localData?.size ?: 0)

    override val centralDirectoryLength: ZipShort
        get() = centralData?.let { ZipShort(it.size) } ?: localFileDataLength

    override val localFileDataData: ByteArray?
        get() = localData?.copyOf()

    override val centralDirectoryData: ByteArray?
        get() = centralData?.copyOf() ?: localFileDataData

    override fun parseFromLocalFileData(buffer: ByteArray, offset: Int, length: Int) {
        localData = buffer.copyOfRange(offset, offset + length)
    }

    override fun parseFromCentralDirectoryData(buffer: ByteArray, offset: Int, length: Int) {
        centralData = buffer.copyOfRange(offset, offset + length)
        if (localData == null) {
            parseFromLocalFileData(buffer, offset, length)
        }
    }

    companion object {
        private val HEADER_ID = ZipShort(0xACC1)
    }
}
