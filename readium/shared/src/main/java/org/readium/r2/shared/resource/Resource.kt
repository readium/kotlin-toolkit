/*
 * Copyright 2023 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.shared.resource

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.annotation.StringRes
import java.io.ByteArrayInputStream
import java.io.File
import java.nio.charset.Charset
import kotlinx.coroutines.CancellationException
import org.json.JSONObject
import org.readium.r2.shared.R
import org.readium.r2.shared.UserException
import org.readium.r2.shared.parser.xml.ElementNode
import org.readium.r2.shared.parser.xml.XmlParser
import org.readium.r2.shared.util.SuspendingCloseable
import org.readium.r2.shared.util.Try
import org.readium.r2.shared.util.flatMap

typealias ResourceTry<SuccessT> = Try<SuccessT, Resource.Exception>

/**
 * Acts as a proxy to an actual resource by handling read access.
 */
interface Resource : SuspendingCloseable {

    /**
     * Direct file to this resource, when available.
     *
     * This is meant to be used as an optimization for consumers which can't work efficiently
     * with streams. However, [file] is not guaranteed to be set, for example if the resource
     * underwent transformations or is being read from an archive. Therefore, consumers should
     * always fallback on regular stream reading, using [read] or [ResourceInputStream].
     */
    val file: File? get() = null

    /**
     * Returns the resource media type if known.
     */
    suspend fun mediaType(): ResourceTry<String?> = ResourceTry.success(null)

    /**
     * Returns the name of the resource if any.
     */
    suspend fun name(): ResourceTry<String?> = ResourceTry.success(null)

    /**
     * Returns data length from metadata if available, or calculated from reading the bytes otherwise.
     *
     * This value must be treated as a hint, as it might not reflect the actual bytes length. To get
     * the real length, you need to read the whole resource.
     */
    suspend fun length(): ResourceTry<Long>

    /**
     * Reads the bytes at the given range.
     *
     * When [range] is null, the whole content is returned. Out-of-range indexes are clamped to the
     * available length automatically.
     */
    suspend fun read(range: LongRange? = null): ResourceTry<ByteArray>

    /**
     * Reads the full content as a [String].
     *
     * If [charset] is null, then it is parsed from the `charset` parameter of link().type,
     * or falls back on UTF-8.
     */
    suspend fun readAsString(charset: Charset? = null): ResourceTry<String> =
        read().mapCatching {
            String(it, charset = charset ?: Charsets.UTF_8)
        }

    /**
     * Reads the full content as a JSON object.
     */
    suspend fun readAsJson(): ResourceTry<JSONObject> =
        readAsString(charset = Charsets.UTF_8).mapCatching { JSONObject(it) }

    /**
     * Reads the full content as an XML document.
     */
    suspend fun readAsXml(): ResourceTry<ElementNode> =
        read().mapCatching { XmlParser().parse(ByteArrayInputStream(it)) }

    /**
     * Reads the full content as a [Bitmap].
     */
    suspend fun readAsBitmap(): ResourceTry<Bitmap> =
        read().mapCatching {
            BitmapFactory.decodeByteArray(it, 0, it.size)
                ?: throw kotlin.Exception("Could not decode resource as a bitmap")
        }

    /**
     * Errors occurring while accessing a resource.
     */
    sealed class Exception(@StringRes userMessageId: Int, cause: Throwable? = null) : UserException(userMessageId, cause = cause) {

        /** Equivalent to a 400 HTTP error. */
        class BadRequest(val parameters: Map<String, String> = emptyMap(), cause: Throwable? = null) :
            Exception(R.string.r2_shared_resource_exception_bad_request, cause)

        /** Equivalent to a 404 HTTP error. */
        class NotFound(cause: Throwable? = null) :
            Exception(R.string.r2_shared_resource_exception_not_found, cause)

        /**
         * Equivalent to a 403 HTTP error.
         *
         * This can be returned when trying to read a resource protected with a DRM that is not
         * unlocked.
         */
        class Forbidden(cause: Throwable? = null) :
            Exception(R.string.r2_shared_resource_exception_forbidden, cause)

        /**
         * Equivalent to a 503 HTTP error.
         *
         * Used when the source can't be reached, e.g. no Internet connection, or an issue with the
         * file system. Usually this is a temporary error.
         */
        class Unavailable(cause: Throwable? = null) :
            Exception(R.string.r2_shared_resource_exception_unavailable, cause)

        /**
         * The Internet connection appears to be offline.
         */
        object Offline : Exception(R.string.r2_shared_resource_exception_offline)

        /**
         * Equivalent to a 507 HTTP error.
         *
         * Used when the requested range is too large to be read in memory.
         */
        class OutOfMemory(override val cause: OutOfMemoryError) :
            Exception(R.string.r2_shared_resource_exception_out_of_memory)

        /**
         * The request was cancelled by the caller.
         *
         * For example, when a coroutine is cancelled.
         */
        object Cancelled : Exception(R.string.r2_shared_resource_exception_cancelled)

        /** For any other error, such as HTTP 500. */
        class Other(cause: Throwable) : Exception(R.string.r2_shared_resource_exception_other, cause)

        companion object {

            fun wrap(e: Throwable): Exception =
                when (e) {
                    is Exception -> e
                    is CancellationException -> Cancelled
                    is OutOfMemoryError -> OutOfMemory(e)
                    else -> Other(e)
                }
        }
    }
}

/** Creates a Resource that will always return the given [error]. */
class FailureResource(private val error: Resource.Exception) : Resource {

    internal constructor(cause: Throwable) : this(Resource.Exception.wrap(cause))

    override suspend fun read(range: LongRange?): ResourceTry<ByteArray> = Try.failure(error)

    override suspend fun length(): ResourceTry<Long> = Try.failure(error)

    override suspend fun close() {}

    override fun toString(): String =
        "${javaClass.simpleName}($error)"
}

/**
 * Maps the result with the given [transform]
 *
 * If the [transform] throws an [Exception], it is wrapped in a failure with Resource.Exception.Other.
 */
inline fun <R, S> ResourceTry<S>.mapCatching(transform: (value: S) -> R): ResourceTry<R> =
    try {
        Try.success((transform(getOrThrow())))
    } catch (e: Exception) {
        Try.failure(Resource.Exception.wrap(e))
    } catch (e: OutOfMemoryError) { // We don't want to catch any Error, only OOM.
        Try.failure(Resource.Exception.wrap(e))
    }

inline fun <R, S> ResourceTry<S>.flatMapCatching(transform: (value: S) -> ResourceTry<R>): ResourceTry<R> =
    mapCatching(transform).flatMap { it }
