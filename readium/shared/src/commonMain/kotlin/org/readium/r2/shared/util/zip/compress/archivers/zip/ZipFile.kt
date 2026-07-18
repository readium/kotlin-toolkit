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
// (Java source: `util/zip/compress/archivers/zip/ZipFile.java`, phase 05b).
// Adaptations for common code:
// - I/O is suspending (05a channels): the constructor became the suspending factory
//   `ZipFile(channel, ...)` on the companion object.
// - `InputStream` results are the suspending `ZipInputStream` (see `compress/utils`).
// - `copyRawEntries` and `getUnixSymlink` (writer-oriented helpers) were dropped, and the central
//   directory signature constants of the deleted `ZipArchiveOutputStream` moved here.

package org.readium.r2.shared.util.zip.compress.archivers.zip

import okio.EOFException
import okio.IOException
import org.readium.r2.shared.util.Closeable
import org.readium.r2.shared.util.zip.compress.archivers.EntryStreamOffsets
import org.readium.r2.shared.util.zip.compress.utils.BoundedArchiveInputStream
import org.readium.r2.shared.util.zip.compress.utils.BoundedSeekableByteChannelInputStream
import org.readium.r2.shared.util.zip.compress.utils.BufferedZipInputStream
import org.readium.r2.shared.util.zip.compress.utils.CountingInputStream
import org.readium.r2.shared.util.zip.compress.utils.IOUtils
import org.readium.r2.shared.util.zip.compress.utils.InputStreamStatistics
import org.readium.r2.shared.util.zip.compress.utils.ZipInputStream
import org.readium.r2.shared.util.zip.jvm.SeekableByteChannel
import org.readium.r2.shared.util.zip.jvm.ZipBuffer

/**
 * Replacement for `java.util.zip.ZipFile`.
 *
 * This class adds support for file name encodings other than UTF-8 (which is required to work on
 * ZIP files created by native ZIP tools and is able to skip a preamble like the one found in self
 * extracting archives. Furthermore it returns instances of [ZipArchiveEntry] instead of
 * `java.util.zip.ZipEntry`.
 *
 * It transparently supports Zip64 extensions and thus individual entries and archives larger
 * than 4 GB or with more than 65536 entries.
 *
 * The method signatures mimic the ones of `java.util.zip.ZipFile`, with a couple of exceptions:
 * - There is no getName method.
 * - `entries` returns a [List].
 * - `close` is allowed to throw [IOException].
 */
internal class ZipFile private constructor(
    /** The actual data source. */
    private val archive: SeekableByteChannel,
    /** File name of actual source, used for error messages only. */
    private val archiveName: String,
    /** The encoding to use for file names and the file comment. */
    val encoding: String?,
    /** Whether to look for and use Unicode extra fields. */
    private val useUnicodeExtraFields: Boolean,
) : Closeable {

    /**
     * Extends [ZipArchiveEntry] to store the offset within the archive.
     */
    private class Entry : ZipArchiveEntry() {

        override fun equals(other: Any?): Boolean {
            if (super.equals(other)) {
                // super.equals would return false if other were not an Entry
                other as Entry
                return localHeaderOffset == other.localHeaderOffset &&
                    dataOffset == other.dataOffset &&
                    diskNumberStart == other.diskNumberStart
            }
            return false
        }

        override fun hashCode(): Int =
            3 * super.hashCode() + localHeaderOffset.toInt() + (localHeaderOffset shr 32).toInt()
    }

    private class NameAndComment(val name: ByteArray, val comment: ByteArray)

    private class StoredStatisticsStream(input: ZipInputStream) :
        CountingInputStream(input), InputStreamStatistics {

        override val compressedCount: Long
            get() = bytesRead

        override val uncompressedCount: Long
            get() = compressedCount
    }

    /** The ZIP encoding to use for file names and the file comment. */
    private val zipEncoding: ZipEncoding = ZipEncodingHelper.getZipEncoding(encoding)

    /** List of entries in the order they appear inside the central directory. */
    private val entriesList: MutableList<ZipArchiveEntry> = mutableListOf()

    /** Maps name -> actual entries. */
    private val nameMap: MutableMap<String, MutableList<ZipArchiveEntry>> = HashMap(HASH_SIZE)

    /** Whether the ZIP archive is a split ZIP archive. */
    private val isSplitZipArchive: Boolean =
        archive is ZipSplitReadOnlySeekableByteChannel

    // cached buffers - must only be used locally in the class (COMPRESS-172)
    private val dwordBuf = ByteArray(ZipConstants.DWORD)
    private val wordBuf = ByteArray(ZipConstants.WORD)
    private val cfhBuf = ByteArray(CFH_LEN)
    private val shortBuf = ByteArray(ZipConstants.SHORT)
    private val dwordBbuf = ZipBuffer.wrap(dwordBuf)
    private val wordBbuf = ZipBuffer.wrap(wordBuf)
    private val cfhBbuf = ZipBuffer.wrap(cfhBuf)

    private var centralDirectoryStartDiskNumber: Long = 0
    private var centralDirectoryStartRelativeOffset: Long = 0
    private var centralDirectoryStartOffset: Long = 0

    /** The offset of the first local file header in the file. */
    var firstLocalFileHeaderOffset: Long = 0
        private set

    private suspend fun initialize(ignoreLocalFileHeader: Boolean) {
        try {
            val entriesWithoutUTF8Flag = populateFromCentralDirectory()
            if (!ignoreLocalFileHeader) {
                resolveLocalFileHeaderData(entriesWithoutUTF8Flag)
            }
            fillNameMap()
        } catch (e: IOException) {
            throw IOException("Error on ZipFile $archiveName", e)
        }
    }

    /**
     * Whether this class is able to read the given entry.
     *
     * May return false if it is set up to use encryption or a compression method that hasn't been
     * implemented yet.
     */
    fun canReadEntryData(ze: ZipArchiveEntry): Boolean =
        ZipUtil.canHandleEntryData(ze)

    /**
     * Closes the archive.
     */
    override fun close() {
        // The Java original also tracked a `closed` flag, only to warn from a finalizer when the
        // file was garbage collected without being closed; Kotlin common has no finalizers, and
        // the underlying channels make `close()` idempotent themselves.
        archive.close()
    }

    /**
     * Creates new BoundedInputStream, according to implementation of underlying archive channel.
     */
    private fun createBoundedInputStream(start: Long, remaining: Long): BoundedArchiveInputStream {
        require(start >= 0 && remaining >= 0 && start + remaining >= start) {
            "Corrupted archive, stream boundaries are out of range"
        }
        return BoundedSeekableByteChannelInputStream(start, remaining, archive)
    }

    private fun fillNameMap() {
        for (ze in entriesList) {
            // entries is filled in populateFromCentralDirectory and never modified
            val name = ze.name
            val entriesOfThatName = nameMap.getOrPut(name) { mutableListOf() }
            entriesOfThatName.add(ze)
        }
    }

    /**
     * Gets a [ZipInputStream] for reading the content before the first local file header.
     *
     * @return null if there is no content before the first local file header. Otherwise returns a
     * stream to read the content before the first local file header.
     */
    fun getContentBeforeFirstLocalFileHeader(): ZipInputStream? =
        if (firstLocalFileHeaderOffset == 0L) {
            null
        } else {
            createBoundedInputStream(0, firstLocalFileHeaderOffset)
        }

    private suspend fun getDataOffset(ze: ZipArchiveEntry): Long {
        val s = ze.dataOffset
        if (s == EntryStreamOffsets.OFFSET_UNKNOWN) {
            setDataOffset(ze)
            return ze.dataOffset
        }
        return s
    }

    /**
     * All entries, in the same order they appear within the archive's central directory.
     */
    val entries: List<ZipArchiveEntry>
        get() = entriesList.toList()

    /**
     * All named entries in the same order they appear within the archive's central directory.
     */
    fun getEntries(name: String): List<ZipArchiveEntry> =
        nameMap[name]?.toList() ?: emptyList()

    /**
     * All entries in the same order their contents appear within the archive.
     */
    fun getEntriesInPhysicalOrder(): List<ZipArchiveEntry> =
        entriesList.sortedWith(offsetComparator)

    /**
     * All named entries in the same order their contents appear within the archive.
     */
    fun getEntriesInPhysicalOrder(name: String): List<ZipArchiveEntry> =
        nameMap[name]?.sortedWith(offsetComparator) ?: emptyList()

    /**
     * Gets a named entry or null if no entry by that name exists.
     *
     * If multiple entries with the same name exist the first entry in the archive's central
     * directory by that name is returned.
     */
    fun getEntry(name: String): ZipArchiveEntry? =
        nameMap[name]?.firstOrNull()

    /**
     * Gets a [ZipInputStream] for reading the contents of the given entry.
     *
     * The returned stream implements [InputStreamStatistics].
     *
     * @throws IOException if unable to create an input stream from the zip entry.
     */
    suspend fun getInputStream(ze: ZipArchiveEntry): ZipInputStream? {
        if (ze !is Entry) {
            return null
        }
        // cast validity is checked just above
        ZipUtil.checkRequestedFeatures(ze)

        // doesn't get closed if the method is not supported - which should never happen because
        // of the checkRequestedFeatures call above
        val rawInput = getRawInputStream(ze)
            ?: throw IOException("Cannot read data of entry ${ze.name}")
        val input = BufferedZipInputStream(rawInput)
        return when (ZipMethod.getMethodByCode(ze.method)) {
            ZipMethod.STORED ->
                StoredStatisticsStream(input)
            ZipMethod.DEFLATED -> {
                val inflater = Inflater(true)
                object : InflaterInputStreamWithStatistics(input, inflater) {
                    override fun close() {
                        try {
                            super.close()
                        } finally {
                            inflater.end()
                        }
                    }
                }
            }
            else ->
                throw UnsupportedZipFeatureException(ZipMethod.getMethodByCode(ze.method), ze)
        }
    }

    /**
     * Gets the raw stream of the archive entry (compressed form).
     *
     * This method does not relate to how/if we understand the payload in the stream, since we
     * really only intend to move it on to somewhere else.
     *
     * @throws IOException if there is a problem reading data offset.
     */
    suspend fun getRawInputStream(ze: ZipArchiveEntry): ZipInputStream? {
        if (ze !is Entry) {
            return null
        }

        val start = getDataOffset(ze)
        if (start == EntryStreamOffsets.OFFSET_UNKNOWN) {
            return null
        }
        return createBoundedInputStream(start, ze.compressedSize)
    }

    /**
     * Gets the raw stream of the stored archive entry starting from [fromIndex]. (Readium-added.)
     *
     * This method does not relate to how/if we understand the payload in the stream, since we
     * really only intend to move it on to somewhere else.
     *
     * @param ze The stored entry to get the stream for
     * @param fromIndex The index in the entry that the stream will start from
     * @throws IOException if there is a problem reading data offset.
     */
    suspend fun getRawInputStream(ze: ZipArchiveEntry, fromIndex: Long): ZipInputStream? {
        if (ze !is Entry) {
            return null
        }

        val start = getDataOffset(ze)
        if (start == EntryStreamOffsets.OFFSET_UNKNOWN) {
            return null
        }

        require(ZipMethod.getMethodByCode(ze.method) == ZipMethod.STORED) {
            "Cannot begin a stream at a specific index in compressed entries."
        }
        require(fromIndex < ze.size) { "fromIndex out of bounds." }

        return createBoundedInputStream(start + fromIndex, ze.size - fromIndex)
    }

    /**
     * Reads the central directory of the given archive and populates the internal tables with
     * [ZipArchiveEntry] instances.
     *
     * The [ZipArchiveEntry]s will know all data that can be obtained from the central directory
     * alone, but not the data that requires the local file header or additional data to be read.
     *
     * @return a map of zip entries that didn't have the language encoding flag set when read.
     */
    private suspend fun populateFromCentralDirectory(): MutableMap<ZipArchiveEntry, NameAndComment> {
        val noUTF8Flag = mutableMapOf<ZipArchiveEntry, NameAndComment>()

        positionAtCentralDirectory()
        centralDirectoryStartOffset = archive.position()

        wordBbuf.rewind()
        IOUtils.readFully(archive, wordBbuf)
        var sig = ZipLong.getValue(wordBuf)

        if (sig != CFH_SIG_VALUE && startsWithLocalFileHeader()) {
            throw IOException("Central directory is empty, can't expand corrupt archive.")
        }

        while (sig == CFH_SIG_VALUE) {
            readCentralDirectoryEntry(noUTF8Flag)
            wordBbuf.rewind()
            IOUtils.readFully(archive, wordBbuf)
            sig = ZipLong.getValue(wordBuf)
        }
        return noUTF8Flag
    }

    /**
     * Searches for either the "Zip64 end of central directory locator" or the "End of central dir
     * record", parses it and positions the stream at the first central directory record.
     */
    private suspend fun positionAtCentralDirectory() {
        positionAtEndOfCentralDirectoryRecord()
        var found = false
        val searchedForZip64EOCD = archive.position() > ZIP64_EOCDL_LENGTH
        if (searchedForZip64EOCD) {
            archive.position(archive.position() - ZIP64_EOCDL_LENGTH)
            wordBbuf.rewind()
            IOUtils.readFully(archive, wordBbuf)
            found = ZIP64_EOCD_LOC_SIG.contentEquals(wordBuf)
        }
        if (!found) {
            // not a ZIP64 archive
            if (searchedForZip64EOCD) {
                skipBytes(ZIP64_EOCDL_LENGTH - ZipConstants.WORD)
            }
            positionAtCentralDirectory32()
        } else {
            positionAtCentralDirectory64()
        }
    }

    /**
     * Parses the "End of central dir record" and positions the stream at the first central
     * directory record.
     *
     * Expects stream to be positioned at the beginning of the "End of central dir record".
     */
    private suspend fun positionAtCentralDirectory32() {
        val endOfCentralDirectoryRecordOffset = archive.position()
        if (isSplitZipArchive) {
            skipBytes(CFD_DISK_OFFSET)
            val shortBbuf = ZipBuffer.wrap(shortBuf)
            IOUtils.readFully(archive, shortBbuf)
            centralDirectoryStartDiskNumber = ZipShort.getValue(shortBuf).toLong()

            skipBytes(CFD_LOCATOR_RELATIVE_OFFSET)

            wordBbuf.rewind()
            IOUtils.readFully(archive, wordBbuf)
            centralDirectoryStartRelativeOffset = ZipLong.getValue(wordBuf)
            (archive as ZipSplitReadOnlySeekableByteChannel)
                .position(centralDirectoryStartDiskNumber, centralDirectoryStartRelativeOffset)
        } else {
            skipBytes(CFD_LENGTH_OFFSET)
            wordBbuf.rewind()
            IOUtils.readFully(archive, wordBbuf)
            val centralDirectoryLength = ZipLong.getValue(wordBuf)

            wordBbuf.rewind()
            IOUtils.readFully(archive, wordBbuf)
            centralDirectoryStartDiskNumber = 0
            centralDirectoryStartRelativeOffset = ZipLong.getValue(wordBuf)

            firstLocalFileHeaderOffset = maxOf(
                endOfCentralDirectoryRecordOffset - centralDirectoryLength -
                    centralDirectoryStartRelativeOffset,
                0L
            )
            archive.position(centralDirectoryStartRelativeOffset + firstLocalFileHeaderOffset)
        }
    }

    /**
     * Parses the "Zip64 end of central directory locator", finds the "Zip64 end of central
     * directory record" using the parsed information, parses that and positions the stream at the
     * first central directory record.
     *
     * Expects stream to be positioned right behind the "Zip64 end of central directory locator"'s
     * signature.
     */
    private suspend fun positionAtCentralDirectory64() {
        if (isSplitZipArchive) {
            wordBbuf.rewind()
            IOUtils.readFully(archive, wordBbuf)
            val diskNumberOfEOCD = ZipLong.getValue(wordBuf)

            dwordBbuf.rewind()
            IOUtils.readFully(archive, dwordBbuf)
            val relativeOffsetOfEOCD = ZipEightByteInteger.getLongValue(dwordBuf)
            (archive as ZipSplitReadOnlySeekableByteChannel)
                .position(diskNumberOfEOCD, relativeOffsetOfEOCD)
        } else {
            skipBytes(
                ZIP64_EOCDL_LOCATOR_OFFSET - ZipConstants.WORD // signature has already been read
            )
            dwordBbuf.rewind()
            IOUtils.readFully(archive, dwordBbuf)
            archive.position(ZipEightByteInteger.getLongValue(dwordBuf))
        }

        wordBbuf.rewind()
        IOUtils.readFully(archive, wordBbuf)
        if (!wordBuf.contentEquals(ZIP64_EOCD_SIG)) {
            throw ZipException("Archive's ZIP64 end of central directory locator is corrupt.")
        }

        if (isSplitZipArchive) {
            skipBytes(
                ZIP64_EOCD_CFD_DISK_OFFSET - ZipConstants.WORD // signature has already been read
            )
            wordBbuf.rewind()
            IOUtils.readFully(archive, wordBbuf)
            centralDirectoryStartDiskNumber = ZipLong.getValue(wordBuf)

            skipBytes(ZIP64_EOCD_CFD_LOCATOR_RELATIVE_OFFSET)

            dwordBbuf.rewind()
            IOUtils.readFully(archive, dwordBbuf)
            centralDirectoryStartRelativeOffset = ZipEightByteInteger.getLongValue(dwordBuf)
            (archive as ZipSplitReadOnlySeekableByteChannel)
                .position(centralDirectoryStartDiskNumber, centralDirectoryStartRelativeOffset)
        } else {
            skipBytes(
                ZIP64_EOCD_CFD_LOCATOR_OFFSET - ZipConstants.WORD // signature has already been read
            )
            dwordBbuf.rewind()
            IOUtils.readFully(archive, dwordBbuf)
            centralDirectoryStartDiskNumber = 0
            centralDirectoryStartRelativeOffset = ZipEightByteInteger.getLongValue(dwordBuf)
            archive.position(centralDirectoryStartRelativeOffset)
        }
    }

    /**
     * Searches for and positions the stream at the start of the "End of central dir record".
     */
    private suspend fun positionAtEndOfCentralDirectoryRecord() {
        val found = tryToLocateSignature(
            MIN_EOCD_SIZE.toLong(),
            MAX_EOCD_SIZE.toLong(),
            EOCD_SIG
        )
        if (!found) {
            throw ZipException("Archive is not a ZIP archive")
        }
    }

    /**
     * Reads an individual entry of the central directory, creates a [ZipArchiveEntry] from it and
     * adds it to the global maps.
     *
     * @param noUTF8Flag map used to collect entries that don't have their UTF-8 flag set and
     * whose name will be set by data read from the local file header later. The current entry may
     * be added to this map.
     */
    private suspend fun readCentralDirectoryEntry(
        noUTF8Flag: MutableMap<ZipArchiveEntry, NameAndComment>,
    ) {
        cfhBbuf.rewind()
        IOUtils.readFully(archive, cfhBbuf)
        var off = 0
        val ze = Entry()

        val versionMadeBy = ZipShort.getValue(cfhBuf, off)
        off += ZipConstants.SHORT
        ze.versionMadeBy = versionMadeBy
        ze.platform = (versionMadeBy shr BYTE_SHIFT) and NIBLET_MASK

        ze.versionRequired = ZipShort.getValue(cfhBuf, off)
        off += ZipConstants.SHORT // version required

        val gpFlag = GeneralPurposeBit.parse(cfhBuf, off)
        val hasUTF8Flag = gpFlag.usesUTF8ForNames()
        val entryEncoding = if (hasUTF8Flag) ZipEncodingHelper.UTF8_ZIP_ENCODING else zipEncoding
        if (hasUTF8Flag) {
            ze.nameSource = ZipArchiveEntry.NameSource.NAME_WITH_EFS_FLAG
        }
        ze.generalPurposeBit = gpFlag
        ze.rawFlag = ZipShort.getValue(cfhBuf, off)

        off += ZipConstants.SHORT

        ze.method = ZipShort.getValue(cfhBuf, off)
        off += ZipConstants.SHORT

        val time = ZipUtil.dosToJavaTime(ZipLong.getValue(cfhBuf, off))
        ze.time = time
        off += ZipConstants.WORD

        ze.crc = ZipLong.getValue(cfhBuf, off)
        off += ZipConstants.WORD

        var size = ZipLong.getValue(cfhBuf, off)
        if (size < 0) {
            throw IOException("broken archive, entry with negative compressed size")
        }
        ze.compressedSize = size
        off += ZipConstants.WORD

        size = ZipLong.getValue(cfhBuf, off)
        if (size < 0) {
            throw IOException("broken archive, entry with negative size")
        }
        ze.setSize(size)
        off += ZipConstants.WORD

        val fileNameLen = ZipShort.getValue(cfhBuf, off)
        off += ZipConstants.SHORT
        if (fileNameLen < 0) {
            throw IOException("broken archive, entry with negative fileNameLen")
        }

        val extraLen = ZipShort.getValue(cfhBuf, off)
        off += ZipConstants.SHORT
        if (extraLen < 0) {
            throw IOException("broken archive, entry with negative extraLen")
        }

        val commentLen = ZipShort.getValue(cfhBuf, off)
        off += ZipConstants.SHORT
        if (commentLen < 0) {
            throw IOException("broken archive, entry with negative commentLen")
        }

        ze.diskNumberStart = ZipShort.getValue(cfhBuf, off).toLong()
        off += ZipConstants.SHORT

        ze.internalAttributes = ZipShort.getValue(cfhBuf, off)
        off += ZipConstants.SHORT

        ze.externalAttributes = ZipLong.getValue(cfhBuf, off)
        off += ZipConstants.WORD

        val fileName = IOUtils.readRange(archive, fileNameLen)
        if (fileName.size < fileNameLen) {
            throw EOFException()
        }
        ze.setName(entryEncoding.decode(fileName), fileName)

        // LFH offset
        ze.localHeaderOffset = ZipLong.getValue(cfhBuf, off) + firstLocalFileHeaderOffset
        // data offset will be filled later
        entriesList.add(ze)

        val cdExtraData = IOUtils.readRange(archive, extraLen)
        if (cdExtraData.size < extraLen) {
            throw EOFException()
        }
        try {
            ze.setCentralDirectoryExtra(cdExtraData)
        } catch (ex: RuntimeException) {
            throw ZipException("Invalid extra data in entry ${ze.name}", ex)
        }

        setSizesAndOffsetFromZip64Extra(ze)
        sanityCheckLFHOffset(ze)

        val comment = IOUtils.readRange(archive, commentLen)
        if (comment.size < commentLen) {
            throw EOFException()
        }
        ze.comment = entryEncoding.decode(comment)

        if (!hasUTF8Flag && useUnicodeExtraFields) {
            noUTF8Flag[ze] = NameAndComment(fileName, comment)
        }

        ze.setStreamContiguous(true)
    }

    /**
     * Walks through all recorded entries and adds the data available from the local file header.
     *
     * Also records the offsets for the data to read from the entries.
     */
    private suspend fun resolveLocalFileHeaderData(
        entriesWithoutUTF8Flag: Map<ZipArchiveEntry, NameAndComment>,
    ) {
        for (zipArchiveEntry in entriesList) {
            // entries is filled in populateFromCentralDirectory and never modified
            val ze = zipArchiveEntry as Entry
            val (fileNameLen, extraFieldLen) = setDataOffset(ze)
            skipBytes(fileNameLen)
            val localExtraData = IOUtils.readRange(archive, extraFieldLen)
            if (localExtraData.size < extraFieldLen) {
                throw EOFException()
            }
            try {
                ze.setExtra(localExtraData)
            } catch (ex: RuntimeException) {
                throw ZipException("Invalid extra data in entry ${ze.name}", ex)
            }

            entriesWithoutUTF8Flag[ze]?.let { nc ->
                ZipUtil.setNameAndCommentFromExtraFields(ze, nc.name, nc.comment)
            }
        }
    }

    private fun sanityCheckLFHOffset(ze: ZipArchiveEntry) {
        if (ze.diskNumberStart < 0) {
            throw IOException("broken archive, entry with negative disk number")
        }
        if (ze.localHeaderOffset < 0) {
            throw IOException("broken archive, entry with negative local file header offset")
        }
        if (isSplitZipArchive) {
            if (ze.diskNumberStart > centralDirectoryStartDiskNumber) {
                throw IOException(
                    "local file header for ${ze.name} starts on a later disk than central directory"
                )
            }
            if (ze.diskNumberStart == centralDirectoryStartDiskNumber &&
                ze.localHeaderOffset > centralDirectoryStartRelativeOffset
            ) {
                throw IOException(
                    "local file header for ${ze.name} starts after central directory"
                )
            }
        } else if (ze.localHeaderOffset > centralDirectoryStartOffset) {
            throw IOException("local file header for ${ze.name} starts after central directory")
        }
    }

    private suspend fun setDataOffset(ze: ZipArchiveEntry): Pair<Int, Int> {
        var offset = ze.localHeaderOffset
        if (isSplitZipArchive) {
            (archive as ZipSplitReadOnlySeekableByteChannel)
                .position(ze.diskNumberStart, offset + LFH_OFFSET_FOR_FILENAME_LENGTH)
            // the offset should be updated to the global offset
            offset = archive.position() - LFH_OFFSET_FOR_FILENAME_LENGTH
        } else {
            archive.position(offset + LFH_OFFSET_FOR_FILENAME_LENGTH)
        }
        wordBbuf.rewind()
        IOUtils.readFully(archive, wordBbuf)
        wordBbuf.flip()
        wordBbuf.get(shortBuf)
        val fileNameLen = ZipShort.getValue(shortBuf)
        wordBbuf.get(shortBuf)
        val extraFieldLen = ZipShort.getValue(shortBuf)
        ze.setDataOffset(
            offset + LFH_OFFSET_FOR_FILENAME_LENGTH +
                ZipConstants.SHORT + ZipConstants.SHORT + fileNameLen + extraFieldLen
        )
        if (ze.dataOffset + ze.compressedSize > centralDirectoryStartOffset) {
            throw IOException("data for ${ze.name} overlaps with central directory.")
        }
        return Pair(fileNameLen, extraFieldLen)
    }

    /**
     * If the entry holds a Zip64 extended information extra field, read sizes from there if the
     * entry's sizes are set to 0xFFFFFFFF, do the same for the offset of the local file header.
     *
     * Ensures the Zip64 extra either knows both compressed and uncompressed size or neither of
     * both as the internal logic in ExtraFieldUtils forces the field to create local header data
     * even if they are never used - and here a field with only one size would be invalid.
     */
    private fun setSizesAndOffsetFromZip64Extra(ze: ZipArchiveEntry) {
        val extra = ze.getExtraField(Zip64ExtendedInformationExtraField.HEADER_ID) ?: return
        val z64 = extra as? Zip64ExtendedInformationExtraField
            ?: throw ZipException("archive contains unparseable zip64 extra field")

        val hasUncompressedSize = ze.size == ZipConstants.ZIP64_MAGIC
        val hasCompressedSize = ze.compressedSize == ZipConstants.ZIP64_MAGIC
        val hasRelativeHeaderOffset = ze.localHeaderOffset == ZipConstants.ZIP64_MAGIC
        val hasDiskStart = ze.diskNumberStart == ZipConstants.ZIP64_MAGIC_SHORT.toLong()
        z64.reparseCentralDirectoryData(
            hasUncompressedSize,
            hasCompressedSize,
            hasRelativeHeaderOffset,
            hasDiskStart
        )

        if (hasUncompressedSize) {
            val size = z64.size?.longValue
                ?: throw ZipException("archive contains corrupted zip64 extra field")
            if (size < 0) {
                throw IOException("broken archive, entry with negative size")
            }
            ze.setSize(size)
        } else if (hasCompressedSize) {
            z64.size = ZipEightByteInteger(ze.size)
        }

        if (hasCompressedSize) {
            val size = z64.compressedSize?.longValue
                ?: throw ZipException("archive contains corrupted zip64 extra field")
            if (size < 0) {
                throw IOException("broken archive, entry with negative compressed size")
            }
            ze.compressedSize = size
        } else if (hasUncompressedSize) {
            z64.compressedSize = ZipEightByteInteger(ze.compressedSize)
        }

        if (hasRelativeHeaderOffset) {
            ze.localHeaderOffset = z64.relativeHeaderOffset?.longValue
                ?: throw ZipException("archive contains corrupted zip64 extra field")
        }

        if (hasDiskStart) {
            ze.diskNumberStart = z64.diskStartNumber?.value
                ?: throw ZipException("archive contains corrupted zip64 extra field")
        }
    }

    /**
     * Skips the given number of bytes or throws an EOFException if skipping failed.
     */
    private suspend fun skipBytes(count: Int) {
        val currentPosition = archive.position()
        val newPosition = currentPosition + count
        if (newPosition > archive.size()) {
            throw EOFException()
        }
        archive.position(newPosition)
    }

    /**
     * Checks whether the archive starts with a LFH. If it doesn't, it may be an empty archive.
     */
    private suspend fun startsWithLocalFileHeader(): Boolean {
        archive.position(firstLocalFileHeaderOffset)
        wordBbuf.rewind()
        IOUtils.readFully(archive, wordBbuf)
        return wordBuf.contentEquals(LFH_SIG)
    }

    /**
     * Searches the archive backwards from minDistance to maxDistance for the given signature,
     * positions the channel right at the signature if it has been found.
     */
    private suspend fun tryToLocateSignature(
        minDistanceFromEnd: Long,
        maxDistanceFromEnd: Long,
        sig: ByteArray,
    ): Boolean {
        var found = false
        var off = archive.size() - minDistanceFromEnd
        val stopSearching = maxOf(0L, archive.size() - maxDistanceFromEnd)
        if (off >= 0) {
            while (off >= stopSearching) {
                archive.position(off)
                try {
                    wordBbuf.rewind()
                    IOUtils.readFully(archive, wordBbuf)
                    wordBbuf.flip()
                } catch (_: EOFException) {
                    break
                }
                var curr = wordBbuf.get()
                if (curr == sig[POS_0]) {
                    curr = wordBbuf.get()
                    if (curr == sig[POS_1]) {
                        curr = wordBbuf.get()
                        if (curr == sig[POS_2]) {
                            curr = wordBbuf.get()
                            if (curr == sig[POS_3]) {
                                found = true
                                break
                            }
                        }
                    }
                }
                off--
            }
        }
        if (found) {
            archive.position(off)
        }
        return found
    }

    companion object {

        private const val HASH_SIZE = 509

        internal const val NIBLET_MASK = 0x0f

        internal const val BYTE_SHIFT = 8

        private const val POS_0 = 0
        private const val POS_1 = 1
        private const val POS_2 = 2
        private const val POS_3 = 3

        /**
         * Length of a "central directory" entry structure without file name, extra fields or
         * comment.
         */
        private const val CFH_LEN =
            ZipConstants.SHORT + // version made by
                ZipConstants.SHORT + // version needed to extract
                ZipConstants.SHORT + // general purpose bit flag
                ZipConstants.SHORT + // compression method
                ZipConstants.SHORT + // last mod file time
                ZipConstants.SHORT + // last mod file date
                ZipConstants.WORD + // crc-32
                ZipConstants.WORD + // compressed size
                ZipConstants.WORD + // uncompressed size
                ZipConstants.SHORT + // file name length
                ZipConstants.SHORT + // extra field length
                ZipConstants.SHORT + // file comment length
                ZipConstants.SHORT + // disk number start
                ZipConstants.SHORT + // internal file attributes
                ZipConstants.WORD + // external file attributes
                ZipConstants.WORD // relative offset of local header

        // Signatures, moved here from the deleted ZipArchiveOutputStream.

        /** Central file header signature. */
        private val CFH_SIG_VALUE: Long = ZipLong.CFH_SIG.value

        /** Local file header signature. */
        private val LFH_SIG: ByteArray = ZipLong.LFH_SIG.bytes

        /** End of central directory record signature. */
        private val EOCD_SIG: ByteArray = ZipLong.getBytes(0X06054B50L)

        /** ZIP64 end of central directory record signature. */
        private val ZIP64_EOCD_SIG: ByteArray = ZipLong.getBytes(0X06064B50L)

        /** ZIP64 end of central directory locator signature. */
        private val ZIP64_EOCD_LOC_SIG: ByteArray = ZipLong.getBytes(0X07064B50L)

        /**
         * Length of the "End of central directory record" - which is supposed to be the last
         * structure of the archive - without file comment.
         */
        internal const val MIN_EOCD_SIZE =
            ZipConstants.WORD + // end of central dir signature
                ZipConstants.SHORT + // number of this disk
                // number of the disk with the
                ZipConstants.SHORT + // start of the central directory
                // total number of entries in
                ZipConstants.SHORT + // the central dir on this disk
                // total number of entries in
                ZipConstants.SHORT + // the central dir
                ZipConstants.WORD + // size of the central directory
                // offset of start of central
                // directory with respect to
                ZipConstants.WORD + // the starting disk number
                ZipConstants.SHORT // zipfile comment length

        /**
         * Maximum length of the "End of central directory record" with a file comment.
         */
        private const val MAX_EOCD_SIZE = MIN_EOCD_SIZE +
            ZipConstants.ZIP64_MAGIC_SHORT // maximum length of zipfile comment

        /**
         * Offset of the field that holds the location of the length of the central directory
         * inside the "End of central directory record" relative to the start of the "End of
         * central directory record".
         */
        private const val CFD_LENGTH_OFFSET =
            ZipConstants.WORD + // end of central dir signature
                ZipConstants.SHORT + // number of this disk
                // number of the disk with the
                ZipConstants.SHORT + // start of the central directory
                // total number of entries in
                ZipConstants.SHORT + // the central dir on this disk
                // total number of entries in
                ZipConstants.SHORT // the central dir

        /**
         * Offset of the field that holds the disk number of the first central directory entry
         * inside the "End of central directory record" relative to the start of the "End of
         * central directory record".
         */
        private const val CFD_DISK_OFFSET =
            ZipConstants.WORD + // end of central dir signature
                ZipConstants.SHORT // number of this disk

        /**
         * Offset of the field that holds the location of the first central directory entry
         * inside the "End of central directory record" relative to the "number of the disk with
         * the start of the central directory".
         */
        private const val CFD_LOCATOR_RELATIVE_OFFSET =
            // total number of entries in
            ZipConstants.SHORT + // the central dir on this disk
                // total number of entries in
                ZipConstants.SHORT + // the central dir
                ZipConstants.WORD // size of the central directory

        /**
         * Length of the "Zip64 end of central directory locator" - which should be right in front
         * of the "end of central directory record" if one is present at all.
         */
        private const val ZIP64_EOCDL_LENGTH =
            ZipConstants.WORD + // zip64 end of central dir locator sig
                // number of the disk with the start
                // start of the zip64 end of
                ZipConstants.WORD + // central directory
                // relative offset of the zip64
                ZipConstants.DWORD + // end of central directory record
                ZipConstants.WORD // total number of disks

        /**
         * Offset of the field that holds the location of the "Zip64 end of central directory
         * record" inside the "Zip64 end of central directory locator" relative to the start of
         * the "Zip64 end of central directory locator".
         */
        private const val ZIP64_EOCDL_LOCATOR_OFFSET =
            ZipConstants.WORD + // zip64 end of central dir locator sig
                // number of the disk with the start
                // start of the zip64 end of
                ZipConstants.WORD // central directory

        /**
         * Offset of the field that holds the location of the first central directory entry
         * inside the "Zip64 end of central directory record" relative to the start of the "Zip64
         * end of central directory record".
         */
        private const val ZIP64_EOCD_CFD_LOCATOR_OFFSET =
            // zip64 end of central dir
            ZipConstants.WORD + // signature
                // size of zip64 end of central
                ZipConstants.DWORD + // directory record
                ZipConstants.SHORT + // version made by
                ZipConstants.SHORT + // version needed to extract
                ZipConstants.WORD + // number of this disk
                // number of the disk with the
                ZipConstants.WORD + // start of the central directory
                // total number of entries in the
                ZipConstants.DWORD + // central directory on this disk
                // total number of entries in the
                ZipConstants.DWORD + // central directory
                ZipConstants.DWORD // size of the central directory

        /**
         * Offset of the field that holds the disk number of the first central directory entry
         * inside the "Zip64 end of central directory record" relative to the start of the "Zip64
         * end of central directory record".
         */
        private const val ZIP64_EOCD_CFD_DISK_OFFSET =
            // zip64 end of central dir
            ZipConstants.WORD + // signature
                // size of zip64 end of central
                ZipConstants.DWORD + // directory record
                ZipConstants.SHORT + // version made by
                ZipConstants.SHORT + // version needed to extract
                ZipConstants.WORD // number of this disk

        /**
         * Offset of the field that holds the location of the first central directory entry
         * inside the "Zip64 end of central directory record" relative to the "number of the disk
         * with the start of the central directory".
         */
        private const val ZIP64_EOCD_CFD_LOCATOR_RELATIVE_OFFSET =
            // total number of entries in the
            ZipConstants.DWORD + // central directory on this disk
                // total number of entries in the
                ZipConstants.DWORD + // central directory
                ZipConstants.DWORD // size of the central directory

        /**
         * Number of bytes in local file header up to the "length of file name" entry.
         */
        private val LFH_OFFSET_FOR_FILENAME_LENGTH: Long =
            ZipConstants.WORD.toLong() + // local file header signature
                ZipConstants.SHORT + // version needed to extract
                ZipConstants.SHORT + // general purpose bit flag
                ZipConstants.SHORT + // compression method
                ZipConstants.SHORT + // last mod file time
                ZipConstants.SHORT + // last mod file date
                ZipConstants.WORD + // crc-32
                ZipConstants.WORD + // compressed size
                ZipConstants.WORD // uncompressed size

        /**
         * Compares two ZipArchiveEntries based on their offset within the archive.
         *
         * Won't return any meaningful results if one of the entries isn't part of the archive at
         * all.
         */
        private val offsetComparator: Comparator<ZipArchiveEntry> =
            compareBy({ it.diskNumberStart }, { it.localHeaderOffset })

        /**
         * Opens the given channel for reading, assuming "UTF8" for file names.
         *
         * @param channel the archive.
         * @throws IOException if an error occurs while reading the file.
         */
        suspend operator fun invoke(channel: SeekableByteChannel): ZipFile =
            invoke(channel, "unknown archive", ZipEncodingHelper.UTF8, true)

        /**
         * Opens the given channel for reading, assuming the specified encoding for file names.
         *
         * @param channel the archive.
         * @param encoding the encoding to use for file names.
         * @throws IOException if an error occurs while reading the file.
         */
        suspend operator fun invoke(channel: SeekableByteChannel, encoding: String?): ZipFile =
            invoke(channel, "unknown archive", encoding, true)

        /**
         * Opens the given channel for reading, assuming "UTF8" for file names.
         *
         * @param channel the archive.
         * @param ignoreLocalFileHeader whether to ignore information stored inside the local file
         * header.
         * @throws IOException if an error occurs while reading the file.
         */
        suspend operator fun invoke(
            channel: SeekableByteChannel,
            ignoreLocalFileHeader: Boolean,
        ): ZipFile =
            invoke(
                channel,
                "unknown archive",
                ZipEncodingHelper.UTF8,
                true,
                ignoreLocalFileHeader
            )

        /**
         * Opens the given channel for reading, assuming the specified encoding for file names.
         *
         * By default the central directory record and all local file headers of the archive will
         * be read immediately which may take a considerable amount of time when the archive is
         * big. The [ignoreLocalFileHeader] parameter can be set to true which restricts parsing
         * to the central directory. Unfortunately the local file header may contain information
         * not present inside of the central directory which will not be available when the
         * argument is set to true. This includes the content of the Unicode extra field, so
         * setting [ignoreLocalFileHeader] to true means [useUnicodeExtraFields] will be ignored
         * effectively.
         *
         * @param channel the archive.
         * @param archiveName name of the archive, used for error messages only.
         * @param encoding the encoding to use for file names.
         * @param useUnicodeExtraFields whether to use InfoZIP Unicode Extra Fields (if present)
         * to set the file names.
         * @param ignoreLocalFileHeader whether to ignore information stored inside the local file
         * header.
         * @throws IOException if an error occurs while reading the file.
         */
        suspend operator fun invoke(
            channel: SeekableByteChannel,
            archiveName: String,
            encoding: String?,
            useUnicodeExtraFields: Boolean,
            ignoreLocalFileHeader: Boolean = false,
        ): ZipFile {
            val zipFile = ZipFile(channel, archiveName, encoding, useUnicodeExtraFields)
            zipFile.initialize(ignoreLocalFileHeader)
            return zipFile
        }

        /**
         * Closes a zip file quietly; throws no IOException, does nothing on null input.
         */
        fun closeQuietly(zipFile: ZipFile?) {
            IOUtils.closeQuietly(zipFile)
        }
    }
}
