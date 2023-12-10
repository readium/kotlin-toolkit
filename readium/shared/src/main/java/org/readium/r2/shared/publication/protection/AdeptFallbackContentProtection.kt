/*
 * Copyright 2023 Readium Foundation. All rights reserved.
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
 * [ContentProtection] implementation used as a fallback by the Streamer to detect Adept DRM,
 * if it is not supported by the app.
 */
@InternalReadiumApi
public class AdeptFallbackContentProtection : ContentProtection {

    override val scheme: Scheme = Scheme.Adept

    override suspend fun open(
        asset: Asset,
        credentials: String?,
        allowUserInteraction: Boolean
    ): Try<ContentProtection.Asset, ContentProtection.OpenError> {
        if (asset !is ContainerAsset || !asset.format.conformsTo(Format.EPUB_ADEPT)) {
            return Try.failure(ContentProtection.OpenError.AssetNotSupported())
        }

        val protectedFile = ContentProtection.Asset(
            asset.format,
            asset.container,
            onCreatePublication = {
                servicesBuilder.contentProtectionServiceFactory =
                    FallbackContentProtectionService.createFactory(scheme, "Adobe ADEPT")
            }
        )

        return Try.success(protectedFile)
    }
}
