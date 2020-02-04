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
import java.io.InputStream
import java.net.URI
import java.util.zip.ZipEntry
import java.util.zip.ZipFile

/**
 * Template for an archive container ( E.g.: EPub, CBZ, .. )
 *
 * Contains the zipFile and methods to gather data, size of
 * the content or an inputStream of the archive
 *
 */


open class ArchiveContainer(path: String, mimetype: String) : Container {
    override var rootFile: RootFile = RootFile(rootPath = path, mimetype = mimetype)
    override var drm: DRM? = null
    val archive: ZipFile = ZipFile(path)

    override fun data(relativePath: String): ByteArray {
        val zipEntry = getEntry(relativePath)// ?: return ByteArray(0)
        val inputStream = archive.getInputStream(zipEntry)
        val outputStream = ByteArrayOutputStream()
        var readLength: Int
        val buffer = ByteArray(16384)

        while (inputStream.read(buffer).let { readLength = it; it != -1 })
            outputStream.write(buffer, 0, readLength)

        inputStream.close()
        return outputStream.toByteArray()

    }

    override fun contains(relativePath: String): Boolean =
        getEntry(relativePath) != null

    override fun dataLength(relativePath: String): Long {
        return getEntry(relativePath)?.size ?: 0
    }

    override fun dataInputStream(relativePath: String): InputStream {
        return archive.getInputStream(getEntry(relativePath))
    }
    
    private fun getEntry(relativePath: String): ZipEntry? {
        var path: String = try {
            URI(relativePath).path
        } catch (e: Exception) {
            relativePath
        }

        // [ZipFile] doesn't expect a / at the beginning of relative paths, but [Link]'s [href]
        // have them.
        path = path.removePrefix("/")

        var zipEntry = archive.getEntry(path)
        if (zipEntry != null)
            return zipEntry

        val zipEntries = archive.entries()
        while (zipEntries.hasMoreElements()) {
            zipEntry = zipEntries.nextElement()
            if (path.equals(zipEntry.name, true))
                return zipEntry
        }

        return null
    }

}

