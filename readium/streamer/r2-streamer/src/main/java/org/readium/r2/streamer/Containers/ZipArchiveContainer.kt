package org.readium.r2.streamer.Containers

import android.renderscript.ScriptGroup
import android.util.Log
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.util.zip.ZipFile

interface ZipArchiveContainer: Container {
    var zipFile: ZipFile

    override fun data(relativePath: String) : ByteArray{

        val zipEntry = zipFile.getEntry(relativePath)// ?: return ByteArray(0)
        val fis = zipFile.getInputStream(zipEntry)
        val buffer = ByteArrayOutputStream()
        var nRead: Int
        val data = ByteArray(16384)

        nRead = fis.read(data, 0, data.size)
        while (nRead != -1) {
            buffer.write(data, 0, nRead)
            nRead = fis.read(data, 0, data.size)
        }
        buffer.flush()
        return buffer.toByteArray()
    }

    override fun dataLength(relativePath: String) =
            zipFile.size().toLong()

    override fun dataInputStream(relativePath: String) =
            zipFile.getInputStream(zipFile.getEntry(relativePath))

}

