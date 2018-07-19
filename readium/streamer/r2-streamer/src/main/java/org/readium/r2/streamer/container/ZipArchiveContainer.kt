/*
 * Copyright 2018 Readium Foundation. All rights reserved.
 * Use of this source code is governed by a BSD-style license which is detailed in the
 * LICENSE file present in the project repository where this source code is maintained.
 */

package org.readium.r2.streamer.container

import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.util.zip.ZipFile

/**
 * Template for an archive container ( E.g.: EPub, CBZ, .. )
 *
 * Contains the zipFile and methods to gather data, size of
 * the content or an inputStream of the archive
 *
 */

interface ZipArchiveContainer : Container {
    var zipFile: ZipFile

    override fun data(relativePath: String): ByteArray {

        val zipEntry = zipFile.getEntry(relativePath)// ?: return ByteArray(0)
        val fis = zipFile.getInputStream(zipEntry)
        val buffer = ByteArrayOutputStream()
        var nRead: Int
        val data = ByteArray(16384)

        nRead = fis!!.read(data, 0, data.size)
        while (nRead != -1) {
            buffer.write(data, 0, nRead)
            nRead = fis.read(data, 0, data.size)
        }
        buffer.flush()
        return buffer.toByteArray()
    }

    override fun dataLength(relativePath: String): Long {
        return zipFile.size().toLong()
    }

    override fun dataInputStream(relativePath: String): InputStream {
        return zipFile.getInputStream(zipFile.getEntry(relativePath))
    }

}

