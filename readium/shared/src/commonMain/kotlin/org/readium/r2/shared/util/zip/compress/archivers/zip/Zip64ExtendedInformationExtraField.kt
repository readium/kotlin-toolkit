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
// (Java source: `util/zip/compress/archivers/zip/Zip64ExtendedInformationExtraField.java`,
// phase 05b).

package org.readium.r2.shared.util.zip.compress.archivers.zip

import org.readium.r2.shared.util.zip.compress.utils.ByteUtils

/**
 * Holds size and other extended information for entries that use Zip64 features.
 *
 * From a read perspective the fields of the central directory entry are optional: whenever a size
 * or offset in the regular entry equals the zip64 magic value, the real value is stored here.
 */
internal class Zip64ExtendedInformationExtraField(
    var size: ZipEightByteInteger? = null,
    var compressedSize: ZipEightByteInteger? = null,
    var relativeHeaderOffset: ZipEightByteInteger? = null,
    var diskStartNumber: ZipLong? = null,
) : ZipExtraField {

    private var rawCentralDirectoryData: ByteArray? = null

    override val headerId: ZipShort
        get() = HEADER_ID

    override val localFileDataLength: ZipShort
        get() = ZipShort(if (size != null) 2 * ZipConstants.DWORD else 0)

    override val centralDirectoryLength: ZipShort
        get() = ZipShort(
            (if (size != null) ZipConstants.DWORD else 0) +
                (if (compressedSize != null) ZipConstants.DWORD else 0) +
                (if (relativeHeaderOffset != null) ZipConstants.DWORD else 0) +
                (if (diskStartNumber != null) ZipConstants.WORD else 0)
        )

    override val localFileDataData: ByteArray
        get() {
            if (size != null || compressedSize != null) {
                require(size != null && compressedSize != null) { LFH_MUST_HAVE_BOTH_SIZES_MSG }
                val data = ByteArray(2 * ZipConstants.DWORD)
                addSizes(data)
                return data
            }
            return ByteUtils.EMPTY_BYTE_ARRAY
        }

    override val centralDirectoryData: ByteArray
        get() {
            val data = ByteArray(centralDirectoryLength.value)
            var off = addSizes(data)
            relativeHeaderOffset?.let {
                it.bytes.copyInto(data, off)
                off += ZipConstants.DWORD
            }
            diskStartNumber?.let {
                it.bytes.copyInto(data, off)
                off += ZipConstants.WORD
            }
            return data
        }

    private fun addSizes(data: ByteArray): Int {
        var off = 0
        size?.let {
            it.bytes.copyInto(data, 0)
            off += ZipConstants.DWORD
        }
        compressedSize?.let {
            it.bytes.copyInto(data, off)
            off += ZipConstants.DWORD
        }
        return off
    }

    override fun parseFromLocalFileData(buffer: ByteArray, offset: Int, length: Int) {
        if (length == 0) {
            // no local file data at all, may happen if an archive only holds a ZIP64 extended
            // information extra field inside the central directory but not inside the local file
            // header
            return
        }
        if (length < 2 * ZipConstants.DWORD) {
            throw ZipException(LFH_MUST_HAVE_BOTH_SIZES_MSG)
        }
        var off = offset
        size = ZipEightByteInteger(buffer, off)
        off += ZipConstants.DWORD
        compressedSize = ZipEightByteInteger(buffer, off)
        off += ZipConstants.DWORD
        var remaining = length - 2 * ZipConstants.DWORD
        if (remaining >= ZipConstants.DWORD) {
            relativeHeaderOffset = ZipEightByteInteger(buffer, off)
            off += ZipConstants.DWORD
            remaining -= ZipConstants.DWORD
        }
        if (remaining >= ZipConstants.WORD) {
            diskStartNumber = ZipLong(buffer, off)
        }
    }

    override fun parseFromCentralDirectoryData(buffer: ByteArray, offset: Int, length: Int) {
        // store for processing in reparseCentralDirectoryData
        rawCentralDirectoryData = buffer.copyOfRange(offset, offset + length)

        // if there is no size information in here, we are screwed and can only hope things will
        // get resolved by LFH data later. But there are some cases that can be detected:
        // * all data is there
        // * length == 24 -> both sizes and offset
        // * length % 8 == 4 -> at least we can identify the diskStart field
        when {
            length >= 3 * ZipConstants.DWORD + ZipConstants.WORD ->
                parseFromLocalFileData(buffer, offset, length)
            length == 3 * ZipConstants.DWORD -> {
                size = ZipEightByteInteger(buffer, offset)
                compressedSize = ZipEightByteInteger(buffer, offset + ZipConstants.DWORD)
                relativeHeaderOffset = ZipEightByteInteger(buffer, offset + 2 * ZipConstants.DWORD)
            }
            length % ZipConstants.DWORD == ZipConstants.WORD ->
                diskStartNumber = ZipLong(buffer, offset + length - ZipConstants.WORD)
        }
    }

    /**
     * Parses the raw bytes read from the central directory extra field with knowledge which
     * fields are expected to be there.
     *
     * All four fields inside the zip64 extended information extra field are optional and must
     * only be present if their corresponding entry inside the central directory contains the
     * correct magic value.
     */
    fun reparseCentralDirectoryData(
        hasUncompressedSize: Boolean,
        hasCompressedSize: Boolean,
        hasRelativeHeaderOffset: Boolean,
        hasDiskStart: Boolean,
    ) {
        val rawCentralDirectoryData = rawCentralDirectoryData ?: return
        val expectedLength = (if (hasUncompressedSize) ZipConstants.DWORD else 0)
            .plus(if (hasCompressedSize) ZipConstants.DWORD else 0)
            .plus(if (hasRelativeHeaderOffset) ZipConstants.DWORD else 0)
            .plus(if (hasDiskStart) ZipConstants.WORD else 0)
        if (rawCentralDirectoryData.size < expectedLength) {
            throw ZipException(
                "Central directory zip64 extended information extra field's length doesn't " +
                    "match central directory data.  Expected length $expectedLength but is " +
                    "${rawCentralDirectoryData.size}"
            )
        }
        var offset = 0
        if (hasUncompressedSize) {
            size = ZipEightByteInteger(rawCentralDirectoryData, offset)
            offset += ZipConstants.DWORD
        }
        if (hasCompressedSize) {
            compressedSize = ZipEightByteInteger(rawCentralDirectoryData, offset)
            offset += ZipConstants.DWORD
        }
        if (hasRelativeHeaderOffset) {
            relativeHeaderOffset = ZipEightByteInteger(rawCentralDirectoryData, offset)
            offset += ZipConstants.DWORD
        }
        if (hasDiskStart) {
            diskStartNumber = ZipLong(rawCentralDirectoryData, offset)
        }
    }

    companion object {
        val HEADER_ID: ZipShort = ZipShort(0x0001)

        private const val LFH_MUST_HAVE_BOTH_SIZES_MSG =
            "Zip64 extended information must contain both size values in the local file header."
    }
}
