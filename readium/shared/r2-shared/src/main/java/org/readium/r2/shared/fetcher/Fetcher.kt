/*
 * Module: r2-shared-kotlin
 * Developers: Quentin Gliosca
 *
 * Copyright (c) 2020. Readium Foundation. All rights reserved.
 * Use of this source code is governed by a BSD-style license which is detailed in the
 * LICENSE file present in the project repository where this source code is maintained.
 */

package org.readium.r2.shared.fetcher

import org.readium.r2.shared.publication.Link

/** Provides access to a [Resource] from a [Link]. */
interface Fetcher {

    /**
     * Known resources available in the medium, such as file paths on the file system
     * or entries in a ZIP archive. This list is not exhaustive, and additional
     * unknown resources might be reachable.
     *
     * If the medium has an inherent resource order, it should be followed.
     * Otherwise, HREFs are sorted alphabetically.
     */
    val links: List<Link>

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

    /** Shortcut to get the data of a file at given [Link]. */
    @Throws(Resource.Error::class)
    fun readBytes(link: Link): ByteArray =
        get(link).read().get()

    /** Shortcut to get the data of a file at given [href]. */
    @Throws(Resource.Error::class)
    fun readBytes(href: String): ByteArray =
        get(href).read().get()

    /** Closes any opened file handles, removes temporary files, etc. */
    fun close()

}

/** A [Fetcher] providing no resources at all. */
class EmptyFetcher : Fetcher {

    override val links: List<Link> = emptyList()

    override fun get(link: Link): Resource = FailureResource(link, Resource.Error.NotFound)

    override fun close() {}

}
