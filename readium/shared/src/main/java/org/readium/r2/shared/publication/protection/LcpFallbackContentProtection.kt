/*
 * Copyright 2021 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.shared.publication.protection

import org.readium.r2.shared.InternalReadiumApi
import org.readium.r2.shared.publication.protection.ContentProtection.Scheme
import org.readium.r2.shared.publication.services.contentProtectionServiceFactory
import org.readium.r2.shared.util.Try
import org.readium.r2.shared.util.asset.Asset
import org.readium.r2.shared.util.asset.ContainerAsset
import org.readium.r2.shared.util.format.Format

/**
 * [ContentProtection] implementation used as a fallback by the Streamer to detect LCP DRM
 * if it is not supported by the app.
 */
@InternalReadiumApi
public class LcpFallbackContentProtection : ContentProtection {

    override val scheme: Scheme =
        Scheme.Lcp

    override suspend fun open(
        asset: Asset,
        credentials: String?,
        allowUserInteraction: Boolean
    ): Try<ContentProtection.Asset, ContentProtection.OpenError> {
        if (
            !asset.format.conformsTo(Format.EPUB_LCP) &&
            !asset.format.conformsTo(Format.RPF_LCP) &&
            !asset.format.conformsTo(Format.RPF_AUDIO_LCP) &&
            !asset.format.conformsTo(Format.RPF_IMAGE_LCP) &&
            !asset.format.conformsTo(Format.RPF_PDF_LCP)
            ) {
            return Try.failure(ContentProtection.OpenError.AssetNotSupported())
        }

        if (asset !is ContainerAsset) {
            return Try.failure(ContentProtection.OpenError.AssetNotSupported()
            )
        }

        val protectedFile = ContentProtection.Asset(
            asset.format,
            asset.container,
            onCreatePublication = {
                servicesBuilder.contentProtectionServiceFactory =
                    FallbackContentProtectionService.createFactory(scheme, "Readium LCP")
            }
        )

        return Try.success(protectedFile)
    }
}
