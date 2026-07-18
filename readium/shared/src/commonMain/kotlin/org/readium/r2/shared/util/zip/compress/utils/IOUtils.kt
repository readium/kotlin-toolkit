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
// (Java source: `util/zip/compress/utils/IOUtils.java`, phase 05b). Only the helpers used by the
// read-only zip stack were kept, adapted to the suspending channels and [ZipInputStream].

package org.readium.r2.shared.util.zip.compress.utils

import okio.EOFException
import org.readium.r2.shared.util.Closeable
import org.readium.r2.shared.util.zip.jvm.ReadableByteChannel
import org.readium.r2.shared.util.zip.jvm.ZipBuffer

internal object IOUtils {

    private const val COPY_BUF_SIZE = 8024

    /** Closes the given [Closeable], ignoring any error. */
    fun closeQuietly(c: Closeable?) {
        try {
            c?.close()
        } catch (_: Exception) {
        }
    }

    /**
     * Reads as much from the channel as possible to fill the given buffer, and throws
     * [EOFException] if it cannot be filled entirely.
     */
    suspend fun readFully(channel: ReadableByteChannel, byteBuffer: ZipBuffer) {
        val expectedLength = byteBuffer.remaining()
        var read = 0
        while (read < expectedLength) {
            val readNow = channel.read(byteBuffer)
            if (readNow <= 0) {
                break
            }
            read += readNow
        }
        if (read < expectedLength) {
            throw EOFException()
        }
    }

    /**
     * Reads [len] bytes from the [input] channel. The returned array may be shorter if the channel
     * doesn't have that many bytes left.
     */
    suspend fun readRange(input: ReadableByteChannel, len: Int): ByteArray {
        var output = ByteArray(len)
        val b = ZipBuffer.allocate(minOf(len, COPY_BUF_SIZE))
        var read = 0
        while (read < len) {
            // Make sure we never read more than len bytes
            b.clear()
            b.limit(minOf(len - read, b.capacity()))
            val readNow = input.read(b)
            if (readNow <= 0) {
                break
            }
            b.array().copyInto(output, read, 0, readNow)
            read += readNow
        }
        if (read < len) {
            output = output.copyOf(read)
        }
        return output
    }

    /** Reads the whole content of the given stream. */
    suspend fun toByteArray(input: ZipInputStream): ByteArray {
        val output = okio.Buffer()
        val buffer = ByteArray(COPY_BUF_SIZE)
        while (true) {
            val n = input.read(buffer)
            if (n == -1) {
                break
            }
            output.write(buffer, 0, n)
        }
        return output.readByteArray()
    }
}
