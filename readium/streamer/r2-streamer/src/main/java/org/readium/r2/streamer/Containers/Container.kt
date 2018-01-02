package org.readium.r2.streamer.Containers

import org.readium.r2.shared.Link
import org.readium.r2.shared.RootFile
import org.readium.r2.streamer.XmlParser.XmlParser
import java.io.InputStream

interface Container{

    var rootFile: RootFile

    var successCreated: Boolean

    fun data(relativePath: String) : ByteArray

    fun dataLength(relativePath: String) : Long

    fun dataInputStream(relativePath: String) : InputStream
}

interface EpubContainer : Container {

    fun xmlDocumentforFile(relativePath: String) : XmlParser
    fun xmlDocumentforResource(link: Link?) : XmlParser

}

interface CbzContainer : Container {
    fun getFilesList() : List<String>
}
