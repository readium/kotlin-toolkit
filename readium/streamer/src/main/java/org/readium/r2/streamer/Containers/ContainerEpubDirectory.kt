package org.readium.r2.streamer.Containers

import org.readium.r2.shared.Link
import org.readium.r2.shared.RootFile
import org.readium.r2.streamer.XmlParser.XmlParser
import java.io.File

class ContainerEpubDirectory(path: String) : EpubContainer, DirectoryContainer {

    override var successCreated: Boolean = false
    lateinit override var rootFile: RootFile

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

    init {
        if (File(path).exists())
            successCreated = true
        rootFile = RootFile(rootPath = path, version = null)
    }

    override fun data(relativePath: String): ByteArray {
        return super.data(relativePath)
    }
}