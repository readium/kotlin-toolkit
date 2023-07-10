/*
 * Copyright 2023 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.shared.fetcher

import java.io.File
import org.readium.r2.shared.fetcher.Fetcher.Resource
import org.readium.r2.shared.publication.Link
import org.readium.r2.shared.resource.ResourceTry
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

class ResourceFetcher(
    private val link: Link,
    private val resource: org.readium.r2.shared.resource.Resource
) : Fetcher {

    companion object {

        suspend operator fun invoke(resource: Resource): ResourceFetcher {
            val link = resource.link()
            return ResourceFetcher(link, resource)
        }
    }

    class Resource(
        private val link: Link,
        private val resource: org.readium.r2.shared.resource.Resource
    ) : Fetcher.Resource {

        override val file: File? =
            resource.file

        override suspend fun link() =
            link

        override suspend fun length(): ResourceTry<Long> =
            resource.length()

        override suspend fun read(range: LongRange?): ResourceTry<ByteArray> =
            resource.read(range)

        override suspend fun close() {
        }
    }

    override suspend fun links(): List<Link> =
        listOf(link)

    override fun get(link: Link): Fetcher.Resource {
        if (link.href.takeWhile { it !in "#?" } != this.link.href) {
            val exception = org.readium.r2.shared.resource.Resource.Exception.NotFound()
            return FailureResource(link, exception)
        }

        return Resource(link, resource)
    }

    override suspend fun close() {
        resource.close()
    }
}
