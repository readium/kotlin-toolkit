/*
 * Copyright 2021 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.shared.publication.protection

import org.json.JSONObject
import org.readium.r2.shared.asset.Asset
import org.readium.r2.shared.fetcher.ContainerFetcher
import org.readium.r2.shared.parser.xml.ElementNode
import org.readium.r2.shared.publication.Publication
import org.readium.r2.shared.publication.protection.ContentProtection.Scheme
import org.readium.r2.shared.publication.services.contentProtectionServiceFactory
import org.readium.r2.shared.resource.Container
import org.readium.r2.shared.resource.Resource
import org.readium.r2.shared.util.Try
import org.readium.r2.shared.util.mediatype.MediaType
import org.readium.r2.shared.util.mediatype.MediaTypeRetriever

/**
 * [ContentProtection] implementation used as a fallback by the Streamer to detect known DRM
 * schemes (e.g. LCP or ADEPT), if they are not supported by the app.
 */
class LcpFallbackContentProtection(
    private val mediaTypeRetriever: MediaTypeRetriever = MediaTypeRetriever()
) : ContentProtection {

    override val scheme: Scheme =
        Scheme.Lcp

    override suspend fun supports(asset: Asset): Boolean {
        if (asset !is Asset.Container) {
            return false
        }

        return isLcp(asset.container, asset.mediaType)
    }

    override suspend fun open(
        asset: Asset,
        credentials: String?,
        allowUserInteraction: Boolean,
        sender: Any?
    ): Try<ContentProtection.Asset, Publication.OpeningException>? {
        if (asset !is Asset.Container) {
            return null
        }

        if (!isLcp(asset.container, asset.mediaType)) {
            return null
        }

        val protectedFile = ContentProtection.Asset(
            asset.name,
            asset.mediaType,
            ContainerFetcher(asset.container, mediaTypeRetriever),
            onCreatePublication = {
                servicesBuilder.contentProtectionServiceFactory = FallbackContentProtectionService.createFactory(scheme)
            }
        )

        return Try.success(protectedFile)
    }

    private suspend fun isLcp(container: Container, mediaType: MediaType): Boolean {
        if (container.entry("/license.lcpl").readAsJsonOrNull() != null) {
            return true
        }

        if (!mediaType.matches(MediaType.EPUB)) {
            return false
        }

        val encryptionXml = container.entry("/META-INF/encryption.xml").readAsXmlOrNull()
            ?: return false

        return encryptionXml
            .get("EncryptedData", EpubEncryption.ENC)
            .flatMap { it.get("KeyInfo", EpubEncryption.SIG) }
            .flatMap { it.get("RetrievalMethod", "license.lcpl#/encryption/content_key") }
            .isNotEmpty()
    }
}

private suspend inline fun Resource.readAsJsonOrNull(): JSONObject? =
    readAsJson().getOrNull()

private suspend inline fun Resource.readAsXmlOrNull(): ElementNode? =
    readAsXml().getOrNull()
