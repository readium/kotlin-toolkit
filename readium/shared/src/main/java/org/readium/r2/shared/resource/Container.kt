/*
 * Copyright 2023 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.shared.resource

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
         * Absolute path to the entry in the archive.
         *
         * It MUST start with /.
         */
        public val path: String
    }

    /**
     * Direct source to this container, when available.
     */
    public val source: Url? get() = null

    /**
     * List of all the container entries of null if such a list is not available.
     */
    public suspend fun entries(): Set<Entry>?

    /**
     * Returns the [Entry] at the given [path].
     *
     * A [Entry] is always returned, since for some cases we can't know if it exists before actually
     * fetching it, such as HTTP. Therefore, errors are handled at the Entry level.
     */
    public fun get(path: String): Entry
}

/** A [Container] providing no resources at all. */
public class EmptyContainer : Container {

    override suspend fun entries(): Set<Container.Entry> = emptySet()

    override fun get(path: String): Container.Entry =
        FailureResource(Resource.Exception.NotFound()).toEntry(path)

    override suspend fun close() {}
}

/** A [Container] for a single [Resource]. */
public class ResourceContainer(path: String, resource: Resource) : Container {

    private val entry = resource.toEntry(path)

    override suspend fun entries(): Set<Container.Entry> = setOf(entry)

    override fun get(path: String): Container.Entry {
        if (path.takeWhile { it !in "#?" } != entry.path) {
            return FailureResource(Resource.Exception.NotFound()).toEntry(path)
        }

        return entry
    }

    override suspend fun close() {
        entry.close()
    }
}

/** Convenience helper to wrap a [Resource] and a [path] into a [Container.Entry]. */
internal fun Resource.toEntry(path: String) : Container.Entry =
    object : Container.Entry, Resource by this {
        override val path: String = path
    }