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
// (Java sources: `BoundedArchiveInputStream.java` and
// `BoundedSeekableByteChannelInputStream.java`, phase 05b). The `synchronized` blocks of the
// original were dropped: per the 05a thread-safety contract, streams and channels are not
// thread-safe and callers serialize accesses externally.

package org.readium.r2.shared.util.zip.compress.utils

import org.readium.r2.shared.util.zip.jvm.SeekableByteChannel
import org.readium.r2.shared.util.zip.jvm.ZipBuffer

/**
 * A stream that limits reading from a hard-coded region of the underlying storage.
 */
internal abstract class BoundedArchiveInputStream(
    start: Long,
    remaining: Long,
) : ZipInputStream() {

    private val end: Long = start + remaining

    private var singleZipBuffer: ZipBuffer? = null

    private var loc: Long = start

    init {
        require(end >= start) {
            // check for potential vulnerability due to overflow
            "Invalid length of stream at offset=$start, length=$remaining"
        }
    }

    override suspend fun read(): Int {
        if (loc >= end) {
            return -1
        }
        val buffer = singleZipBuffer?.also { it.rewind() }
            ?: ZipBuffer.allocate(1).also { singleZipBuffer = it }
        val read = read(loc, buffer)
        if (read < 1) {
            return -1
        }
        loc++
        return buffer.array()[0].toInt() and 0xff
    }

    override suspend fun read(b: ByteArray, off: Int, len: Int): Int {
        if (loc >= end) {
            return -1
        }
        val maxLen = minOf(len.toLong(), end - loc)
        if (maxLen <= 0) {
            return 0
        }
        if (off < 0 || off > b.size || maxLen > b.size - off) {
            throw IndexOutOfBoundsException("offset or len are out of bounds")
        }

        val buf = ZipBuffer.wrap(b, off, maxLen.toInt())
        val ret = read(loc, buf)
        if (ret > 0) {
            loc += ret
        }
        return ret
    }

    /**
     * Reads content of the stream into a [ZipBuffer].
     *
     * @param pos position to start the read.
     * @param buf buffer to add the read content.
     * @return number of read bytes.
     */
    protected abstract suspend fun read(pos: Long, buf: ZipBuffer): Int
}

/**
 * A [ZipInputStream] that reads a bounded region from a [SeekableByteChannel].
 */
internal class BoundedSeekableByteChannelInputStream(
    start: Long,
    remaining: Long,
    private val channel: SeekableByteChannel,
) : BoundedArchiveInputStream(start, remaining) {

    override suspend fun read(pos: Long, buf: ZipBuffer): Int {
        channel.position(pos)
        val read = channel.read(buf)
        buf.flip()
        return read
    }
}
