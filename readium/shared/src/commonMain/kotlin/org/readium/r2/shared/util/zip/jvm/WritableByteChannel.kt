/*
 * Copyright 2026 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

// Translated from the vendored `java.nio.channels` mirror previously located in
// the vendored Java channel shims (same package, deleted at the end of phase 05), themselves
// derived from AOSP.

package org.readium.r2.shared.util.zip.jvm

/**
 * A [WritableByteChannel] is a type of [Channel] that can write bytes.
 */
internal interface WritableByteChannel : Channel {

    /**
     * Writes bytes from the given buffer to the channel.
     *
     * The maximum number of bytes that will be written is the [ZipBuffer.remaining] number of
     * bytes in the buffer when the method is invoked. The bytes will be written from the buffer
     * starting at the buffer's current [ZipBuffer.position].
     *
     * Upon completion, the buffer's `position` is updated to the end of the bytes that were
     * written. The buffer's [ZipBuffer.limit] is unmodified.
     *
     * @param buffer the buffer containing the bytes to be written.
     * @return the number of bytes actually written.
     * @throws NonWritableChannelException if the channel was not opened for writing.
     * @throws ClosedChannelException if the channel was already closed.
     * @throws okio.IOException if another I/O error occurs.
     */
    suspend fun write(buffer: ZipBuffer): Int
}
