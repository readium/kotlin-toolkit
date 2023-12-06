/*
 * Copyright 2023 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.shared.publication.protection

import org.readium.r2.shared.InternalReadiumApi
import org.readium.r2.shared.publication.protection.ContentProtection.Scheme
import org.readium.r2.shared.publication.services.contentProtectionServiceFactory
import org.readium.r2.shared.util.DebugError
import org.readium.r2.shared.util.Try
import org.readium.r2.shared.util.Url
import org.readium.r2.shared.util.asset.Asset
import org.readium.r2.shared.util.asset.ContainerAsset
import org.readium.r2.shared.util.data.DecodeError
import org.readium.r2.shared.util.data.ReadError
import org.readium.r2.shared.util.data.readAsXml
import org.readium.r2.shared.util.getOrElse
import org.readium.r2.shared.util.mediatype.MediaType

/**
 * [ContentProtection] implementation used as a fallback by the Streamer to detect Adept DRM,
 * if it is not supported by the app.
 */
@InternalReadiumApi
public class AdeptFallbackContentProtection : ContentProtection {

    override val scheme: Scheme = Scheme.Adept

    override suspend fun supports(asset: Asset): Try<Boolean, ReadError> {
        if (asset !is ContainerAsset) {
            return Try.success(false)
        }

        return isAdept(asset)
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
                    FallbackContentProtectionService.createFactory(scheme, "Adobe ADEPT")
            }
        )

        return Try.success(protectedFile)
    }

    private suspend fun isAdept(asset: ContainerAsset): Try<Boolean, ReadError> {
        if (!asset.mediaType.matches(MediaType.EPUB)) {
            return Try.success(false)
        }

        asset.container.get(Url("META-INF/encryption.xml")!!)
            ?.readAsXml()
            ?.getOrElse {
                when (it) {
                    is DecodeError.Decoding ->
                        return Try.success(false)
                    is DecodeError.Reading ->
                        return Try.failure(it.cause)
                }
            }?.get("EncryptedData", EpubEncryption.ENC)
            ?.flatMap { it.get("KeyInfo", EpubEncryption.SIG) }
            ?.flatMap { it.get("resource", "http://ns.adobe.com/adept") }
            ?.takeIf { it.isNotEmpty() }
            ?.let { return Try.success(true) }

        return asset.container.get(Url("META-INF/rights.xml")!!)
            ?.readAsXml()
            ?.getOrElse {
                when (it) {
                    is DecodeError.Decoding ->
                        return Try.success(false)
                    is DecodeError.Reading ->
                        return Try.failure(it.cause)
                }
            }?.takeIf { it.namespace == "http://ns.adobe.com/adept" }
            ?.let { Try.success(true) }
            ?: Try.success(false)
    }
}
