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
// (Java sources: `AbstractUnicodeExtraField.java`, `UnicodePathExtraField.java` and
// `UnicodeCommentExtraField.java`, phase 05b).

package org.readium.r2.shared.util.zip.compress.archivers.zip

import org.readium.r2.shared.util.zip.compress.utils.Crc32

/**
 * A common base class for Unicode extra information extra fields.
 */
internal abstract class AbstractUnicodeExtraField : ZipExtraField {

    /** The CRC32 checksum of the file name or comment as encoded in the central directory. */
    var nameCRC32: Long = 0
        private set

    private var _unicodeName: ByteArray? = null

    private var data: ByteArray? = null

    protected constructor()

    /**
     * Assembles as unicode extension from the name/comment and encoding of the original zip entry.
     *
     * @param text the file name or comment.
     * @param bytes the bytes actually written to the archive.
     */
    protected constructor(text: String, bytes: ByteArray, off: Int = 0, len: Int = bytes.size) {
        val crc32 = Crc32()
        crc32.update(bytes, off, len)
        nameCRC32 = crc32.value
        _unicodeName = text.encodeToByteArray()
    }

    /** The UTF-8 encoded name. */
    val unicodeName: ByteArray?
        get() = _unicodeName?.copyOf()

    private fun assembleData(): ByteArray? {
        val unicodeName = _unicodeName ?: return null
        val data = ByteArray(5 + unicodeName.size)
        // version 1
        data[0] = 0x01
        ZipLong.getBytes(nameCRC32).copyInto(data, 1)
        unicodeName.copyInto(data, 5)
        this.data = data
        return data
    }

    override val centralDirectoryData: ByteArray?
        get() = (data ?: assembleData())?.copyOf()

    override val centralDirectoryLength: ZipShort
        get() = ZipShort((data ?: assembleData())?.size ?: 0)

    override val localFileDataData: ByteArray?
        get() = centralDirectoryData

    override val localFileDataLength: ZipShort
        get() = centralDirectoryLength

    override fun parseFromCentralDirectoryData(buffer: ByteArray, offset: Int, length: Int) {
        parseFromLocalFileData(buffer, offset, length)
    }

    override fun parseFromLocalFileData(buffer: ByteArray, offset: Int, length: Int) {
        if (length < 5) {
            throw ZipException("UniCode path extra data must have at least 5 bytes.")
        }

        val version = buffer[offset].toInt()
        if (version != 0x01) {
            throw ZipException("Unsupported version [$version] for UniCode path extra data.")
        }

        nameCRC32 = ZipLong.getValue(buffer, offset + 1)
        _unicodeName = buffer.copyOfRange(offset + 5, offset + length)
        data = null
    }
}

/**
 * Info-ZIP Unicode Path Extra Field (0x7075).
 *
 * Stores the UTF-8 version of the file name field as stored in the local header and central
 * directory header.
 */
internal class UnicodePathExtraField : AbstractUnicodeExtraField {

    constructor() : super()

    /**
     * Assembles as unicode path extension from the name given as text as well as the encoded
     * bytes actually written to the archive.
     */
    constructor(name: String, bytes: ByteArray, off: Int = 0, len: Int = bytes.size) :
        super(name, bytes, off, len)

    override val headerId: ZipShort
        get() = UPATH_ID

    companion object {
        val UPATH_ID: ZipShort = ZipShort(0x7075)
    }
}

/**
 * Info-ZIP Unicode Comment Extra Field (0x6375).
 *
 * Stores the UTF-8 version of the file comment as stored in the central directory header.
 */
internal class UnicodeCommentExtraField : AbstractUnicodeExtraField {

    constructor() : super()

    /**
     * Assembles as unicode comment extension from the comment given as text as well as the
     * encoded bytes actually written to the archive.
     */
    constructor(comment: String, bytes: ByteArray, off: Int = 0, len: Int = bytes.size) :
        super(comment, bytes, off, len)

    override val headerId: ZipShort
        get() = UCOM_ID

    companion object {
        val UCOM_ID: ZipShort = ZipShort(0x6375)
    }
}
