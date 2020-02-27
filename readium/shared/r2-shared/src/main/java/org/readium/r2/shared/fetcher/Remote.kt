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
import java.net.HttpURLConnection
import java.net.URL
import kotlin.reflect.KProperty0

class HttpFetcher : Fetcher {

    override fun fetch(link: Link): ResourceHandle? =
        if (link.href.startsWith("http")) HttpHandle(link.href) else null
}

class HttpHandle(href: String): ResourceHandle(href) {

    private val headConnection: HttpURLConnection? by lazy {
        URL(href).openConnection().apply {
            if (this is HttpURLConnection)
                requestMethod = "HEAD"
        } as? HttpURLConnection
    }

    private val getConnection: HttpURLConnection? by lazy {
        URL(href).openConnection() as? HttpURLConnection
    }

    private val headerFields: Map<String, List<String>>? by lazy {
        if (::getConnection.isLazyInitialized) {
            getConnection?.getHeaderFields()
        } else {
            headConnection?.getHeaderFields()
        }
    }

    override fun stream(): InputStream? =
        try {
            getConnection?.getInputStream()
        } catch (e: Exception) {
            null
        }

    override val length: Long? by lazy { computeLength() }

    private fun computeLength(): Long? {
        if (::bytes.isLazyInitialized)
            return bytes?.size?.toLong()

        headerFields?.get("content-length")
            ?.mapNotNull(String::toLongOrNull)
            ?.firstOrNull()
            ?.let { return it }

        return bytes?.size?.toLong()
    }

    override val encoding: String? by lazy {
        headerFields?.get("content-encoding")?.firstOrNull()
    }
}

val KProperty0<*>.isLazyInitialized: Boolean
    get() = (getDelegate() as Lazy<*>).isInitialized()
