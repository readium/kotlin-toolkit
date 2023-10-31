/*
 * Copyright 2023 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.shared.util.resource

import org.readium.r2.shared.util.AbsoluteUrl
import org.readium.r2.shared.util.Error
import org.readium.r2.shared.util.FilesystemError
import org.readium.r2.shared.util.MessageError
import org.readium.r2.shared.util.NetworkError
import org.readium.r2.shared.util.SuspendingCloseable
import org.readium.r2.shared.util.ThrowableError
import org.readium.r2.shared.util.Try
import org.readium.r2.shared.util.flatMap
import org.readium.r2.shared.util.mediatype.MediaType

public typealias ResourceTry<SuccessT> = Try<SuccessT, ResourceError>

/**
 * Acts as a proxy to an actual resource by handling read access.
 */
public interface Resource : SuspendingCloseable {

    /**
     * URL locating this resource, if any.
     */
    public val source: AbsoluteUrl?

    /**
     * Returns the resource media type if known.
     */
    public suspend fun mediaType(): ResourceTry<MediaType>

    /**
     * Properties associated to the resource.
     *
     * This is opened for extensions.
     */
    public suspend fun properties(): ResourceTry<Properties>

    public class Properties(
        properties: Map<String, Any> = emptyMap()
    ) : Map<String, Any> by properties {

        public companion object {
            public inline operator fun invoke(build: Builder.() -> Unit): Properties =
                Properties(Builder().apply(build))
        }

        public inline fun copy(build: Builder.() -> Unit): Properties =
            Properties(Builder(this).apply(build))

        public class Builder(properties: Map<String, Any> = emptyMap()) :
            MutableMap<String, Any> by properties.toMutableMap()
    }

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
}

/**
 * Errors occurring while accessing a resource.
 */
public sealed class ResourceError(
    override val message: String,
    override val cause: Error? = null
) : Error {

    /** Equivalent to a 404 HTTP error. */
    public class NotFound(cause: Error? = null) :
        ResourceError("Resource not found.", cause) {

        public constructor(exception: Exception) : this(ThrowableError(exception))
    }

    /**
     * Equivalent to a 403 HTTP error.
     *
     * This can be returned when trying to read a resource protected with a DRM that is not
     * unlocked.
     */
    public class Forbidden(cause: Error? = null) :
        ResourceError("You are not allowed to access the resource.", cause) {
        public constructor(exception: Exception) : this(ThrowableError(exception))
    }

    public class Network(public override val cause: NetworkError) :
        ResourceError("A network error occurred.", cause)

    public class Filesystem(public override val cause: FilesystemError) :
        ResourceError("A filesystem error occurred.", cause) {

        public constructor(exception: Exception) : this(FilesystemError(exception))
    }

    /**
     * Equivalent to a 507 HTTP error.
     *
     * Used when the requested range is too large to be read in memory.
     */
    public class OutOfMemory(override val cause: ThrowableError<OutOfMemoryError>) :
        ResourceError("The resource is too large to be read on this device.", cause) {

        public constructor(error: OutOfMemoryError) : this(ThrowableError(error))
    }

    public class InvalidContent(cause: Error? = null) :
        ResourceError("Content seems invalid. ", cause) {

        public constructor(message: String) : this(MessageError(message))
        public constructor(exception: Exception) : this(ThrowableError(exception))
    }

    /** For any other error, such as HTTP 500. */
    public class Other(cause: Error) :
        ResourceError("An unclassified error occurred.", cause) {

        public constructor(message: String) : this(MessageError(message))
        public constructor(exception: Exception) : this(ThrowableError(exception))
    }

    internal companion object
}

/** Creates a Resource that will always return the given [error]. */
public class FailureResource(
    private val error: ResourceError
) : Resource {

    override val source: AbsoluteUrl? = null
    override suspend fun mediaType(): ResourceTry<MediaType> = Try.failure(error)
    override suspend fun properties(): ResourceTry<Resource.Properties> = Try.failure(error)
    override suspend fun length(): ResourceTry<Long> = Try.failure(error)
    override suspend fun read(range: LongRange?): ResourceTry<ByteArray> = Try.failure(error)
    override suspend fun close() {}

    override fun toString(): String =
        "${javaClass.simpleName}($error)"
}

@Deprecated(
    "Catch exceptions yourself to the most suitable ResourceError.",
    level = DeprecationLevel.ERROR,
    replaceWith = ReplaceWith("map(transform)")
)
@Suppress("UnusedReceiverParameter")
public fun <R, S> ResourceTry<S>.mapCatching(): ResourceTry<R> =
    throw NotImplementedError()

public inline fun <R, S> ResourceTry<S>.flatMapCatching(transform: (value: S) -> ResourceTry<R>): ResourceTry<R> =
    flatMap {
        try {
            transform(it)
        } catch (e: Exception) {
            Try.failure(ResourceError.Other(e))
        } catch (e: OutOfMemoryError) { // We don't want to catch any Error, only OOM.
            Try.failure(ResourceError.OutOfMemory(e))
        }
    }
