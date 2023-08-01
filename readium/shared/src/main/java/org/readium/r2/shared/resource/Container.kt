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
     * Known entries available in the container, such as file paths on the file system or entries in
     * a ZIP archive. This list is not exhaustive, and additional unknown resources might be
     * reachable.
     *
     * If the container has an inherent resource order, it should be followed. Otherwise, entries
     * are sorted alphabetically.
     */
    public suspend fun entries(): Iterable<Entry>

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

    override suspend fun entries(): Iterable<Container.Entry> = emptyList()

    override fun get(path: String): Container.Entry =
        FailureResource(Resource.Exception.NotFound()).toEntry(path)

    override suspend fun close() {}
}

/** A [Container] for a single [Resource]. */
public class ResourceContainer(path: String, resource: Resource) : Container {

    private val entry = resource.toEntry(path)

    override suspend fun entries(): Iterable<Container.Entry> = listOf(entry)

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