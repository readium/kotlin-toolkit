/*
 * Copyright 2023 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.shared.publication.asset

import org.readium.r2.shared.publication.Publication
import org.readium.r2.shared.util.Try
import org.readium.r2.shared.util.Url
import org.readium.r2.shared.util.archive.ArchiveFactory
import org.readium.r2.shared.util.archive.DefaultArchiveFactory
import org.readium.r2.shared.util.http.DefaultHttpClient
import org.readium.r2.shared.util.http.HttpClient
import org.readium.r2.shared.util.mediatype.AssetType
import org.readium.r2.shared.util.mediatype.MediaType
import org.readium.r2.shared.util.mediatype.MediaTypeRetriever

interface AssetFactory {

    suspend fun createAsset(
        url: Url,
        mediaType: MediaType,
        assetType: AssetType
    ): Try<PublicationAsset, Publication.OpeningException>
}

class DefaultAssetFactory(
    private val fileAssetFactory: FileAssetFactory,
    private val remoteAssetFactory: RemoteAssetFactory
) : AssetFactory {

    companion object {

        operator fun invoke(): DefaultAssetFactory {
            val httpClient = DefaultHttpClient()
            val archiveFactory = DefaultArchiveFactory()
            val mediaTypeRetriever = MediaTypeRetriever(archiveFactory)

            return DefaultAssetFactory(archiveFactory, httpClient, mediaTypeRetriever)
        }

        operator fun invoke(
            archiveFactory: ArchiveFactory,
            httpClient: HttpClient,
            mediaTypeRetriever: MediaTypeRetriever
        ): DefaultAssetFactory {
            return DefaultAssetFactory(
                FileAssetFactory(archiveFactory, httpClient, mediaTypeRetriever),
                RemoteAssetFactory(archiveFactory, httpClient, mediaTypeRetriever)
            )
        }
    }

    override suspend fun createAsset(
        url: Url,
        mediaType: MediaType,
        assetType: AssetType
    ): Try<PublicationAsset, Publication.OpeningException> {
        return when {
            url.scheme == "file" ->
                fileAssetFactory.createAsset(url, mediaType, assetType)
            url.scheme.startsWith("http") ->
                remoteAssetFactory.createAsset(url, mediaType, assetType)
            else ->
                Try.failure(Publication.OpeningException.UnsupportedFormat())
        }
    }
}
