/*
 * Copyright 2023 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.shared.util.data

import okio.IOException
import org.readium.r2.shared.InternalReadiumApi
import org.readium.r2.shared.OutOfMemoryError
import org.readium.r2.shared.util.Closeable
import org.readium.r2.shared.util.DebugError
import org.readium.r2.shared.util.Error
import org.readium.r2.shared.util.ErrorException
import org.readium.r2.shared.util.ThrowableError
import org.readium.r2.shared.util.Try
import org.readium.r2.shared.util.getOrElse

/**
 * Acts as a proxy to an actual data source by handling read access.
 */
public interface Readable : Closeable {

    /**
     * Returns data length from metadata if available, or calculated from reading the bytes otherwise.
     *
     * This value must be treated as a hint, as it might not reflect the actual bytes length. To get
     * the real length, you need to read the whole resource.
     */
    public suspend fun length(): Try<Long, ReadError>

    /**
     * Reads the bytes at the given range in a streaming fashion.
     *
     * When [range] is null, the whole content is streamed. Out-of-range indexes are clamped to the
     * available length automatically.
     *
     * [consume] is called for each chunk received, in order. Callers are responsible for
     * accumulating the data if they need it whole. Each chunk is a freshly allocated array owned by
     * the consumer, so implementations must not hand out a reused buffer.
     *
     * This is the primary member: [read] is derived from it. Implementations that transform the
     * bytes (decryption, injection, deobfuscation) **must** override it — delegating with `by`
     * would silently serve the wrapped source's untransformed bytes.
     */
    public suspend fun stream(
        range: LongRange? = null,
        consume: (ByteArray) -> Unit,
    ): Try<Unit, ReadError>

    /**
     * Reads the bytes at the given range.
     *
     * When [range] is null, the whole content is returned. Out-of-range indexes are clamped to the
     * available length automatically.
     *
     * The default accumulates the chunks emitted by [stream]. Override it when the implementation
     * can produce the whole buffer more cheaply, but then **also** override [stream]: overriding
     * only one of the pair recurses infinitely.
     */
    public suspend fun read(range: LongRange? = null): Try<ByteArray, ReadError> {
        val chunks = mutableListOf<ByteArray>()
        return stream(range) { chunks.add(it) }
            .map { chunks.join() }
    }
}

/** Chunk size used by [Readable] implementations that drain an underlying stream. */
@InternalReadiumApi
public const val STREAM_CHUNK_SIZE: Int = 64 * 1024

/** Concatenates [this] into a single array, avoiding a copy when there is nothing to join. */
internal fun List<ByteArray>.join(): ByteArray =
    when (size) {
        0 -> ByteArray(0)
        1 -> first()
        else -> {
            val result = ByteArray(sumOf { it.size })
            var offset = 0
            for (chunk in this) {
                chunk.copyInto(result, offset)
                offset += chunk.size
            }
            result
        }
    }

public typealias ReadTry<SuccessT> = Try<SuccessT, ReadError>

/**
 * Errors occurring while reading a resource.
 */
public sealed class ReadError(
    override val message: String,
    override val cause: Error,
) : Error {

    /**
     * An error occurred while trying to access the content.
     *
     * At the moment, [AccessError]s constructed by the toolkit can be either a FileSystemError,
     * a ContentResolverError or an HttpError.
     */
    public class Access(public override val cause: AccessError) :
        ReadError("An error occurred while attempting to access data.", cause)

    /**
     * Content doesn't match what was expected and cannot be interpreted.
     *
     * For instance, this error can be reported if a ZIP archive looks invalid,
     * a publication doesn't conform to its format, or a JSON resource cannot be decoded.
     */
    public class Decoding(cause: Error) :
        ReadError("An error occurred while attempting to decode the content.", cause) {

        public constructor(message: String) : this(DebugError(message))
        public constructor(exception: Exception) : this(ThrowableError(exception))
    }

    /**
     * Content could not be successfully read because there is not enough memory available.
     *
     * This error can be produced while trying to put the content into memory or while
     * trying to decode it.
     */
    public class OutOfMemory(override val cause: ThrowableError<OutOfMemoryError>) :
        ReadError("The resource is too large to be read on this device.", cause) {

        public constructor(error: OutOfMemoryError) : this(ThrowableError(error))
    }

    /**
     * An operation could not be performed at some point.
     *
     * For instance, this error can occur no matter the level of indirection when trying
     * to read ranges or getting length if any component the data has to pass through
     * doesn't support that.
     */
    public class UnsupportedOperation(cause: Error) :
        ReadError("Could not proceed because an operation was not supported.", cause) {

        public constructor(message: String) : this(DebugError(message))
    }
}

/**
 * Marker interface for source-specific access errors.
 */
public interface AccessError : Error

/**
 * An [IOException] wrapping a [ReadError].
 *
 * This is meant to be used in contexts where [IOException] are expected.
 */
public class ReadException(
    public val error: ReadError,
) : IOException(error.message, ErrorException(error))

/**
 * Returns a new [Readable] accessing the same data but not owning them.
 */
public fun Readable.borrow(): Readable =
    BorrowedReadable(this)

private class BorrowedReadable(
    private val readable: Readable,
) : Readable by readable {

    override fun close() {
        // Do nothing
    }
}

@InternalReadiumApi
public suspend inline fun Readable.readOrElse(
    recover: (ReadError) -> ByteArray,
): ByteArray =
    read().getOrElse(recover)
