/*
 * Copyright 2023 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.shared.resource

import org.readium.r2.shared.util.AbsoluteUrl
import org.readium.r2.shared.util.SuspendingCloseable
import org.readium.r2.shared.util.Url

/**
 * A container provides access to a list of [Resource] entries.
 */
public interface Container : SuspendingCloseable {

    /**
     * Represents a container entry's.
     */
    public interface Entry : Resource {

        /**
         * URL used to access the resource in the container.
         */
        public val url: Url
    }

    /**
     * Direct source to this container, when available.
     */
    public val source: AbsoluteUrl? get() = null

    /**
     * List of all the container entries of null if such a list is not available.
     */
    public suspend fun entries(): Set<Entry>?

    /**
     * Returns the [Entry] at the given [url].
     *
     * A [Entry] is always returned, since for some cases we can't know if it exists before actually
     * fetching it, such as HTTP. Therefore, errors are handled at the Entry level.
     */
    public fun get(url: Url): Entry
}

/** A [Container] providing no resources at all. */
public class EmptyContainer : Container {

    override suspend fun entries(): Set<Container.Entry> = emptySet()

    override fun get(url: Url): Container.Entry =
        FailureResource(Resource.Exception.NotFound()).toEntry(url)

    override suspend fun close() {}
}

/** A [Container] for a single [Resource]. */
public class ResourceContainer(url: Url, resource: Resource) : Container {

    private val entry = resource.toEntry(url)

    override suspend fun entries(): Set<Container.Entry> = setOf(entry)

    override fun get(url: Url): Container.Entry {
        if (url.removeFragment().removeQuery() != entry.url) {
            return FailureResource(Resource.Exception.NotFound()).toEntry(url)
        }

        return entry
    }

    override suspend fun close() {
        entry.close()
    }
}

/** Convenience helper to wrap a [Resource] and a [path] into a [Container.Entry]. */
internal fun Resource.toEntry(url: Url): Container.Entry =
    object : Container.Entry, Resource by this {
        override val url: Url = url
    }
