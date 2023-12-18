/*
 * Copyright 2020 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.lcp

import org.readium.r2.lcp.auth.LcpPassphraseAuthentication
import org.readium.r2.lcp.license.model.LicenseDocument
import org.readium.r2.shared.publication.encryption.Encryption
import org.readium.r2.shared.publication.encryption.encryption
import org.readium.r2.shared.publication.protection.ContentProtection
import org.readium.r2.shared.publication.services.contentProtectionServiceFactory
import org.readium.r2.shared.util.AbsoluteUrl
import org.readium.r2.shared.util.DebugError
import org.readium.r2.shared.util.ThrowableError
import org.readium.r2.shared.util.Try
import org.readium.r2.shared.util.Url
import org.readium.r2.shared.util.asset.Asset
import org.readium.r2.shared.util.asset.AssetOpener
import org.readium.r2.shared.util.asset.ContainerAsset
import org.readium.r2.shared.util.asset.ResourceAsset
import org.readium.r2.shared.util.data.Container
import org.readium.r2.shared.util.data.ReadError
import org.readium.r2.shared.util.data.decodeRwpm
import org.readium.r2.shared.util.data.decodeXml
import org.readium.r2.shared.util.data.readDecodeOrElse
import org.readium.r2.shared.util.flatMap
import org.readium.r2.shared.util.format.Format
import org.readium.r2.shared.util.format.Trait
import org.readium.r2.shared.util.getOrElse
import org.readium.r2.shared.util.resource.Resource
import org.readium.r2.shared.util.resource.TransformingContainer

internal class LcpContentProtection(
    private val lcpService: LcpService,
    private val authentication: LcpAuthenticating,
    private val assetOpener: AssetOpener
) : ContentProtection {

    override suspend fun open(
        asset: Asset,
        credentials: String?,
        allowUserInteraction: Boolean
    ): Try<ContentProtection.OpenResult, ContentProtection.OpenError> {
        if (
            !asset.format.conformsTo(Trait.LCP_PROTECTED) &&
            !asset.format.conformsTo(Format.LCP_LICENSE_DOCUMENT)
        ) {
            return Try.failure(ContentProtection.OpenError.AssetNotSupported())
        }

        return when (asset) {
            is ContainerAsset -> openPublication(asset, credentials, allowUserInteraction)
            is ResourceAsset -> openLicense(asset, credentials, allowUserInteraction)
        }
    }

    private suspend fun openPublication(
        asset: ContainerAsset,
        credentials: String?,
        allowUserInteraction: Boolean
    ): Try<ContentProtection.OpenResult, ContentProtection.OpenError> {
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

    private suspend fun createResultAsset(
        asset: ContainerAsset,
        license: Try<LcpLicense, LcpError>
    ): Try<ContentProtection.OpenResult, ContentProtection.OpenError> {
        val serviceFactory = LcpContentProtectionService
            .createFactory(license.getOrNull(), license.failureOrNull())

        val encryptionData =
            when {
                asset.format.conformsTo(Trait.EPUB) -> parseEncryptionDataEpub(asset.container)
                else -> parseEncryptionDataRpf(asset.container)
            }
                .getOrElse { return Try.failure(ContentProtection.OpenError.Reading(it)) }

        val decryptor = LcpDecryptor(license.getOrNull(), encryptionData)

        val container = TransformingContainer(asset.container, decryptor::transform)

        val protectedFile = ContentProtection.OpenResult(
            asset = ContainerAsset(
                format = asset.format,
                container = container
            ),
            onCreatePublication = {
                servicesBuilder.contentProtectionServiceFactory = serviceFactory
            }
        )

        return Try.success(protectedFile)
    }

    private suspend fun parseEncryptionDataEpub(container: Container<Resource>): Try<Map<Url, Encryption>, ReadError> {
        val encryptionResource = container[Url("META-INF/encryption.xml")!!]
            ?: return Try.failure(ReadError.Decoding("Missing encryption.xml"))

        val encryptionDocument = encryptionResource
            .readDecodeOrElse(
                decode = { it.decodeXml() },
                recover = { return Try.failure(it) }
            )

        return Try.success(EpubEncryptionParser.parse(encryptionDocument))
    }

    private suspend fun parseEncryptionDataRpf(container: Container<Resource>): Try<Map<Url, Encryption>, ReadError> {
        val manifestResource = container[Url("manifest.json")!!]
            ?: return Try.failure(ReadError.Decoding("Missing manifest"))

        val manifest = manifestResource
            .readDecodeOrElse(
                decode = { it.decodeRwpm() },
                recover = { return Try.failure(it) }
            )

        val encryptionData = manifest
            .let { (it.readingOrder + it.resources) }
            .mapNotNull { link -> link.properties.encryption?.let { link.url() to it } }
            .toMap()

        return Try.success(encryptionData)
    }

    private suspend fun openLicense(
        licenseAsset: ResourceAsset,
        credentials: String?,
        allowUserInteraction: Boolean
    ): Try<ContentProtection.OpenResult, ContentProtection.OpenError> {
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
                assetOpener.open(
                    url,
                    mediaType = link.mediaType
                )
                    .map { it as ContainerAsset }
                    .mapFailure { it.wrap() }
            } else {
                assetOpener.open(url)
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

    private fun AssetOpener.OpenError.wrap(): ContentProtection.OpenError =
        when (this) {
            is AssetOpener.OpenError.FormatNotSupported ->
                ContentProtection.OpenError.AssetNotSupported(this)
            is AssetOpener.OpenError.Reading ->
                ContentProtection.OpenError.Reading(cause)
            is AssetOpener.OpenError.SchemeNotSupported ->
                ContentProtection.OpenError.AssetNotSupported(this)
        }
}
