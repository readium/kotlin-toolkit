/*
 * Copyright 2023 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.shared.publication.protection

import org.readium.r2.shared.InternalReadiumApi
import org.readium.r2.shared.asset.Asset
import org.readium.r2.shared.error.Try
import org.readium.r2.shared.parser.xml.ElementNode
import org.readium.r2.shared.publication.Publication
import org.readium.r2.shared.publication.protection.ContentProtection.Scheme
import org.readium.r2.shared.publication.services.contentProtectionServiceFactory
import org.readium.r2.shared.resource.Container
import org.readium.r2.shared.resource.Resource
import org.readium.r2.shared.resource.readAsXml
import org.readium.r2.shared.util.mediatype.MediaType

/**
 * [ContentProtection] implementation used as a fallback by the Streamer to detect Adept DRM,
 * if it is not supported by the app.
 */
@InternalReadiumApi
public class AdeptFallbackContentProtection : ContentProtection {

    override val scheme: Scheme = Scheme.Adept

    override suspend fun supports(asset: Asset): Boolean {
        if (asset !is Asset.Container) {
            return false
        }

        return isAdept(asset.container, asset.mediaType)
    }

    override suspend fun open(
        asset: Asset,
        credentials: String?,
        allowUserInteraction: Boolean,
        sender: Any?
    ): Try<ContentProtection.Asset, Publication.OpeningException> {
        if (asset !is Asset.Container) {
            return Try.failure(
                Publication.OpeningException.UnsupportedAsset("A container asset was expected.")
            )
        }

        val protectedFile = ContentProtection.Asset(
            asset.mediaType,
            asset.container,
            onCreatePublication = {
                servicesBuilder.contentProtectionServiceFactory =
                    FallbackContentProtectionService.createFactory(scheme, "Adobe ADEPT")
            }
        )

        return Try.success(protectedFile)
    }

    private suspend fun isAdept(container: Container, mediaType: MediaType): Boolean {
        if (!mediaType.matches(MediaType.EPUB)) {
            return false
        }
        val rightsXml = container.get("/META-INF/rights.xml").readAsXmlOrNull()
        val encryptionXml = container.get("/META-INF/encryption.xml").readAsXmlOrNull()

        return encryptionXml != null && (
            rightsXml?.namespace == "http://ns.adobe.com/adept" ||
                encryptionXml
                    .get("EncryptedData", EpubEncryption.ENC)
                    .flatMap { it.get("KeyInfo", EpubEncryption.SIG) }
                    .flatMap { it.get("resource", "http://ns.adobe.com/adept") }
                    .isNotEmpty()
            )
    }
}

private suspend inline fun Resource.readAsXmlOrNull(): ElementNode? =
    readAsXml().getOrNull()
