/*
 * Copyright 2023 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.shared.util.data

import org.readium.r2.shared.InternalReadiumApi
import org.readium.r2.shared.util.AbsoluteUrl
import org.readium.r2.shared.util.Closeable
import org.readium.r2.shared.util.Try
import org.readium.r2.shared.util.Url
import org.readium.r2.shared.util.use

/**
 * A container provides access to a list of [Readable] entries.
 */
public interface Container<out E : Readable> : Iterable<Url>, Closeable {

    /**
     * Direct source to this container, when available.
     */
    public val sourceUrl: AbsoluteUrl? get() = null

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

/** A [Container] providing no entries at all. */
public class EmptyContainer<E : Readable> :
    Container<E> {

    override val entries: Set<Url> = emptySet()

    override fun get(url: Url): E? = null

    override fun close() {}
}

/**
 * Concatenates several containers.
 *
 * This can be used for example to serve a publication containing both local and remote resources,
 * and more generally to concatenate different content sources.
 *
 * The [containers] will be tested in the given order.
 */
public class CompositeContainer<E : Readable>(
    private val containers: List<Container<E>>,
) : Container<E> {

    public constructor(vararg containers: Container<E>) :
        this(containers.toList())

    override val entries: Set<Url> =
        containers.fold(emptySet()) { acc, container -> acc + container.entries }

    override fun get(url: Url): E? =
        containers.firstNotNullOfOrNull { it[url] }

    override fun close() {
        containers.forEach { it.close() }
    }
}

@InternalReadiumApi
public suspend inline fun <S> Container<Readable>.readDecodeOrNull(
    url: Url,
    decode: (ByteArray) -> Try<S, DecodeError>,
): S? =
    get(url)?.use { resource ->
        resource.readDecodeOrNull(decode)
    }
