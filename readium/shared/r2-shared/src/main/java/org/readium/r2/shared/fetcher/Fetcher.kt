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
import java.io.InputStream

interface Fetcher {

    fun fetch(link: Link): ResourceHandle?

    fun close() {}
}

abstract class ResourceHandle(val href: String) {

    abstract fun stream(): InputStream?

    open val bytes: ByteArray? by lazy {
        try {
            stream().use { it?.readBytes() }
        } catch (e: Exception) {
            null
        }
    }

    open val length: Long? by lazy {
        bytes?.size?.toLong()
    }

    open val encoding: String? = null
}

class CompositeFetcher(val selector: (Link) -> Fetcher, val children: Collection<Fetcher>) : Fetcher {
    /* FIXME: `children` argument is required for `close`, but `selector` can enclose other `Fetcher`s
               Should `selector` return an index? It could be out of range.
    */

    constructor(local: Fetcher, remote: Fetcher)
            : this({ if (hrefIsRemote(it.href)) remote else local }, listOf(local, remote))

    override fun fetch(link: Link): ResourceHandle? = selector(link).fetch(link)

    override fun close() {
        children.forEach(Fetcher::close)
    }
}

private fun hrefIsRemote(href: String) = !href.startsWith("/")
