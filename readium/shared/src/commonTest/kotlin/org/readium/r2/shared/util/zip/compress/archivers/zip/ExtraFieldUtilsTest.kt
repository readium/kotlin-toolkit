/*
 * Copyright 2026 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.shared.util.zip.compress.archivers.zip

import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.datetime.toLocalDateTime

class ExtraFieldUtilsTest {

    /** Encodes a raw extra field block: header id + length + payload. */
    private fun block(headerId: Int, payload: ByteArray): ByteArray =
        ZipShort.getBytes(headerId) + ZipShort.getBytes(payload.size) + payload

    @Test
    fun parsesUnknownFieldAsUnrecognized() {
        val payload = byteArrayOf(1, 2, 3, 4, 5)
        val fields = ExtraFieldUtils.parse(block(0x6666, payload))
        assertEquals(1, fields.size)
        val field = assertIs<UnrecognizedExtraField>(fields[0])
        assertEquals(ZipShort(0x6666), field.headerId)
        assertContentEquals(payload, field.localFileDataData)
    }

    @Test
    fun parsesSeveralFields() {
        val data = block(0x6666, byteArrayOf(1)) + block(0x7777, byteArrayOf(2, 3))
        val fields = ExtraFieldUtils.parse(data)
        assertEquals(2, fields.size)
        assertEquals(ZipShort(0x6666), fields[0].headerId)
        assertEquals(ZipShort(0x7777), fields[1].headerId)
    }

    @Test
    fun parsesZip64ExtraFieldFromCentralDirectory() {
        val payload = ZipEightByteInteger.getBytes(1234567891011L) + // size
            ZipEightByteInteger.getBytes(987654321L) + // compressed size
            ZipEightByteInteger.getBytes(42L) // relative header offset
        val fields = ExtraFieldUtils.parse(block(0x0001, payload), local = false)
        assertEquals(1, fields.size)
        val z64 = assertIs<Zip64ExtendedInformationExtraField>(fields[0])
        assertEquals(1234567891011L, z64.size?.longValue)
        assertEquals(987654321L, z64.compressedSize?.longValue)
        assertEquals(42L, z64.relativeHeaderOffset?.longValue)
    }

    @Test
    fun parsesUnicodePathExtraField() {
        val name = "fête.txt"
        val rawName = name.encodeToByteArray()
        val assembled = UnicodePathExtraField(name, rawName)
        val payload = assembled.localFileDataData!!
        val fields = ExtraFieldUtils.parse(block(0x7075, payload))
        val upath = assertIs<UnicodePathExtraField>(fields[0])
        assertContentEquals(rawName, upath.unicodeName)
        assertEquals(assembled.nameCRC32, upath.nameCRC32)
    }

    @Test
    fun truncatedBlockThrowsWithThrowBehavior() {
        // Claims 10 bytes of payload but only 2 available.
        val data = ZipShort.getBytes(0x6666) + ZipShort.getBytes(10) + byteArrayOf(1, 2)
        assertFailsWith<ZipException> {
            ExtraFieldUtils.parse(data, true, ExtraFieldUtils.UnparseableExtraField.THROW)
        }
    }

    @Test
    fun truncatedBlockIsReadWithReadBehavior() {
        val data = ZipShort.getBytes(0x6666) + ZipShort.getBytes(10) + byteArrayOf(1, 2)
        val fields = ExtraFieldUtils.parse(data, true, ExtraFieldUtils.UnparseableExtraField.READ)
        assertEquals(1, fields.size)
        val unparseable = assertIs<UnparseableExtraFieldData>(fields[0])
        assertContentEquals(data, unparseable.localFileDataData)
    }

    @Test
    fun truncatedBlockIsSkippedWithSkipBehavior() {
        val data = ZipShort.getBytes(0x6666) + ZipShort.getBytes(10) + byteArrayOf(1, 2)
        val fields = ExtraFieldUtils.parse(data, true, ExtraFieldUtils.UnparseableExtraField.SKIP)
        assertTrue(fields.isEmpty())
    }

    @Test
    fun mergeLocalRoundTrips() {
        val data = block(0x6666, byteArrayOf(1)) + block(0x7777, byteArrayOf(2, 3))
        val fields = ExtraFieldUtils.parse(data)
        assertContentEquals(data, ExtraFieldUtils.mergeLocalFileDataData(fields))
    }

    @Test
    fun createExtraFieldKnowsKeptImplementations() {
        assertIs<Zip64ExtendedInformationExtraField>(
            ExtraFieldUtils.createExtraField(ZipShort(0x0001))
        )
        assertIs<UnicodePathExtraField>(ExtraFieldUtils.createExtraField(ZipShort(0x7075)))
        assertIs<UnicodeCommentExtraField>(ExtraFieldUtils.createExtraField(ZipShort(0x6375)))
        assertIs<ResourceAlignmentExtraField>(ExtraFieldUtils.createExtraField(ZipShort(0xa11e)))
        assertIs<UnrecognizedExtraField>(ExtraFieldUtils.createExtraField(ZipShort(0x4242)))
        assertNull(ExtraFieldUtils.createExtraFieldNoDefault(ZipShort(0x4242)))
    }
}

class ZipArchiveEntryTest {

    @Test
    fun nameAndDirectory() {
        assertEquals("dir/", ZipArchiveEntry("dir/").name)
        assertTrue(ZipArchiveEntry("dir/").isDirectory)
        assertTrue(!ZipArchiveEntry("file.txt").isDirectory)
    }

    @Test
    fun backslashesAreNormalizedOnFatPlatform() {
        assertEquals("dir/file.txt", ZipArchiveEntry("dir\\file.txt").name)
    }

    @Test
    fun sizeValidation() {
        val entry = ZipArchiveEntry("file")
        assertFailsWith<IllegalArgumentException> { entry.setSize(-1) }
        entry.setSize(42)
        assertEquals(42L, entry.size)
    }

    @Test
    fun extraFieldsRoundTripThroughLocalData() {
        val entry = ZipArchiveEntry("file")
        val field = UnrecognizedExtraField().apply {
            setHeaderId(ZipShort(0x6666))
            setLocalFileDataData(byteArrayOf(1, 2, 3))
        }
        entry.addExtraField(field)
        val extra = entry.getLocalFileDataExtra()
        val parsed = ExtraFieldUtils.parse(extra)
        assertEquals(1, parsed.size)
        assertEquals(ZipShort(0x6666), parsed[0].headerId)
        assertContentEquals(byteArrayOf(1, 2, 3), parsed[0].localFileDataData)
    }

    @Test
    fun unixModeAndSymlink() {
        val entry = ZipArchiveEntry("link")
        entry.setUnixMode(UnixStat.LINK_FLAG or UnixStat.DEFAULT_LINK_PERM)
        assertEquals(ZipArchiveEntry.PLATFORM_UNIX, entry.platform)
        assertTrue(entry.isUnixSymlink)
    }
}

class ZipUtilTest {

    @Test
    fun dosToJavaTimeRoundTripsKnownDate() {
        // 2020-06-15 12:30:42 in DOS format:
        // year-1980 = 40, month = 6, day = 15, hour = 12, minute = 30, second/2 = 21
        val dos = (40L shl 25) or (6L shl 21) or (15L shl 16) or
            (12L shl 11) or (30L shl 5) or 21L
        val millis = ZipUtil.dosToJavaTime(dos)
        val instant = kotlin.time.Instant.fromEpochMilliseconds(millis)
        val local = kotlinx.datetime.TimeZone.currentSystemDefault()
        val dateTime = instant.toLocalDateTime(local)
        assertEquals(2020, dateTime.year)
        assertEquals(kotlinx.datetime.Month.JUNE, dateTime.month)
        assertEquals(15, dateTime.day)
        assertEquals(12, dateTime.hour)
        assertEquals(30, dateTime.minute)
        assertEquals(42, dateTime.second)
    }

    @Test
    fun adjustToLong() {
        assertEquals(42L, ZipUtil.adjustToLong(42))
        assertEquals(4294967294L, ZipUtil.adjustToLong(-2))
    }

    @Test
    fun byteConversions() {
        assertEquals(255, ZipUtil.signedByteToUnsignedInt((-1).toByte()))
        assertEquals((-1).toByte(), ZipUtil.unsignedIntToSignedByte(255))
        assertFailsWith<IllegalArgumentException> { ZipUtil.unsignedIntToSignedByte(256) }
    }

    @Test
    fun checkRequestedFeaturesRejectsEncryptedEntries() {
        val entry = ZipArchiveEntry("file")
        entry.method = ZipArchiveEntry.DEFLATED
        entry.generalPurposeBit = GeneralPurposeBit().apply { useEncryption(true) }
        assertFailsWith<UnsupportedZipFeatureException> { ZipUtil.checkRequestedFeatures(entry) }
    }

    @Test
    fun checkRequestedFeaturesAcceptsStoredAndDeflated() {
        val entry = ZipArchiveEntry("file")
        entry.method = ZipArchiveEntry.STORED
        ZipUtil.checkRequestedFeatures(entry)
        entry.method = ZipArchiveEntry.DEFLATED
        ZipUtil.checkRequestedFeatures(entry)
        assertTrue(ZipUtil.canHandleEntryData(entry))
    }
}
