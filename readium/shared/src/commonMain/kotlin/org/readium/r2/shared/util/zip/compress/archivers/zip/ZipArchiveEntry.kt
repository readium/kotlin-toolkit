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
// (Java source: `util/zip/compress/archivers/zip/ZipArchiveEntry.java`, phase 05b).
// The `java.util.zip.ZipEntry` base class is not available in common code: its state used by the
// read path (comment, crc, compressed size, time, extra bytes) was folded into this class. The
// `java.io.File`-based constructor, `clone()` and the `Date`-based accessors were dropped.

package org.readium.r2.shared.util.zip.compress.archivers.zip

import org.readium.r2.shared.util.zip.compress.archivers.ArchiveEntry
import org.readium.r2.shared.util.zip.compress.archivers.EntryStreamOffsets
import org.readium.r2.shared.util.zip.compress.utils.ByteUtils

/**
 * A zip archive entry with better handling of extra fields and access to the internal and
 * external file attributes.
 *
 * The extra data is expected to follow the recommendation of
 * [APPNOTE.TXT](http://www.pkware.com/documents/casestudies/APPNOTE.TXT):
 * - the extra byte array consists of a sequence of extra fields
 * - each extra field starts by a two byte header id followed by a two byte sequence holding the
 *   length of the remainder of data.
 *
 * Any extra data that cannot be parsed by the rules above will be consumed as "unparseable" extra
 * data and treated differently by the methods of this class.
 *
 * This class is not thread-safe.
 */
internal open class ZipArchiveEntry(name: String = "") : ArchiveEntry, EntryStreamOffsets {

    /** Indicates how the comment of this entry has been determined. */
    enum class CommentSource {
        /** The comment has been read from the archive using the encoding of the archive. */
        COMMENT,

        /** The comment has been read from an Unicode Comment Extra Field. */
        UNICODE_EXTRA_FIELD,
    }

    /** Indicates how the name of this entry has been determined. */
    enum class NameSource {
        /** The name has been read from the archive using the encoding of the archive. */
        NAME,

        /**
         * The name has been read from the archive and the archive specified the EFS flag which
         * indicates the name has been encoded as UTF-8.
         */
        NAME_WITH_EFS_FLAG,

        /** The name has been read from an Unicode Path Extra Field. */
        UNICODE_EXTRA_FIELD,
    }

    /**
     * How to try to parse the extra fields.
     *
     * Configures the behavior for:
     * - What shall happen if the extra field content doesn't follow the recommended pattern of
     *   two-byte id followed by a two-byte length?
     * - What shall happen if an extra field is generally supported but its content cannot be
     *   parsed correctly?
     */
    enum class ExtraFieldParsingMode(
        private val onUnparseableData: ExtraFieldUtils.UnparseableExtraField,
        private val makeUnrecognizedOnError: Boolean,
    ) : ExtraFieldParsingBehavior {
        /**
         * Try to parse as many extra fields as possible and wrap unknown extra fields as well as
         * supported extra fields that cannot be parsed in [UnrecognizedExtraField].
         *
         * Wrap extra data that doesn't follow the recommended pattern in an
         * [UnparseableExtraFieldData] instance.
         */
        BEST_EFFORT(ExtraFieldUtils.UnparseableExtraField.READ, true),

        /**
         * Try to parse as many extra fields as possible and wrap unknown extra fields in
         * [UnrecognizedExtraField].
         *
         * Wrap extra data that doesn't follow the recommended pattern in an
         * [UnparseableExtraFieldData] instance.
         *
         * Throw an exception if an extra field that is generally supported cannot be parsed.
         */
        STRICT_FOR_KNOW_EXTRA_FIELDS(ExtraFieldUtils.UnparseableExtraField.READ, false),

        /**
         * Try to parse as many extra fields as possible and wrap unknown as well as unparseable
         * extra fields in [UnrecognizedExtraField]. Ignore extra data that doesn't follow the
         * recommended pattern.
         */
        ONLY_PARSEABLE_LENIENT(ExtraFieldUtils.UnparseableExtraField.SKIP, true),

        /**
         * Try to parse as many extra fields as possible and wrap unknown extra fields in
         * [UnrecognizedExtraField]. Ignore extra data that doesn't follow the recommended
         * pattern. Throw an exception if an extra field that is generally supported cannot be
         * parsed.
         */
        ONLY_PARSEABLE_STRICT(ExtraFieldUtils.UnparseableExtraField.SKIP, false),

        /**
         * Throw an exception if any of the recognized extra fields cannot be parsed or any extra
         * field violates the recommended pattern.
         */
        DRACONIC(ExtraFieldUtils.UnparseableExtraField.THROW, false),
        ;

        override fun createExtraField(headerId: ZipShort): ZipExtraField =
            ExtraFieldUtils.createExtraField(headerId)

        override fun fill(
            field: ZipExtraField,
            data: ByteArray,
            off: Int,
            len: Int,
            local: Boolean,
        ): ZipExtraField =
            if (makeUnrecognizedOnError) {
                fillAndMakeUnrecognizedOnError(field, data, off, len, local)
            } else {
                ExtraFieldUtils.fillExtraField(field, data, off, len, local)
            }

        override fun onUnparseableExtraField(
            data: ByteArray,
            off: Int,
            len: Int,
            local: Boolean,
            claimedLength: Int,
        ): ZipExtraField? =
            onUnparseableData.onUnparseableExtraField(data, off, len, local, claimedLength)

        private companion object {

            private fun fillAndMakeUnrecognizedOnError(
                field: ZipExtraField,
                data: ByteArray,
                off: Int,
                len: Int,
                local: Boolean,
            ): ZipExtraField =
                try {
                    ExtraFieldUtils.fillExtraField(field, data, off, len, local)
                } catch (_: ZipException) {
                    UnrecognizedExtraField().apply {
                        setHeaderId(field.headerId)
                        if (local) {
                            setLocalFileDataData(data.copyOfRange(off, off + len))
                        } else {
                            setCentralDirectoryData(data.copyOfRange(off, off + len))
                        }
                    }
                }
        }
    }

    private var _name: String? = null

    private var _rawName: ByteArray? = null

    /**
     * The compression method of this entry, or -1 if the compression method has not been
     * specified.
     */
    var method: Int = ZipMethod.UNKNOWN_CODE
        set(value) {
            require(value >= 0) { "ZIP compression method can not be negative: $value" }
            field = value
        }

    /** The uncompressed size of the entry data. */
    private var _size: Long = ArchiveEntry.SIZE_UNKNOWN

    /** The compressed size of the entry data, or -1 if unknown. */
    var compressedSize: Long = ArchiveEntry.SIZE_UNKNOWN

    /** The last modification time of the entry in milliseconds since the epoch, or -1. */
    var time: Long = -1

    /** The CRC-32 checksum of the uncompressed entry data, or -1 if unknown. */
    var crc: Long = -1
        set(value) {
            require(value in 0..0xFFFFFFFFL) { "invalid entry crc-32: $value" }
            field = value
        }

    /** The optional comment of the entry. */
    var comment: String? = null

    /** The internal file attributes. */
    var internalAttributes: Int = 0

    /** The "version required to expand" field. */
    var versionRequired: Int = 0

    /** The "version made by" field. */
    var versionMadeBy: Int = 0

    /**
     * The platform specification of the "version made by" part of the central file header:
     * [PLATFORM_FAT] unless [setUnixMode] has been called, in which case [PLATFORM_UNIX] will be
     * returned.
     */
    var platform: Int = PLATFORM_FAT

    /** The content of the flags field. */
    var rawFlag: Int = 0

    /** The external file attributes. */
    var externalAttributes: Long = 0

    /** Alignment for this entry. */
    var alignment: Int = 0
        set(value) {
            require(value and (value - 1) == 0 && value <= 0xffff) {
                "Invalid value for alignment, must be power of two and no bigger than " +
                    "${0xffff} but is $value"
            }
            field = value
        }

    private var extraFields: Array<ZipExtraField>? = null

    private var unparseableExtra: UnparseableExtraFieldData? = null

    /** The raw local file data extra bytes. */
    private var extra: ByteArray? = null

    /** The "general purpose bit" field. */
    var generalPurposeBit: GeneralPurposeBit = GeneralPurposeBit()

    /** The local header offset, or [EntryStreamOffsets.OFFSET_UNKNOWN]. */
    var localHeaderOffset: Long = EntryStreamOffsets.OFFSET_UNKNOWN

    private var _dataOffset: Long = EntryStreamOffsets.OFFSET_UNKNOWN

    private var _isStreamContiguous: Boolean = false

    /** The source of the name field value. */
    var nameSource: NameSource = NameSource.NAME

    /** The source of the comment field value. */
    var commentSource: CommentSource = CommentSource.COMMENT

    /** The number of the split segment this entry starts at. */
    var diskNumberStart: Long = 0

    init {
        if (name.isNotEmpty()) {
            setName(name)
        }
    }

    /**
     * The name of the entry.
     *
     * This is the raw name as it is stored inside of the archive.
     */
    override val name: String
        get() = _name.orEmpty()

    /** Is this entry a directory? */
    override val isDirectory: Boolean
        get() = name.endsWith("/")

    /** The uncompressed size of the entry data. */
    override val size: Long
        get() = _size

    /**
     * Sets the uncompressed size of the entry data.
     *
     * @throws IllegalArgumentException if the specified size is less than 0.
     */
    fun setSize(size: Long) {
        require(size >= 0) { "Invalid entry size" }
        _size = size
    }

    override val dataOffset: Long
        get() = _dataOffset

    fun setDataOffset(dataOffset: Long) {
        _dataOffset = dataOffset
    }

    override val isStreamContiguous: Boolean
        get() = _isStreamContiguous

    fun setStreamContiguous(isStreamContiguous: Boolean) {
        _isStreamContiguous = isStreamContiguous
    }

    /**
     * Sets the name of the entry.
     */
    fun setName(name: String?) {
        var adjusted = name
        if (adjusted != null && platform == PLATFORM_FAT && !adjusted.contains("/")) {
            adjusted = adjusted.replace('\\', '/')
        }
        _name = adjusted
    }

    /**
     * Sets the name using the raw bytes and the string created from it by guessing or using the
     * configured encoding.
     *
     * @param name the name to use created from the raw bytes using the guessed or configured
     * encoding
     * @param rawName the bytes originally read as name from the archive
     */
    fun setName(name: String?, rawName: ByteArray?) {
        setName(name)
        _rawName = rawName
    }

    /**
     * The raw bytes that made up the name before it has been converted using the configured or
     * guessed encoding.
     *
     * Null if this instance has not been read from an archive.
     */
    val rawName: ByteArray?
        get() = _rawName?.copyOf()

    /** Unix permission. */
    val unixMode: Int
        get() = if (platform != PLATFORM_UNIX) {
            0
        } else {
            ((externalAttributes shr SHORT_SHIFT) and SHORT_MASK.toLong()).toInt()
        }

    /**
     * Sets Unix permissions in a way that is understood by Info-Zip's unzip command.
     */
    fun setUnixMode(mode: Int) {
        externalAttributes = (mode.toLong() shl SHORT_SHIFT)
            // MS-DOS read-only attribute
            .or(if (mode and 0x80 == 0) 1L else 0L) // 0200
            // MS-DOS directory flag
            .or(if (isDirectory) 0x10L else 0L)
        platform = PLATFORM_UNIX
    }

    /**
     * Returns true if this entry represents a unix symlink, in which case the entry's content
     * contains the target path for the symlink.
     */
    val isUnixSymlink: Boolean
        get() = unixMode and UnixStat.FILE_TYPE_FLAG == UnixStat.LINK_FLAG

    /**
     * Adds an extra field - replacing an already present extra field of the same type.
     *
     * The new extra field will be the first one.
     */
    fun addAsFirstExtraField(ze: ZipExtraField) {
        if (ze is UnparseableExtraFieldData) {
            unparseableExtra = ze
        } else {
            if (getExtraField(ze.headerId) != null) {
                internalRemoveExtraField(ze.headerId)
            }
            extraFields = arrayOf(ze) + (extraFields ?: emptyArray())
        }
        setExtra()
    }

    /**
     * Adds an extra field - replacing an already present extra field of the same type.
     *
     * If no extra field of the same type exists, the field will be added as last field.
     */
    fun addExtraField(ze: ZipExtraField) {
        internalAddExtraField(ze)
        setExtra()
    }

    /**
     * Removes an extra field.
     *
     * @throws NoSuchElementException if no extra field of that type exists.
     */
    fun removeExtraField(type: ZipShort) {
        if (getExtraField(type) == null) {
            throw NoSuchElementException()
        }
        internalRemoveExtraField(type)
        setExtra()
    }

    private fun internalAddExtraField(ze: ZipExtraField) {
        if (ze is UnparseableExtraFieldData) {
            unparseableExtra = ze
        } else if (extraFields == null) {
            extraFields = arrayOf(ze)
        } else {
            if (getExtraField(ze.headerId) != null) {
                internalRemoveExtraField(ze.headerId)
            }
            extraFields = (extraFields ?: emptyArray()) + ze
        }
    }

    private fun internalRemoveExtraField(type: ZipShort) {
        val fields = extraFields ?: return
        val newResult = fields.filterNot { it.headerId == type }
        if (fields.size == newResult.size) {
            return
        }
        extraFields = newResult.toTypedArray()
    }

    /**
     * Looks up an extra field by its header id.
     *
     * @return null if no such field exists.
     */
    fun getExtraField(type: ZipShort): ZipExtraField? =
        extraFields?.firstOrNull { it.headerId == type }

    /**
     * Retrieves all extra fields that have been parsed successfully.
     */
    fun getExtraFields(): Array<ZipExtraField> =
        getParseableExtraFields()

    /**
     * Retrieves extra fields.
     *
     * @param includeUnparseable whether to also return unparseable extra fields as
     * [UnparseableExtraFieldData] if such data exists.
     */
    fun getExtraFields(includeUnparseable: Boolean): Array<ZipExtraField> =
        if (includeUnparseable) {
            getAllExtraFields()
        } else {
            getParseableExtraFields()
        }

    private fun getParseableExtraFields(): Array<ZipExtraField> =
        extraFields?.copyOf() ?: ExtraFieldUtils.EMPTY_ZIP_EXTRA_FIELD_ARRAY

    private fun getAllExtraFields(): Array<ZipExtraField> =
        getAllExtraFieldsNoCopy().let { fields ->
            if (fields === extraFields) fields.copyOf() else fields
        }

    /**
     * Gets all extra fields, including unparseable ones. Not necessarily a copy of internal data
     * structures, hence private method.
     */
    private fun getAllExtraFieldsNoCopy(): Array<ZipExtraField> {
        val fields = extraFields ?: return getUnparseableOnly()
        return unparseableExtra?.let { fields + it } ?: fields
    }

    private fun getUnparseableOnly(): Array<ZipExtraField> =
        unparseableExtra?.let { arrayOf<ZipExtraField>(it) }
            ?: ExtraFieldUtils.EMPTY_ZIP_EXTRA_FIELD_ARRAY

    /**
     * Retrieves the extra data for the central directory.
     */
    fun getCentralDirectoryExtra(): ByteArray =
        ExtraFieldUtils.mergeCentralDirectoryData(getAllExtraFieldsNoCopy())

    /**
     * Sets the central directory part of extra fields.
     *
     * @param b an array of bytes to be parsed into extra fields
     */
    fun setCentralDirectoryExtra(b: ByteArray) {
        try {
            mergeExtraFields(
                ExtraFieldUtils.parse(b, false, ExtraFieldParsingMode.BEST_EFFORT),
                false
            )
        } catch (e: ZipException) {
            // actually this is not possible as of Commons Compress 1.19
            throw IllegalArgumentException(e.message, e)
        }
    }

    /**
     * Retrieves the extra data for the local file data.
     */
    fun getLocalFileDataExtra(): ByteArray =
        extra ?: ByteUtils.EMPTY_BYTE_ARRAY

    /** The raw local file data extra bytes, or null if not set. */
    fun getExtra(): ByteArray? =
        extra

    /**
     * Parses the given bytes as extra field data and consumes any unparseable data as an
     * [UnparseableExtraFieldData] instance.
     *
     * @param extra an array of bytes to be parsed into extra fields
     * @throws IllegalArgumentException if the bytes cannot be parsed
     */
    fun setExtra(extra: ByteArray) {
        try {
            mergeExtraFields(
                ExtraFieldUtils.parse(extra, true, ExtraFieldParsingMode.BEST_EFFORT),
                true
            )
        } catch (e: ZipException) {
            // actually this is not possible as of Commons Compress 1.1
            throw IllegalArgumentException(
                "Error parsing extra fields for entry: $name - ${e.message}",
                e
            )
        }
    }

    /**
     * Refreshes the raw local file data extra bytes from the attached extra fields.
     */
    fun setExtra() {
        extra = ExtraFieldUtils.mergeLocalFileDataData(getAllExtraFieldsNoCopy())
    }

    /**
     * Replaces all currently attached extra fields with the new array.
     */
    fun setExtraFields(fields: Array<ZipExtraField>?) {
        unparseableExtra = null
        val newFields = mutableListOf<ZipExtraField>()
        fields?.forEach { field ->
            if (field is UnparseableExtraFieldData) {
                unparseableExtra = field
            } else {
                newFields.add(field)
            }
        }
        extraFields = newFields.toTypedArray()
        setExtra()
    }

    /**
     * If there are no extra fields, use the given fields as new extra data - otherwise merge the
     * fields assuming the existing fields and the new fields stem from different locations inside
     * the archive.
     *
     * @param f the extra fields to merge
     * @param local whether the new fields originate from local data
     */
    private fun mergeExtraFields(f: Array<ZipExtraField>, local: Boolean) {
        if (extraFields == null) {
            setExtraFields(f)
            return
        }
        for (element in f) {
            val existing: ZipExtraField? = if (element is UnparseableExtraFieldData) {
                unparseableExtra
            } else {
                getExtraField(element.headerId)
            }
            if (existing == null) {
                internalAddExtraField(element)
            } else {
                val b = if (local) {
                    element.localFileDataData
                } else {
                    element.centralDirectoryData
                } ?: ByteUtils.EMPTY_BYTE_ARRAY
                try {
                    if (local) {
                        existing.parseFromLocalFileData(b, 0, b.size)
                    } else {
                        existing.parseFromCentralDirectoryData(b, 0, b.size)
                    }
                } catch (_: ZipException) {
                    // emulate ExtraFieldParsingMode.fillAndMakeUnrecognizedOnError
                    val u = UnrecognizedExtraField()
                    u.setHeaderId(existing.headerId)
                    if (local) {
                        u.setLocalFileDataData(b)
                        u.setCentralDirectoryData(existing.centralDirectoryData)
                    } else {
                        u.setLocalFileDataData(existing.localFileDataData)
                        u.setCentralDirectoryData(b)
                    }
                    internalRemoveExtraField(existing.headerId)
                    internalAddExtraField(u)
                }
            }
        }
        setExtra()
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) {
            return true
        }
        if (other == null || this::class != other::class) {
            return false
        }
        other as ZipArchiveEntry
        if (name != other.name) {
            return false
        }
        val myComment = comment.orEmpty()
        val otherComment = other.comment.orEmpty()
        return myComment == otherComment &&
            internalAttributes == other.internalAttributes &&
            platform == other.platform &&
            externalAttributes == other.externalAttributes &&
            method == other.method &&
            size == other.size &&
            crc == other.crc &&
            compressedSize == other.compressedSize &&
            getCentralDirectoryExtra().contentEquals(other.getCentralDirectoryExtra()) &&
            getLocalFileDataExtra().contentEquals(other.getLocalFileDataExtra()) &&
            localHeaderOffset == other.localHeaderOffset &&
            dataOffset == other.dataOffset &&
            generalPurposeBit == other.generalPurposeBit
    }

    override fun hashCode(): Int =
        name.hashCode()

    companion object {
        const val PLATFORM_UNIX: Int = 3
        const val PLATFORM_FAT: Int = 0
        const val CRC_UNKNOWN: Int = -1

        /** Compression method for uncompressed entries, as in `java.util.zip.ZipEntry.STORED`. */
        const val STORED: Int = 0

        /** Compression method for compressed entries, as in `java.util.zip.ZipEntry.DEFLATED`. */
        const val DEFLATED: Int = 8

        private const val SHORT_MASK = 0xFFFF
        private const val SHORT_SHIFT = 16
    }
}
