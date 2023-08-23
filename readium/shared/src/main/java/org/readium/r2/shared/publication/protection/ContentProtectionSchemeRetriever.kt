/*
 * Copyright 2023 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.shared.publication.protection

import org.readium.r2.shared.asset.Asset
import org.readium.r2.shared.util.mediatype.MediaTypeRetriever

/**
 * Retrieves [ContentProtection] schemes of assets.
 */
public class ContentProtectionSchemeRetriever(
    contentProtections: List<ContentProtection>,
    mediaTypeRetriever: MediaTypeRetriever
) {
    private val contentProtections: List<ContentProtection> =
        contentProtections + listOf(
            LcpFallbackContentProtection(mediaTypeRetriever),
            AdeptFallbackContentProtection()
        )

    public suspend fun retrieve(asset: Asset): ContentProtection.Scheme? =
        contentProtections
            .firstOrNull { it.supports(asset) }
            ?.scheme
}
