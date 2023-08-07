/*
 * Copyright 2023 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.shared.resource

import org.readium.r2.shared.util.Url
import org.readium.r2.shared.util.isFile

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
        public val accepts: (String) -> Boolean = { true }
    )

    public constructor(local: Container, remote: Container) :
        this(listOf(
            Route(local, accepts = ::isLocal),
            Route(remote)
        ))

    override suspend fun entries(): Iterable<Container.Entry> =
        routes.flatMap { it.container.entries() }

    override fun get(path: String): Container.Entry =
        routes.firstOrNull { it.accepts(path) }?.container?.get(path)
            ?: FailureResource(Resource.Exception.NotFound()).toEntry(path)

    override suspend fun close() {
        routes.forEach { it.container.close() }
    }
}

private fun isLocal(path: String): Boolean {
    val url = Url(path) ?: return false
    return url.isFile()
}
