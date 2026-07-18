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
// (Java source: `util/zip/compress/archivers/zip/InflaterInputStreamWithStatistics.java`,
// phase 05b). The `java.util.zip.InflaterInputStream` base class is not available in common
// code: its decompression loop was folded into this class on top of the expect/actual [Inflater].
// The single trailing zero byte historically required by `Inflater(nowrap = true)` (which the
// Java code injected with a `SequenceInputStream`) is supplied by [fill] after the underlying
// stream is exhausted.

package org.readium.r2.shared.util.zip.compress.archivers.zip

import okio.EOFException
import org.readium.r2.shared.util.zip.compress.utils.InputStreamStatistics
import org.readium.r2.shared.util.zip.compress.utils.ZipInputStream

/**
 * Helper class to provide statistics on the compressed and uncompressed size of a deflated
 * stream, while inflating it.
 */
internal open class InflaterInputStreamWithStatistics(
    private val input: ZipInputStream,
    protected val inflater: Inflater,
    bufferSize: Int = DEFAULT_BUFFER_SIZE,
) : ZipInputStream(), InputStreamStatistics {

    init {
        require(bufferSize > 0) { "bufferSize must be positive" }
    }

    private val buffer = ByteArray(bufferSize)

    private var reachedEOF = false

    private var dummyByteSupplied = false

    final override var compressedCount: Long = 0
        private set

    final override var uncompressedCount: Long = 0
        private set

    /**
     * Fills the inflater input buffer with more data from the underlying stream.
     *
     * `Inflater(nowrap = true)` has this odd contract for a zero padding byte following the data
     * stream; this used to be zlib's requirement and has been fixed a long time ago, but the
     * contract persists so we comply.
     */
    private suspend fun fill() {
        while (true) {
            val n = input.read(buffer, 0, buffer.size)
            if (n > 0) {
                inflater.setInput(buffer, 0, n)
                compressedCount += inflater.getRemaining()
                return
            }
            if (n == -1) {
                break
            }
            // n == 0 is not end of stream: `java.io.InputStream.read` blocks until at least one
            // byte is available, so mirror that contract by retrying (a suspending channel may
            // legitimately deliver empty reads).
        }
        if (!dummyByteSupplied) {
            dummyByteSupplied = true
            buffer[0] = 0
            inflater.setInput(buffer, 0, 1)
            compressedCount += inflater.getRemaining()
        } else {
            throw EOFException("Unexpected end of ZLIB input stream")
        }
    }

    override suspend fun read(): Int {
        val singleByte = ByteArray(1)
        val n = read(singleByte, 0, 1)
        return if (n < 1) -1 else singleByte[0].toInt() and 0xff
    }

    override suspend fun read(b: ByteArray, off: Int, len: Int): Int {
        if (len == 0) {
            return 0
        }
        if (reachedEOF) {
            return -1
        }
        while (true) {
            val n = inflater.inflate(b, off, len)
            if (n > 0) {
                uncompressedCount += n
                return n
            }
            when {
                inflater.finished() || inflater.needsDictionary() -> {
                    reachedEOF = true
                    return -1
                }
                inflater.needsInput() -> fill()
                else -> throw ZipException("Inflater stalled without requesting input")
            }
        }
    }

    override fun close() {
        input.close()
    }

    private companion object {
        // The Java original used 512; raised to 8 KB (Readium) to match the buffered channel
        // chunk size and halve the number of suspending fill round-trips per chunk.
        private const val DEFAULT_BUFFER_SIZE = 8192
    }
}
