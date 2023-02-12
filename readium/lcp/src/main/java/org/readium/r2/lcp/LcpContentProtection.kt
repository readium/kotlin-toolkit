/*
 * Copyright 2020 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.lcp

import org.readium.r2.lcp.auth.LcpPassphraseAuthentication
import org.readium.r2.lcp.service.LcpRemoteAsset
import org.readium.r2.shared.extensions.tryOrNull
import org.readium.r2.shared.fetcher.Fetcher
import org.readium.r2.shared.fetcher.TransformingFetcher
import org.readium.r2.shared.publication.ContentProtection
import org.readium.r2.shared.publication.Publication
import org.readium.r2.shared.publication.asset.FileAsset
import org.readium.r2.shared.publication.asset.PublicationAsset
import org.readium.r2.shared.publication.asset.RemoteAsset
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
        return createProtectedAsset(asset, license, fetcher)
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
            is RemoteAsset ->
                lcpService.retrieveLicense(fetcher, asset.mediaType(), authentication, allowUserInteraction, sender)
            is LcpRemoteAsset ->
                lcpService.retrieveLicense(asset.licenseFile, authentication, allowUserInteraction, sender)
            else ->
                null
        }

        return license?.takeUnless { result ->
            result is Try.Failure<*, *> && result.exception is LcpException.Container
        }
    }

    private suspend fun createProtectedAsset(
        originalAsset: PublicationAsset,
        license: Try<LcpLicense, LcpException>,
        fetcher: Fetcher
    ): Try<ContentProtection.ProtectedAsset, Publication.OpeningException> {
        val newAsset = when (originalAsset) {
            is LcpRemoteAsset -> {
                // Try to update the asset with the new license
                val remoteAsset = tryOrNull { license.getOrNull()?.license?.remoteAsset }
                    ?: RemoteAsset(originalAsset.url, originalAsset.mediaType())
                LcpRemoteAsset(remoteAsset, originalAsset.licenseFile)
            }
            else ->
                originalAsset
        }

        val serviceFactory = LcpContentProtectionService
            .createFactory(license.getOrNull(), license.exceptionOrNull())

        val protectedFile = ContentProtection.ProtectedAsset(
            asset = newAsset,
            fetcher = TransformingFetcher(fetcher, LcpDecryptor(license.getOrNull())::transform),
            onCreatePublication = {
                servicesBuilder.contentProtectionServiceFactory = serviceFactory
            }
        )

        return Try.success(protectedFile)
    }
}
