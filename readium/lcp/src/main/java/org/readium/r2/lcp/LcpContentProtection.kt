/*
 * Copyright 2020 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.lcp

import org.readium.r2.lcp.auth.LcpPassphraseAuthentication
import org.readium.r2.lcp.service.LcpLicensedAsset
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
        val license = retrieveLicense(asset, fetcher, credentials, allowUserInteraction, sender)
            ?: return null
        return createProtectedAsset(asset, fetcher, license)
    }

    /* Returns null if the publication is not protected by LCP. */
    private suspend fun retrieveLicense(
        asset: PublicationAsset,
        fetcher: Fetcher,
        credentials: String?,
        allowUserInteraction: Boolean,
        sender: Any?
    ): Try<LcpLicense, LcpException>? {

        val authentication = credentials
            ?.let { LcpPassphraseAuthentication(it, fallback = this.authentication) }
            ?: this.authentication

        val license = when (asset) {
            is FileAsset ->
                lcpService.retrieveLicense(asset.file, authentication, allowUserInteraction, sender)
            is LcpLicensedAsset ->
                asset.license
                    ?.let { Try.success(it) }
                    ?: lcpService.retrieveLicense(asset.licenseFile, authentication, allowUserInteraction, sender)
            else ->
                lcpService.retrieveLicense(fetcher, asset.mediaType, authentication, allowUserInteraction, sender)
        }

        return license.takeUnless { result ->
            result is Try.Failure<*, *> && result.exception is LcpException.Container
        }
    }

    private fun createProtectedAsset(
        asset: PublicationAsset,
        fetcher: Fetcher,
        license: Try<LcpLicense, LcpException>,
    ): Try<ContentProtection.ProtectedAsset, Publication.OpeningException> {
        val serviceFactory = LcpContentProtectionService
            .createFactory(license.getOrNull(), license.exceptionOrNull())

        val newFetcher = TransformingFetcher(
            fetcher,
            LcpDecryptor(license.getOrNull())::transform
        )

        val protectedFile = ContentProtection.ProtectedAsset(
            asset = asset,
            fetcher  = newFetcher,
            onCreatePublication = {
                servicesBuilder.contentProtectionServiceFactory = serviceFactory
            }
        )

        return Try.success(protectedFile)
    }
}
