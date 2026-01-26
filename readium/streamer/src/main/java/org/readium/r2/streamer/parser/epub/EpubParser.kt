/*
 * Copyright 2022 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

@file:OptIn(InternalReadiumApi::class)

package org.readium.r2.streamer.parser.epub

import org.readium.r2.shared.ExperimentalReadiumApi
import org.readium.r2.shared.InternalReadiumApi
import org.readium.r2.shared.publication.Link
import org.readium.r2.shared.publication.Publication
import org.readium.r2.shared.publication.encryption.Encryption
import org.readium.r2.shared.publication.epub.EpubEncryptionParser
import org.readium.r2.shared.publication.epub.MediaOverlaysService
import org.readium.r2.shared.publication.services.content.DefaultContentService
import org.readium.r2.shared.publication.services.content.iterators.HtmlResourceContentIterator
import org.readium.r2.shared.publication.services.search.StringSearchService
import org.readium.r2.shared.util.DebugError
import org.readium.r2.shared.util.Try
import org.readium.r2.shared.util.Url
import org.readium.r2.shared.util.asset.Asset
import org.readium.r2.shared.util.asset.ContainerAsset
import org.readium.r2.shared.util.data.Container
import org.readium.r2.shared.util.data.DecodeError
import org.readium.r2.shared.util.data.ReadError
import org.readium.r2.shared.util.data.Readable
import org.readium.r2.shared.util.data.decodeXml
import org.readium.r2.shared.util.data.readDecodeOrElse
import org.readium.r2.shared.util.data.readDecodeOrNull
import org.readium.r2.shared.util.format.Specification
import org.readium.r2.shared.util.fromEpubHref
import org.readium.r2.shared.util.getOrElse
import org.readium.r2.shared.util.logging.WarningLogger
import org.readium.r2.shared.util.mediatype.MediaType
import org.readium.r2.shared.util.resource.Resource
import org.readium.r2.shared.util.resource.TransformingContainer
import org.readium.r2.shared.util.use
import org.readium.r2.shared.util.xml.ElementNode
import org.readium.r2.streamer.parser.PublicationParser

/**
 * Parses a Publication from an EPUB publication.
 *
 * @param reflowablePositionsStrategy Strategy used to calculate the number of positions in a
 *        reflowable resource.
 */
@OptIn(ExperimentalReadiumApi::class)
public class EpubParser(
    private val reflowablePositionsStrategy: EpubPositionsService.ReflowableStrategy = EpubPositionsService.ReflowableStrategy.recommended,
) : PublicationParser {

    override suspend fun parse(
        asset: Asset,
        warnings: WarningLogger?,
    ): Try<Publication.Builder, PublicationParser.ParseError> {
        if (asset !is ContainerAsset || !asset.format.conformsTo(Specification.Epub)) {
            return Try.failure(PublicationParser.ParseError.FormatNotSupported())
        }

        val opfPath = getRootFilePath(asset.container)
            .getOrElse { return Try.failure(it) }
        val opfResource = asset.container[opfPath]
            ?: return Try.failure(
                PublicationParser.ParseError.Reading(
                    ReadError.Decoding(
                        DebugError("Missing OPF file.")
                    )
                )
            )
        val opfXmlDocument = opfResource.use { resource ->
            resource.readDecodeOrElse(
                decode = { it.decodeXml() },
                recover = { return Try.failure(PublicationParser.ParseError.Reading(it)) }
            )
        }
        val packageDocument = PackageDocument.parse(opfXmlDocument, opfPath)
            ?: return Try.failure(
                PublicationParser.ParseError.Reading(
                    ReadError.Decoding(
                        DebugError("Invalid OPF file.")
                    )
                )
            )

        val encryptionData = parseEncryptionData(asset.container)

        val (manifest, mediaOverlays) = ManifestAdapter(
            packageDocument = packageDocument,
            navigationData = parseNavigationData(packageDocument, asset.container),
            encryptionData = encryptionData,
            displayOptions = parseDisplayOptions(asset.container)
        ).adapt()

        val smils = mediaOverlays.map { it.url() }

        var container = asset.container
        manifest.metadata.identifier?.let { id ->
            val deobfuscator = EpubDeobfuscator(id, encryptionData)
            container = TransformingContainer(container, deobfuscator::transform)
        }

        val builder = Publication.Builder(
            manifest = manifest,
            container = container,
            servicesBuilder = Publication.ServicesBuilder(
                positions = EpubPositionsService.createFactory(reflowablePositionsStrategy),
                search = StringSearchService.createDefaultFactory(),
                content = DefaultContentService.createFactory(
                    resourceContentIteratorFactories = listOf(
                        HtmlResourceContentIterator.Factory()
                    )
                )
            ).also {
                if (smils.isNotEmpty()) {
                    it[MediaOverlaysService::class] = SmilBasedMediaOverlaysService.createFactory(smils)
                }
            }
        )

        return Try.success(builder)
    }

    private suspend fun getRootFilePath(container: Container<Resource>): Try<Url, PublicationParser.ParseError> {
        val containerXmlUrl = Url("META-INF/container.xml")!!

        val containerXmlResource = container[containerXmlUrl]
            ?: return Try.failure(
                PublicationParser.ParseError.Reading(
                    ReadError.Decoding("container.xml not found.")
                )
            )

        return containerXmlResource
            .readDecodeOrElse(
                decode = { it.decodeXml() },
                recover = { return Try.failure(PublicationParser.ParseError.Reading(it)) }
            )
            .getFirst("rootfiles", Namespaces.OPC)
            ?.getFirst("rootfile", Namespaces.OPC)
            ?.getAttr("full-path")
            ?.let { Url.fromEpubHref(it) }
            ?.let { Try.success(it) }
            ?: Try.failure(
                PublicationParser.ParseError.Reading(
                    ReadError.Decoding("Cannot successfully parse OPF.")
                )
            )
    }

    private suspend fun parseEncryptionData(container: Container<Resource>): Map<Url, Encryption> =
        container.readDecodeXmlOrNull(path = "META-INF/encryption.xml")
            ?.let { EpubEncryptionParser.parse(it) }
            ?: emptyMap()

    private suspend fun parseNavigationData(
        packageDocument: PackageDocument,
        container: Container<Resource>,
    ): Map<String, List<Link>> =
        parseNavigationDocument(packageDocument, container)
            ?: parseNcx(packageDocument, container)
            ?: emptyMap()

    private suspend fun parseNavigationDocument(
        packageDocument: PackageDocument,
        container: Container<Resource>,
    ): Map<String, List<Link>>? =
        packageDocument.manifest
            .firstOrNull { it.properties.contains(Vocabularies.ITEM + "nav") }
            ?.let { navItem ->
                container.readDecodeXmlOrNull(navItem.href)
                    ?.let { NavigationDocumentParser.parse(it, navItem.href) }
            }
            ?.takeUnless { it.isEmpty() }

    private suspend fun parseNcx(
        packageDocument: PackageDocument,
        container: Container<Resource>,
    ): Map<String, List<Link>>? {
        val ncxItem =
            if (packageDocument.spine.toc != null) {
                packageDocument.manifest.firstOrNull { it.id == packageDocument.spine.toc }
            } else {
                packageDocument.manifest.firstOrNull { MediaType.NCX.contains(it.mediaType) }
            }

        return ncxItem
            ?.let { item ->
                container.readDecodeXmlOrNull(item.href)
                    ?.let { NcxParser.parse(it, item.href) }
            }
            ?.takeUnless { it.isEmpty() }
    }

    private suspend fun parseDisplayOptions(container: Container<Resource>): Map<String, String> {
        val displayOptionsXml =
            container.readDecodeXmlOrNull("META-INF/com.apple.ibooks.display-options.xml")
                ?: container.readDecodeXmlOrNull("META-INF/com.kobobooks.display-options.xml")

        return displayOptionsXml?.getFirst("platform", "")
            ?.get("option", "")
            ?.mapNotNull { element ->
                val optName = element.getAttr("name")
                val optVal = element.text
                if (optName != null && optVal != null) Pair(optName, optVal) else null
            }
            ?.toMap().orEmpty()
    }

    public suspend inline fun <R> Readable.readDecodeOrElse(
        url: Url,
        decode: (value: ByteArray) -> Try<R, DecodeError>,
        recover: (ReadError) -> R,
    ): R =
        readDecodeOrElse(decode, recover) {
            recover(
                ReadError.Decoding(
                    DebugError("Couldn't decode resource at $url", it.cause)
                )
            )
        }

    private suspend inline fun Container<Readable>.readDecodeXmlOrNull(
        path: String,
    ): ElementNode? =
        Url.fromDecodedPath(path)?.let { url -> readDecodeXmlOrNull(url) }

    /** Returns the resource data as an XML Document at the given [url], or null. */
    private suspend inline fun Container<Readable>.readDecodeXmlOrNull(
        url: Url,
    ): ElementNode? =
        readDecodeOrNull(url) { it.decodeXml() }
}
