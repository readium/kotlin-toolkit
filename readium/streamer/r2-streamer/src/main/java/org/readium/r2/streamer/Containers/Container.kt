/*
 * Copyright 2018 Readium Foundation. All rights reserved.
 * Use of this source code is governed by a BSD-style license which is detailed in the
 * LICENSE file present in the project repository where this source code is maintained.
 */

package org.readium.r2.streamer.Containers

import org.readium.r2.shared.drm.Drm
import org.readium.r2.shared.Link
import org.readium.r2.shared.RootFile
import org.readium.r2.shared.XmlParser.XmlParser
import java.io.InputStream

interface Container{

    var rootFile: RootFile

    var drm: Drm?

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
