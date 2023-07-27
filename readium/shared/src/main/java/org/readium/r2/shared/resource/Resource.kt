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
import java.nio.charset.Charset
import org.json.JSONObject
import org.readium.r2.shared.R
import org.readium.r2.shared.UserException
import org.readium.r2.shared.error.Try
import org.readium.r2.shared.error.flatMap
import org.readium.r2.shared.parser.xml.ElementNode
import org.readium.r2.shared.parser.xml.XmlParser
import org.readium.r2.shared.util.SuspendingCloseable
import org.readium.r2.shared.util.Url
import org.readium.r2.shared.util.mediatype.MediaType

public typealias ResourceTry<SuccessT> = Try<SuccessT, Resource.Exception>

/**
 * Acts as a proxy to an actual resource by handling read access.
 */
public interface Resource : SuspendingCloseable {

    /**
     * URL locating this resource, if any.
     */
    public val source: Url?

    /**
     * Returns the resource media type if known.
     */
    public suspend fun mediaType(): ResourceTry<MediaType?>

    /**
     * Returns the name of the resource if any.
     */
    public suspend fun name(): ResourceTry<String?>

    /**
     * Properties associated to the resource.
     *
     * This is opened for extensions.
     */
    public suspend fun properties(): ResourceTry<Properties>

    public class Properties(
        properties: Map<String, Any> = emptyMap()
    ) : Map<String, Any> by properties

    /**
     * Returns data length from metadata if available, or calculated from reading the bytes otherwise.
     *
     * This value must be treated as a hint, as it might not reflect the actual bytes length. To get
     * the real length, you need to read the whole resource.
     */
    public suspend fun length(): ResourceTry<Long>

    /**
     * Reads the bytes at the given range.
     *
     * When [range] is null, the whole content is returned. Out-of-range indexes are clamped to the
     * available length automatically.
     */
    public suspend fun read(range: LongRange? = null): ResourceTry<ByteArray>

    /**
     * Errors occurring while accessing a resource.
     */
    public sealed class Exception(@StringRes userMessageId: Int, cause: Throwable? = null) : UserException(userMessageId, cause = cause) {

        /** Equivalent to a 400 HTTP error. */
        public class BadRequest(
            public val parameters: Map<String, String> = emptyMap(),
            cause: Throwable? = null
        ) :
            Exception(R.string.readium_shared_resource_exception_bad_request, cause)

        /** Equivalent to a 404 HTTP error. */
        public class NotFound(cause: Throwable? = null) :
            Exception(R.string.readium_shared_resource_exception_not_found, cause)

        /**
         * Equivalent to a 403 HTTP error.
         *
         * This can be returned when trying to read a resource protected with a DRM that is not
         * unlocked.
         */
        public class Forbidden(cause: Throwable? = null) :
            Exception(R.string.readium_shared_resource_exception_forbidden, cause)

        /**
         * Equivalent to a 503 HTTP error.
         *
         * Used when the source can't be reached, e.g. no Internet connection, or an issue with the
         * file system. Usually this is a temporary error.
         */
        public class Unavailable(cause: Throwable? = null) :
            Exception(R.string.readium_shared_resource_exception_unavailable, cause)

        /**
         * The Internet connection appears to be offline.
         */
        public object Offline : Exception(R.string.readium_shared_resource_exception_offline)

        /**
         * Equivalent to a 507 HTTP error.
         *
         * Used when the requested range is too large to be read in memory.
         */
        public class OutOfMemory(override val cause: OutOfMemoryError) :
            Exception(R.string.readium_shared_resource_exception_out_of_memory)

        /** For any other error, such as HTTP 500. */
        public class Other(cause: Throwable) : Exception(R.string.readium_shared_resource_exception_other, cause)

        public companion object {

            public fun wrap(e: Throwable): Exception =
                when (e) {
                    is Exception -> e
                    is OutOfMemoryError -> OutOfMemory(e)
                    else -> Other(e)
                }
        }
    }
}

/** Creates a Resource that will always return the given [error]. */
public class FailureResource(
    private val error: Resource.Exception
) : Resource {

    internal constructor(cause: Throwable) : this(Resource.Exception.wrap(cause))

    override val source: Url? = null
    override suspend fun mediaType(): ResourceTry<MediaType?> = Try.failure(error)
    override suspend fun name(): ResourceTry<String?> = Try.failure(error)
    override suspend fun properties(): ResourceTry<Resource.Properties> = Try.failure(error)
    override suspend fun length(): ResourceTry<Long> = Try.failure(error)
    override suspend fun read(range: LongRange?): ResourceTry<ByteArray> = Try.failure(error)
    override suspend fun close() {}

    override fun toString(): String =
        "${javaClass.simpleName}($error)"
}

/**
 * Maps the result with the given [transform]
 *
 * If the [transform] throws an [Exception], it is wrapped in a failure with Resource.Exception.Other.
 */
public inline fun <R, S> ResourceTry<S>.mapCatching(transform: (value: S) -> R): ResourceTry<R> =
    try {
        map(transform)
    } catch (e: Exception) {
        Try.failure(Resource.Exception.wrap(e))
    } catch (e: OutOfMemoryError) { // We don't want to catch any Error, only OOM.
        Try.failure(Resource.Exception.wrap(e))
    }

public inline fun <R, S> ResourceTry<S>.flatMapCatching(transform: (value: S) -> ResourceTry<R>): ResourceTry<R> =
    mapCatching(transform).flatMap { it }

/**
 * Reads the full content as a [String].
 *
 * If [charset] is null, then it is parsed from the `charset` parameter of link().type,
 * or falls back on UTF-8.
 */
public suspend fun Resource.readAsString(charset: Charset? = null): ResourceTry<String> =
    read().mapCatching {
        String(it, charset = charset ?: Charsets.UTF_8)
    }

/**
 * Reads the full content as a JSON object.
 */
public suspend fun Resource.readAsJson(): ResourceTry<JSONObject> =
    readAsString(charset = Charsets.UTF_8).mapCatching { JSONObject(it) }

/**
 * Reads the full content as an XML document.
 */
public suspend fun Resource.readAsXml(): ResourceTry<ElementNode> =
    read().mapCatching { XmlParser().parse(ByteArrayInputStream(it)) }

/**
 * Reads the full content as a [Bitmap].
 */
public suspend fun Resource.readAsBitmap(): ResourceTry<Bitmap> =
    read().mapCatching {
        BitmapFactory.decodeByteArray(it, 0, it.size)
            ?: throw kotlin.Exception("Could not decode resource as a bitmap")
    }
