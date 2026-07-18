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
// (Java source: `util/zip/compress/archivers/zip/GeneralPurposeBit.java`, phase 05b).

package org.readium.r2.shared.util.zip.compress.archivers.zip

/**
 * Parser/encoder for the "general purpose bit" field in ZIP's local file and central directory
 * headers.
 */
internal class GeneralPurposeBit {

    private var languageEncodingFlag = false

    private var dataDescriptorFlag = false

    private var encryptionFlag = false

    private var strongEncryptionFlag = false

    /** The sliding dictionary size used by the compression method 6 (imploding). */
    var slidingDictionarySize: Int = 0
        private set

    /** The number of trees used by the compression method 6 (imploding). */
    var numberOfShannonFanoTrees: Int = 0
        private set

    /** Returns true if the entry uses UTF8 for file name and comment. */
    fun usesUTF8ForNames(): Boolean = languageEncodingFlag

    /** Whether the entry will use UTF8 for file name and comment. */
    fun useUTF8ForNames(b: Boolean) {
        languageEncodingFlag = b
    }

    /**
     * Returns true if the entry uses a data descriptor to store CRC and size information.
     */
    fun usesDataDescriptor(): Boolean = dataDescriptorFlag

    /** Whether the entry will use a data descriptor to store CRC and size information. */
    fun useDataDescriptor(b: Boolean) {
        dataDescriptorFlag = b
    }

    /** Returns true if the entry is encrypted. */
    fun usesEncryption(): Boolean = encryptionFlag

    /** Whether the entry is encrypted. */
    fun useEncryption(b: Boolean) {
        encryptionFlag = b
    }

    /** Returns true if the entry is encrypted using strong encryption. */
    fun usesStrongEncryption(): Boolean = encryptionFlag && strongEncryptionFlag

    /** Whether the entry is encrypted using strong encryption. */
    fun useStrongEncryption(b: Boolean) {
        strongEncryptionFlag = b
        if (b) {
            useEncryption(true)
        }
    }

    /** Encodes the set bits in a form suitable for ZIP archives. */
    fun encode(): ByteArray {
        val result = ByteArray(2)
        encode(result, 0)
        return result
    }

    /**
     * Encodes the set bits in a form suitable for ZIP archives.
     *
     * @param buf the output buffer
     * @param offset the offset of the first byte to write into
     */
    fun encode(buf: ByteArray, offset: Int) {
        ZipShort.putShort(
            (if (dataDescriptorFlag) DATA_DESCRIPTOR_FLAG else 0)
                .or(if (languageEncodingFlag) UFT8_NAMES_FLAG else 0)
                .or(if (encryptionFlag) ENCRYPTION_FLAG else 0)
                .or(if (strongEncryptionFlag) STRONG_ENCRYPTION_FLAG else 0),
            buf,
            offset
        )
    }

    override fun equals(other: Any?): Boolean =
        other is GeneralPurposeBit &&
            other.encryptionFlag == encryptionFlag &&
            other.strongEncryptionFlag == strongEncryptionFlag &&
            other.languageEncodingFlag == languageEncodingFlag &&
            other.dataDescriptorFlag == dataDescriptorFlag

    override fun hashCode(): Int =
        3 * (
            7 * (
                13 * (
                    17 * (if (encryptionFlag) 1 else 0) +
                        (if (strongEncryptionFlag) 1 else 0)
                    ) + (if (languageEncodingFlag) 1 else 0)
                ) + (if (dataDescriptorFlag) 1 else 0)
            )

    fun copy(): GeneralPurposeBit {
        val b = GeneralPurposeBit()
        b.languageEncodingFlag = languageEncodingFlag
        b.dataDescriptorFlag = dataDescriptorFlag
        b.encryptionFlag = encryptionFlag
        b.strongEncryptionFlag = strongEncryptionFlag
        b.slidingDictionarySize = slidingDictionarySize
        b.numberOfShannonFanoTrees = numberOfShannonFanoTrees
        return b
    }

    companion object {

        /** Indicates that the file is encrypted. */
        private const val ENCRYPTION_FLAG = 1 shl 0

        /** Indicates the size of the sliding dictionary used by the compression method 6. */
        private const val SLIDING_DICTIONARY_SIZE_FLAG = 1 shl 1

        /** Indicates the number of Shannon-Fano trees used by the compression method 6. */
        private const val NUMBER_OF_SHANNON_FANO_TREES_FLAG = 1 shl 2

        /**
         * Indicates that a data descriptor stored after the file contents will hold CRC and size
         * information.
         */
        private const val DATA_DESCRIPTOR_FLAG = 1 shl 3

        /** Indicates strong encryption. */
        private const val STRONG_ENCRYPTION_FLAG = 1 shl 6

        /** Indicates that file names are written in UTF-8. */
        const val UFT8_NAMES_FLAG: Int = 1 shl 11

        /**
         * Parses the supported flags from the given archive data.
         *
         * @param data local file header or a central directory entry.
         * @param offset offset at which the general purpose bit starts
         */
        fun parse(data: ByteArray, offset: Int): GeneralPurposeBit {
            val generalPurposeFlag = ZipShort.getValue(data, offset)
            val b = GeneralPurposeBit()
            b.useDataDescriptor(generalPurposeFlag and DATA_DESCRIPTOR_FLAG != 0)
            b.useUTF8ForNames(generalPurposeFlag and UFT8_NAMES_FLAG != 0)
            b.useStrongEncryption(generalPurposeFlag and STRONG_ENCRYPTION_FLAG != 0)
            b.useEncryption(generalPurposeFlag and ENCRYPTION_FLAG != 0)
            b.slidingDictionarySize =
                if (generalPurposeFlag and SLIDING_DICTIONARY_SIZE_FLAG != 0) 8192 else 4096
            b.numberOfShannonFanoTrees =
                if (generalPurposeFlag and NUMBER_OF_SHANNON_FANO_TREES_FLAG != 0) 3 else 2
            return b
        }
    }
}
