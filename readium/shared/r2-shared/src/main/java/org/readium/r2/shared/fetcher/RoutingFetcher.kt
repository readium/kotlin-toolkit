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

/**
 * Routes requests to child fetchers, depending on a provided predicate.
 *
 * This can be used for example to serve a publication containing both local and remote resources,
 * and more generally to concatenate different content sources.
 *
 * The [routes] will be tested in the given order.
 */
class RoutingFetcher(private val routes: List<Route>) : Fetcher {

    /**
     * Holds a child fetcher and the predicate used to determine if it can answer a request.
     *
     * The default value for [accepts] means that the fetcher will accept any link.
     */
    class Route(val fetcher: Fetcher, val accepts: (Link) -> Boolean = { true })

    constructor(local: Fetcher, remote: Fetcher)
            : this(listOf( Route(local, Link::isLocal), Route(remote) ))

    override suspend fun links(): List<Link> = routes.flatMap { it.fetcher.links() }

    override fun get(link: Link): Resource =
        routes.firstOrNull { it.accepts(link) }?.fetcher?.get(link) ?: FailureResource(link, Resource.Exception.NotFound)

    override suspend fun close() {
        routes.forEach { it.fetcher.close() }
    }
}

private val Link.isLocal: Boolean get() = href.startsWith("/")
