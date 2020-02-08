/*
 * Module: r2-streamer-kotlin
 * Developers: Aferdita Muriqi, Clément Baumann, Quentin Gliosca
 *
 * Copyright (c) 2018. Readium Foundation. All rights reserved.
 * Use of this source code is governed by a BSD-style license which is detailed in the
 * LICENSE file present in the project repository where this source code is maintained.
 */

package org.readium.r2.streamer.parser.epub

import org.readium.r2.shared.*
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
import org.readium.r2.streamer.parser.normalize
import timber.log.Timber
import java.io.File

object EPUBConstant {
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
    private fun generateContainerFrom(path: String): Container {
        if (!File(path).exists())
            throw ContainerError.missingFile(path)

        val container = if (File(path).isDirectory) {
                DirectoryContainer(path = path, mimetype = EPUBConstant.mimetype)
            } else {
                ArchiveContainer(path = path, mimetype = EPUBConstant.mimetype)
            }
        container.drm =  if (container.contains(relativePath = Paths.lcpl)) DRM(DRM.Brand.lcp) else null
        return container
    }

    override fun parse(fileAtPath: String, title: String): PubBox? {
        val container = try {
            generateContainerFrom(fileAtPath)
        } catch (e: Exception) {
            Timber.e(e, "Could not generate container")
            return null
        }

        val containerXml = parseXmlDocument(Paths.container, container) ?: return null
        container.rootFile.mimetype = EPUBConstant.mimetype
        container.rootFile.rootFilePath = getRootFilePath(containerXml)

        val encryptionData =
            if (container.contains(Paths.encryption))
                parseXmlDocument(Paths.encryption, container)?.let { EncryptionParser.parse(it, container.drm) }.orEmpty()
            else
                emptyMap()

        val packageXml = parseXmlDocument(container.rootFile.rootFilePath, container) ?: return null
        val packageDocument = PackageDocument.parse(packageXml, container.rootFile.rootFilePath) ?: return null

        val navigationData = if (packageDocument.epubVersion < 3.0) {
            val ncxItem = packageDocument.manifest.firstOrNull { it.mediaType == Mimetypes.Ncx }
            ncxItem?.let {
                val ncxPath = normalize(packageDocument.path, ncxItem.href)
                parseXmlDocument(ncxPath, container)?.let { NcxParser.parse(it, ncxPath) }
            }
        } else {
            val navItem = packageDocument.manifest.firstOrNull { it.properties.contains(DEFAULT_VOCAB.ITEM.iri + "nav") }
            navItem?.let {
                val navPath = normalize(packageDocument.path, navItem.href)
                parseXmlDocument(navPath, container)?.let { NavigationDocumentParser.parse(it, navPath) }
            }
        }.orEmpty()

        val publication = Epub(packageDocument, navigationData, encryptionData).toPublication()
        publication.internalData["type"] = "epub"
        publication.internalData["rootfile"] = container.rootFile.rootFilePath

        /*
         * This might need to be moved as it's not really about parsing the Epub
         * but it sets values needed (in UserSettings & ContentFilter)
         */
        setLayoutStyle(publication)

        return PubBox(publication, container)
    }

    private fun getRootFilePath(document: ElementNode): String =
            document.getFirst("rootfiles", Namespaces.Opc)
                    ?.getFirst("rootfile", Namespaces.Opc)
                    ?.getAttr("full-path")
                    ?: "content.opf"

    private fun setLayoutStyle(publication: Publication) {
        publication.cssStyle = publication.contentLayout.name
        EPUBConstant.userSettingsUIPreset[publication.contentLayout]?.let {
            if (publication.type == Publication.TYPE.WEBPUB) {
                publication.userSettingsUIPreset = EPUBConstant.forceScrollPreset
            } else {
                publication.userSettingsUIPreset = it
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
        val document = try {
            XmlParser().parse(data.inputStream())
        } catch (e: Exception) {
            null
        }
        return document
    }

    @Deprecated("This is done automatically in [parse], you can remove the call to [fillEncryption]", ReplaceWith(""))
    @Suppress("UNUSED_PARAMETER")
    fun fillEncryption(container: Container, publication: Publication, drm: DRM?): Pair<Container, Publication> {
        return Pair(container, publication)
    }

}
