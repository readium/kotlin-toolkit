/*
 * Module: r2-streamer-kotlin
 * Developers: Aferdita Muriqi, Clément Baumann, Quentin Gliosca
 *
 * Copyright (c) 2018. Readium Foundation. All rights reserved.
 * Use of this source code is governed by a BSD-style license which is detailed in the
 * LICENSE file present in the project repository where this source code is maintained.
 */

package org.readium.r2.streamer.parser.epub

import org.readium.r2.shared.ReadiumCSSName
import org.readium.r2.shared.drm.DRM
import org.readium.r2.shared.publication.Publication
import org.readium.r2.shared.parser.xml.ElementNode
import org.readium.r2.shared.parser.xml.XmlParser
import org.readium.r2.shared.publication.ContentLayout
import org.readium.r2.streamer.container.ArchiveContainer
import org.readium.r2.streamer.container.Container
import org.readium.r2.streamer.container.ContainerError
import org.readium.r2.streamer.container.DirectoryContainer
import org.readium.r2.streamer.parser.PubBox
import org.readium.r2.streamer.parser.PublicationParser
import org.readium.r2.shared.normalize
import org.readium.r2.shared.publication.Link
import org.readium.r2.shared.publication.encryption.Encryption
import timber.log.Timber
import java.io.File

object EPUBConstant {

    // FIXME: To refactor into r2-shared's ContentType
    const val mimetype: String = "application/epub+zip"

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

    override fun parse(fileAtPath: String, fallbackTitle: String): PubBox? {
        val container = try {
            generateContainerFrom(fileAtPath)
        } catch (e: Exception) {
            Timber.e(e, "Could not generate container")
            return null
        }

        val containerXml = parseXmlDocument(Paths.CONTAINER, container)
            ?: return null
        val opfPath = getRootFilePath(containerXml)
        val packageXml = parseXmlDocument(opfPath, container)
            ?: return null
        val packageDocument = PackageDocument.parse(packageXml, opfPath)
            ?: return null

        container.rootFile.apply {
            mimetype = EPUBConstant.mimetype
            rootFilePath = opfPath
        }

        val publication = PublicationFactory(
                fallbackTitle = fallbackTitle,
                packageDocument = packageDocument,
                navigationData = parseNavigationData(packageDocument, container),
                encryptionData = parseEncryptionData(container),
                displayOptions = parseDisplayOptions(container)
            ).create().apply {
                internalData["type"] = "epub"
                internalData["rootfile"] = opfPath

                // This might need to be moved as it's not really about parsing the EPUB but it
                // sets values needed (in UserSettings & ContentFilter)
                setLayoutStyle()
            }

        return PubBox(publication, container)
    }

    private fun generateContainerFrom(path: String): Container {
        if (!File(path).exists())
            throw ContainerError.missingFile(path)

        val container = if (File(path).isDirectory) {
            DirectoryContainer(path = path, mimetype = EPUBConstant.mimetype)
        } else {
            ArchiveContainer(path = path, mimetype = EPUBConstant.mimetype)
        }
        container.drm =
            if (container.dataLength(relativePath = Paths.LCPL) > 0) DRM(DRM.Brand.lcp)
            else null
        return container
    }

    private fun getRootFilePath(document: ElementNode): String =
        document.getFirst("rootfiles", Namespaces.OPC)
            ?.getFirst("rootfile", Namespaces.OPC)
            ?.getAttr("full-path")
            ?: "content.opf"

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

    private fun parseXmlDocument(path: String, container: Container): ElementNode? {
        val data = try {
            container.data(path)
        } catch (e: Exception) {
            Timber.e(e, "Missing File : $path")
            return null
        }
        return try {
            XmlParser().parse(data.inputStream())
        } catch (e: Exception) {
            null
        }
    }

    private fun parseEncryptionData(container: Container): Map<String, Encryption> =
        if (container.dataLength(Paths.ENCRYPTION) > 0) {
            parseXmlDocument(Paths.ENCRYPTION, container)?.let {
                EncryptionParser.parse(it, container.drm)
            }.orEmpty()
        } else {
            emptyMap()
        }

    private fun parseNavigationData(packageDocument: PackageDocument, container: Container): Map<String, List<Link>> =
        if (packageDocument.epubVersion < 3.0) {
            val ncxItem = packageDocument.manifest.firstOrNull { it.mediaType == Mimetypes.NCX }
            ncxItem?.let {
                val ncxPath = normalize(packageDocument.path, ncxItem.href)
                parseXmlDocument(ncxPath, container)?.let { NcxParser.parse(it, ncxPath) }
            }
        } else {
            val navItem = packageDocument.manifest.firstOrNull { it.properties.contains(Vocabularies.ITEM + "nav") }
            navItem?.let {
                val navPath = normalize(packageDocument.path, navItem.href)
                parseXmlDocument(navPath, container)?.let { NavigationDocumentParser.parse(it, navPath) }
            }
        }.orEmpty()

    private fun parseDisplayOptions(container: Container): Map<String, String> {
        val displayOptionsXml = parseXmlDocument(Paths.KOBO_DISPLAY_OPTIONS, container)
            ?: parseXmlDocument(Paths.IBOOKS_DISPLAY_OPTIONS, container)

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
