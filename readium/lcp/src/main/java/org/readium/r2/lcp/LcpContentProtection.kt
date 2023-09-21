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
import org.readium.r2.shared.resource.Resource
import org.readium.r2.shared.resource.TransformingContainer
import org.readium.r2.shared.util.AbsoluteUrl
import org.readium.r2.shared.util.ThrowableError
import org.readium.r2.shared.util.Try
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
    ): Try<ContentProtection.Asset, Publication.OpenError> {
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
    ): Try<ContentProtection.Asset, Publication.OpenError> {
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
    ): Try<ContentProtection.Asset, Publication.OpenError> {
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
                    .mapNotNull {
                        it.properties.encryption?.let { enc -> it.url() to enc }
                    }
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
    ): Try<ContentProtection.Asset, Publication.OpenError> {
        val license = retrieveLicense(licenseAsset, credentials, allowUserInteraction, sender)

        val licenseDoc = license.getOrNull()?.license
            ?: licenseAsset.resource.read()
                .map {
                    try {
                        LicenseDocument(it)
                    } catch (e: Exception) {
                        return Try.failure(
                            Publication.OpenError.InvalidAsset(cause = ThrowableError(e))
                        )
                    }
                }
                .getOrElse {
                    return Try.failure(
                        it.wrap()
                    )
                }

        val link = licenseDoc.publicationLink
        val url = (link.url() as? AbsoluteUrl)
            ?: return Try.failure(
                Publication.OpenError.InvalidAsset(
                    cause = ThrowableError(
                        LcpException.Parsing.Url(rel = LicenseDocument.Rel.Publication.value)
                    )
                )
            )

        val asset =
            if (link.mediaType != null) {
                assetRetriever.retrieve(
                    url,
                    mediaType = link.mediaType,
                    assetType = AssetType.Archive
                )
                    .map { it as Asset.Container }
                    .mapFailure { it.wrap() }
            } else {
                (assetRetriever.retrieve(url) as? Asset.Container)
                    ?.let { Try.success(it) }
                    ?: Try.failure(Publication.OpenError.UnsupportedAsset())
            }

        return asset.flatMap { createResultAsset(it, license) }
    }

    private fun Resource.Exception.wrap(): Publication.OpenError =
        when (this) {
            is Resource.Exception.Forbidden ->
                Publication.OpenError.Forbidden(ThrowableError(this))
            is Resource.Exception.NotFound ->
                Publication.OpenError.NotFound(ThrowableError(this))
            Resource.Exception.Offline, is Resource.Exception.Unavailable ->
                Publication.OpenError.Unavailable(ThrowableError(this))
            is Resource.Exception.Other, is Resource.Exception.BadRequest ->
                Publication.OpenError.Unknown(this)
            is Resource.Exception.OutOfMemory ->
                Publication.OpenError.OutOfMemory(ThrowableError(this))
        }

    private fun AssetRetriever.Error.wrap(): Publication.OpenError =
        when (this) {
            is AssetRetriever.Error.ArchiveFormatNotSupported ->
                Publication.OpenError.UnsupportedAsset(this)
            is AssetRetriever.Error.Forbidden ->
                Publication.OpenError.Forbidden(this)
            is AssetRetriever.Error.InvalidAsset ->
                Publication.OpenError.InvalidAsset(this)
            is AssetRetriever.Error.NotFound ->
                Publication.OpenError.NotFound(this)
            is AssetRetriever.Error.OutOfMemory ->
                Publication.OpenError.OutOfMemory(this)
            is AssetRetriever.Error.SchemeNotSupported ->
                Publication.OpenError.UnsupportedAsset(this)
            is AssetRetriever.Error.Unavailable ->
                Publication.OpenError.Unavailable(this)
            is AssetRetriever.Error.Unknown ->
                Publication.OpenError.Unknown(this)
        }
}
