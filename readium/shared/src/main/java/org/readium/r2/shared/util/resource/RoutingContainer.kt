/*
 * Copyright 2023 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.shared.util.resource

import org.readium.r2.shared.util.AbsoluteUrl
import org.readium.r2.shared.util.Url

/**
 * Routes requests to child containers, depending on a provided predicate.
 *
 * This can be used for example to serve a publication containing both local and remote resources,
 * and more generally to concatenate different content sources.
 *
 * The [routes] will be tested in the given order.
 */
public class RoutingContainer(private val routes: List<Route>) : Container {

    /**
     * Holds a child fetcher and the predicate used to determine if it can answer a request.
     *
     * The default value for [accepts] means that the fetcher will accept any link.
     */
    public class Route(
        public val container: Container,
        public val accepts: (Url) -> Boolean = { true }
    )

    public constructor(local: Container, remote: Container) :
        this(
            listOf(
                Route(local, accepts = ::isLocal),
                Route(remote)
            )
        )

    override suspend fun entries(): Set<Container.Entry>? =
        null // We can't guarantee the list of entries is exhaustive, so we return null

    override fun get(url: Url): Container.Entry =
        routes.firstOrNull { it.accepts(url) }?.container?.get(url)
            ?: FailureResource(Resource.Exception.NotFound(url)).toEntry(url)

    override suspend fun close() {
        routes.forEach { it.container.close() }
    }
}

private fun isLocal(url: Url): Boolean {
    if (url !is AbsoluteUrl) {
        return true
    }
    return !url.isHttp
}
