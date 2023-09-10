/*
 * Copyright 2020 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.lcp

import org.readium.r2.lcp.auth.LcpPassphraseAuthentication
import org.readium.r2.lcp.license.model.LicenseDocument
import org.readium.r2.shared.asset.Asset
import org.readium.r2.shared.asset.AssetRetriever
import org.readium.r2.shared.asset.AssetType
import org.readium.r2.shared.publication.Publication
import org.readium.r2.shared.publication.encryption.encryption
import org.readium.r2.shared.publication.flatten
import org.readium.r2.shared.publication.protection.ContentProtection
import org.readium.r2.shared.publication.services.contentProtectionServiceFactory
import org.readium.r2.shared.resource.ArchiveFactory
import org.readium.r2.shared.resource.Resource
import org.readium.r2.shared.resource.ResourceFactory
import org.readium.r2.shared.resource.TransformingContainer
import org.readium.r2.shared.util.ThrowableError
import org.readium.r2.shared.util.Try
import org.readium.r2.shared.util.Url
import org.readium.r2.shared.util.flatMap
import org.readium.r2.shared.util.getOrElse

internal class LcpContentProtection(
    private val lcpService: LcpService,
    private val authentication: LcpAuthenticating,
    private val assetRetriever: AssetRetriever
) : ContentProtection {

    override val scheme: ContentProtection.Scheme =
        ContentProtection.Scheme.Lcp

    override suspend fun supports(
        asset: Asset
    ): Boolean =
        lcpService.isLcpProtected(asset)

    override suspend fun open(
        asset: Asset,
        credentials: String?,
        allowUserInteraction: Boolean,
        sender: Any?
    ): Try<ContentProtection.Asset, Publication.OpeningException> {
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

        return lcpService.retrieveLicense(asset, authentication, allowUserInteraction, sender)
    }

    private fun createResultAsset(
        asset: Asset.Container,
        license: Try<LcpLicense, LcpException>
    ): Try<ContentProtection.Asset, Publication.OpeningException> {
        val serviceFactory = LcpContentProtectionService
            .createFactory(license.getOrNull(), license.failureOrNull())

        val decryptor = LcpDecryptor(license.getOrNull())

        val container = TransformingContainer(asset.container, decryptor::transform)

        val protectedFile = ContentProtection.Asset(
            mediaType = asset.mediaType,
            container = container,
            onCreatePublication = {
                decryptor.encryptionData = (manifest.readingOrder + manifest.resources + manifest.links)
                    .flatten()
                    .mapNotNull { it.properties.encryption?.let { enc -> it.href to enc } }
                    .toMap()

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

        val licenseDoc = license.getOrNull()?.license
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

        val link = checkNotNull(licenseDoc.link(LicenseDocument.Rel.Publication))
        val url = Url(link.url.toString())
            ?: return Try.failure(
                Publication.OpeningException.ParsingFailed(
                    ThrowableError(
                        LcpException.Parsing.Url(rel = LicenseDocument.Rel.Publication.value)
                    )
                )
            )

        return assetRetriever.retrieve(
            url,
            mediaType = link.mediaType,
            assetType = AssetType.Archive
        )
            .mapFailure { Publication.OpeningException.ParsingFailed(it) }
            .flatMap { createResultAsset(it as Asset.Container, license) }
    }

    private fun ResourceFactory.Error.wrap(): Publication.OpeningException =
        when (this) {
            is ResourceFactory.Error.NotAResource ->
                Publication.OpeningException.NotFound()
            is ResourceFactory.Error.Forbidden ->
                Publication.OpeningException.Forbidden()
            is ResourceFactory.Error.SchemeNotSupported ->
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
