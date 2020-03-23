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

class CompositeFetcher(val selectors: List<Selector>) : Fetcher {

    class Selector(val fetcher: Fetcher, val accepts: (Link) -> Boolean)

    constructor(local: Fetcher, remote: Fetcher)
            : this(listOf( Selector(remote, ::hrefIsRemote), Selector(local, { true }) ))

    override fun get(link: Link): Resource =
        selectors.firstOrNull { it.accepts(link) }?.fetcher?.get(link) ?: NotFoundResource(link)

    override fun close() {
        selectors.forEach { it.fetcher.close() }
    }
}

private fun hrefIsRemote(link: Link) = link.href.startsWith("/")
