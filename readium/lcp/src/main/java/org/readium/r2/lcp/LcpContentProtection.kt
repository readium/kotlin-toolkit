/*
 * Copyright 2020 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.lcp

import org.readium.r2.lcp.auth.LcpPassphraseAuthentication
import org.readium.r2.shared.fetcher.Fetcher
import org.readium.r2.shared.fetcher.TransformingFetcher
import org.readium.r2.shared.publication.ContentProtection
import org.readium.r2.shared.publication.Publication
import org.readium.r2.shared.publication.asset.FileAsset
import org.readium.r2.shared.publication.asset.PublicationAsset
import org.readium.r2.shared.publication.services.contentProtectionServiceFactory
import org.readium.r2.shared.util.Try

internal class LcpContentProtection(
    private val lcpService: LcpService,
    private val authentication: LcpAuthenticating
) : ContentProtection {

    override suspend fun open(
        asset: PublicationAsset,
        fetcher: Fetcher,
        credentials: String?,
        allowUserInteraction: Boolean,
        sender: Any?
    ): Try<ContentProtection.ProtectedAsset, Publication.OpeningException>? {
        if (asset !is FileAsset) {
            return null
        }

        if (!lcpService.isLcpProtected(asset.file)) {
            return null
        }

        val authentication = credentials?.let { LcpPassphraseAuthentication(it, fallback = this.authentication) }
            ?: this.authentication

        val license = lcpService
            .retrieveLicense(asset.file,  authentication, allowUserInteraction, sender)

        val serviceFactory = LcpContentProtectionService
            .createFactory(license?.getOrNull(), license?.exceptionOrNull())

        val protectedFile = ContentProtection.ProtectedAsset(
            asset = asset,
            fetcher = TransformingFetcher(fetcher, LcpDecryptor(license?.getOrNull())::transform),
            onCreatePublication = {
                servicesBuilder.contentProtectionServiceFactory = serviceFactory
            }
        )

        return Try.success(protectedFile)
    }

}
