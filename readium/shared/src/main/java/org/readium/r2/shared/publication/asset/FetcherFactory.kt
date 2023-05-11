/*
 * Copyright 2023 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.shared.publication.asset

import org.readium.r2.shared.fetcher.Fetcher
import org.readium.r2.shared.publication.Publication
import org.readium.r2.shared.util.Try
import org.readium.r2.shared.util.archive.ArchiveFactory
import org.readium.r2.shared.util.archive.DefaultArchiveFactory
import org.readium.r2.shared.util.http.DefaultHttpClient
import org.readium.r2.shared.util.http.HttpClient

interface FetcherFactory {

    suspend fun createFetcher(asset: PublicationAsset): Try<Fetcher, Publication.OpeningException>

}

class DefaultFetcherFactory(
    private val fileFetcherFactory: FileFetcherFactory,
    private val remoteFetcherFactory: RemoteFetcherFactory
) : FetcherFactory {

    companion object {

        operator fun invoke(): DefaultFetcherFactory {
            val httpClient = DefaultHttpClient()
            val archiveFactory = DefaultArchiveFactory()

            return DefaultFetcherFactory(archiveFactory, httpClient)
        }

        operator fun invoke(archiveFactory: ArchiveFactory, httpClient: HttpClient): DefaultFetcherFactory {
            return DefaultFetcherFactory(
                FileFetcherFactory(archiveFactory, httpClient),
                RemoteFetcherFactory(archiveFactory, httpClient)
            )
        }
    }

    override suspend fun createFetcher(asset: PublicationAsset): Try<Fetcher, Publication.OpeningException> {
        return when (asset) {
            is FileAsset -> fileFetcherFactory.createFetcher(asset)
            is RemoteAsset -> remoteFetcherFactory.createFetcher(asset)
            else -> Try.failure(Publication.OpeningException.UnsupportedFormat())
        }
    }

}