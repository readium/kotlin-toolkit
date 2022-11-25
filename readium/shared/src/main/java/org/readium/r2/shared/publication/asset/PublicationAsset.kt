/*
 * Copyright 2020 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.shared.publication.asset

import org.readium.r2.shared.fetcher.Fetcher
import org.readium.r2.shared.publication.Publication
import org.readium.r2.shared.util.Try
import org.readium.r2.shared.util.archive.ArchiveFactory
import org.readium.r2.shared.util.mediatype.MediaType

/** Represents a digital medium (e.g. a file) offering access to a publication. */
interface PublicationAsset {

    /** Name of the asset, e.g. a filename. */
    val name: String

    /**
     * Media type of the asset.
     *
     * If unknown, fallback on `MediaType.BINARY`.
     */
    suspend fun mediaType(): MediaType

    /**
     * Creates a fetcher used to access the asset's content.
     */
    suspend fun createFetcher(dependencies: Dependencies, credentials: String?): Try<Fetcher, Publication.OpeningException>

    data class Dependencies(val archiveFactory: ArchiveFactory)
}
