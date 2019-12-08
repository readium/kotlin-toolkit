/*
 * Module: r2-streamer-kotlin
 * Developers: Aferdita Muriqi, Clément Baumann
 *
 * Copyright (c) 2018. Readium Foundation. All rights reserved.
 * Use of this source code is governed by a BSD-style license which is detailed in the
 * LICENSE file present in the project repository where this source code is maintained.
 */

package org.readium.r2.streamer.container

import org.readium.r2.shared.RootFile
import org.readium.r2.shared.drm.DRM
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileInputStream
import java.net.URI


open class DirectoryContainer(path: String, mimetype: String) : Container {
    override var rootFile: RootFile = RootFile(rootPath = path, mimetype = mimetype)
    override var drm: DRM? = null

    override fun data(relativePath: String): ByteArray {

        val decodedFilePath = rootFile.rootPath + "/" + getDecodedRelativePath(relativePath)
        val file = File(decodedFilePath)

        if (!file.exists())
            throw ContainerError.fileNotFound

        val outputStream = ByteArrayOutputStream()
        var readLength = 0
        val buffer = ByteArray(16384)
        val inputStream = FileInputStream(file)

        while (inputStream.read(buffer).let { readLength = it; it != -1 })
            outputStream.write(buffer, 0, readLength)

        inputStream.close()
        return outputStream.toByteArray()
    }

    override fun dataLength(relativePath: String) =
            File(rootFile.toString() + "/" + getDecodedRelativePath(relativePath)).length()

    override fun dataInputStream(relativePath: String) =
            FileInputStream(File(rootFile.toString() + "/" + getDecodedRelativePath(relativePath)))


    private fun getDecodedRelativePath(relativePath: String): String {
        val replacedPath = relativePath.replace(" ", "%20")
        return URI(replacedPath).path
    }

}