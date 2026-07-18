/*
 * Copyright 2026 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.shared.util.http

import io.ktor.utils.io.ByteReadChannel
import io.ktor.utils.io.cancel
import io.ktor.utils.io.discard
import io.ktor.utils.io.readAvailable
import kotlin.concurrent.Volatile
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import okio.Buffer
import org.readium.r2.shared.util.DebugError
import org.readium.r2.shared.util.ThrowableError
import org.readium.r2.shared.util.Try
import org.readium.r2.shared.util.data.ReadError
import org.readium.r2.shared.util.data.Readable

/**
 * A [Readable] streaming an HTTP response body from a Ktor [ByteReadChannel].
 *
 * Only forward reads are supported: a range starting before the current position fails with
 * [ReadError.UnsupportedOperation].
 */
internal class HttpChannelReadable(
    private val channel: ByteReadChannel,
    private val knownLength: Long?,
    private val onClose: () -> Unit,
) : Readable {

    private val mutex = Mutex()

    /** Offset of the next byte to read, from the beginning of the response body. */
    private var position: Long = 0

    @Volatile
    private var closed: Boolean = false

    override suspend fun length(): Try<Long, ReadError> =
        knownLength?.let { Try.success(it) }
            ?: Try.failure(
                ReadError.UnsupportedOperation(
                    DebugError("Length of the HTTP response is unknown.")
                )
            )

    override suspend fun read(range: LongRange?): Try<ByteArray, ReadError> = mutex.withLock {
        if (closed) {
            return Try.failure(
                ReadError.Access(
                    HttpError.IO(DebugError("The HTTP response body is closed."))
                )
            )
        }

        try {
            if (range == null) {
                val bytes = channel.readUpTo(Long.MAX_VALUE)
                position += bytes.size
                return Try.success(bytes)
            }

            val start = range.first.coerceAtLeast(0)
            if (range.last < start) {
                return Try.success(ByteArray(0))
            }
            // `range.last - start + 1` could overflow for an unbounded range like
            // `0..Long.MAX_VALUE`, so the distance is clamped instead.
            val count = (range.last - start)
                .let { if (it == Long.MAX_VALUE) it else it + 1 }

            if (start < position) {
                return Try.failure(
                    ReadError.UnsupportedOperation(
                        DebugError(
                            "Cannot read backward from an HTTP response body (position: $position, requested: $start)."
                        )
                    )
                )
            }

            if (start > position) {
                position += channel.discard(start - position)
                if (position < start) {
                    // End of the body was reached while skipping.
                    return Try.success(ByteArray(0))
                }
            }

            val bytes = channel.readUpTo(count)
            position += bytes.size
            Try.success(bytes)
        } catch (e: CancellationException) {
            // Rethrows only if the caller itself was cancelled; a cancellation of the
            // underlying channel (e.g. because the client was closed) is reported as an error.
            currentCoroutineContext().ensureActive()
            Try.failure(
                ReadError.Access(
                    HttpError.IO(DebugError("The HTTP response body was cancelled.", ThrowableError(e)))
                )
            )
        } catch (e: Throwable) {
            Try.failure(ReadError.Access(mapHttpException(e)))
        }
    }

    override fun close() {
        if (closed) {
            return
        }
        closed = true
        channel.cancel()
        onClose()
    }
}

/**
 * Reads up to [count] bytes from the channel, stopping at the end of the body.
 */
internal suspend fun ByteReadChannel.readUpTo(count: Long): ByteArray {
    val buffer = Buffer()
    val chunk = ByteArray(8192)
    var remaining = count

    while (remaining > 0) {
        val toRead = minOf(remaining, chunk.size.toLong()).toInt()
        val read = readAvailable(chunk, 0, toRead)
        if (read == -1) {
            break
        }
        if (read > 0) {
            buffer.write(chunk, 0, read)
            remaining -= read
        }
    }

    return buffer.readByteArray()
}
