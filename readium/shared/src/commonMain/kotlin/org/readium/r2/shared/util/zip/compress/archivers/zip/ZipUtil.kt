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
// (Java source: `util/zip/compress/archivers/zip/ZipUtil.java`, phase 05b). Only the helpers
// used by the read path were kept: the `toDosTime`/`BigInteger` writer helpers were dropped, and
// `dosToJavaTime` was rewritten on kotlinx-datetime (lenient rollover like `java.util.Calendar`,
// evaluated in the current system time zone as DOS times are local times).

package org.readium.r2.shared.util.zip.compress.archivers.zip

import kotlin.time.Duration.Companion.seconds
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.plus
import org.readium.r2.shared.util.zip.compress.utils.Crc32

/**
 * Utility class for handling DOS and Java time conversions.
 */
internal object ZipUtil {

    /**
     * Converts DOS time to Java time (number of milliseconds since epoch).
     */
    fun dosToJavaTime(dosTime: Long): Long {
        val year = ((dosTime shr 25) and 0x7f).toInt() + 1980
        val month = ((dosTime shr 21) and 0x0f).toInt()
        val day = ((dosTime shr 16) and 0x1f).toInt()
        val hour = ((dosTime shr 11) and 0x1f).toInt()
        val minute = ((dosTime shr 5) and 0x3f).toInt()
        val second = ((dosTime shl 1) and 0x3e).toInt()

        // Mirror the lenient rollover behavior of java.util.Calendar for out-of-range fields.
        val date = LocalDate(year, 1, 1)
            .plus(month - 1, DateTimeUnit.MONTH)
            .plus(day - 1, DateTimeUnit.DAY)
        val secondsOfDay = hour * 3600 + minute * 60 + second
        return (date.atStartOfDayIn(TimeZone.currentSystemDefault()) + secondsOfDay.seconds)
            .toEpochMilliseconds()
    }

    /**
     * Assumes a negative integer really is a positive integer that has wrapped around and re-creates
     * the original value.
     */
    fun adjustToLong(i: Int): Long =
        if (i < 0) {
            2 * Int.MAX_VALUE.toLong() + 2 + i
        } else {
            i.toLong()
        }

    /** Returns a copy of the given array, or null. */
    fun copy(from: ByteArray?): ByteArray? =
        from?.copyOf()

    /**
     * Whether this library is able to read or write the given entry.
     */
    fun canHandleEntryData(entry: ZipArchiveEntry): Boolean =
        supportsEncryptionOf(entry) && supportsMethodOf(entry)

    /**
     * Checks whether the entry requires features not (yet) supported by the library and throws an
     * exception if it does.
     *
     * @throws UnsupportedZipFeatureException if the entry requires an unsupported feature.
     */
    fun checkRequestedFeatures(ze: ZipArchiveEntry) {
        if (!supportsEncryptionOf(ze)) {
            throw UnsupportedZipFeatureException(
                UnsupportedZipFeatureException.Feature.ENCRYPTION,
                ze
            )
        }
        if (!supportsMethodOf(ze)) {
            val m = ZipMethod.getMethodByCode(ze.method)
                ?: throw UnsupportedZipFeatureException(
                    UnsupportedZipFeatureException.Feature.METHOD,
                    ze
                )
            throw UnsupportedZipFeatureException(m, ze)
        }
    }

    /**
     * Whether the library supports the encryption used by the given entry.
     */
    private fun supportsEncryptionOf(entry: ZipArchiveEntry): Boolean =
        !entry.generalPurposeBit.usesEncryption()

    /**
     * Whether the library supports the compression method used by the given entry.
     */
    private fun supportsMethodOf(entry: ZipArchiveEntry): Boolean =
        entry.method == ZipMethod.STORED.code ||
            entry.method == ZipMethod.UNSHRINKING.code ||
            entry.method == ZipMethod.IMPLODING.code ||
            entry.method == ZipMethod.DEFLATED.code ||
            entry.method == ZipMethod.ENHANCED_DEFLATED.code ||
            entry.method == ZipMethod.BZIP2.code

    /**
     * If the stored CRC matches the one of the given name, returns the Unicode name of the given
     * field.
     *
     * If the field is null or the CRCs don't match, returns null instead.
     */
    private fun getUnicodeStringIfOriginalMatches(
        f: AbstractUnicodeExtraField?,
        orig: ByteArray,
    ): String? {
        if (f == null) {
            return null
        }
        val crc32 = Crc32()
        crc32.update(orig)
        if (crc32.value != f.nameCRC32) {
            return null
        }
        val unicodeName = f.unicodeName ?: return null
        return ZipEncodingHelper.UTF8_ZIP_ENCODING.decode(unicodeName)
    }

    /**
     * Sets name and comment of the entry from the InfoZIP Unicode extra fields, if the CRCs of
     * the original names match.
     */
    fun setNameAndCommentFromExtraFields(
        ze: ZipArchiveEntry,
        originalNameBytes: ByteArray,
        commentBytes: ByteArray?,
    ) {
        val nameCandidate = ze.getExtraField(UnicodePathExtraField.UPATH_ID)
        val name = nameCandidate as? UnicodePathExtraField
        val newName = getUnicodeStringIfOriginalMatches(name, originalNameBytes)
        if (newName != null) {
            ze.setName(newName)
            ze.nameSource = ZipArchiveEntry.NameSource.UNICODE_EXTRA_FIELD
        }

        if (commentBytes != null && commentBytes.isNotEmpty()) {
            val cmtCandidate = ze.getExtraField(UnicodeCommentExtraField.UCOM_ID)
            val cmt = cmtCandidate as? UnicodeCommentExtraField
            val newComment = getUnicodeStringIfOriginalMatches(cmt, commentBytes)
            if (newComment != null) {
                ze.comment = newComment
                ze.commentSource = ZipArchiveEntry.CommentSource.UNICODE_EXTRA_FIELD
            }
        }
    }

    /**
     * Converts a signed byte into an unsigned integer representation (e.g., -1 becomes 255).
     */
    fun signedByteToUnsignedInt(b: Byte): Int =
        b.toInt() and 0xff

    /**
     * Converts an unsigned integer to a signed byte (e.g., 255 becomes -1).
     *
     * @throws IllegalArgumentException if the provided integer is not inside the range [0,255].
     */
    fun unsignedIntToSignedByte(i: Int): Byte {
        require(i in 0..255) {
            "Can only convert non-negative integers between [0,255] to byte: [$i]"
        }
        return i.toByte()
    }
}
