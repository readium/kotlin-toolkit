/*
 * Copyright 2023 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.shared.util.data

import org.readium.r2.shared.util.AbsoluteUrl
import org.readium.r2.shared.util.SuspendingCloseable
import org.readium.r2.shared.util.Url
import org.readium.r2.shared.util.mediatype.MediaType
import org.readium.r2.shared.util.resource.Resource

/**
 * A container provides access to a list of [Resource] entries.
 */
public interface Container<out E : Readable> : Iterable<Url>, SuspendingCloseable {

    public val archiveMediaType: MediaType? get() = null

    /**
     * Direct source to this container, when available.
     */
    public val source: AbsoluteUrl? get() = null

    /**
     * List of all the container entries.
     */
    public val entries: Set<Url>

    override fun iterator(): Iterator<Url> =
        entries.iterator()

    /**
     * Returns the entry at the given [url] or null if there is none.
     */
    public operator fun get(url: Url): E?
}

/** A [Container] providing no resources at all. */
public class EmptyContainer<E : Readable> :
    Container<E> {

    override val entries: Set<Url> = emptySet()

    override fun get(url: Url): E? = null

    override suspend fun close() {}
}

/**
 * Routes requests to child containers, depending on a provided predicate.
 *
 * This can be used for example to serve a publication containing both local and remote resources,
 * and more generally to concatenate different content sources.
 *
 * The [containers] will be tested in the given order.
 */
public class CompositeContainer<E : Readable>(
    private val containers: List<Container<E>>
) : Container<E> {

    public constructor(vararg containers: Container<E>) :
        this(containers.toList())

    override val entries: Set<Url> =
        containers.fold(emptySet()) { acc, container -> acc + container.entries }

    override fun get(url: Url): E? =
        containers.firstNotNullOfOrNull { it[url] }

    override suspend fun close() {
        containers.forEach { it.close() }
    }
}
