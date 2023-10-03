/*
 * Copyright 2021 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.shared.publication.protection

import org.json.JSONObject
import org.readium.r2.shared.InternalReadiumApi
import org.readium.r2.shared.asset.Asset
import org.readium.r2.shared.parser.xml.ElementNode
import org.readium.r2.shared.publication.Manifest
import org.readium.r2.shared.publication.Publication
import org.readium.r2.shared.publication.encryption.encryption
import org.readium.r2.shared.publication.protection.ContentProtection.Scheme
import org.readium.r2.shared.publication.services.contentProtectionServiceFactory
import org.readium.r2.shared.resource.Container
import org.readium.r2.shared.resource.Resource
import org.readium.r2.shared.resource.readAsJson
import org.readium.r2.shared.resource.readAsXml
import org.readium.r2.shared.util.Try
import org.readium.r2.shared.util.Url
import org.readium.r2.shared.util.mediatype.MediaType
import org.readium.r2.shared.util.mediatype.MediaTypeRetriever

/**
 * [ContentProtection] implementation used as a fallback by the Streamer to detect LCP DRM
 * if it is not supported by the app.
 */
@InternalReadiumApi
public class LcpFallbackContentProtection(
    private val mediaTypeRetriever: MediaTypeRetriever
) : ContentProtection {

    override val scheme: Scheme =
        Scheme.Lcp

    override suspend fun supports(asset: Asset): Boolean =
        when (asset) {
            is Asset.Container -> isLcpProtected(asset.container, asset.mediaType)
            is Asset.Resource -> asset.mediaType.matches(MediaType.LCP_LICENSE_DOCUMENT)
        }

    override suspend fun open(
        asset: Asset,
        credentials: String?,
        allowUserInteraction: Boolean
    ): Try<ContentProtection.Asset, Publication.OpenError> {
        if (asset !is Asset.Container) {
            return Try.failure(
                Publication.OpenError.UnsupportedAsset("A container asset was expected.")
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

    private suspend fun isLcpProtected(container: Container, mediaType: MediaType): Boolean {
        return when {
            mediaType.matches(MediaType.READIUM_WEBPUB) ||
                mediaType.matches(MediaType.LCP_PROTECTED_PDF) ||
                mediaType.matches(MediaType.LCP_PROTECTED_AUDIOBOOK) -> {
                if (container.get(Url("license.lcpl")!!).readAsJsonOrNull() != null) {
                    return true
                }

                val manifestAsJson = container.get(Url("manifest.json")!!).readAsJsonOrNull()
                    ?: return false

                val manifest = Manifest.fromJSON(
                    manifestAsJson,
                    mediaTypeRetriever = mediaTypeRetriever
                )
                    ?: return false

                return manifest
                    .readingOrder
                    .any { it.properties.encryption?.scheme == "http://readium.org/2014/01/lcp" }
            }
            mediaType.matches(MediaType.EPUB) -> {
                if (container.get(Url("META-INF/license.lcpl")!!).readAsJsonOrNull() != null) {
                    return true
                }

                val encryptionXml = container.get(Url("META-INF/encryption.xml")!!).readAsXmlOrNull()
                    ?: return false

                return encryptionXml
                    .get("EncryptedData", EpubEncryption.ENC)
                    .flatMap { it.get("KeyInfo", EpubEncryption.SIG) }
                    .flatMap { it.get("RetrievalMethod", EpubEncryption.SIG) }
                    .any { it.getAttr("URI") == "license.lcpl#/encryption/content_key" }
            }
            else -> false
        }
    }
}

private suspend inline fun Resource.readAsJsonOrNull(): JSONObject? =
    readAsJson().getOrNull()

private suspend inline fun Resource.readAsXmlOrNull(): ElementNode? =
    readAsXml().getOrNull()
