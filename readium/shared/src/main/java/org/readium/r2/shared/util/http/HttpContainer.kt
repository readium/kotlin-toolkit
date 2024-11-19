/*
 * Copyright 2021 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.shared.util.http

import org.readium.r2.shared.DelicateReadiumApi
import org.readium.r2.shared.util.AbsoluteUrl
import org.readium.r2.shared.util.Url
import org.readium.r2.shared.util.data.Container
import org.readium.r2.shared.util.resource.Resource

/**
 * Fetches remote resources through HTTP.
 *
 * Since this container is used when doing progressive download streaming (e.g. audiobook), the HTTP
 * byte range requests are open-ended and reused. This helps to avoid issuing too many requests.
 *
 * @param baseUrl Base URL from which relative URLs are served.
 * @param entries Entries of this container as Urls absolute or relative to [baseUrl].
 * @param client HTTP client used to perform HTTP requests.
 */
public class HttpContainer(
    private val baseUrl: Url? = null,
    override val entries: Set<Url>,
    private val client: HttpClient,
) : Container<Resource> {

    @OptIn(DelicateReadiumApi::class)
    override fun get(url: Url): Resource? {
        // We don't check that url matches any entry because that might save us from edge cases.

        val absoluteUrl = (baseUrl?.resolve(url) ?: url) as? AbsoluteUrl

        return if (absoluteUrl == null || !absoluteUrl.isHttp) {
            null
        } else {
            HttpResource(absoluteUrl, client)
        }
    }

    override fun close() {}
}
