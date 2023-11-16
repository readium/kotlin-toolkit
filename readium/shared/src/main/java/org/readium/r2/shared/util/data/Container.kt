/*
 * Copyright 2023 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.shared.util.data

import org.readium.r2.shared.util.AbsoluteUrl
import org.readium.r2.shared.util.SuspendingCloseable
import org.readium.r2.shared.util.Try
import org.readium.r2.shared.util.Url
import org.readium.r2.shared.util.resource.Resource

/**
 * A container provides access to a list of [Resource] entries.
 */
public interface Container<E : Blob> : SuspendingCloseable {

    /**
     * Direct source to this container, when available.
     */
    public val source: AbsoluteUrl? get() = null

    /**
     * Returns the [Entry] at the given [url].
     *
     * A [Entry] is always returned, since for some cases we can't know if it exists before actually
     * fetching it, such as HTTP. Therefore, errors are handled at the Entry level.
     */
    public fun get(url: Url): E?
}

public interface ClosedContainer<E : Blob> : Container<E> {

    /**
     * List of all the container entries.
     */
    public suspend fun entries(): Set<Url>
}

/** A [Container] providing no resources at all. */
public class EmptyContainer<E : Blob> : ClosedContainer<E> {

    override suspend fun entries(): Set<Url> = emptySet()

    override fun get(url: Url): E? = null

    override suspend fun close() {}
}

/**
 * Returns whether an entry exists in the container.
 */
internal suspend fun<E : Blob> Container<E>.contains(url: Url): Try<Boolean, ReadError> {
    if (this is ClosedContainer) {
        return Try.success(url in entries())
    }

    return get(url)
        ?.read(range = 0L..1L)
        ?.map { true }
        ?: Try.success(false)
}
