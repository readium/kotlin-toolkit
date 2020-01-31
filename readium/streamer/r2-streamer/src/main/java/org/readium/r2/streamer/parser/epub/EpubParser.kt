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
import org.readium.r2.streamer.BuildConfig.DEBUG
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

        val isDirectory = File(path).isDirectory
        return {
            if (isDirectory) {
                DirectoryContainer(path = path, mimetype = EPUBConstant.mimetype)
            } else {
                ArchiveContainer(path = path, mimetype = EPUBConstant.mimetype)
            }
        }()
    }


    override fun parse(fileAtPath: String, title: String): PubBox? {
        val container = try {
            generateContainerFrom(fileAtPath)
        } catch (e: Exception) {
            if (DEBUG) Timber.e(e, "Could not generate container")
            return null
        }
        val data = try {
            container.data(Paths.container)
        } catch (e: Exception) {
            if (DEBUG) Timber.e(e, "Missing File : ${Paths.container}")
            return null
        }

        container.rootFile.mimetype = EPUBConstant.mimetype
        container.rootFile.rootFilePath = getRootFilePath(data)

        val documentData = try {
            container.data(container.rootFile.rootFilePath)
        } catch (e: Exception) {
            if (DEBUG) Timber.e(e, "Missing File : ${container.rootFile.rootFilePath}")
            return null
        }

        val packageDocument = try {
            val packageXml = XmlParser().parse(documentData.inputStream())
            PackageDocumentParser.parse(packageXml, container.rootFile.rootFilePath) ?: return null
        } catch (e: Exception) {
            if (DEBUG) Timber.e(e, "Invalid File : ${container.rootFile.rootFilePath}")
            return null
        }

        val publication =  packageDocument.toPublication()
        publication.internalData["type"] = "epub"
        publication.internalData["rootfile"] = container.rootFile.rootFilePath

        val drm = scanForDRM(container)
        container.drm = drm
        parseEncryption(container, publication)

        if (packageDocument.epubVersion < 3.0) {
            val ncxItem = packageDocument.manifest.firstOrNull { it.mediaType == "application/x-dtbncx+xml" }
            if (ncxItem != null) {
                val ncxPath = normalize(packageDocument.path, ncxItem.href)
                parseNcxDocument(ncxPath, container, publication)
            }
        } else {
            val navItem = packageDocument.manifest.firstOrNull { it.properties.contains("nav") }
            if (navItem != null) {
                val navPath = normalize(packageDocument.path, navItem.href)
                parseNavigationDocument(navPath, container, publication)
            }
            parseMediaOverlays(container, publication)
        }


        /*
         * This might need to be moved as it's not really about parsing the Epub
         * but it sets values needed (in UserSettings & ContentFilter)
         */
        setLayoutStyle(publication)

        return PubBox(publication, container)
    }

    private fun scanForDRM(container: Container): DRM? {
        if (((try {
                    container.data(relativePath = Paths.lcpl)
                } catch (e: Throwable) {
                    null
                }) != null)) {
            return DRM(DRM.Brand.lcp)
        }
        return null
    }

    private fun getRootFilePath(data: ByteArray): String {
        val container = XmlParser().parse(data.inputStream())
        return container.getFirst("rootfiles", Namespaces.Opc)
                ?.getFirst("rootfile", Namespaces.Opc)
                ?.getAttr("full-path")
                ?: "content.opf"
    }

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

    fun fillEncryption(container: Container, publication: Publication, drm: DRM?): Pair<Container, Publication> {
        container.drm = drm
        fillEncryptionProfile(publication, drm)

        return Pair(container, publication)
    }

    private fun fillEncryptionProfile(publication: Publication, drm: DRM?): Publication {
        drm?.let {
            for (link in publication.resources) {
                if (link.properties.encryption?.scheme == it.scheme) {
                    link.properties.encryption?.profile = it.license?.encryptionProfile
                }
            }
            for (link in publication.readingOrder) {
                if (link.properties.encryption?.scheme == it.scheme) {
                    link.properties.encryption?.profile = it.license?.encryptionProfile
                }
            }
        }
        return publication
    }

    private fun parseXmlDocument(path: String, container: Container) : ElementNode? =
        try {
            val data = container.data(path)
            XmlParser().parse(data.inputStream())
        } catch (e: Exception) {
            null
        }

    private fun parseEncryption(container: Container, publication: Publication) {
        val document = parseXmlDocument(Paths.encryption, container) ?: return
        val encryption = EncryptionParser.parse(document)
        encryption.forEach {
            val resourceURI = normalize("/", it.key)
            val link = publication.linkWithHref(resourceURI)
            if (link != null) link.properties.encryption = it.value}
    }

    private fun parseNavigationDocument(navPath: String, container: Container, publication: Publication) {
        val document = parseXmlDocument(navPath, container) ?: return
        val navDoc = NavigationDocumentParser.parse(document, navPath)
        if (navDoc != null) {
            publication.tableOfContents = navDoc.toc.toMutableList()
            publication.landmarks = navDoc.landmarks.toMutableList()
            publication.listOfAudioFiles = navDoc.loa.toMutableList()
            publication.listOfIllustrations = navDoc.loi.toMutableList ()
            publication.listOfTables = navDoc.lot.toMutableList()
            publication.listOfVideos = navDoc.lov.toMutableList()
            publication.pageList = navDoc.pageList.toMutableList()
        }
    }

    private fun parseNcxDocument(ncxPath: String, container: Container, publication: Publication) {
        val document = parseXmlDocument(ncxPath, container) ?: return
        val ncx = NcxParser.parse(document, ncxPath)
        if (ncx != null) {
            publication.tableOfContents = ncx.toc.toMutableList()
            publication.pageList = ncx.pageList.toMutableList()
        }
    }

    private fun parseMediaOverlays(container: Container, publication: Publication) {
        val xmlParser = XmlParser()
        publication.otherLinks.forEach {
            val path = if (it.href?.first() == '/') it.href?.substring(1) else it.href
            if (it.typeLink == "application/smil+xml" && path != null) {
                it.mediaOverlays = try {
                    xmlParser.parse(container.dataInputStream(path)).let { SmilParser.parse(it, path) }
                } catch (e: Exception) {
                    if (DEBUG) Timber.e(e)
                    null
                }
                if (it.mediaOverlays != null) it.rel.add("media-overlay")
            }
        }
    }
}
