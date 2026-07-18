/*
 * Copyright 2026 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

@file:OptIn(ExperimentalForeignApi::class)

package org.readium.r2.shared.util.zip.compress.archivers.zip

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.alloc
import kotlinx.cinterop.free
import kotlinx.cinterop.nativeHeap
import kotlinx.cinterop.ptr
import kotlinx.cinterop.reinterpret
import kotlinx.cinterop.sizeOf
import kotlinx.cinterop.toKString
import kotlinx.cinterop.usePinned
import platform.zlib.Z_BUF_ERROR
import platform.zlib.Z_DATA_ERROR
import platform.zlib.Z_MEM_ERROR
import platform.zlib.Z_NEED_DICT
import platform.zlib.Z_NO_FLUSH
import platform.zlib.Z_OK
import platform.zlib.Z_STREAM_END
import platform.zlib.inflateEnd
import platform.zlib.inflateInit2_
import platform.zlib.z_stream
import platform.zlib.zlibVersion

/**
 * iOS actual of the zip [Inflater], wrapping the platform zlib.
 */
internal actual class Inflater actual constructor(nowrap: Boolean) {

    private val stream = nativeHeap.alloc<z_stream>()

    private var input: ByteArray = ByteArray(0)

    private var inputPosition: Int = 0

    private var inputEnd: Int = 0

    private var finished: Boolean = false

    private var needsDictionary: Boolean = false

    private var bytesRead: Long = 0

    private var bytesWritten: Long = 0

    private var ended: Boolean = false

    init {
        // `nativeHeap.alloc` zero-initializes the struct, but zlib requires zalloc/zfree/opaque
        // to be null for its default allocators: set them explicitly rather than relying on it.
        stream.zalloc = null
        stream.zfree = null
        stream.opaque = null
        // Negative window bits configure a raw deflate stream (no zlib header nor checksum),
        // mirroring `java.util.zip.Inflater(nowrap = true)`.
        val windowBits = if (nowrap) -MAX_WINDOW_BITS else MAX_WINDOW_BITS
        val result = inflateInit2_(
            stream.ptr,
            windowBits,
            zlibVersion()?.toKString(),
            sizeOf<z_stream>().toInt()
        )
        if (result != Z_OK) {
            nativeHeap.free(stream)
            ended = true
            throw IllegalStateException("Failed to initialize zlib inflate: $result")
        }
    }

    actual fun setInput(b: ByteArray, off: Int, len: Int) {
        checkNotEnded()
        require(off >= 0 && len >= 0 && off + len <= b.size)
        input = b
        inputPosition = off
        inputEnd = off + len
    }

    actual fun needsInput(): Boolean =
        inputPosition >= inputEnd

    actual fun needsDictionary(): Boolean =
        needsDictionary

    actual fun finished(): Boolean =
        finished

    actual fun inflate(b: ByteArray, off: Int, len: Int): Int {
        checkNotEnded()
        require(off >= 0 && len >= 0 && off + len <= b.size)
        if (len == 0 || finished || needsDictionary) {
            return 0
        }

        val available = inputEnd - inputPosition
        var produced = 0
        var consumed = 0

        b.usePinned { output ->
            fun runInflate(): Int {
                stream.next_out = output.addressOf(off).reinterpret()
                stream.avail_out = len.toUInt()
                val result = platform.zlib.inflate(stream.ptr, Z_NO_FLUSH)
                produced = len - stream.avail_out.toInt()
                // Don't keep a dangling pointer into the array once it is unpinned.
                stream.next_out = null
                stream.avail_out = 0u
                return result
            }

            val result =
                if (available > 0) {
                    input.usePinned { pinnedInput ->
                        stream.next_in = pinnedInput.addressOf(inputPosition).reinterpret()
                        stream.avail_in = available.toUInt()
                        val res = runInflate()
                        consumed = available - stream.avail_in.toInt()
                        // Don't keep a dangling pointer into the array once it is unpinned.
                        stream.next_in = null
                        stream.avail_in = 0u
                        res
                    }
                } else {
                    stream.next_in = null
                    stream.avail_in = 0u
                    runInflate()
                }

            when (result) {
                Z_OK, Z_BUF_ERROR -> {
                    // Z_BUF_ERROR indicates no progress was possible: more input is needed.
                }
                Z_STREAM_END -> finished = true
                Z_NEED_DICT -> needsDictionary = true
                Z_DATA_ERROR -> throw ZipException(
                    stream.msg?.toKString() ?: "Invalid deflate data"
                )
                Z_MEM_ERROR -> throw IllegalStateException("zlib inflate ran out of memory")
                else -> throw IllegalStateException("zlib inflate failed with code $result")
            }
        }

        inputPosition += consumed
        bytesRead += consumed
        bytesWritten += produced
        return produced
    }

    actual fun getRemaining(): Int =
        inputEnd - inputPosition

    actual fun getBytesRead(): Long =
        bytesRead

    actual fun getBytesWritten(): Long =
        bytesWritten

    actual fun end() {
        if (!ended) {
            ended = true
            inflateEnd(stream.ptr)
            nativeHeap.free(stream)
            input = ByteArray(0)
            inputPosition = 0
            inputEnd = 0
        }
    }

    private fun checkNotEnded() {
        check(!ended) { "The Inflater has already been ended." }
    }

    private companion object {
        private const val MAX_WINDOW_BITS = 15
    }
}
