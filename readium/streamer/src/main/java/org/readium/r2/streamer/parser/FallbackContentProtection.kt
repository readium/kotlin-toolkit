/*
 * Copyright 2021 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.streamer.parser

import org.json.JSONObject
import org.readium.r2.shared.UserException
import org.readium.r2.shared.asset.Asset
import org.readium.r2.shared.fetcher.ContainerFetcher
import org.readium.r2.shared.parser.xml.ElementNode
import org.readium.r2.shared.publication.ContentProtection
import org.readium.r2.shared.publication.ContentProtection.Scheme
import org.readium.r2.shared.publication.Publication
import org.readium.r2.shared.publication.asset.PublicationAsset
import org.readium.r2.shared.publication.services.ContentProtectionService
import org.readium.r2.shared.publication.services.contentProtectionServiceFactory
import org.readium.r2.shared.resource.Container
import org.readium.r2.shared.resource.Resource
import org.readium.r2.shared.util.Try
import org.readium.r2.shared.util.mediatype.MediaType
import org.readium.r2.shared.util.mediatype.MediaTypeRetriever
import org.readium.r2.streamer.parser.epub.EncryptionParser
import org.readium.r2.streamer.parser.epub.Namespaces

/**
 * [ContentProtection] implementation used as a fallback by the Streamer to detect known DRM
 * schemes (e.g. LCP or ADEPT), if they are not supported by the app.
 */
internal class FallbackContentProtection(
    private val mediaTypeRetriever: MediaTypeRetriever = MediaTypeRetriever()
) : ContentProtection {

    class Service(override val scheme: Scheme?) : ContentProtectionService {

        override val isRestricted: Boolean = true
        override val credentials: String? = null
        override val rights = ContentProtectionService.UserRights.AllRestricted
        override val error: UserException = ContentProtection.Exception.SchemeNotSupported(scheme)

        companion object {

            fun createFactory(scheme: Scheme?): (Publication.Service.Context) -> Service =
                { Service(scheme) }
        }
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

        val scheme: Scheme = sniffScheme(asset.container, asset.mediaType)
            ?: return null

        val protectedFile = ContentProtection.Asset(
            asset = PublicationAsset(
                asset.name,
                asset.mediaType,
                ContainerFetcher(asset.container, mediaTypeRetriever)
            ),
            onCreatePublication = {
                servicesBuilder.contentProtectionServiceFactory = Service.createFactory(scheme)
            }
        )

        return Try.success(protectedFile)
    }

    internal suspend fun sniffScheme(container: Container, mediaType: MediaType): Scheme? =
        when {
            container.entry("/license.lcpl").readAsJsonOrNull() != null ->
                Scheme.Lcp

            mediaType.matches(MediaType.EPUB) -> {
                val rightsXml = container.entry("/META-INF/rights.xml").readAsXmlOrNull()
                val encryptionXml = container.entry("/META-INF/encryption.xml").readAsXmlOrNull()
                val encryption = encryptionXml?.let { EncryptionParser.parse(it) }

                when {
                    (
                        container.entry("/META-INF/license.lcpl").readAsJsonOrNull() != null ||
                            encryption?.any { it.value.scheme == "http://readium.org/2014/01/lcp" } == true
                        ) -> Scheme.Lcp

                    (
                        encryptionXml != null && (
                            rightsXml?.namespace == "http://ns.adobe.com/adept" ||
                                encryptionXml
                                    .get("EncryptedData", Namespaces.ENC)
                                    .flatMap { it.get("KeyInfo", Namespaces.SIG) }
                                    .flatMap { it.get("resource", "http://ns.adobe.com/adept") }
                                    .isNotEmpty()
                            )
                        ) -> Scheme.Adept

                    // A file with only obfuscated fonts might still have an `encryption.xml` file.
                    // To make sure that we don't lock a readable publication, we ignore unknown
                    // encryption.xml schemes.
                    else -> null
                }
            }

            else -> null
        }
}

private suspend inline fun Resource.readAsJsonOrNull(): JSONObject? =
    readAsJson().getOrNull()

private suspend inline fun Resource.readAsXmlOrNull(): ElementNode? =
    readAsXml().getOrNull()
