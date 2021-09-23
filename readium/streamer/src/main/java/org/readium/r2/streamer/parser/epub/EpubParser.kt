/*
 * Module: r2-streamer-kotlin
 * Developers: Aferdita Muriqi, Clément Baumann, Quentin Gliosca
 *
 * Copyright (c) 2018. Readium Foundation. All rights reserved.
 * Use of this source code is governed by a BSD-style license which is detailed in the
 * LICENSE file present in the project repository where this source code is maintained.
 */

package org.readium.r2.streamer.parser.epub

import kotlinx.coroutines.runBlocking
import org.readium.r2.shared.ReadiumCSSName
import org.readium.r2.shared.Search
import org.readium.r2.shared.drm.DRM
import org.readium.r2.shared.extensions.addPrefix
import org.readium.r2.shared.fetcher.Fetcher
import org.readium.r2.shared.fetcher.TransformingFetcher
import org.readium.r2.shared.publication.Link
import org.readium.r2.shared.publication.Publication
import org.readium.r2.shared.publication.asset.FileAsset
import org.readium.r2.shared.publication.asset.PublicationAsset
import org.readium.r2.shared.publication.encryption.Encryption
import org.readium.r2.shared.publication.services.search.StringSearchService
import org.readium.r2.shared.util.Href
import org.readium.r2.shared.util.logging.WarningLogger
import org.readium.r2.shared.util.mediatype.MediaType
import org.readium.r2.shared.util.use
import org.readium.r2.streamer.PublicationParser
import org.readium.r2.streamer.container.Container
import org.readium.r2.streamer.container.ContainerError
import org.readium.r2.streamer.container.PublicationContainer
import org.readium.r2.streamer.extensions.fromArchiveOrDirectory
import org.readium.r2.streamer.extensions.readAsXmlOrNull
import org.readium.r2.streamer.fetcher.LcpDecryptor
import org.readium.r2.streamer.parser.PubBox
import java.io.File

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
class EpubParser(
    private val reflowablePositionsStrategy: EpubPositionsService.ReflowableStrategy = EpubPositionsService.ReflowableStrategy.recommended
) : PublicationParser, org.readium.r2.streamer.parser.PublicationParser {

    override suspend fun parse(asset: PublicationAsset, fetcher: Fetcher, warnings: WarningLogger?): Publication.Builder? =
        _parse(asset, fetcher, asset.name)

    @OptIn(Search::class)
    suspend fun _parse(asset: PublicationAsset, fetcher: Fetcher, fallbackTitle: String): Publication.Builder? {

        if (asset.mediaType() != MediaType.EPUB)
            return null

        val opfPath = getRootFilePath(fetcher).addPrefix("/")
        val opfXmlDocument = fetcher.get(opfPath).readAsXml().getOrThrow()
        val packageDocument = PackageDocument.parse(opfXmlDocument, opfPath)
            ?:  throw Exception("Invalid OPF file.")

        val manifest = PublicationFactory(
                fallbackTitle = fallbackTitle,
                packageDocument = packageDocument,
                navigationData = parseNavigationData(packageDocument, fetcher),
                encryptionData = parseEncryptionData(fetcher),
                displayOptions = parseDisplayOptions(fetcher)
            ).create()

        @Suppress("NAME_SHADOWING")
        var fetcher = fetcher
        manifest.metadata.identifier?.let {
            fetcher = TransformingFetcher(fetcher, EpubDeobfuscator(it)::transform)
        }

        return Publication.Builder(
            manifest = manifest,
            fetcher = fetcher,
            servicesBuilder = Publication.ServicesBuilder(
                positions = EpubPositionsService.createFactory(reflowablePositionsStrategy),
                search = StringSearchService.createDefaultFactory(),
            )
        )
    }

    override fun parse(
        fileAtPath: String,
        fallbackTitle: String
    ): PubBox? = runBlocking {

        val file = File(fileAtPath)
        val asset = FileAsset(file)

        var fetcher = Fetcher.fromArchiveOrDirectory(fileAtPath)
            ?: throw ContainerError.missingFile(fileAtPath)

        val drm = if (fetcher.isProtectedWithLcp()) DRM(DRM.Brand.lcp) else null
        if (drm?.brand == DRM.Brand.lcp) {
            fetcher = TransformingFetcher(fetcher, LcpDecryptor(drm)::transform)
        }

        val builder = try {
            _parse(asset, fetcher, fallbackTitle)
        } catch (e: Exception) {
            return@runBlocking null
        } ?: return@runBlocking null

        val publication = builder.build()
            .apply {
                type = Publication.TYPE.EPUB

                // This might need to be moved as it's not really about parsing the EPUB but it
                // sets values needed (in UserSettings & ContentFilter)
                setLayoutStyle()
            }

        val container = PublicationContainer(
            publication = publication,
            path = file.canonicalPath,
            mediaType = MediaType.EPUB,
            drm = drm
        ).apply {
            rootFile.rootFilePath = getRootFilePath(fetcher)
        }

        PubBox(publication, container)
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
        if (packageDocument.epubVersion < 3.0) {
            val ncxItem =
                if (packageDocument.spine.toc != null) {
                    packageDocument.manifest.firstOrNull { it.id == packageDocument.spine.toc }
                } else {
                    packageDocument.manifest.firstOrNull { MediaType.NCX.contains(it.mediaType) }
                }
            ncxItem?.let {
                val ncxPath = Href(ncxItem.href, baseHref = packageDocument.path).string
                fetcher.readAsXmlOrNull(ncxPath)?.let { NcxParser.parse(it, ncxPath) }
            }
        } else {
            val navItem = packageDocument.manifest.firstOrNull { it.properties.contains(Vocabularies.ITEM + "nav") }
            navItem?.let {
                val navPath = Href(navItem.href, baseHref = packageDocument.path).string
                fetcher.readAsXmlOrNull(navPath)?.let { NavigationDocumentParser.parse(it, navPath) }
            }
        }.orEmpty()

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

private suspend fun Fetcher.isProtectedWithLcp(): Boolean =
    get("/META-INF/license.lcpl").use { it.length().isSuccess }
