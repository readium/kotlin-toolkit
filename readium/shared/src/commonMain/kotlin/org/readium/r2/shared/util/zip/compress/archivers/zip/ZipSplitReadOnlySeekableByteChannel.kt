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
// (Java source: `util/zip/compress/archivers/zip/ZipSplitReadOnlySeekableByteChannel.java`,
// phase 05b). The suspending split-signature check moved from the constructor to the [create]
// factory, and the `java.io.File`/`Path` factories were dropped.

package org.readium.r2.shared.util.zip.compress.archivers.zip

import okio.IOException
import org.readium.r2.shared.util.zip.compress.utils.MultiReadOnlySeekableByteChannel
import org.readium.r2.shared.util.zip.jvm.SeekableByteChannel
import org.readium.r2.shared.util.zip.jvm.ZipBuffer

/**
 * [MultiReadOnlySeekableByteChannel] that knows what a split ZIP archive should look like.
 *
 * If you want to read a split archive using [ZipFile] then create an instance of this class from
 * the parts of the archive.
 */
internal class ZipSplitReadOnlySeekableByteChannel private constructor(
    channels: List<SeekableByteChannel>,
) : MultiReadOnlySeekableByteChannel(channels) {

    companion object {

        private const val ZIP_SPLIT_SIGNATURE_LENGTH = 4

        /**
         * Concatenates the given channels into a split zip archive channel.
         *
         * The first channel should begin with the zip split signature `0x08074B50`.
         *
         * @throws IOException if the first channel doesn't begin with the zip split signature.
         */
        suspend fun create(
            channels: List<SeekableByteChannel>,
        ): ZipSplitReadOnlySeekableByteChannel {
            val channel = ZipSplitReadOnlySeekableByteChannel(channels)
            assertSplitSignature(channels)
            return channel
        }

        /**
         * Concatenates the given channels.
         *
         * @param channels the channels to concatenate, note that the LAST CHANNEL of channels
         * should be the LAST SEGMENT(.zip) and theses channels should be added in ascending order
         * (e.g. .z01, .z02... .z99, .zip)
         */
        suspend fun forOrderedSeekableByteChannels(
            vararg channels: SeekableByteChannel,
        ): SeekableByteChannel {
            if (channels.size == 1) {
                return channels[0]
            }
            return create(channels.toList())
        }

        /**
         * The first split zip segment should begin with the zip split signature.
         */
        private suspend fun assertSplitSignature(channels: List<SeekableByteChannel>) {
            val channel = channels[0]
            // the zip split file signature is at the beginning of the first split segment
            channel.position(0L)

            val signatureBuffer = ZipBuffer.allocate(ZIP_SPLIT_SIGNATURE_LENGTH)
            channel.read(signatureBuffer)
            val signature = ZipLong(signatureBuffer.array())
            if (signature != ZipLong.DD_SIG) {
                channel.position(0L)
                throw IOException(
                    "The first zip split segment does not begin with split zip file signature"
                )
            }

            channel.position(0L)
        }
    }
}
