/*
 * Copyright 2023 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.shared.util.resource

import org.readium.r2.shared.util.AbsoluteUrl
import org.readium.r2.shared.util.Try
import org.readium.r2.shared.util.data.ReadError
import org.readium.r2.shared.util.data.Readable

/**
 * Acts as a proxy to an actual resource by handling read access.
 */
public interface Resource : Readable {

    /**
     * URL locating this resource, if any.
     */
    public val sourceUrl: AbsoluteUrl?

    /**
     * Properties associated to the resource.
     *
     * This is opened for extensions.
     */
    public suspend fun properties(): Try<Properties, ReadError>

    public class Properties(
        properties: Map<String, Any> = emptyMap(),
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
    private val error: ReadError,
) : Resource {

    override val sourceUrl: AbsoluteUrl? = null
    override suspend fun properties(): Try<Resource.Properties, ReadError> = Try.failure(error)
    override suspend fun length(): Try<Long, ReadError> = Try.failure(error)
    override suspend fun read(range: LongRange?): Try<ByteArray, ReadError> = Try.failure(error)
    override fun close() {}

    override fun toString(): String =
        "${javaClass.simpleName}($error)"
}

/**
 * Returns a new [Resource] accessing the same data but not owning them.
 *
 * This is useful when you want to pass a [Resource] to a component which might close it, but you
 * want to keep using it after.
 */
public fun Resource.borrow(): Resource =
    BorrowedResource(this)

private class BorrowedResource(
    private val resource: Resource,
) : Resource by resource {

    override fun close() {
        // Do nothing
    }
}
