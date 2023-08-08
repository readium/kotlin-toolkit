/*
 * Copyright 2022 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.streamer.parser.epub

import org.readium.r2.shared.ExperimentalReadiumApi
import org.readium.r2.shared.error.Try
import org.readium.r2.shared.error.getOrElse
import org.readium.r2.shared.extensions.addPrefix
import org.readium.r2.shared.publication.Link
import org.readium.r2.shared.publication.Publication
import org.readium.r2.shared.publication.encryption.Encryption
import org.readium.r2.shared.publication.encryption.encryption
import org.readium.r2.shared.publication.services.content.DefaultContentService
import org.readium.r2.shared.publication.services.content.iterators.HtmlResourceContentIterator
import org.readium.r2.shared.publication.services.search.StringSearchService
import org.readium.r2.shared.resource.Container
import org.readium.r2.shared.resource.TransformingContainer
import org.readium.r2.shared.resource.readAsXml
import org.readium.r2.shared.util.Href
import org.readium.r2.shared.util.logging.WarningLogger
import org.readium.r2.shared.util.mediatype.MediaType
import org.readium.r2.shared.util.use
import org.readium.r2.streamer.extensions.readAsXmlOrNull
import org.readium.r2.streamer.parser.PublicationParser

/**
 * Parses a Publication from an EPUB publication.
 *
 * @param reflowablePositionsStrategy Strategy used to calculate the number of positions in a
 *        reflowable resource.
 */
@OptIn(ExperimentalReadiumApi::class)
public class EpubParser(
    private val reflowablePositionsStrategy: EpubPositionsService.ReflowableStrategy = EpubPositionsService.ReflowableStrategy.recommended
) : PublicationParser {

    override suspend fun parse(
        asset: PublicationParser.Asset,
        warnings: WarningLogger?
    ): Try<Publication.Builder, PublicationParser.Error> {
        if (asset.mediaType != MediaType.EPUB)
            return Try.failure(PublicationParser.Error.FormatNotSupported())

        val opfPath = getRootFilePath(asset.container)
            .getOrElse { return Try.failure(it) }
            .addPrefix("/")
        val opfXmlDocument = asset.container.get(opfPath).readAsXml()
            .getOrElse { return Try.failure(PublicationParser.Error.IO(it)) }
        val packageDocument = PackageDocument.parse(opfXmlDocument, opfPath)
            ?: return Try.failure(PublicationParser.Error.ParsingFailed("Invalid OPF file."))

        val manifest = ManifestAdapter(
            packageDocument = packageDocument,
            navigationData = parseNavigationData(packageDocument, asset.container),
            encryptionData = parseEncryptionData(asset.container),
            displayOptions = parseDisplayOptions(asset.container)
        ).adapt()

        var container = asset.container
        manifest.metadata.identifier?.let { id ->
            val deobfuscator = EpubDeobfuscator(id) { url ->
                manifest.linkWithHref(url.toString())
                    ?.properties?.encryption
            }

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
                ),
            )
        )

        return Try.success(builder)
    }

    private suspend fun getRootFilePath(container: Container): Try<String, PublicationParser.Error> =
        container.get("/META-INF/container.xml")
            .use { it.readAsXml() }
            .getOrElse { return Try.failure(PublicationParser.Error.IO(it)) }
            .getFirst("rootfiles", Namespaces.OPC)
            ?.getFirst("rootfile", Namespaces.OPC)
            ?.getAttr("full-path")
            ?.let { Try.success(it) }
            ?: Try.failure(PublicationParser.Error.ParsingFailed("Cannot successfully parse OPF."))

    private suspend fun parseEncryptionData(container: Container): Map<String, Encryption> =
        container.readAsXmlOrNull("/META-INF/encryption.xml")
            ?.let { EncryptionParser.parse(it) }
            ?: emptyMap()

    private suspend fun parseNavigationData(packageDocument: PackageDocument, container: Container): Map<String, List<Link>> =
        parseNavigationDocument(packageDocument, container)
            ?: parseNcx(packageDocument, container)
            ?: emptyMap()

    private suspend fun parseNavigationDocument(
        packageDocument: PackageDocument,
        container: Container
    ): Map<String, List<Link>>? =
        packageDocument.manifest
            .firstOrNull { it.properties.contains(Vocabularies.ITEM + "nav") }
            ?.let { navItem ->
                val navPath = Href(navItem.href, baseHref = packageDocument.path).value
                container.readAsXmlOrNull(navPath)
                    ?.let { NavigationDocumentParser.parse(it, navPath) }
            }
            ?.takeUnless { it.isEmpty() }

    private suspend fun parseNcx(packageDocument: PackageDocument, container: Container): Map<String, List<Link>>? {
        val ncxItem =
            if (packageDocument.spine.toc != null) {
                packageDocument.manifest.firstOrNull { it.id == packageDocument.spine.toc }
            } else {
                packageDocument.manifest.firstOrNull { MediaType.NCX.contains(it.mediaType) }
            }

        return ncxItem
            ?.let {
                val ncxPath = Href(ncxItem.href, baseHref = packageDocument.path).value
                container.readAsXmlOrNull(ncxPath)?.let { NcxParser.parse(it, ncxPath) }
            }
            ?.takeUnless { it.isEmpty() }
    }

    private suspend fun parseDisplayOptions(container: Container): Map<String, String> {
        val displayOptionsXml =
            container.readAsXmlOrNull("/META-INF/com.apple.ibooks.display-options.xml")
                ?: container.readAsXmlOrNull("/META-INF/com.kobobooks.display-options.xml")

        return displayOptionsXml?.getFirst("platform", "")
            ?.get("option", "")
            ?.mapNotNull { element ->
                val optName = element.getAttr("name")
                val optVal = element.text
                if (optName != null && optVal != null) Pair(optName, optVal) else null
            }
            ?.toMap().orEmpty()
    }
}
