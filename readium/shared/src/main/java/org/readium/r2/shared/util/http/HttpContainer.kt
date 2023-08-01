/*
 * Copyright 2021 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.shared.util.http

import org.readium.r2.shared.resource.Container
import org.readium.r2.shared.resource.FailureResource
import org.readium.r2.shared.resource.Resource
import org.readium.r2.shared.resource.toEntry
import org.readium.r2.shared.util.Href
import org.readium.r2.shared.util.isHttp
import timber.log.Timber

/**
 * Fetches remote resources through HTTP.
 *
 * Since this fetcher is used when doing progressive download streaming (e.g. audiobook), the HTTP
 * byte range requests are open-ended and reused. This helps to avoid issuing too many requests.
 *
 * @param client HTTP client used to perform HTTP requests.
 * @param baseUrl Base URL from which relative HREF are served.
 * @param paths A set of paths that are known to be available through this container.
 */
public class HttpContainer(
    private val client: HttpClient,
    private val baseUrl: String? = null,
    private val paths: List<String> = emptyList(),
) : Container {

    override suspend fun entries(): Iterable<Container.Entry> =
        paths.map { get(it) }

    override suspend fun get(path: String): Container.Entry {
        val url = Href(path.removePrefix("/"), baseHref = baseUrl ?: "/").toUrl()

        return if (url == null || !url.isHttp()) {
            val cause = IllegalArgumentException("Invalid HREF: ${path}, produced URL: $url")
            Timber.e(cause)
            FailureResource(Resource.Exception.BadRequest(cause = cause))
        } else {
            HttpResource(client, url)
        }
            .toEntry(path)
    }

    override suspend fun close() {}
}
