/*
 * Copyright 2021 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.shared.util.http

import org.readium.r2.shared.util.AbsoluteUrl
import org.readium.r2.shared.util.Url
import org.readium.r2.shared.util.resource.Container
import org.readium.r2.shared.util.resource.FailureResource
import org.readium.r2.shared.util.resource.Resource
import org.readium.r2.shared.util.resource.toEntry

/**
 * Fetches remote resources through HTTP.
 *
 * Since this fetcher is used when doing progressive download streaming (e.g. audiobook), the HTTP
 * byte range requests are open-ended and reused. This helps to avoid issuing too many requests.
 *
 * @param client HTTP client used to perform HTTP requests.
 * @param baseUrl Base URL from which relative URLs are served.
 */
public class HttpContainer(
    private val client: HttpClient,
    private val baseUrl: Url? = null
) : Container {

    override suspend fun entries(): Set<Container.Entry>? = null

    override fun get(url: Url): Container.Entry {
        val absoluteUrl = (baseUrl?.resolve(url) ?: url) as? AbsoluteUrl

        return if (absoluteUrl == null || !absoluteUrl.isHttp) {
            FailureResource(
                Resource.Exception.NotFound(
                    url,
                    Exception("URL scheme is not supported: ${absoluteUrl?.scheme}.")
                )
            )
        } else {
            HttpResource(client, absoluteUrl)
        }
            .toEntry(url)
    }

    override suspend fun close() {}
}
