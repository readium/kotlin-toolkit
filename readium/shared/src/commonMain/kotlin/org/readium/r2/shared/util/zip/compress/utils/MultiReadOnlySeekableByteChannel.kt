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
// (Java source: `util/zip/compress/utils/MultiReadOnlySeekableByteChannel.java`, phase 05b).
// The `java.io.File`/`java.nio.file.Path` factories were dropped (channels are provided by the
// Readium adapters) and the `synchronized` blocks removed per the 05a thread-safety contract.

package org.readium.r2.shared.util.zip.compress.utils

import okio.IOException
import org.readium.r2.shared.util.zip.jvm.ClosedChannelException
import org.readium.r2.shared.util.zip.jvm.NonWritableChannelException
import org.readium.r2.shared.util.zip.jvm.SeekableByteChannel
import org.readium.r2.shared.util.zip.jvm.ZipBuffer

/**
 * Read-Only Implementation of [SeekableByteChannel] that concatenates a collection of other
 * [SeekableByteChannel]s.
 *
 * This is a lose port of
 * [MultiReadOnlySeekableByteChannel](https://github.com/frugalmechanic/fm-common/blob/master/jvm/src/main/scala/fm/common/MultiReadOnlySeekableByteChannel.scala)
 * by Tim Underwood.
 */
internal open class MultiReadOnlySeekableByteChannel(
    channels: List<SeekableByteChannel>,
) : SeekableByteChannel {

    private val channels: List<SeekableByteChannel> = channels.toList()

    private var globalPosition: Long = 0

    private var currentChannelIdx: Int = 0

    override fun close() {
        var first: IOException? = null
        for (ch in channels) {
            try {
                ch.close()
            } catch (ex: IOException) {
                if (first == null) {
                    first = ex
                }
            }
        }
        if (first != null) {
            throw IOException("failed to close wrapped channel", first)
        }
    }

    override val isOpen: Boolean
        get() = channels.all { it.isOpen }

    /**
     * Returns this channel's position.
     *
     * This method violates the contract of [SeekableByteChannel.position] as it will not throw
     * any exception when invoked on a closed channel. Instead it will return the position the
     * channel had when close has been called.
     */
    override suspend fun position(): Long =
        globalPosition

    override suspend fun position(newPosition: Long): SeekableByteChannel {
        require(newPosition >= 0) { "Negative position: $newPosition" }
        if (!isOpen) {
            throw ClosedChannelException()
        }

        globalPosition = newPosition

        var pos = newPosition

        for (i in channels.indices) {
            val currentChannel = channels[i]
            val size = currentChannel.size()

            val newChannelPos: Long
            when {
                pos == -1L -> {
                    // Position is already set for the correct channel, the rest of the channels
                    // get reset to 0
                    newChannelPos = 0
                }
                pos <= size -> {
                    // This channel is where we want to be
                    currentChannelIdx = i
                    val tmp = pos
                    pos = -1L // Mark pos as already being set
                    newChannelPos = tmp
                }
                else -> {
                    // newPosition is past this channel. Set channel position to the end and
                    // subtract channel size from pos
                    pos -= size
                    newChannelPos = size
                }
            }

            currentChannel.position(newChannelPos)
        }
        return this
    }

    /**
     * Sets the position based on the given channel number and relative offset.
     *
     * @param channelNumber the channel number
     * @param relativeOffset the relative offset in the corresponding channel
     * @return global position of all channels as if they are a single channel
     */
    suspend fun position(channelNumber: Long, relativeOffset: Long): SeekableByteChannel {
        if (!isOpen) {
            throw ClosedChannelException()
        }
        var newGlobalPosition = relativeOffset
        for (i in 0 until channelNumber) {
            newGlobalPosition += channels[i.toInt()].size()
        }

        return position(newGlobalPosition)
    }

    override suspend fun read(buffer: ZipBuffer): Int {
        if (!isOpen) {
            throw ClosedChannelException()
        }
        if (!buffer.hasRemaining()) {
            return 0
        }

        var totalBytesRead = 0
        while (buffer.hasRemaining() && currentChannelIdx < channels.size) {
            val currentChannel = channels[currentChannelIdx]
            val newBytesRead = currentChannel.read(buffer)
            if (newBytesRead == -1) {
                // EOF for this channel -- advance to next channel idx
                currentChannelIdx += 1
                continue
            }
            if (currentChannel.position() >= currentChannel.size()) {
                // we are at the end of the current channel
                currentChannelIdx++
            }
            totalBytesRead += newBytesRead
        }
        if (totalBytesRead > 0) {
            globalPosition += totalBytesRead
            return totalBytesRead
        }
        return -1
    }

    override suspend fun size(): Long {
        if (!isOpen) {
            throw ClosedChannelException()
        }
        var acc = 0L
        for (ch in channels) {
            acc += ch.size()
        }
        return acc
    }

    /**
     * @throws NonWritableChannelException since this implementation is read-only.
     */
    override suspend fun truncate(size: Long): SeekableByteChannel {
        throw NonWritableChannelException()
    }

    /**
     * @throws NonWritableChannelException since this implementation is read-only.
     */
    override suspend fun write(buffer: ZipBuffer): Int {
        throw NonWritableChannelException()
    }

    companion object {

        /**
         * Concatenates the given channels.
         */
        fun forSeekableByteChannels(vararg channels: SeekableByteChannel): SeekableByteChannel {
            if (channels.size == 1) {
                return channels[0]
            }
            return MultiReadOnlySeekableByteChannel(channels.toList())
        }
    }
}
