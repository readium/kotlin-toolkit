/*
 * Module: r2-shared-kotlin
 * Developers: Quentin Gliosca
 *
 * Copyright (c) 2020. Readium Foundation. All rights reserved.
 * Use of this source code is governed by a BSD-style license which is detailed in the
 * LICENSE file present in the project repository where this source code is maintained.
 */

package org.readium.r2.shared.fetcher

import org.readium.r2.shared.fetcher.Fetcher.Resource
import org.readium.r2.shared.publication.Link
import org.readium.r2.shared.util.SuspendingCloseable

/** Provides access to a [Resource] from a [Link]. */
interface Fetcher : SuspendingCloseable {

    /**
     * Acts as a proxy to an actual resource by handling read access.
     */
    interface Resource : org.readium.r2.shared.resource.Resource {

        /**
         * Returns the link from which the resource was retrieved.
         *
         * It might be modified by the [Resource] to include additional metadata, e.g. the
         * `Content-Type` HTTP header in [Link.type].
         */
        suspend fun link(): Link
    }

    /**
     * Known resources available in the medium, such as file paths on the file system
     * or entries in a ZIP archive. This list is not exhaustive, and additional
     * unknown resources might be reachable.
     *
     * If the medium has an inherent resource order, it should be followed.
     * Otherwise, HREFs are sorted alphabetically.
     */
    suspend fun links(): List<Link>

    /**
     * Returns the [Resource] at the given [link]'s HREF.
     *
     * A [Resource] is always returned, since for some cases we can't know if it exists before
     * actually fetching it, such as HTTP. Therefore, errors are handled at the Resource level.
     */
    fun get(link: Link): Resource

    /** Returns the [Resource] at the given [href]. */
    fun get(href: String): Resource =
        get(Link(href = href))

    // To be able to add extensions on Fetcher.Companion in other components...
    companion object
}

/** A [Fetcher] providing no resources at all. */
class EmptyFetcher : Fetcher {

    override suspend fun links(): List<Link> =
        emptyList()

    override fun get(link: Link): Resource =
        FailureResource(link, org.readium.r2.shared.resource.Resource.Exception.NotFound())

    override suspend fun close() {}
}

class SingleResourceFetcher(
    private val link: Link,
    private val resource: Resource
) : Fetcher {

    companion object {

        suspend operator fun invoke(resource: Resource): SingleResourceFetcher {
            val link = resource.link()
            return SingleResourceFetcher(link, resource)
        }
    }

    override suspend fun links(): List<Link> =
        listOf(link)

    override fun get(link: Link): Resource {
        if (link.href != this.link.href) {
            val exception = org.readium.r2.shared.resource.Resource.Exception.NotFound()
            return FailureResource(link, exception)
        }

        return resource
    }

    override suspend fun close() {
        resource.close()
    }
}
