/*
 * Copyright 2023 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.shared.fetcher

import org.readium.r2.shared.publication.Link
import org.readium.r2.shared.publication.Publication
import org.readium.r2.shared.resource.FailureResource
import org.readium.r2.shared.resource.Resource
import org.readium.r2.shared.util.SuspendingCloseable

/** Provides access to a [Resource] from a [Link]. */
public interface Fetcher : SuspendingCloseable {

    /**
     * Known resources available in the medium, such as file paths on the file system
     * or entries in a ZIP archive. This list is not exhaustive, and additional
     * unknown resources might be reachable.
     *
     * If the medium has an inherent resource order, it should be followed.
     * Otherwise, HREFs are sorted alphabetically.
     */
    public suspend fun links(): List<Link>

    /**
     * Returns the [Resource] at the given [link]'s HREF.
     *
     * A [Resource] is always returned, since for some cases we can't know if it exists before
     * actually fetching it, such as HTTP. Therefore, errors are handled at the Resource level.
     */
    public fun get(link: Link): Publication.Resource

    /** Returns the [Resource] at the given [href]. */
    public fun get(href: String): Publication.Resource =
        get(Link(href = href))

    // To be able to add extensions on Fetcher.Companion in other components...
    public companion object
}

/** A [Fetcher] providing no resources at all. */
public class EmptyFetcher : Fetcher {

    override suspend fun links(): List<Link> =
        emptyList()

    override fun get(link: Link): Publication.Resource =
        Publication.Resource(FailureResource(Resource.Exception.NotFound()), link)

    override suspend fun close() {}
}

public class ResourceFetcher(
    private val link: Link,
    private val resource: Publication.Resource
) : Fetcher {

    public companion object {

        public suspend operator fun invoke(resource: Publication.Resource): ResourceFetcher {
            val link = resource.link()
            return ResourceFetcher(link, resource)
        }
    }

    override suspend fun links(): List<Link> =
        listOf(link)

    override fun get(link: Link): Publication.Resource {
        if (link.href.takeWhile { it !in "#?" } != this.link.href) {
            return Publication.Resource(FailureResource(Resource.Exception.NotFound()), link)
        }

        return Publication.Resource(resource, link)
    }

    override suspend fun close() {
        resource.close()
    }
}
