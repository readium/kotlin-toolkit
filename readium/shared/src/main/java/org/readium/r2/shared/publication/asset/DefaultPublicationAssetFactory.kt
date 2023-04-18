/*
 * Copyright 2023 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.shared.publication.asset

import java.io.File
import java.net.URL
import org.readium.r2.shared.publication.Publication
import org.readium.r2.shared.util.Try
import org.readium.r2.shared.util.archive.ArchiveFactory
import org.readium.r2.shared.util.archive.DefaultArchiveFactory
import org.readium.r2.shared.util.http.DefaultHttpClient
import org.readium.r2.shared.util.http.HttpClient
import org.readium.r2.shared.util.mediatype.MediaType

/**
 * A factory for various [PublicationAsset]s.
 *
 * Supported protocols are file, http and https.
 */
class DefaultPublicationAssetFactory(
    val archiveFactory: ArchiveFactory = DefaultArchiveFactory(),
    val httpClient: HttpClient = DefaultHttpClient()
) : PublicationAssetFactory {

    private val fileAssetFactory: FileAsset.Factory =
        FileAsset.Factory(archiveFactory, httpClient)

    private val remoteAssetFactory: RemoteAsset.Factory =
        RemoteAsset.Factory(archiveFactory, httpClient)

    /**
     * Creates an asset for a publication available at [url].
     *
     * @param url the url at which the publication is available
     * @param mediaType the publication media type
     */
    override suspend fun createAsset(
        url: URL,
        mediaType: MediaType,
    ): Try<PublicationAsset, Publication.OpeningException> {
        return when (url.protocol) {
            "file" -> fileAssetFactory.createAsset(File(url.path), mediaType)
            "http", "https" -> remoteAssetFactory.createAsset(url, mediaType)
            else -> throw IllegalArgumentException("Protocol not supported.")
        }
    }
}
