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
import org.readium.r2.shared.Link
import org.readium.r2.shared.drm.DRM
import org.readium.r2.shared.parser.xml.ElementNode
import org.readium.r2.shared.parser.xml.XmlParser
import org.readium.r2.streamer.BuildConfig.DEBUG
import org.readium.r2.streamer.container.ArchiveContainer
import org.readium.r2.streamer.container.Container
import org.readium.r2.streamer.container.ContainerError
import org.readium.r2.streamer.container.DirectoryContainer
import org.readium.r2.streamer.parser.PubBox
import org.readium.r2.streamer.parser.PublicationParser
import timber.log.Timber
import java.io.File

class EPUBConstant {
    companion object {
        const val lcplFilePath: String = "META-INF/license.lcpl"
        const val mimetype: String = "application/epub+zip"
        const val mimetypeOEBPS: String = "application/oebps-package+xml"
        const val mediaOverlayURL: String = "media-overlay?resource="
        const val containerDotXmlPath = "META-INF/container.xml"
        const val encryptionDotXmlPath = "META-INF/encryption.xml"
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

        val userSettingsUIPreset: MutableMap<ContentLayoutStyle, MutableMap<ReadiumCSSName, Boolean>> = mutableMapOf(
                ContentLayoutStyle.layout("ltr") to ltrPreset,
                ContentLayoutStyle.layout("rtl") to rtlPreset,
                ContentLayoutStyle.layout("cjkv") to cjkVerticalPreset,
                ContentLayoutStyle.layout("cjkh") to cjkHorizontalPreset
        )
    }
}


class EpubParser : PublicationParser {

//    companion object {
        // Some constants useful to parse an Epub document
//        const val defaultEpubVersion = 1.2
//        const val containerDotXmlPath = "META-INF/container.xml"
//        const val encryptionDotXmlPath = "META-INF/encryption.xml"
//        const val lcplFilePath = "META-INF/license.lcpl"
//        const val mimetypeEpub = "application/epub+zip"
//        const val mimetypeOEBPS = "application/oebps-package+xml"
//        const val mediaOverlayURL = "media-overlay?resource="
//    }

    private val ndp = NavigationDocumentParser()
    private val ncxp = NcxParser()
    private val encp = EncryptionParser()

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
            container.data(EPUBConstant.containerDotXmlPath)
        } catch (e: Exception) {
            if (DEBUG) Timber.e(e, "Missing File : ${EPUBConstant.containerDotXmlPath}")
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

        val packageXml = XmlParser().parse(documentData.inputStream())
        val packageDocument = PackageDocumentParser.parse(packageXml, container.rootFile.rootFilePath)  ?: return null
        val publication = packageDocument.toPublication()
        publication.internalData["type"] = "epub"
        publication.internalData["rootfile"] = container.rootFile.rootFilePath

        val drm = scanForDRM(container)
        container.drm = drm
        parseEncryption(container, publication, drm)

        if (packageDocument.epubVersion < 3.0) {
            parseNcxDocument(container, publication)
        } else {
            parseNavigationDocument(container, publication)
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
                    container.data(relativePath = EPUBConstant.lcplFilePath)
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
        var langType = LangType.other

        langTypeLoop@ for (lang in publication.metadata.languages) {
            when (lang) {
                "zh", "ja", "ko" -> {
                    langType = LangType.cjk
                    break@langTypeLoop
                }
                "ar", "fa", "he" -> {
                    langType = LangType.afh
                    break@langTypeLoop
                }
            }
        }

        val pageDirection = publication.metadata.direction
        val contentLayoutStyle = publication.metadata.contentLayoutStyle(langType, pageDirection)

        publication.cssStyle = contentLayoutStyle.name

        EPUBConstant.userSettingsUIPreset[ContentLayoutStyle.layout(publication.cssStyle as String)]?.let {
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

    private fun parseEncryption(container: Container, publication: Publication, drm: DRM?) {
        val documentData = try {
            container.data(EPUBConstant.encryptionDotXmlPath)
        } catch (e: Exception) {
            return
        }
        val document = XmlParser().parse(documentData.inputStream())
        val encryptedDataElements = document.getFirst("encryption", Namespaces.Opc)
                ?.get("EncryptedData", Namespaces.Enc) ?: return
        for (encryptedDataElement in encryptedDataElements) {
            val encryption = Encryption()
            val keyInfoUri = encryptedDataElement.getFirst("KeyInfo", Namespaces.Sig)
                    ?.getFirst("RetrievalMethod", Namespaces.Sig)?.getAttr("URI")
            if (keyInfoUri == "license.lcpl#/encryption/content_key" && drm?.brand == DRM.Brand.lcp)
                encryption.scheme = DRM.Scheme.lcp
            encryption.algorithm = encryptedDataElement.getFirst("EncryptionMethod", Namespaces.Enc)
                    ?.getAttr("Algorithm")
            encp.parseEncryptionProperties(encryptedDataElement, encryption)
            encp.add(encryption, publication, encryptedDataElement)
        }
    }

    private fun parseNavigationDocument(container: Container, publication: Publication) {
        val navLink = publication.linkWithRel("contents") ?: return

        val navDocument = try {
            xmlDocumentForResource(navLink, container)
        } catch (e: Exception) {
            if (DEBUG) Timber.e(e)
            return
        }

        val navByteArray = try {
            xmlAsByteArray(navLink, container)
        } catch (e: Exception) {
            if (DEBUG) Timber.e(e)
            return
        }

        ndp.navigationDocumentPath = navLink.href ?: return
        publication.tableOfContents.plusAssign(ndp.tableOfContent(navByteArray))
        publication.landmarks.plusAssign(ndp.landmarks(navDocument))
        publication.listOfAudioFiles.plusAssign(ndp.listOfAudiofiles(navDocument))
        publication.listOfIllustrations.plusAssign(ndp.listOfIllustrations(navDocument))
        publication.listOfTables.plusAssign(ndp.listOfTables(navDocument))
        publication.listOfVideos.plusAssign(ndp.listOfVideos(navDocument))
        publication.pageList.plusAssign(ndp.pageList(navDocument))
    }

    private fun parseNcxDocument(container: Container, publication: Publication) {

        val ncxLink = publication.resources.firstOrNull { it.typeLink == "application/x-dtbncx+xml" }
                ?: return
        val ncxDocument = try {
            xmlDocumentForResource(ncxLink, container)
        } catch (e: Exception) {
            if (DEBUG) Timber.e(e)
            return
        }
        ncxp.ncxDocumentPath = ncxLink.href ?: return
        if (publication.tableOfContents.isEmpty())
            publication.tableOfContents.plusAssign(ncxp.tableOfContents(ncxDocument))
        if (publication.pageList.isEmpty())
            publication.pageList.plusAssign(ncxp.pageList(ncxDocument))
        return
    }

    private fun parseMediaOverlays(container: Container, publication: Publication) {
        val xmlParser = XmlParser()
       publication.otherLinks.forEach {
            val path = if (it.href?.first() == '/') it.href?.substring(1) else it.href
            if (it.typeLink == "application/smil+xml" && path != null) {
                it.mediaOverlays = xmlParser.parse(container.dataInputStream(path)).let { SmilParser.parse(it, path) }
                it.rel.add("media-overlay")
            }
        }
    }

    private fun xmlAsByteArray(link: Link?, container: Container): ByteArray {
        var pathFile = link?.href ?: throw ContainerError.missingLink(link?.title)
        if (pathFile.first() == '/')
            pathFile = pathFile.substring(1)

        return container.data(pathFile)
    }

    private fun xmlDocumentForResource(link: Link?, container: Container): ElementNode {
        var pathFile = link?.href ?: throw ContainerError.missingLink(link?.title)
        if (pathFile.first() == '/')
            pathFile = pathFile.substring(1)

        val containerData = container.data(pathFile)
        return XmlParser().parse(containerData.inputStream())
    }
}
