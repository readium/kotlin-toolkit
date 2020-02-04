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

    override fun contains(relativePath: String): Boolean =
        getFile(relativePath).exists()

    override fun data(relativePath: String): ByteArray {
        val file = getFile(relativePath)
        if (!file.exists())
            throw ContainerError.fileNotFound

        val outputStream = ByteArrayOutputStream()
        var readLength: Int
        val buffer = ByteArray(16384)
        val inputStream = FileInputStream(file)

        while (inputStream.read(buffer).let { readLength = it; it != -1 })
            outputStream.write(buffer, 0, readLength)

        inputStream.close()
        return outputStream.toByteArray()
    }

    override fun dataLength(relativePath: String) =
        getFile(relativePath).length()

    override fun dataInputStream(relativePath: String) =
        FileInputStream(getFile(relativePath))


    private fun getFile(relativePath: String): File {
        val path = relativePath
            // Decode the path
            .replace(" ", "%20")
            // [Link]'s [href] are prefixed with /
            .removePrefix("/")

        return File("${rootFile.rootPath}/${URI(path).path}")
    }

}
