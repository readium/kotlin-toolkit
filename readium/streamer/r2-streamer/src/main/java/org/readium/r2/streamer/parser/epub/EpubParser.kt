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
import org.readium.r2.shared.drm.DRM
import org.readium.r2.shared.fetcher.Fetcher
import org.readium.r2.shared.fetcher.Resource
import org.readium.r2.shared.fetcher.TransformingFetcher
import org.readium.r2.shared.format.MediaType
import org.readium.r2.shared.normalize
import org.readium.r2.shared.publication.ContentLayout
import org.readium.r2.shared.publication.Link
import org.readium.r2.shared.publication.Publication
import org.readium.r2.shared.publication.encryption.Encryption
import org.readium.r2.streamer.container.Container
import org.readium.r2.streamer.container.ContainerError
import org.readium.r2.streamer.container.PublicationContainer
import org.readium.r2.streamer.extensions.fromArchiveOrDirectory
import org.readium.r2.streamer.extensions.readAsXmlOrNull
import org.readium.r2.streamer.fetcher.LcpDecryptor
import org.readium.r2.streamer.parser.PubBox
import org.readium.r2.streamer.parser.PublicationParser

object EPUBConstant {

    @Deprecated("Use [MediaType.EPUB.toString()] instead", replaceWith = ReplaceWith("MediaType.EPUB.toString()"))
    val mimetype: String get() = MediaType.EPUB.toString()

    private val ltrPreset: MutableMap<ReadiumCSSName, Boolean> = mutableMapOf(
        ReadiumCSSName.ref("hyphens") to false,
        ReadiumCSSName.ref("ligatures") to false
    )

    private val rtlPreset: MutableMap<ReadiumCSSName, Boolean> = mutableMapOf(
        ReadiumCSSName.ref("hyphens") to false,
        ReadiumCSSName.ref("wordSpacing") to false,
        ReadiumCSSName.ref("letterSpacing") to false,
        ReadiumCSSName.ref("ligatures") to true
    )

    private val cjkHorizontalPreset: MutableMap<ReadiumCSSName, Boolean> = mutableMapOf(
        ReadiumCSSName.ref("textAlignment") to false,
        ReadiumCSSName.ref("hyphens") to false,
        ReadiumCSSName.ref("paraIndent") to false,
        ReadiumCSSName.ref("wordSpacing") to false,
        ReadiumCSSName.ref("letterSpacing") to false
    )

    private val cjkVerticalPreset: MutableMap<ReadiumCSSName, Boolean> = mutableMapOf(
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

    val userSettingsUIPreset: MutableMap<ContentLayout, MutableMap<ReadiumCSSName, Boolean>> = mutableMapOf(
        ContentLayout.LTR to ltrPreset,
        ContentLayout.RTL to rtlPreset,
        ContentLayout.CJK_VERTICAL to cjkVerticalPreset,
        ContentLayout.CJK_HORIZONTAL to cjkHorizontalPreset
    )
}


class EpubParser : PublicationParser {

    override fun parse(fileAtPath: String, fallbackTitle: String): PubBox? = runBlocking {
        _parse(fileAtPath, fallbackTitle)
    }

    private suspend fun _parse(fileAtPath: String, fallbackTitle: String): PubBox? {
        var fetcher = Fetcher.fromArchiveOrDirectory(fileAtPath)
            ?: throw ContainerError.missingFile(fileAtPath)

        val drm =
            if (fetcher.get("/META-INF/license.lcpl").length().getOrNull() != null) DRM(DRM.Brand.lcp)
            else null

        val opfPath = getRootFilePath(fetcher) ?: return null
        val opfXmlDocument = fetcher.readAsXmlOrNull(opfPath) ?: return null
        val packageDocument = PackageDocument.parse(opfXmlDocument, opfPath) ?: return null

        val manifest = PublicationFactory(
                fallbackTitle = fallbackTitle,
                packageDocument = packageDocument,
                navigationData = parseNavigationData(packageDocument, fetcher),
                encryptionData = parseEncryptionData(fetcher, drm),
                displayOptions = parseDisplayOptions(fetcher)
            ).create()

        fetcher = TransformingFetcher(fetcher, listOfNotNull(
            drm?.let { LcpDecryptor(it)::transform },
            manifest.metadata.identifier?.let { EpubDeobfuscator(it)::transform }
        ))

        val publication = Publication(
            manifest = manifest,
            fetcher = fetcher,
            servicesBuilder = Publication.ServicesBuilder(
                positions = (EpubPositionsService)::create
            )
        ).apply {
            internalData["type"] = "epub"
            internalData["rootfile"] = opfPath

            type = Publication.TYPE.EPUB
            version = packageDocument.epubVersion

            // This might need to be moved as it's not really about parsing the EPUB but it
            // sets values needed (in UserSettings & ContentFilter)
            setLayoutStyle()
        }

        val container = PublicationContainer(
            publication = publication,
            path = fileAtPath,
            mediaType = MediaType.EPUB,
            drm = drm
        ).apply {
            rootFile.rootFilePath = opfPath
        }

        return PubBox(publication, container)
    }

    private suspend fun getRootFilePath(fetcher: Fetcher): String? =
        fetcher.readAsXmlOrNull("/META-INF/container.xml")
            ?.getFirst("rootfiles", Namespaces.OPC)
            ?.getFirst("rootfile", Namespaces.OPC)
            ?.getAttr("full-path")

    private fun Publication.setLayoutStyle() {
        cssStyle = contentLayout.cssId
        EPUBConstant.userSettingsUIPreset[contentLayout]?.let {
            if (type == Publication.TYPE.WEBPUB) {
                userSettingsUIPreset = EPUBConstant.forceScrollPreset
            } else {
                userSettingsUIPreset = it
            }
        }
    }

    private suspend fun parseEncryptionData(fetcher: Fetcher, drm: DRM?): Map<String, Encryption> =
        fetcher.readAsXmlOrNull("/META-INF/encryption.xml")
            ?.let { EncryptionParser.parse(it, drm) }
            ?: emptyMap()

    private suspend fun parseNavigationData(packageDocument: PackageDocument, fetcher: Fetcher): Map<String, List<Link>> =
        if (packageDocument.epubVersion < 3.0) {
            val ncxItem = packageDocument.manifest.firstOrNull { MediaType.NCX.contains(it.mediaType) }
            ncxItem?.let {
                val ncxPath = normalize(packageDocument.path, ncxItem.href)
                fetcher.readAsXmlOrNull(ncxPath)?.let { NcxParser.parse(it, ncxPath) }
            }
        } else {
            val navItem = packageDocument.manifest.firstOrNull { it.properties.contains(Vocabularies.ITEM + "nav") }
            navItem?.let {
                val navPath = normalize(packageDocument.path, navItem.href)
                fetcher.readAsXmlOrNull(navPath)?.let { NavigationDocumentParser.parse(it, navPath) }
            }
        }.orEmpty()

    private suspend fun parseDisplayOptions(fetcher: Fetcher): Map<String, String> {
        val displayOptionsXml =
            fetcher.readAsXmlOrNull("/META-INF/com.kobobooks.display-options.xml")
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
