/*
 * Copyright 2021 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.shared.publication.protection

import org.readium.r2.shared.publication.Publication
import org.readium.r2.shared.publication.protection.ContentProtection.Scheme
import org.readium.r2.shared.publication.services.ContentProtectionService
import org.readium.r2.shared.publication.services.contentProtectionServiceFactory
import org.readium.r2.shared.util.Error
import org.readium.r2.shared.util.Try
import org.readium.r2.shared.util.asset.Asset
import org.readium.r2.shared.util.asset.ContainerAsset
import org.readium.r2.shared.util.format.Specification

/**
 * [ContentProtection] implementation used as a fallback when detecting known DRMs
 * not supported by the app.
 */
public class FallbackContentProtection : ContentProtection {

    override suspend fun open(
        asset: Asset,
        credentials: String?,
        allowUserInteraction: Boolean,
    ): Try<ContentProtection.OpenResult, ContentProtection.OpenError> {
        if (asset !is ContainerAsset) {
            return Try.failure(
                ContentProtection.OpenError.AssetNotSupported()
            )
        }

        val protectionServiceFactory = when {
            asset.format.conformsTo(Specification.Lcp) ->
                Service.createFactory(Scheme.Lcp, "Readium LCP")
            asset.format.conformsTo(Specification.Adept) ->
                Service.createFactory(Scheme.Adept, "Adobe ADEPT")
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

    public class SchemeNotSupportedError(
        public val scheme: Scheme,
        public val name: String,
    ) : Error {

        override val message: String = "$name DRM scheme is not supported."

        override val cause: Error? = null
    }

    private class Service(
        override val scheme: Scheme,
        override val name: String,
    ) : ContentProtectionService {

        override val isRestricted: Boolean =
            true

        override val credentials: String? =
            null

        override val rights: ContentProtectionService.UserRights =
            ContentProtectionService.UserRights.AllRestricted

        override val error: Error =
            SchemeNotSupportedError(scheme, name)

        companion object {

            fun createFactory(
                scheme: Scheme,
                name: String,
            ): (Publication.Service.Context) -> ContentProtectionService =
                { Service(scheme, name) }
        }
    }
}
