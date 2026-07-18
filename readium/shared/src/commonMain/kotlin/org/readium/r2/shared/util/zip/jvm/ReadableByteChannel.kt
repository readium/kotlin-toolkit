/*
 * Copyright 2026 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

// Translated from the vendored `java.nio.channels` mirror previously located in
// `:readium:readium-shared-zip-legacy` (same package, Java), itself derived from AOSP.

package org.readium.r2.shared.util.zip.jvm

/**
 * A [ReadableByteChannel] is a type of [Channel] that can read bytes.
 */
internal interface ReadableByteChannel : Channel {

    /**
     * Reads bytes from the channel into the given buffer.
     *
     * The maximum number of bytes that will be read is the [ZipBuffer.remaining] number of bytes
     * in the buffer when the method is invoked. The bytes will be read into the buffer starting at
     * the buffer's current [ZipBuffer.position].
     *
     * Upon completion, the buffer's `position` is updated to the end of the bytes that were read.
     * The buffer's [ZipBuffer.limit] is not changed.
     *
     * @param buffer the buffer to receive the bytes.
     * @return the number of bytes actually read, or -1 if the end of the data has been reached.
     * @throws ClosedChannelException if the channel is closed.
     * @throws okio.IOException if another I/O error occurs.
     */
    suspend fun read(buffer: ZipBuffer): Int
}
