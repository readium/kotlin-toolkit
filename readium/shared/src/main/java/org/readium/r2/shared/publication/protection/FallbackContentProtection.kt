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
import org.readium.r2.shared.util.format.Trait

/**
 * [ContentProtection] implementation used as a fallback by the Streamer to detect LCP DRM
 * if it is not supported by the app.
 */
@InternalReadiumApi
public class FallbackContentProtection : ContentProtection {

    override suspend fun open(
        asset: Asset,
        credentials: String?,
        allowUserInteraction: Boolean
    ): Try<ContentProtection.OpenResult, ContentProtection.OpenError> {
        if (asset !is ContainerAsset) {
            return Try.failure(
                ContentProtection.OpenError.AssetNotSupported()
            )
        }

        val protectionServiceFactory = when {
            asset.format.conformsTo(Trait.LCP_PROTECTED) ->
                FallbackContentProtectionService.createFactory(Scheme.Lcp, "Readium LCP")
            asset.format.conformsTo(Trait.ADEPT_PROTECTED) ->
                FallbackContentProtectionService.createFactory(Scheme.Adept, "Adobe ADEPT")
            else ->
                return Try.failure(ContentProtection.OpenError.AssetNotSupported())
        }

        val protectedFile = ContentProtection.OpenResult(
            asset = asset,
            onCreatePublication = {
                servicesBuilder.contentProtectionServiceFactory = protectionServiceFactory
            }
        )

        return Try.success(protectedFile)
    }
}
