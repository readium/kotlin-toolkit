/*
 * Copyright 2023 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.shared.util.data

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
public class RoutingClosedContainer<E : ContainerEntry>(
    private val routes: List<Route<E>>
) : ClosedContainer<E> {

    /**
     * Holds a child fetcher and the predicate used to determine if it can answer a request.
     *
     * The default value for [accepts] means that the fetcher will accept any link.
     */
    public class Route<E : ContainerEntry>(
        public val container: ClosedContainer<E>,
        public val accepts: (Url) -> Boolean = { true }
    )

    public constructor(local: ClosedContainer<E>, remote: ClosedContainer<E>) :
        this(
            listOf(
                Route(local, accepts = ::isLocal),
                Route(remote)
            )
        )

    override suspend fun entries(): Set<Url> =
        routes.fold(emptySet()) { acc, route -> acc + route.container.entries() }

    override fun get(url: Url): E? =
        routes.firstOrNull { it.accepts(url) }?.container?.get(url)

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
