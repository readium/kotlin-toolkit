package org.readium.r2.streamer.Containers

import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileInputStream

interface DirectoryContainer : Container {

    override fun data(relativePath: String) : ByteArray {
        val filePath = rootFile.toString() + "/" + relativePath
        val epubFile = File(filePath)

        if (!epubFile.exists())
            throw Exception("Missing File")

        val buffer = ByteArrayOutputStream()
        var nRead: Int
        val data = ByteArray(16384)
        val fis = FileInputStream(epubFile)

        do {
            nRead = fis.read(data, 0, data.size)
            buffer.write(data, 0, nRead)
        } while (nRead != -1)

        buffer.flush()

        return buffer.toByteArray()
    }

    override fun dataLength(relativePath: String) =
            File(rootFile.toString() + "/" + relativePath).length()

    override fun dataInputStream(relativePath: String) =
            FileInputStream(File(rootFile.toString() + "/" + relativePath))
}

