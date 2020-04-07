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
import org.readium.r2.shared.util.Try
import org.readium.r2.shared.util.isLazyInitialized
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL

class HttpFetcher : Fetcher {

    override fun get(link: Link): Resource = HttpResource(link)

    override fun close() {}

    private class HttpResource(link: Link): StreamResource() {

        private val headConnection: HttpURLConnection by lazy {
            (URL(link.href).openConnection() as HttpURLConnection).apply { requestMethod = "HEAD" }
        }

        private val getConnection: HttpURLConnection by lazy {
            (URL(link.href).openConnection() as HttpURLConnection).apply { requestMethod = "GET" }
        }

        private val headerFields: Map<String, List<String>>?
            get() = if (::getConnection.isLazyInitialized) {
                getConnection.headerFields
            } else {
                headConnection.headerFields
            }

        private val acceptsByteRanges by lazy {
            headerFields?.get("Accept-Ranges")?.firstOrNull() == "bytes"
        }

        override val link: Link by lazy {
            link.copy(type = headerFields?.get("Content-Type")?.firstOrNull { it != "application/octet-stream" } ?: link.type)
        }

        override fun stream(): Try<InputStream, Resource.Error> =
            Try.success(bytes.inputStream())  // getConnection?.inputStream always returns the same stream

        private val bytes: ByteArray by lazy {
            getConnection.inputStream.use { it.readBytes() }
        }

        override val metadataLength: Long? by lazy {
            headerFields
                ?.get("Content-Length")
                ?.mapNotNull(String::toLongOrNull)
                ?.firstOrNull()
        }

        override fun close() {}
    }
}


