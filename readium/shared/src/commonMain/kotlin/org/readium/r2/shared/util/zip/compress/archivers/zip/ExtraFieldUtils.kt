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
// (Java source: `util/zip/compress/archivers/zip/ExtraFieldUtils.java`, phase 05b).
// The reflective implementation registry was replaced with a hardcoded factory for the extra
// fields kept by the read-only subset. Note that the vendored Java registry was empty (the
// implementations had been stripped), so the legacy code parsed every extra field as
// [UnrecognizedExtraField]; this port restores the typed parsing of the zip64, Unicode and
// resource alignment fields (required to read zip64 archives).

package org.readium.r2.shared.util.zip.compress.archivers.zip

/**
 * ZipExtraField related methods.
 */
internal object ExtraFieldUtils {

    private const val WORD = 4

    val EMPTY_ZIP_EXTRA_FIELD_ARRAY: Array<ZipExtraField> = emptyArray()

    /**
     * Creates an instance of the appropriate ExtraField for the [headerId], or an instance of
     * [UnrecognizedExtraField] if the header id is unsupported.
     */
    fun createExtraField(headerId: ZipShort): ZipExtraField =
        createExtraFieldNoDefault(headerId)
            ?: UnrecognizedExtraField().apply { setHeaderId(headerId) }

    /**
     * Creates an instance of the appropriate [ZipExtraField], or null if the header id is
     * unsupported.
     */
    fun createExtraFieldNoDefault(headerId: ZipShort): ZipExtraField? =
        when (headerId) {
            Zip64ExtendedInformationExtraField.HEADER_ID -> Zip64ExtendedInformationExtraField()
            UnicodePathExtraField.UPATH_ID -> UnicodePathExtraField()
            UnicodeCommentExtraField.UCOM_ID -> UnicodeCommentExtraField()
            ResourceAlignmentExtraField.ID -> ResourceAlignmentExtraField()
            else -> null
        }

    /**
     * Fills in the extra field data into the given instance.
     *
     * @param ze the extra field instance to fill
     * @param data the array of extra field data
     * @param off offset into data where this field's data starts
     * @param len the length of this field's data
     * @param local whether the extra field data stems from the local file header
     * @return the filled field, will never be null
     * @throws ZipException if the data crashes this field
     */
    fun fillExtraField(
        ze: ZipExtraField,
        data: ByteArray,
        off: Int,
        len: Int,
        local: Boolean,
    ): ZipExtraField {
        try {
            if (local) {
                ze.parseFromLocalFileData(data, off, len)
            } else {
                ze.parseFromCentralDirectoryData(data, off, len)
            }
            return ze
        } catch (e: IndexOutOfBoundsException) {
            throw ZipException(
                "Failed to parse corrupt ZIP extra field of type " +
                    ze.headerId.value.toString(16),
                e
            )
        }
    }

    /**
     * Merges the central directory fields of the given ZipExtraFields.
     */
    fun mergeCentralDirectoryData(data: Array<ZipExtraField>): ByteArray =
        merge(data) { field -> field.centralDirectoryData to field.centralDirectoryLength }

    /**
     * Merges the local file data fields of the given ZipExtraFields.
     */
    fun mergeLocalFileDataData(data: Array<ZipExtraField>): ByteArray =
        merge(data) { field -> field.localFileDataData to field.localFileDataLength }

    private inline fun merge(
        data: Array<ZipExtraField>,
        payload: (ZipExtraField) -> Pair<ByteArray?, ZipShort>,
    ): ByteArray {
        val dataLength = data.size
        val lastIsUnparseableHolder = dataLength > 0 &&
            data[dataLength - 1] is UnparseableExtraFieldData
        val regularExtraFieldCount =
            if (lastIsUnparseableHolder) dataLength - 1 else dataLength

        // Materialize each field's payload only once: the accessors may rebuild the byte arrays
        // on every call.
        val payloads = data.map(payload)

        var sum = WORD * regularExtraFieldCount
        for ((_, length) in payloads) {
            sum += length.value
        }
        val result = ByteArray(sum)
        var start = 0
        for (i in 0 until regularExtraFieldCount) {
            val (bytes, length) = payloads[i]
            data[i].headerId.bytes.copyInto(result, start)
            length.bytes.copyInto(result, start + 2)
            start += WORD
            if (bytes != null) {
                bytes.copyInto(result, start)
                start += bytes.size
            }
        }
        if (lastIsUnparseableHolder) {
            payloads[dataLength - 1].first?.copyInto(result, start)
        }
        return result
    }

    /**
     * Parses the given bytes as extra field data and returns an array of [ZipExtraField]
     * instances.
     *
     * @param data an array of bytes
     * @param local whether data originates from the local file data or the central directory
     * @param parsingBehavior controls parsing of extra fields.
     * @throws ZipException on error
     */
    fun parse(
        data: ByteArray,
        local: Boolean = true,
        parsingBehavior: ExtraFieldParsingBehavior = defaultParsingBehavior(UnparseableExtraField.THROW),
    ): Array<ZipExtraField> {
        val v = mutableListOf<ZipExtraField>()
        var start = 0
        val dataLength = data.size
        while (start <= dataLength - WORD) {
            val headerId = ZipShort(data, start)
            val length = ZipShort(data, start + 2).value
            if (start + WORD + length > dataLength) {
                val field = parsingBehavior.onUnparseableExtraField(
                    data,
                    start,
                    dataLength - start,
                    local,
                    length
                )
                if (field != null) {
                    v.add(field)
                }
                // since we cannot parse the data we must assume the extra field consumes the
                // whole rest of the available data
                break
            }
            val ze = parsingBehavior.createExtraField(headerId)
            v.add(parsingBehavior.fill(ze, data, start + WORD, length, local))
            start += length + WORD
        }
        return v.toTypedArray()
    }

    /**
     * Parses the given bytes as extra field data and returns an array of [ZipExtraField]
     * instances.
     *
     * @param data an array of bytes
     * @param local whether data originates from the local file data or the central directory
     * @param onUnparseableData what to do if the extra field data cannot be parsed.
     * @throws ZipException on error
     */
    fun parse(
        data: ByteArray,
        local: Boolean,
        onUnparseableData: UnparseableExtraField,
    ): Array<ZipExtraField> =
        parse(data, local, defaultParsingBehavior(onUnparseableData))

    private fun defaultParsingBehavior(
        onUnparseableData: UnparseableExtraField,
    ): ExtraFieldParsingBehavior =
        object : ExtraFieldParsingBehavior {
            override fun createExtraField(headerId: ZipShort): ZipExtraField =
                ExtraFieldUtils.createExtraField(headerId)

            override fun fill(
                field: ZipExtraField,
                data: ByteArray,
                off: Int,
                len: Int,
                local: Boolean,
            ): ZipExtraField =
                fillExtraField(field, data, off, len, local)

            override fun onUnparseableExtraField(
                data: ByteArray,
                off: Int,
                len: Int,
                local: Boolean,
                claimedLength: Int,
            ): ZipExtraField? =
                onUnparseableData.onUnparseableExtraField(data, off, len, local, claimedLength)
        }

    /**
     * "enum" for the possible actions to take if the extra field cannot be parsed.
     */
    class UnparseableExtraField private constructor(val key: Int) : UnparseableExtraFieldBehavior {

        override fun onUnparseableExtraField(
            data: ByteArray,
            off: Int,
            len: Int,
            local: Boolean,
            claimedLength: Int,
        ): ZipExtraField? =
            when (key) {
                THROW_KEY -> throw ZipException(
                    "Bad extra field starting at $off.  Block length of $claimedLength bytes " +
                        "exceeds remaining data of ${len - WORD} bytes."
                )
                READ_KEY -> UnparseableExtraFieldData().also { field ->
                    if (local) {
                        field.parseFromLocalFileData(data, off, len)
                    } else {
                        field.parseFromCentralDirectoryData(data, off, len)
                    }
                }
                SKIP_KEY -> null
                else -> throw ZipException("Unknown UnparseableExtraField key: $key")
            }

        companion object {
            /** Key for "throw an exception" action. */
            const val THROW_KEY: Int = 0

            /** Key for "skip" action. */
            const val SKIP_KEY: Int = 1

            /** Key for "read" action. */
            const val READ_KEY: Int = 2

            /** Throw an exception if field cannot be parsed. */
            val THROW: UnparseableExtraField = UnparseableExtraField(THROW_KEY)

            /** Skip the extra field entirely and don't make its data available. */
            val SKIP: UnparseableExtraField = UnparseableExtraField(SKIP_KEY)

            /** Read the extra field data into an instance of [UnparseableExtraFieldData]. */
            val READ: UnparseableExtraField = UnparseableExtraField(READ_KEY)
        }
    }
}
