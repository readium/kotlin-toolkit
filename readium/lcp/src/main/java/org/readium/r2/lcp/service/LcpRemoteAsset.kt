/*
 * Copyright 2023 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.lcp.service

import java.io.File
import java.net.URL
import org.readium.r2.shared.fetcher.Fetcher
import org.readium.r2.shared.publication.Publication
import org.readium.r2.shared.publication.asset.PublicationAsset
import org.readium.r2.shared.publication.asset.RemoteAsset
import org.readium.r2.shared.util.Try
import org.readium.r2.shared.util.mediatype.MediaType

/**
 * A compound asset built from a LCP license file and a URL to access the publication.
 */
internal class LcpRemoteAsset(
    private val remoteAsset: RemoteAsset,
    val licenseFile: File
) : PublicationAsset {

    val url: URL =
        remoteAsset.url

    override val name: String =
        remoteAsset.name

    override suspend fun mediaType(): MediaType =
        remoteAsset.mediaType()

    override suspend fun createFetcher(
        dependencies: PublicationAsset.Dependencies,
        credentials: String?
    ): Try<Fetcher, Publication.OpeningException> =
        remoteAsset.createFetcher(dependencies, credentials)

    override fun toString(): String =
        "LcpRemoteAsset($url)"
}
