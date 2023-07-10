/*
 * Copyright 2020 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.lcp

import org.readium.r2.lcp.auth.LcpPassphraseAuthentication
import org.readium.r2.lcp.license.model.LicenseDocument
import org.readium.r2.shared.asset.Asset
import org.readium.r2.shared.error.ThrowableError
import org.readium.r2.shared.error.Try
import org.readium.r2.shared.error.getOrElse
import org.readium.r2.shared.fetcher.ContainerFetcher
import org.readium.r2.shared.fetcher.TransformingFetcher
import org.readium.r2.shared.publication.Publication
import org.readium.r2.shared.publication.protection.ContentProtection
import org.readium.r2.shared.publication.services.contentProtectionServiceFactory
import org.readium.r2.shared.resource.ArchiveFactory
import org.readium.r2.shared.resource.Resource
import org.readium.r2.shared.resource.ResourceFactory
import org.readium.r2.shared.util.Url
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
        if (drmScheme != scheme.uri) {
            return null
        }

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
    ): Try<ContentProtection.Asset, Publication.OpeningException> {
        val license = retrieveLicense(asset, credentials, allowUserInteraction, sender)
        return createResultAsset(asset, license)
    }

    private suspend fun retrieveLicense(
        asset: Asset,
        credentials: String?,
        allowUserInteraction: Boolean,
        sender: Any?
    ): Try<LcpLicense, LcpException> {
        val authentication = credentials
            ?.let { LcpPassphraseAuthentication(it, fallback = this.authentication) }
            ?: this.authentication

        val file = (asset as? Asset.Resource)?.resource?.file
            ?: (asset as? Asset.Container)?.container?.file

        return file
            // This is less restrictive with regard to network availability.
            ?.let { lcpService.retrieveLicense(it, asset.mediaType, authentication, allowUserInteraction, sender) }
            ?: lcpService.retrieveLicense(asset, authentication, allowUserInteraction, sender)
    }

    private fun createResultAsset(
        asset: Asset.Container,
        license: Try<LcpLicense, LcpException>,
    ): Try<ContentProtection.Asset, Publication.OpeningException> {
        val serviceFactory = LcpContentProtectionService
            .createFactory(license.successOrNull(), license.failureOrNull())

        val fetcher = TransformingFetcher(
            ContainerFetcher(asset.container, mediaTypeRetriever),
            LcpDecryptor(license.successOrNull())::transform
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
        licenseAsset: Asset.Resource,
        credentials: String?,
        allowUserInteraction: Boolean,
        sender: Any?
    ): Try<ContentProtection.Asset, Publication.OpeningException> {
        val license = retrieveLicense(licenseAsset, credentials, allowUserInteraction, sender)

        val licenseDoc = license.successOrNull()?.license
            ?: licenseAsset.resource.read()
                .map {
                    try {
                        LicenseDocument(it)
                    } catch (e: Exception) {
                        return Try.failure(
                            Publication.OpeningException.ParsingFailed(
                                ThrowableError(e)
                            )
                        )
                    }
                }
                .getOrElse {
                    return Try.failure(
                        it.wrap()
                    )
                }

        val link = checkNotNull(licenseDoc.link(LicenseDocument.Rel.publication))
        val url = Url(link.url.toString())
            ?: return Try.failure(
                Publication.OpeningException.ParsingFailed(
                    ThrowableError(
                        LcpException.Parsing.Url(rel = LicenseDocument.Rel.publication.rawValue)
                    )
                )
            )

        val resource = resourceFactory.create(url)
            .getOrElse { return Try.failure(it.wrap()) }

        val container = archiveFactory.create(resource, password = null)
            .getOrElse { return Try.failure(it.wrap()) }

        val publicationAsset = Asset.Container(
            url.filename,
            link.mediaType,
            false,
            container
        )

        return createResultAsset(publicationAsset, license)
    }

    private fun ResourceFactory.Error.wrap(): Publication.OpeningException =
        when (this) {
            is ResourceFactory.Error.NotAResource ->
                Publication.OpeningException.NotFound()
            is ResourceFactory.Error.Forbidden ->
                Publication.OpeningException.Forbidden()
            is ResourceFactory.Error.UnsupportedScheme ->
                Publication.OpeningException.UnsupportedAsset()
        }

    private fun ArchiveFactory.Error.wrap(): Publication.OpeningException =
        when (this) {
            is ArchiveFactory.Error.FormatNotSupported ->
                Publication.OpeningException.UnsupportedAsset()
            is ArchiveFactory.Error.PasswordsNotSupported ->
                Publication.OpeningException.UnsupportedAsset()
            is ArchiveFactory.Error.ResourceReading ->
                resourceException.wrap()
        }

    private fun Resource.Exception.wrap(): Publication.OpeningException =
        when (this) {
            is Resource.Exception.Forbidden ->
                Publication.OpeningException.Forbidden(ThrowableError(this))
            is Resource.Exception.NotFound ->
                Publication.OpeningException.NotFound(ThrowableError(this))
            Resource.Exception.Offline, is Resource.Exception.Unavailable ->
                Publication.OpeningException.Unavailable(ThrowableError(this))
            is Resource.Exception.Other, is Resource.Exception.BadRequest ->
                Publication.OpeningException.Unexpected(this)
            is Resource.Exception.OutOfMemory ->
                Publication.OpeningException.OutOfMemory(ThrowableError(this))
        }
}
