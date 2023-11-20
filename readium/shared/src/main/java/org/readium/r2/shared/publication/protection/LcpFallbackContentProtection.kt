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
import org.readium.r2.shared.util.MessageError
import org.readium.r2.shared.util.Try
import org.readium.r2.shared.util.Url
import org.readium.r2.shared.util.asset.Asset
import org.readium.r2.shared.util.data.DecoderError
import org.readium.r2.shared.util.data.ReadError
import org.readium.r2.shared.util.data.readAsJson
import org.readium.r2.shared.util.data.readAsRwpm
import org.readium.r2.shared.util.data.readAsXml
import org.readium.r2.shared.util.getOrElse
import org.readium.r2.shared.util.mediatype.MediaType
import org.readium.r2.shared.util.resource.ResourceContainer

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
            is Asset.Container -> isLcpProtected(
                asset.container,
                asset.mediaType
            )
            is Asset.Resource ->
                Try.success(
                    asset.mediaType.matches(MediaType.LCP_LICENSE_DOCUMENT)
                )
        }

    override suspend fun open(
        asset: Asset,
        credentials: String?,
        allowUserInteraction: Boolean
    ): Try<ContentProtection.Asset, ContentProtection.Error> {
        if (asset !is Asset.Container) {
            return Try.failure(
                ContentProtection.Error.UnsupportedAsset(
                    MessageError("A container asset was expected.")
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

    private suspend fun isLcpProtected(container: ResourceContainer, mediaType: MediaType): Try<Boolean, ReadError> {
        val isReadiumWebpub = mediaType.matches(MediaType.READIUM_WEBPUB) ||
            mediaType.matches(MediaType.LCP_PROTECTED_PDF) ||
            mediaType.matches(MediaType.LCP_PROTECTED_AUDIOBOOK)

        val isEpub = mediaType.matches(MediaType.EPUB)

        if (!isReadiumWebpub && !isEpub) {
            return Try.success(false)
        }

        container.get(Url("license.lcpl")!!)
            ?.readAsJson()
            ?.getOrElse {
                when (it) {
                    is DecoderError.Read ->
                        Try.failure(it.cause.cause)
                    is DecoderError.Decoding ->
                        return Try.success(false)
                }
            }

        return when {
            isReadiumWebpub -> hasLcpSchemeInManifest(container)
            else -> hasLcpSchemeInEncryptionXml(container) // isEpub
        }
    }

    private suspend fun hasLcpSchemeInManifest(container: ResourceContainer): Try<Boolean, ReadError> {
        val manifest = container.get(Url("manifest.json")!!)
            ?.readAsRwpm()
            ?.getOrElse {
                when (it) {
                    is DecoderError.Read ->
                        return Try.failure(ReadError.Decoding(it))
                    is DecoderError.Decoding ->
                        return Try.success(false)
                }
            }
            ?: return Try.success(false)

        val manifestHasLcpScheme = manifest
            .readingOrder
            .any { it.properties.encryption?.scheme == "http://readium.org/2014/01/lcp" }

        return Try.success(manifestHasLcpScheme)
    }

    private suspend fun hasLcpSchemeInEncryptionXml(container: ResourceContainer): Try<Boolean, ReadError> {
        val encryptionXml = container
            .get(Url("META-INF/encryption.xml")!!)
            ?.readAsXml()
            ?.getOrElse {
                when (it) {
                    is DecoderError.Read ->
                        return Try.failure(ReadError.Decoding(it.cause.cause))
                    is DecoderError.Decoding ->
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
