/*
 * Copyright 2023 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.shared.publication.asset

import org.readium.r2.shared.publication.Publication
import org.readium.r2.shared.util.Try
import org.readium.r2.shared.util.Url
import org.readium.r2.shared.util.mediatype.MediaType

/**
 * Builds [PublicationAsset]s for publications that can be accessed through various means.
 */
interface PublicationAssetFactory {

    /**
     * Creates a [PublicationAsset] for a publication with media type [mediaType] available at [url].
     */
    suspend fun createAsset(
        url: Url,
        mediaType: MediaType
    ): Try<PublicationAsset, Publication.OpeningException>
}
