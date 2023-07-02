/*
 * Copyright 2023 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.shared.util.http

import org.readium.r2.shared.error.Try
import org.readium.r2.shared.resource.Resource
import org.readium.r2.shared.resource.ResourceFactory
import org.readium.r2.shared.util.Url

class HttpResourceFactory(
    private val httpClient: HttpClient
) : ResourceFactory {

    override suspend fun create(url: Url): Try<Resource, ResourceFactory.Error> {
        if (!url.scheme.startsWith("http")) {
            return Try.failure(ResourceFactory.Error.UnsupportedScheme(url.scheme))
        }

        val resource = HttpResource(httpClient, url.toString())
        return Try.success(resource)
    }
}
