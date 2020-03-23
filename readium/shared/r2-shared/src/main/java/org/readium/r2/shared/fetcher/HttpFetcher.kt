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
import java.net.HttpURLConnection
import java.net.URL


/**
 * This class implements a lazy fetcher of resources over HTTP.
 *
 * No request is done until a Resource property is called.
 * When metadata is requested before content, a HEAD request is done.
 * Otherwise, the result from the previous GET request is used.
*/
class HttpFetcher : Fetcher {

    override fun get(link: Link): Resource = HttpResource(link)

    private class HttpResource(link: Link): ResourceImpl() {

        private val headConnection: HttpURLConnection? by lazy {
            (URL(link.href).openConnection() as? HttpURLConnection)?.apply { requestMethod = "HEAD" }
        }

        private val getConnection: HttpURLConnection? by lazy {
            (URL(link.href).openConnection() as? HttpURLConnection)?.apply { requestMethod = "GET" }
        }

        private val headerFields: Map<String, List<String>>?
            get() = if (::getConnection.isLazyInitialized) {
                getConnection?.headerFields
            } else {
                headConnection?.headerFields
            }

        override val link: Link by lazy {
            link.copy(type = headerFields?.get("Content-Type")?.firstOrNull { it != "application/octet-stream" } ?: link.type)
        }

        override fun stream(): InputStream? = bytes?.inputStream()  // getConnection?.inputStream always returns the same stream

        override val bytes: ByteArray? by lazy {
            try {
                getConnection?.inputStream?.use { it.readBytes() }
            } catch (e: Exception) {
                null
            }
        }

        override val metadataLength: Long? by lazy {
            headerFields
                ?.get("Content-Length")
                ?.mapNotNull(String::toLongOrNull)
                ?.firstOrNull()
        }
    }
}


