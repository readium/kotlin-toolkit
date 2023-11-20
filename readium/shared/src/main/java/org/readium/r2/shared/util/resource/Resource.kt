/*
 * Copyright 2023 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.shared.util.resource

import org.readium.r2.shared.util.AbsoluteUrl
import org.readium.r2.shared.util.Try
import org.readium.r2.shared.util.data.Blob
import org.readium.r2.shared.util.data.Container
import org.readium.r2.shared.util.data.ReadError
import org.readium.r2.shared.util.mediatype.MediaType

public typealias ResourceTry<SuccessT> = Try<SuccessT, ReadError>

public typealias ResourceContainer = Container<Resource>

/**
 * Acts as a proxy to an actual resource by handling read access.
 */
public interface Resource : Blob {

    /**
     * Returns the resource media type if known.
     */
    public suspend fun mediaType(): Try<MediaType, ReadError>

    /**
     * Properties associated to the resource.
     *
     * This is opened for extensions.
     */
    public suspend fun properties(): Try<Properties, ReadError>

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
}

/** Creates a Resource that will always return the given [error]. */
public class FailureResource(
    private val error: ReadError
) : Resource {

    override val source: AbsoluteUrl? = null
    override suspend fun mediaType(): Try<MediaType, ReadError> = Try.failure(error)
    override suspend fun properties(): Try<Resource.Properties, ReadError> = Try.failure(error)
    override suspend fun length(): Try<Long, ReadError> = Try.failure(error)
    override suspend fun read(range: LongRange?): Try<ByteArray, ReadError> = Try.failure(error)
    override suspend fun close() {}

    override fun toString(): String =
        "${javaClass.simpleName}($error)"
}

@Deprecated(
    "Catch exceptions yourself to the most suitable ReadError.",
    level = DeprecationLevel.ERROR,
    replaceWith = ReplaceWith("map(transform)")
)
@Suppress("UnusedReceiverParameter")
public fun <R, S, E> Try<S, E>.mapCatching(): ResourceTry<R> =
    throw NotImplementedError()

@Suppress("UnusedReceiverParameter")
public fun <R, S, E> Try<S, E>.flatMapCatching(): ResourceTry<R> =
    throw NotImplementedError()

internal fun Resource.withMediaType(mediaType: MediaType?): Resource {
    if (mediaType == null) {
        return this
    }

    return object : Resource by this {
        override suspend fun mediaType(): Try<MediaType, ReadError> =
            Try.success(mediaType)
    }
}
