/*
 * Copyright 2026 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

// Translated from the vendored `java.nio.channels` mirror previously located in
// `:readium:readium-shared-zip-legacy` (same package, Java), itself derived from AOSP.

package org.readium.r2.shared.util.zip.jvm

/**
 * An interface for channels that keep a pointer to a current position within an underlying
 * byte-based data source such as a file.
 *
 * [SeekableByteChannel]s have a pointer into the underlying data source which is referred to as a
 * *position*. The position can be manipulated by moving it within the data source, and the current
 * position can be queried.
 *
 * [SeekableByteChannel]s also have an associated *size*. The size of the channel is the number of
 * bytes that the data source currently contains.
 */
internal interface SeekableByteChannel : ByteChannel {

    /**
     * Returns the current position as a positive number of bytes from the start of the underlying
     * data source.
     *
     * Implementations may throw [ClosedChannelException] if this channel is closed, or
     * [okio.IOException] if another I/O error occurs.
     */
    suspend fun position(): Long

    /**
     * Sets the channel's position to [newPosition].
     *
     * The argument is the number of bytes counted from the start of the data source. The position
     * cannot be set to a value that is negative. The new position can be set beyond the current
     * size. If set beyond the current size, attempts to read will return end-of-file.
     *
     * @return the channel.
     * @throws IllegalArgumentException if the new position is negative.
     * @throws ClosedChannelException if this channel is closed.
     * @throws okio.IOException if another I/O error occurs.
     */
    suspend fun position(newPosition: Long): SeekableByteChannel

    /**
     * Returns the size of the data source underlying this channel in bytes.
     *
     * @throws ClosedChannelException if this channel is closed.
     * @throws okio.IOException if an I/O error occurs.
     */
    suspend fun size(): Long

    /**
     * Truncates the data source underlying this channel to a given size. Any bytes beyond the
     * given size are removed. If there are no bytes beyond the given size then the contents are
     * unmodified.
     *
     * If the position is currently greater than the given size, then it is set to the new size.
     *
     * @return this channel.
     * @throws IllegalArgumentException if the requested size is negative.
     * @throws ClosedChannelException if this channel is closed.
     * @throws NonWritableChannelException if the channel cannot be written to.
     * @throws okio.IOException if another I/O error occurs.
     */
    suspend fun truncate(size: Long): SeekableByteChannel

    /**
     * Reads bytes from this channel into the given buffer.
     *
     * If the channel's position is beyond the current end of the underlying data source then
     * end-of-file (-1) is returned.
     *
     * The bytes are read starting at the channel's current position, and after some number of
     * bytes are read (up to the [ZipBuffer.remaining] number of bytes in the buffer) the channel's
     * position is increased by the number of bytes actually read.
     */
    override suspend fun read(buffer: ZipBuffer): Int

    /**
     * Writes bytes from the given buffer to this channel.
     *
     * The bytes are written starting at the channel's current position, and after some number of
     * bytes are written (up to the [ZipBuffer.remaining] number of bytes in the buffer) the
     * channel's position is increased by the number of bytes actually written.
     */
    override suspend fun write(buffer: ZipBuffer): Int
}
