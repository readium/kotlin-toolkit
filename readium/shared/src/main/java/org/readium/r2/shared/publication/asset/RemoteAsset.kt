/*
 * Copyright 2023 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.shared.publication.asset

import java.net.URL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.readium.r2.shared.fetcher.ArchiveFetcher
import org.readium.r2.shared.fetcher.Fetcher
import org.readium.r2.shared.publication.Publication
import org.readium.r2.shared.util.Try
import org.readium.r2.shared.util.http.HttpException
import org.readium.r2.shared.util.mediatype.MediaType
import timber.log.Timber

/**
 * Represents a publication accessible remotely.
 */
class RemoteAsset(
    val url: URL,
    private val knownMediaType: MediaType
) : PublicationAsset {

    override val name: String =
        url.file

    override suspend fun mediaType(): MediaType {
        return knownMediaType
    }

    override suspend fun createFetcher(
        dependencies: PublicationAsset.Dependencies,
        credentials: String?
    ): Try<Fetcher, Publication.OpeningException> = withContext(Dispatchers.IO) {
        try {
            val archive = dependencies.archiveFactory.open(url, password = null)
            val fetcher = ArchiveFetcher(archive)
            Try.success(fetcher)
        } catch (e: HttpException) {
            val openingException = when (e.kind) {
                HttpException.Kind.Unauthorized -> Publication.OpeningException.IncorrectCredentials
                HttpException.Kind.Forbidden -> Publication.OpeningException.Forbidden(e)
                HttpException.Kind.NotFound -> Publication.OpeningException.NotFound(e)
                else -> Publication.OpeningException.Unavailable(e)
            }
            Try.failure(openingException)
        } catch (e: Exception) {
            Timber.e(e)
            Try.failure(Publication.OpeningException.UnsupportedFormat(e))
        }
    }

    override fun toString(): String = "RemoteAsset($url)"
}
