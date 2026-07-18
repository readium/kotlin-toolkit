/*
 * Copyright 2026 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.shared.util.zip.jvm

/**
 * A minimal Readium-owned replacement for `java.nio.ByteBuffer`, implementing exactly the subset
 * of operations used by the vendored Commons Compress zip stack (see phase 05a of the KMP plan).
 *
 * The buffer is always backed by a heap [ByteArray] and always big-endian-agnostic: the zip stack
 * never uses multi-byte accessors (`getShort`, `putInt`…) nor `order()`, `slice()`, `duplicate()`,
 * `mark()`/`reset()` or `compact()`, so none of these are provided.
 *
 * Semantics mirror `java.nio.Buffer`: `0 <= position <= limit <= capacity`. Relative [get] and
 * [put] operate between `position` and `limit` and advance `position`. Out-of-bounds accesses
 * throw [IndexOutOfBoundsException] (where `java.nio` throws `BufferUnderflowException` /
 * `BufferOverflowException`, which the zip stack never catches).
 *
 * This class is not thread-safe.
 */
internal class ZipBuffer private constructor(
    private val data: ByteArray,
    private var currentPosition: Int,
    private var currentLimit: Int,
) {

    /** Returns the capacity of this buffer. */
    fun capacity(): Int = data.size

    /** Returns the current position of this buffer. */
    fun position(): Int = currentPosition

    /**
     * Sets the position of this buffer.
     *
     * @throws IllegalArgumentException if [newPosition] is negative or greater than the limit.
     */
    fun position(newPosition: Int): ZipBuffer {
        require(newPosition in 0..currentLimit) {
            "position ($newPosition) is out of bounds (limit: $currentLimit)"
        }
        currentPosition = newPosition
        return this
    }

    /** Returns the limit of this buffer. */
    fun limit(): Int = currentLimit

    /**
     * Sets the limit of this buffer. If the position is greater than the new limit, it is set to
     * the new limit.
     *
     * @throws IllegalArgumentException if [newLimit] is negative or greater than the capacity.
     */
    fun limit(newLimit: Int): ZipBuffer {
        require(newLimit in 0..data.size) {
            "limit ($newLimit) is out of bounds (capacity: ${data.size})"
        }
        currentLimit = newLimit
        if (currentPosition > newLimit) {
            currentPosition = newLimit
        }
        return this
    }

    /** Returns the number of remaining bytes between the position and the limit. */
    fun remaining(): Int = currentLimit - currentPosition

    /** Returns true if there are remaining bytes between the position and the limit. */
    fun hasRemaining(): Boolean = currentPosition < currentLimit

    /** Clears this buffer: the position is set to 0 and the limit to the capacity. */
    fun clear(): ZipBuffer {
        currentPosition = 0
        currentLimit = data.size
        return this
    }

    /** Flips this buffer: the limit is set to the position and the position to 0. */
    fun flip(): ZipBuffer {
        currentLimit = currentPosition
        currentPosition = 0
        return this
    }

    /** Rewinds this buffer: the position is set to 0, the limit is unchanged. */
    fun rewind(): ZipBuffer {
        currentPosition = 0
        return this
    }

    /**
     * Reads the byte at the current position and increments the position.
     *
     * @throws IndexOutOfBoundsException if the position is not smaller than the limit.
     */
    fun get(): Byte {
        if (currentPosition >= currentLimit) {
            throw IndexOutOfBoundsException("Buffer underflow: no remaining byte to get.")
        }
        return data[currentPosition++]
    }

    /**
     * Reads [length] bytes into [dst] starting at [offset], from the current position which is
     * incremented accordingly.
     *
     * @throws IndexOutOfBoundsException if [length] is greater than [remaining], or if [offset]
     * and [length] don't fit into [dst].
     */
    fun get(dst: ByteArray, offset: Int = 0, length: Int = dst.size - offset): ZipBuffer {
        checkSubArray(dst, offset, length)
        if (length > remaining()) {
            throw IndexOutOfBoundsException(
                "Buffer underflow: $length bytes requested, ${remaining()} remaining."
            )
        }
        data.copyInto(dst, offset, currentPosition, currentPosition + length)
        currentPosition += length
        return this
    }

    /**
     * Writes the given byte at the current position and increments the position.
     *
     * @throws IndexOutOfBoundsException if the position is not smaller than the limit.
     */
    fun put(byte: Byte): ZipBuffer {
        if (currentPosition >= currentLimit) {
            throw IndexOutOfBoundsException("Buffer overflow: no remaining space to put a byte.")
        }
        data[currentPosition++] = byte
        return this
    }

    /**
     * Writes [length] bytes from [src] starting at [offset], at the current position which is
     * incremented accordingly.
     *
     * @throws IndexOutOfBoundsException if [length] is greater than [remaining], or if [offset]
     * and [length] don't fit into [src].
     */
    fun put(src: ByteArray, offset: Int = 0, length: Int = src.size - offset): ZipBuffer {
        checkSubArray(src, offset, length)
        if (length > remaining()) {
            throw IndexOutOfBoundsException(
                "Buffer overflow: $length bytes given, ${remaining()} remaining."
            )
        }
        src.copyInto(data, currentPosition, offset, offset + length)
        currentPosition += length
        return this
    }

    /**
     * Writes the remaining bytes of [src] at the current position. Both buffer positions are
     * incremented by the number of bytes copied.
     *
     * @throws IllegalArgumentException if [src] is this buffer.
     * @throws IndexOutOfBoundsException if the remaining bytes of [src] are greater than
     * [remaining].
     */
    fun put(src: ZipBuffer): ZipBuffer {
        require(src !== this) { "The source buffer must not be this buffer." }
        val length = src.remaining()
        checkPutBuffer(length)
        src.get(data, currentPosition, length)
        currentPosition += length
        return this
    }

    /** Returns the backing array of this buffer. */
    fun array(): ByteArray = data

    /**
     * Returns the offset in the backing array of the first element of the buffer.
     *
     * Always 0: unlike `java.nio.ByteBuffer`, [ZipBuffer] doesn't support views (`slice()`).
     */
    fun arrayOffset(): Int = 0

    private fun checkPutBuffer(length: Int) {
        if (length > remaining()) {
            throw IndexOutOfBoundsException(
                "Buffer overflow: $length bytes given, ${remaining()} remaining."
            )
        }
    }

    private fun checkSubArray(array: ByteArray, offset: Int, length: Int) {
        if (offset < 0 || length < 0 || offset + length > array.size) {
            throw IndexOutOfBoundsException(
                "Range [$offset, $offset + $length) is out of bounds (size: ${array.size})."
            )
        }
    }

    companion object {

        /**
         * Allocates a new buffer of the given [capacity], with the position set to 0 and the limit
         * to [capacity].
         */
        fun allocate(capacity: Int): ZipBuffer {
            require(capacity >= 0) { "capacity must not be negative" }
            return ZipBuffer(ByteArray(capacity), currentPosition = 0, currentLimit = capacity)
        }

        /**
         * Wraps the given [array]: the buffer is backed by it, with the position set to 0 and the
         * limit to the array size.
         */
        fun wrap(array: ByteArray): ZipBuffer =
            ZipBuffer(array, currentPosition = 0, currentLimit = array.size)

        /**
         * Wraps the given [array]: the buffer is backed by it, with the position set to [offset]
         * and the limit to `offset + length`. The capacity is the full array size.
         *
         * @throws IndexOutOfBoundsException if [offset] and [length] don't fit into [array].
         */
        fun wrap(array: ByteArray, offset: Int, length: Int): ZipBuffer {
            if (offset < 0 || length < 0 || offset + length > array.size) {
                throw IndexOutOfBoundsException(
                    "Range [$offset, $offset + $length) is out of bounds (size: ${array.size})."
                )
            }
            return ZipBuffer(array, currentPosition = offset, currentLimit = offset + length)
        }
    }
}
