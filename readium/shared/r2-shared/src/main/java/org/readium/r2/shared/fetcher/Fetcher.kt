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
import org.readium.r2.shared.util.isLazyInitialized
import java.io.InputStream

interface Fetcher {

    /** Return a handle to try to retrieve a `link`'s content. */
    fun get(link: Link): Resource

    /** Close resources associated with the fetcher if there's any. */
    fun close() {}
}

interface Resource {
    val link: Link

    /** Return a new stream to read the resource. */
    fun stream(): InputStream?

    val bytes: ByteArray?

    /** An estimate of data length. */
    val length: Long?
}

internal abstract class ResourceImpl : Resource {

    override val bytes: ByteArray? by lazy {
        stream().use {
            try {
                it?.readBytes()
            } catch (e: Exception) {
                null
            }
        }
    }

    /**
     * The true length is used if it is already known, or no length is available from metadata.
     */
    override val length: Long?
        get () = if (::bytes.isLazyInitialized)
            bytes?.size?.toLong()
        else
            metadataLength

    /** An estimate of data length from metadata */
    open val metadataLength: Long? = null
}

internal class NotFoundResource(override val link: Link) : Resource {

    override fun stream(): InputStream? = null

    override val bytes: ByteArray? = null

    override val length: Long? = null
}

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
