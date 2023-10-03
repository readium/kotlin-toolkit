/*
 * Copyright 2023 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.shared.publication.protection

import org.readium.r2.shared.InternalReadiumApi
import org.readium.r2.shared.publication.Publication
import org.readium.r2.shared.publication.protection.ContentProtection.Scheme
import org.readium.r2.shared.publication.services.contentProtectionServiceFactory
import org.readium.r2.shared.util.Try
import org.readium.r2.shared.util.Url
import org.readium.r2.shared.util.asset.Asset
import org.readium.r2.shared.util.mediatype.MediaType
import org.readium.r2.shared.util.resource.Resource
import org.readium.r2.shared.util.resource.readAsXml
import org.readium.r2.shared.util.xml.ElementNode

/**
 * [ContentProtection] implementation used as a fallback by the Streamer to detect Adept DRM,
 * if it is not supported by the app.
 */
@InternalReadiumApi
public class AdeptFallbackContentProtection : ContentProtection {

    override val scheme: Scheme = Scheme.Adept

    override suspend fun supports(asset: org.readium.r2.shared.util.asset.Asset): Boolean {
        if (asset !is org.readium.r2.shared.util.asset.Asset.Container) {
            return false
        }

        return isAdept(asset)
    }

    override suspend fun open(
        asset: org.readium.r2.shared.util.asset.Asset,
        credentials: String?,
        allowUserInteraction: Boolean
    ): Try<ContentProtection.Asset, Publication.OpenError> {
        if (asset !is org.readium.r2.shared.util.asset.Asset.Container) {
            return Try.failure(
                Publication.OpenError.UnsupportedAsset("A container asset was expected.")
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

    private suspend fun isAdept(asset: org.readium.r2.shared.util.asset.Asset.Container): Boolean {
        if (!asset.mediaType.matches(MediaType.EPUB)) {
            return false
        }

        val rightsXml = asset.container.get(Url("META-INF/rights.xml")!!)
            .readAsXmlOrNull()

        val encryptionXml = asset.container.get(Url("META-INF/encryption.xml")!!)
            .readAsXmlOrNull()

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
