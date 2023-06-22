/*
 * Copyright 2020 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.lcp

import java.io.File
import org.readium.r2.lcp.auth.LcpDumbAuthentication
import org.readium.r2.lcp.auth.LcpPassphraseAuthentication
import org.readium.r2.lcp.license.model.LicenseDocument
import org.readium.r2.shared.asset.Asset
import org.readium.r2.shared.asset.AssetType
import org.readium.r2.shared.fetcher.ContainerFetcher
import org.readium.r2.shared.fetcher.TransformingFetcher
import org.readium.r2.shared.publication.Publication
import org.readium.r2.shared.publication.protection.ContentProtection
import org.readium.r2.shared.publication.services.contentProtectionServiceFactory
import org.readium.r2.shared.resource.ArchiveFactory
import org.readium.r2.shared.resource.ResourceFactory
import org.readium.r2.shared.util.Try
import org.readium.r2.shared.util.Url
import org.readium.r2.shared.util.getOrElse
import org.readium.r2.shared.util.mediatype.MediaType
import org.readium.r2.shared.util.mediatype.MediaTypeRetriever

internal class LcpContentProtection(
    private val lcpService: LcpService,
    private val authentication: LcpAuthenticating,
    private val mediaTypeRetriever: MediaTypeRetriever,
    private val resourceFactory: ResourceFactory,
    private val archiveFactory: ArchiveFactory
) : ContentProtection {

    override val scheme: ContentProtection.Scheme =
        ContentProtection.Scheme.Lcp

    override suspend fun supports(
        asset: Asset
    ): Boolean =
        lcpService.isLcpProtected(asset)

    override suspend fun open(
        asset: Asset,
        drmScheme: String,
        credentials: String?,
        allowUserInteraction: Boolean,
        sender: Any?
    ): Try<ContentProtection.Asset, Publication.OpeningException>? {
        return when (asset) {
            is Asset.Container -> openPublication(asset, credentials, allowUserInteraction, sender)
            is Asset.Resource -> openLicense(asset, credentials, allowUserInteraction, sender)
        }
    }

    private suspend fun openPublication(
        asset: Asset.Container,
        credentials: String?,
        allowUserInteraction: Boolean,
        sender: Any?
    ): Try<ContentProtection.Asset, Publication.OpeningException>? {
        val license = retrieveLicense(asset, credentials, allowUserInteraction, sender)
            ?: return null

        return createProtectedAsset(asset, license)
    }

    /* Returns null if the publication is not protected by LCP. */
    private suspend fun retrieveLicense(
        asset: Asset,
        credentials: String?,
        allowUserInteraction: Boolean,
        sender: Any?
    ): Try<LcpLicense, LcpException>? {

        val authentication = credentials
            ?.let { LcpPassphraseAuthentication(it, fallback = this.authentication) }
            ?: this.authentication

        val license = when (asset) {
            is Asset.Resource ->
                asset.resource.file?.let { licenseFile ->
                    lcpService.retrieveLicense(licenseFile, authentication, allowUserInteraction, sender)
                }
            is Asset.Container ->
                lcpService.retrieveLicense(asset.container, asset.mediaType, authentication, allowUserInteraction, sender)
        }

        return license.takeUnless { result ->
            result is Try.Failure<*, *> && result.exception is LcpException.Container
        }
    }

    private fun createProtectedAsset(
        asset: Asset.Container,
        license: Try<LcpLicense, LcpException>,
    ): Try<ContentProtection.Asset, Publication.OpeningException> {
        val serviceFactory = LcpContentProtectionService
            .createFactory(license.getOrNull(), license.exceptionOrNull())

        val fetcher = TransformingFetcher(
            ContainerFetcher(asset.container, mediaTypeRetriever),
            LcpDecryptor(license.getOrNull())::transform
        )

        val protectedFile = ContentProtection.Asset(
            name = asset.name,
            mediaType = asset.mediaType,
            fetcher = fetcher,
            onCreatePublication = {
                servicesBuilder.contentProtectionServiceFactory = serviceFactory
            }
        )

        return Try.success(protectedFile)
    }

    private suspend fun openLicense(
        asset: Asset.Resource,
        credentials: String?,
        allowUserInteraction: Boolean,
        sender: Any?
    ): Try<ContentProtection.Asset, Publication.OpeningException>? {
        val assetFile = asset.resource.file

        if (assetFile == null || asset.mediaType != MediaType.LCP_LICENSE_DOCUMENT) {
            return null
        }

        val publicationAsset = try {
            remoteAssetForLicenseThrowing(assetFile)
        } catch (e: Exception) {
            // FIXME: random choice of exception
            val exception = Publication.OpeningException.ParsingFailed(LcpException.wrap(e))
            return Try.failure(exception)
        }

        val license = retrieveLicense(asset, credentials, allowUserInteraction, sender)
            ?: return null

        return createProtectedAsset(publicationAsset, license)
    }

    private suspend fun remoteAssetForLicenseThrowing(licenseFile: File): Asset.Container {
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

        val resource = resourceFactory.create(url)
            .getOrElse { throw it }

        val container = archiveFactory.create(resource, password = null)
            .getOrElse { throw it }

        return Asset.Container(
            url.filename,
            link.mediaType,
            AssetType.Archive,
            container
        )
    }
}
