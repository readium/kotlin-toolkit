/*
 * Copyright 2026 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.shared.util.zip.compress.archivers.zip

/**
 * A decompressor for the deflate compression format, replacing `java.util.zip.Inflater` in the
 * ported zip stack (phase 05b).
 *
 * Only the subset used by [InflaterInputStreamWithStatistics] is exposed. The Android actual
 * delegates to `java.util.zip.Inflater`; the iOS actual wraps the platform zlib.
 *
 * @param nowrap if true, the raw deflate stream is expected (no zlib header and checksum), as
 * used by zip entries.
 */
internal expect class Inflater(nowrap: Boolean) {

    /** Sets input data for decompression. */
    fun setInput(b: ByteArray, off: Int, len: Int)

    /** Returns true if no data remains in the input buffer. */
    fun needsInput(): Boolean

    /** Returns true if a preset dictionary is needed for decompression. */
    fun needsDictionary(): Boolean

    /** Returns true if the end of the compressed data stream has been reached. */
    fun finished(): Boolean

    /**
     * Uncompresses bytes into the specified buffer.
     *
     * @return the actual number of bytes of uncompressed data. 0 indicates that [needsInput] or
     * [needsDictionary] should be checked to determine if more input data or a preset dictionary
     * is required.
     * @throws ZipException if the compressed data format is invalid.
     */
    fun inflate(b: ByteArray, off: Int, len: Int): Int

    /** Returns the total number of bytes remaining in the input buffer. */
    fun getRemaining(): Int

    /** Returns the total number of compressed bytes input so far. */
    fun getBytesRead(): Long

    /** Returns the total number of uncompressed bytes output so far. */
    fun getBytesWritten(): Long

    /**
     * Closes the decompressor and discards any unprocessed input. This method should be called
     * when the decompressor is no longer being used. Once called, the [Inflater] is unusable.
     */
    fun end()
}
