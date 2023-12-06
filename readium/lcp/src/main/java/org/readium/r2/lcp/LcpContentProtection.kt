/*
 * Copyright 2020 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.lcp

import org.readium.r2.lcp.auth.LcpPassphraseAuthentication
import org.readium.r2.lcp.license.model.LicenseDocument
import org.readium.r2.shared.publication.encryption.encryption
import org.readium.r2.shared.publication.flatten
import org.readium.r2.shared.publication.protection.ContentProtection
import org.readium.r2.shared.publication.services.contentProtectionServiceFactory
import org.readium.r2.shared.util.AbsoluteUrl
import org.readium.r2.shared.util.DebugError
import org.readium.r2.shared.util.ThrowableError
import org.readium.r2.shared.util.Try
import org.readium.r2.shared.util.asset.Asset
import org.readium.r2.shared.util.asset.AssetRetriever
import org.readium.r2.shared.util.asset.ContainerAsset
import org.readium.r2.shared.util.asset.ResourceAsset
import org.readium.r2.shared.util.data.ReadError
import org.readium.r2.shared.util.flatMap
import org.readium.r2.shared.util.getOrElse
import org.readium.r2.shared.util.resource.TransformingContainer

internal class LcpContentProtection(
    private val lcpService: LcpService,
    private val authentication: LcpAuthenticating,
    private val assetRetriever: AssetRetriever
) : ContentProtection {

    override val scheme: ContentProtection.Scheme =
        ContentProtection.Scheme.Lcp

    override suspend fun supports(
        asset: Asset
    ): Try<Boolean, Nothing> =
        Try.success(lcpService.isLcpProtected(asset))

    override suspend fun open(
        asset: Asset,
        credentials: String?,
        allowUserInteraction: Boolean
    ): Try<ContentProtection.Asset, ContentProtection.OpenError> {
        return when (asset) {
            is ContainerAsset -> openPublication(asset, credentials, allowUserInteraction)
            is ResourceAsset -> openLicense(asset, credentials, allowUserInteraction)
        }
    }

    private suspend fun openPublication(
        asset: ContainerAsset,
        credentials: String?,
        allowUserInteraction: Boolean
    ): Try<ContentProtection.Asset, ContentProtection.OpenError> {
        val license = retrieveLicense(asset, credentials, allowUserInteraction)
        return createResultAsset(asset, license)
    }

    private suspend fun retrieveLicense(
        asset: Asset,
        credentials: String?,
        allowUserInteraction: Boolean
    ): Try<LcpLicense, LcpError> {
        val authentication = credentials
            ?.let { LcpPassphraseAuthentication(it, fallback = this.authentication) }
            ?: this.authentication

        return lcpService.retrieveLicense(asset, authentication, allowUserInteraction)
    }

    private fun createResultAsset(
        asset: ContainerAsset,
        license: Try<LcpLicense, LcpError>
    ): Try<ContentProtection.Asset, ContentProtection.OpenError> {
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
        licenseAsset: ResourceAsset,
        credentials: String?,
        allowUserInteraction: Boolean
    ): Try<ContentProtection.Asset, ContentProtection.OpenError> {
        val license = retrieveLicense(licenseAsset, credentials, allowUserInteraction)

        val licenseDoc = license.getOrNull()?.license
            ?: licenseAsset.resource.read()
                .map {
                    try {
                        LicenseDocument(it)
                    } catch (e: Exception) {
                        return Try.failure(
                            ContentProtection.OpenError.Reading(
                                ReadError.Decoding(
                                    DebugError(
                                        "Failed to read the LCP license document",
                                        cause = ThrowableError(e)
                                    )
                                )
                            )
                        )
                    }
                }
                .getOrElse {
                    return Try.failure(
                        ContentProtection.OpenError.Reading(it)
                    )
                }

        val link = licenseDoc.publicationLink
        val url = (link.url() as? AbsoluteUrl)
            ?: return Try.failure(
                ContentProtection.OpenError.Reading(
                    ReadError.Decoding(
                        DebugError(
                            "The LCP license document does not contain a valid link to the publication"
                        )
                    )
                )
            )

        val asset =
            if (link.mediaType != null) {
                assetRetriever.retrieve(
                    url,
                    mediaType = link.mediaType
                )
                    .map { it as ContainerAsset }
                    .mapFailure { it.wrap() }
            } else {
                assetRetriever.retrieve(url)
                    .mapFailure { it.wrap() }
                    .flatMap {
                        if (it is ContainerAsset) {
                            Try.success((it))
                        } else {
                            Try.failure(
                                ContentProtection.OpenError.AssetNotSupported(
                                    DebugError(
                                        "LCP license points to an unsupported publication."
                                    )
                                )
                            )
                        }
                    }
            }

        return asset.flatMap { createResultAsset(it, license) }
    }

    private fun AssetRetriever.RetrieveError.wrap(): ContentProtection.OpenError =
        when (this) {
            is AssetRetriever.RetrieveError.FormatNotSupported ->
                ContentProtection.OpenError.AssetNotSupported(this)
            is AssetRetriever.RetrieveError.Reading ->
                ContentProtection.OpenError.Reading(cause)
            is AssetRetriever.RetrieveError.SchemeNotSupported ->
                ContentProtection.OpenError.AssetNotSupported(this)
        }
}
