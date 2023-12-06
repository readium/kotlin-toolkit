/*
 * Copyright 2021 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.shared.publication.protection

import org.readium.r2.shared.InternalReadiumApi
import org.readium.r2.shared.publication.encryption.encryption
import org.readium.r2.shared.publication.protection.ContentProtection.Scheme
import org.readium.r2.shared.publication.services.contentProtectionServiceFactory
import org.readium.r2.shared.util.DebugError
import org.readium.r2.shared.util.Try
import org.readium.r2.shared.util.Url
import org.readium.r2.shared.util.asset.Asset
import org.readium.r2.shared.util.asset.ContainerAsset
import org.readium.r2.shared.util.asset.ResourceAsset
import org.readium.r2.shared.util.data.Container
import org.readium.r2.shared.util.data.DecodeError
import org.readium.r2.shared.util.data.ReadError
import org.readium.r2.shared.util.data.readAsRwpm
import org.readium.r2.shared.util.data.readAsXml
import org.readium.r2.shared.util.getOrElse
import org.readium.r2.shared.util.mediatype.MediaType
import org.readium.r2.shared.util.resource.Resource

/**
 * [ContentProtection] implementation used as a fallback by the Streamer to detect LCP DRM
 * if it is not supported by the app.
 */
@InternalReadiumApi
public class LcpFallbackContentProtection : ContentProtection {

    override val scheme: Scheme =
        Scheme.Lcp

    override suspend fun supports(asset: Asset): Try<Boolean, ReadError> =
        when (asset) {
            is ContainerAsset -> isLcpProtected(
                asset.container,
                asset.mediaType
            )
            is ResourceAsset ->
                Try.success(
                    asset.mediaType.matches(MediaType.LCP_LICENSE_DOCUMENT)
                )
        }

    override suspend fun open(
        asset: Asset,
        credentials: String?,
        allowUserInteraction: Boolean
    ): Try<ContentProtection.Asset, ContentProtection.OpenError> {
        if (asset !is ContainerAsset) {
            return Try.failure(
                ContentProtection.OpenError.AssetNotSupported(
                    DebugError("A container asset was expected.")
                )
            )
        }

        val protectedFile = ContentProtection.Asset(
            asset.mediaType,
            asset.container,
            onCreatePublication = {
                servicesBuilder.contentProtectionServiceFactory =
                    FallbackContentProtectionService.createFactory(scheme, "Readium LCP")
            }
        )

        return Try.success(protectedFile)
    }

    private suspend fun isLcpProtected(container: Container<Resource>, mediaType: MediaType): Try<Boolean, ReadError> {
        val isRpf = mediaType.isRpf
        val isEpub = mediaType.matches(MediaType.EPUB)

        if (!isRpf && !isEpub) {
            return Try.success(false)
        }

        val licenseUrl = when {
            isRpf -> Url("license.lcpl")!!
            else -> Url("META-INF/license.lcpl")!! // isEpub
        }
        container[licenseUrl]
            ?.let { return Try.success(true) }

        return when {
            isRpf -> hasLcpSchemeInManifest(container)
            else -> hasLcpSchemeInEncryptionXml(container) // isEpub
        }
    }

    private suspend fun hasLcpSchemeInManifest(container: Container<Resource>): Try<Boolean, ReadError> {
        val manifest = container[Url("manifest.json")!!]
            ?.readAsRwpm()
            ?.getOrElse {
                when (it) {
                    is DecodeError.Reading ->
                        return Try.failure(ReadError.Decoding(it))
                    is DecodeError.Decoding ->
                        return Try.success(false)
                }
            }
            ?: return Try.success(false)

        val manifestHasLcpScheme = manifest
            .readingOrder
            .any { it.properties.encryption?.scheme == "http://readium.org/2014/01/lcp" }

        return Try.success(manifestHasLcpScheme)
    }

    private suspend fun hasLcpSchemeInEncryptionXml(container: Container<Resource>): Try<Boolean, ReadError> {
        val encryptionXml = container
            .get(Url("META-INF/encryption.xml")!!)
            ?.readAsXml()
            ?.getOrElse {
                when (it) {
                    is DecodeError.Reading ->
                        return Try.failure(ReadError.Decoding(it.cause.cause))
                    is DecodeError.Decoding ->
                        return Try.failure(ReadError.Decoding(it.cause))
                }
            }
            ?: return Try.success(false)

        val hasLcpScheme = encryptionXml
            .get("EncryptedData", EpubEncryption.ENC)
            .flatMap { it.get("KeyInfo", EpubEncryption.SIG) }
            .flatMap { it.get("RetrievalMethod", EpubEncryption.SIG) }
            .any { it.getAttr("URI") == "license.lcpl#/encryption/content_key" }

        return Try.success(hasLcpScheme)
    }
}
