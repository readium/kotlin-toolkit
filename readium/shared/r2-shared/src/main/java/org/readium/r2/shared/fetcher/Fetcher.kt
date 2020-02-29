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
import java.nio.ByteBuffer

interface Fetcher {

    fun fetch(link: Link): ResourceHandle?

    fun close() {}
}

abstract class ResourceHandle(val href: String) {

    /** Return a new stream for reading the resource. */
    abstract fun stream(): InputStream?

    /** The encoding of the resource if it is text and the information is available, null otherwise. */
    open val encoding: String? = null

    open val bytes: ByteArray? by lazy {
        stream().use {
            try {
               it?.readBytes()
            } catch (e: Exception) {
                null
            }
        }
    }

    /** Fetch the data and return the true length. */
    val length: Long? by lazy {
        bytes?.size?.toLong()
    }

    /** Give an estimation of the data length.
     *
     * The true length is used if it is already known, or no length is available from metadata.
     */
    val estimatedLength: Long? by lazy {
        if (::length.isLazyInitialized)
            length
        else
            metadataLength ?: length
    }

    /** An estimate of data length from metadata */
    open protected val metadataLength: Long? = null
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
