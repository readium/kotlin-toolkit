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
// (Java source: `util/zip/compress/archivers/zip/UnrecognizedExtraField.java`, phase 05b).

package org.readium.r2.shared.util.zip.compress.archivers.zip

/**
 * Simple placeholder for all those extra fields we don't want to deal with.
 *
 * Assumes local file data and central directory entries are identical - unless told the opposite.
 */
internal class UnrecognizedExtraField : ZipExtraField {

    private var _headerId: ZipShort? = null

    private var localData: ByteArray? = null

    private var centralData: ByteArray? = null

    override val headerId: ZipShort
        get() = checkNotNull(_headerId) { "The header id has not been set." }

    fun setHeaderId(headerId: ZipShort) {
        _headerId = headerId
    }

    override val localFileDataLength: ZipShort
        get() = ZipShort(localData?.size ?: 0)

    override val centralDirectoryLength: ZipShort
        get() = centralData?.let { ZipShort(it.size) } ?: localFileDataLength

    override val localFileDataData: ByteArray?
        get() = localData?.copyOf()

    override val centralDirectoryData: ByteArray?
        get() = centralData?.copyOf() ?: localFileDataData

    fun setLocalFileDataData(data: ByteArray?) {
        localData = data?.copyOf()
    }

    fun setCentralDirectoryData(data: ByteArray?) {
        centralData = data?.copyOf()
    }

    override fun parseFromLocalFileData(buffer: ByteArray, offset: Int, length: Int) {
        setLocalFileDataData(buffer.copyOfRange(offset, offset + length))
    }

    override fun parseFromCentralDirectoryData(buffer: ByteArray, offset: Int, length: Int) {
        val tmp = buffer.copyOfRange(offset, offset + length)
        setCentralDirectoryData(tmp)
        if (localData == null) {
            setLocalFileDataData(tmp)
        }
    }
}
