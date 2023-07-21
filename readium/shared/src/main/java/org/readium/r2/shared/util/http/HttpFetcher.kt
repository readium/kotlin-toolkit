/*
 * Copyright 2021 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.shared.util.http

import android.webkit.URLUtil
import org.readium.r2.shared.error.getOrDefault
import org.readium.r2.shared.fetcher.FailureResource
import org.readium.r2.shared.fetcher.Fetcher
import org.readium.r2.shared.publication.Link
import org.readium.r2.shared.resource.Resource
import timber.log.Timber

/**
 * Fetches remote resources through HTTP.
 *
 * Since this fetcher is used when doing progressive download streaming (e.g. audiobook), the HTTP
 * byte range requests are open-ended and reused. This helps to avoid issuing too many requests.
 *
 * @param client HTTP client used to perform HTTP requests.
 * @param baseUrl Base URL from which relative HREF are served.
 * @param links A set of links that are known to be available through this fetcher.
 */
public class HttpFetcher(
    private val client: HttpClient,
    private val baseUrl: String? = null,
    private val links: List<Link> = emptyList(),
) : Fetcher {

    override suspend fun links(): List<Link> = links

    override fun get(link: Link): Fetcher.Resource {
        val url = link.toUrl(baseUrl)

        return if (url == null || !URLUtil.isNetworkUrl(url)) {
            val cause = IllegalArgumentException("Invalid HREF: ${link.href}, produced URL: $url")
            Timber.e(cause)
            FailureResource(link, error = Resource.Exception.BadRequest(cause = cause))
        } else {
            HttpResource(link, url, client)
        }
    }

    override suspend fun close() {}

    /** Provides access to an external URL. */
    public class HttpResource(
        private val link: Link,
        private val resource: org.readium.r2.shared.util.http.HttpResource
    ) : Resource by resource, Fetcher.Resource {

        public companion object {

            public operator fun invoke(
                link: Link,
                url: String,
                client: HttpClient,
            ): HttpResource =
                HttpResource(link, HttpResource(client, url))
        }

        override suspend fun link(): Link =
            link.copy(
                type = resource.mediaType().getOrDefault(link.type)
            )
    }
}
