/*
 * Copyright 2023 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.shared.util.http

import org.readium.r2.shared.fetcher.HttpFetcher
import org.readium.r2.shared.fetcher.Resource
import org.readium.r2.shared.publication.Link
import org.readium.r2.shared.util.Either
import org.readium.r2.shared.util.Url
import org.readium.r2.shared.util.archive.Package
import org.readium.r2.shared.util.archive.channel.ChannelZipArchiveFactory
import org.readium.r2.shared.util.io.Protocol


class HttpProtocol(
    private val httpClient: HttpClient
) : Protocol {

    override suspend fun open(url: Url): Either<Resource, Package>? {
        if (!url.scheme.startsWith("http")) {
            return null
        }

        val link = Link(href = url.toString())
        val resource = HttpFetcher.HttpResource(httpClient, link, url.toString())
        val archiveFactory = ChannelZipArchiveFactory(httpClient)
        val archiveResult = archiveFactory.open(resource, password = null)
        if (archiveResult.isSuccess) {
            return Either.Right(archiveResult.getOrThrow())
        }

        return Either.Left(resource)
    }
}
