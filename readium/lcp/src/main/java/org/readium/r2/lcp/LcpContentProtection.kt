/*
 * Copyright 2020 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.lcp

import android.content.ContentResolver
import java.io.File
import org.readium.r2.lcp.auth.LcpDumbAuthentication
import org.readium.r2.lcp.auth.LcpPassphraseAuthentication
import org.readium.r2.lcp.license.model.LicenseDocument
import org.readium.r2.lcp.service.LcpLicensedAsset
import org.readium.r2.shared.fetcher.Fetcher
import org.readium.r2.shared.fetcher.TransformingFetcher
import org.readium.r2.shared.publication.ContentProtection
import org.readium.r2.shared.publication.Publication
import org.readium.r2.shared.publication.asset.AssetFactory
import org.readium.r2.shared.publication.asset.FileAsset
import org.readium.r2.shared.publication.asset.PublicationAsset
import org.readium.r2.shared.publication.asset.SimpleAsset
import org.readium.r2.shared.publication.services.contentProtectionServiceFactory
import org.readium.r2.shared.util.Try
import org.readium.r2.shared.util.Url
import org.readium.r2.shared.util.mediatype.AssetType
import org.readium.r2.shared.util.mediatype.MediaType

internal class LcpContentProtection(
    private val lcpService: LcpService,
    private val authentication: LcpAuthenticating,
    private val assetFactory: AssetFactory
) : ContentProtection {

    override suspend fun open(
        url: Url,
        mediaType: MediaType,
        assetType: AssetType,
        credentials: String?,
        allowUserInteraction: Boolean,
        sender: Any?
    ): Try<ContentProtection.ProtectedAsset, Publication.OpeningException>? {
        if (url.scheme != ContentResolver.SCHEME_FILE || mediaType != MediaType.LCP_LICENSE_DOCUMENT) {
            return null
        }

        val licenseFile = File(url.path)

        val asset = try {
            remoteAssetForLicenseThrowing(licenseFile)
        } catch (e: Exception) {
            // FIXME: random choice of exception
            val exception = Publication.OpeningException.ParsingFailed(LcpException.wrap(e))
            return Try.failure(exception)
        }

        return open(asset, credentials, allowUserInteraction, sender)
    }

    override suspend fun open(
        asset: PublicationAsset,
        credentials: String?,
        allowUserInteraction: Boolean,
        sender: Any?
    ): Try<ContentProtection.ProtectedAsset, Publication.OpeningException>? {
        val license = retrieveLicense(asset, asset.fetcher, credentials, allowUserInteraction, sender)
            ?: return null
        return createProtectedAsset(asset, asset.fetcher, license)
    }

    private suspend fun remoteAssetForLicenseThrowing(licenseFile: File): PublicationAsset {
        // Update the license file to get a fresh publication URL.
        val license = lcpService.retrieveLicense(licenseFile, LcpDumbAuthentication(), false)
            .getOrNull()

        val licenseDoc = license?.license
            ?: LicenseDocument(licenseFile.readBytes())

        val link = checkNotNull(licenseDoc.link(LicenseDocument.Rel.publication))
        val url = try {
            Url(link.url.toString()) ?: throw IllegalStateException()
        } catch (e: Exception) {
            throw LcpException.Parsing.Url(rel = LicenseDocument.Rel.publication.rawValue)
        }

        val asset = assetFactory.createAsset(
            url,
            link.mediaType,
            AssetType.Archive
        ).getOrThrow()

        return LcpLicensedAsset(
            url,
            link.mediaType,
            licenseFile,
            license,
            asset.fetcher
        )
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

        val newAsset = SimpleAsset(asset.name, asset.mediaType, newFetcher)

        val protectedFile = ContentProtection.ProtectedAsset(
            asset = newAsset,
            onCreatePublication = {
                servicesBuilder.contentProtectionServiceFactory = serviceFactory
            }
        )

        return Try.success(protectedFile)
    }
}
