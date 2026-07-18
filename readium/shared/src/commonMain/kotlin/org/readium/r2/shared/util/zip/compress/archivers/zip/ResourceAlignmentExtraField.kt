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
// (Java source: `util/zip/compress/archivers/zip/ResourceAlignmentExtraField.java`, phase 05b).

package org.readium.r2.shared.util.zip.compress.archivers.zip

/**
 * An extra field who's sole purpose is to align and pad the local file header so that the entry's
 * data starts at a certain position.
 */
internal class ResourceAlignmentExtraField() : ZipExtraField {

    /** Requested alignment. */
    var alignment: Short = 0
        private set

    /** Indicates whether method change is allowed when re-compressing the zip file. */
    var allowMethodChange: Boolean = false
        private set

    private var padding: Int = 0

    constructor(alignment: Int, allowMethodChange: Boolean = false, padding: Int = 0) : this() {
        require(alignment in 0..0x7fff) {
            "Alignment must be between 0 and 0x7fff, was: $alignment"
        }
        require(padding >= 0) { "Padding must not be negative, was: $padding" }
        this.alignment = alignment.toShort()
        this.allowMethodChange = allowMethodChange
        this.padding = padding
    }

    override val headerId: ZipShort
        get() = ID

    override val localFileDataLength: ZipShort
        get() = ZipShort(BASE_SIZE + padding)

    override val centralDirectoryLength: ZipShort
        get() = ZipShort(BASE_SIZE)

    override val localFileDataData: ByteArray
        get() {
            val content = ByteArray(BASE_SIZE + padding)
            ZipShort.putShort(
                alignment.toInt() or (if (allowMethodChange) ALLOW_METHOD_MESSAGE_CHANGE_FLAG else 0),
                content,
                0
            )
            return content
        }

    override val centralDirectoryData: ByteArray
        get() = ZipShort.getBytes(
            alignment.toInt() or (if (allowMethodChange) ALLOW_METHOD_MESSAGE_CHANGE_FLAG else 0)
        )

    override fun parseFromCentralDirectoryData(buffer: ByteArray, offset: Int, length: Int) {
        if (length < BASE_SIZE) {
            throw ZipException("Too short content for ResourceAlignmentExtraField (0xa11e): $length")
        }
        val alignmentValue = ZipShort.getValue(buffer, offset)
        this.alignment = (alignmentValue and (ALLOW_METHOD_MESSAGE_CHANGE_FLAG - 1)).toShort()
        this.allowMethodChange = alignmentValue and ALLOW_METHOD_MESSAGE_CHANGE_FLAG != 0
    }

    override fun parseFromLocalFileData(buffer: ByteArray, offset: Int, length: Int) {
        parseFromCentralDirectoryData(buffer, offset, length)
        this.padding = length - BASE_SIZE
    }

    companion object {
        val ID: ZipShort = ZipShort(0xa11e)

        const val BASE_SIZE: Int = 2

        private const val ALLOW_METHOD_MESSAGE_CHANGE_FLAG = 0x8000
    }
}
