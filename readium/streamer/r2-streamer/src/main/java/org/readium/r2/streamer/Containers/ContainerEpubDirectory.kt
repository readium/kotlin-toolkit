package org.readium.r2.streamer.Containers

import org.readium.r2.shared.Drm
import org.readium.r2.shared.Link
import org.readium.r2.shared.RootFile
import org.readium.r2.streamer.XmlParser.XmlParser
import java.io.File

class ContainerEpubDirectory : EpubContainer, DirectoryContainer {

    override var successCreated: Boolean = false
    override var rootFile: RootFile
    override var drm: Drm? = null

    override fun xmlDocumentforFile(relativePath: String): XmlParser {
        val containerData = data(relativePath)
        val document = XmlParser()
        document.parseXml(containerData.inputStream())
        return document
    }

    override fun xmlDocumentforResource(link: Link?): XmlParser {
        var pathFile = link?.href ?: throw Exception("missing Link : ${link?.title}")
        if (pathFile.first() == '/')
            pathFile = pathFile.substring(1)
        return xmlDocumentforFile(pathFile)
    }

    constructor(path: String) {
        if (File(path).exists())
            successCreated = true
        rootFile = RootFile(rootPath = path, version = null)
    }

    override fun data(relativePath: String): ByteArray {
        return super.data(relativePath)
    }
}