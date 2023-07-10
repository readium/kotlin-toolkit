/*
 * Copyright 2022 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

@file:Suppress("DEPRECATION")

package org.readium.r2.streamer.parser.epub

import org.readium.r2.shared.ExperimentalReadiumApi
import org.readium.r2.shared.ReadiumCSSName
import org.readium.r2.shared.Search
import org.readium.r2.shared.drm.DRM
import org.readium.r2.shared.extensions.addPrefix
import org.readium.r2.shared.fetcher.Fetcher
import org.readium.r2.shared.fetcher.TransformingFetcher
import org.readium.r2.shared.publication.Link
import org.readium.r2.shared.publication.Publication
import org.readium.r2.shared.publication.asset.PublicationAsset
import org.readium.r2.shared.publication.encryption.Encryption
import org.readium.r2.shared.publication.services.content.DefaultContentService
import org.readium.r2.shared.publication.services.content.iterators.HtmlResourceContentIterator
import org.readium.r2.shared.publication.services.search.StringSearchService
import org.readium.r2.shared.util.Href
import org.readium.r2.shared.util.logging.WarningLogger
import org.readium.r2.shared.util.mediatype.MediaType
import org.readium.r2.streamer.container.Container
import org.readium.r2.streamer.extensions.readAsXmlOrNull
import org.readium.r2.streamer.parser.PublicationParser

@Suppress("DEPRECATION")
object EPUBConstant {

    @Deprecated("Use [MediaType.EPUB.toString()] instead", replaceWith = ReplaceWith("MediaType.EPUB.toString()"))
    val mimetype: String get() = MediaType.EPUB.toString()

    internal val ltrPreset: MutableMap<ReadiumCSSName, Boolean> = mutableMapOf(
        ReadiumCSSName.ref("hyphens") to false,
        ReadiumCSSName.ref("ligatures") to false
    )

    internal val rtlPreset: MutableMap<ReadiumCSSName, Boolean> = mutableMapOf(
        ReadiumCSSName.ref("hyphens") to false,
        ReadiumCSSName.ref("wordSpacing") to false,
        ReadiumCSSName.ref("letterSpacing") to false,
        ReadiumCSSName.ref("ligatures") to true
    )

    internal val cjkHorizontalPreset: MutableMap<ReadiumCSSName, Boolean> = mutableMapOf(
        ReadiumCSSName.ref("textAlignment") to false,
        ReadiumCSSName.ref("hyphens") to false,
        ReadiumCSSName.ref("paraIndent") to false,
        ReadiumCSSName.ref("wordSpacing") to false,
        ReadiumCSSName.ref("letterSpacing") to false
    )

    internal val cjkVerticalPreset: MutableMap<ReadiumCSSName, Boolean> = mutableMapOf(
        ReadiumCSSName.ref("scroll") to true,
        ReadiumCSSName.ref("columnCount") to false,
        ReadiumCSSName.ref("textAlignment") to false,
        ReadiumCSSName.ref("hyphens") to false,
        ReadiumCSSName.ref("paraIndent") to false,
        ReadiumCSSName.ref("wordSpacing") to false,
        ReadiumCSSName.ref("letterSpacing") to false
    )

    val forceScrollPreset: MutableMap<ReadiumCSSName, Boolean> = mutableMapOf(
        ReadiumCSSName.ref("scroll") to true
    )
}

/**
 * Parses a Publication from an EPUB publication.
 *
 * @param reflowablePositionsStrategy Strategy used to calculate the number of positions in a
 *        reflowable resource.
 */
@OptIn(ExperimentalReadiumApi::class, Search::class)
class EpubParser(
    private val reflowablePositionsStrategy: EpubPositionsService.ReflowableStrategy = EpubPositionsService.ReflowableStrategy.recommended
) : PublicationParser {

    override suspend fun parse(asset: PublicationAsset, warnings: WarningLogger?): Publication.Builder? {
        if (asset.mediaType != MediaType.EPUB)
            return null

        val opfPath = getRootFilePath(asset.fetcher).addPrefix("/")
        val opfXmlDocument = asset.fetcher.get(opfPath).readAsXml().getOrThrow()
        val packageDocument = PackageDocument.parse(opfXmlDocument, opfPath)
            ?: throw Exception("Invalid OPF file.")

        val manifest = ManifestAdapter(
            fallbackTitle = asset.name,
            packageDocument = packageDocument,
            navigationData = parseNavigationData(packageDocument, asset.fetcher),
            encryptionData = parseEncryptionData(asset.fetcher),
            displayOptions = parseDisplayOptions(asset.fetcher)
        ).adapt()

        @Suppress("NAME_SHADOWING")
        var fetcher = asset.fetcher
        manifest.metadata.identifier?.let {
            fetcher = TransformingFetcher(fetcher, EpubDeobfuscator(it)::transform)
        }

        return Publication.Builder(
            manifest = manifest,
            fetcher = fetcher,
            servicesBuilder = Publication.ServicesBuilder(
                positions = EpubPositionsService.createFactory(reflowablePositionsStrategy),
                search = StringSearchService.createDefaultFactory(),
                content = DefaultContentService.createFactory(
                    listOf(
                        HtmlResourceContentIterator.Factory()
                    )
                ),
            )
        )
    }

    private suspend fun getRootFilePath(fetcher: Fetcher): String =
        fetcher.readAsXmlOrNull("/META-INF/container.xml")
            ?.getFirst("rootfiles", Namespaces.OPC)
            ?.getFirst("rootfile", Namespaces.OPC)
            ?.getAttr("full-path")
            ?: throw Exception("Unable to find an OPF file.")

    private suspend fun parseEncryptionData(fetcher: Fetcher): Map<String, Encryption> =
        fetcher.readAsXmlOrNull("/META-INF/encryption.xml")
            ?.let { EncryptionParser.parse(it) }
            ?: emptyMap()

    private suspend fun parseNavigationData(packageDocument: PackageDocument, fetcher: Fetcher): Map<String, List<Link>> =
        parseNavigationDocument(packageDocument, fetcher)
            ?: parseNcx(packageDocument, fetcher)
            ?: emptyMap()

    private suspend fun parseNavigationDocument(packageDocument: PackageDocument, fetcher: Fetcher): Map<String, List<Link>>? =
        packageDocument.manifest
            .firstOrNull { it.properties.contains(Vocabularies.ITEM + "nav") }
            ?.let { navItem ->
                val navPath = Href(navItem.href, baseHref = packageDocument.path).string
                fetcher.readAsXmlOrNull(navPath)
                    ?.let { NavigationDocumentParser.parse(it, navPath) }
            }
            ?.takeUnless { it.isEmpty() }

    private suspend fun parseNcx(packageDocument: PackageDocument, fetcher: Fetcher): Map<String, List<Link>>? {
        val ncxItem =
            if (packageDocument.spine.toc != null) {
                packageDocument.manifest.firstOrNull { it.id == packageDocument.spine.toc }
            } else {
                packageDocument.manifest.firstOrNull { MediaType.NCX.contains(it.mediaType) }
            }

        return ncxItem
            ?.let {
                val ncxPath = Href(ncxItem.href, baseHref = packageDocument.path).string
                fetcher.readAsXmlOrNull(ncxPath)?.let { NcxParser.parse(it, ncxPath) }
            }
            ?.takeUnless { it.isEmpty() }
    }

    private suspend fun parseDisplayOptions(fetcher: Fetcher): Map<String, String> {
        val displayOptionsXml =
            fetcher.readAsXmlOrNull("/META-INF/com.apple.ibooks.display-options.xml")
                ?: fetcher.readAsXmlOrNull("/META-INF/com.kobobooks.display-options.xml")

        return displayOptionsXml?.getFirst("platform", "")
            ?.get("option", "")
            ?.mapNotNull { element ->
                val optName = element.getAttr("name")
                val optVal = element.text
                if (optName != null && optVal != null) Pair(optName, optVal) else null
            }
            ?.toMap().orEmpty()
    }

    @Deprecated("This is done automatically in [parse], you can remove the call to [fillEncryption]", ReplaceWith(""))
    @Suppress("Unused_parameter")
    fun fillEncryption(container: Container, publication: Publication, drm: DRM?): Pair<Container, Publication> {
        return Pair(container, publication)
    }
}

@Suppress("DEPRECATION")
internal fun Publication.setLayoutStyle() {
    val layout = ReadiumCssLayout(metadata)

    cssStyle = layout.cssId

    userSettingsUIPreset = when (layout) {
        ReadiumCssLayout.RTL -> EPUBConstant.rtlPreset
        ReadiumCssLayout.LTR -> EPUBConstant.ltrPreset
        ReadiumCssLayout.CJK_VERTICAL -> EPUBConstant.cjkVerticalPreset
        ReadiumCssLayout.CJK_HORIZONTAL -> EPUBConstant.cjkHorizontalPreset
    }
}
