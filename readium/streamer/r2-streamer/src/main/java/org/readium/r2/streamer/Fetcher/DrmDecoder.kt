package org.readium.r2.streamer.Fetcher

import org.readium.r2.shared.Drm
import org.readium.r2.shared.Link
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.util.zip.Inflater


class DrmDecoder {

    fun decoding(input: InputStream, resourceLink: Link, drm: Drm) : InputStream {
        val encryption = resourceLink.properties?.encryption ?: return input
        val scheme = encryption.scheme ?: return input
        var data = decipher(input, drm) ?: return input
        if (scheme != drm.scheme)
            return input
        if (resourceLink.properties!!.encryption?.compression == "deflate"){
            val padding = data[data.size - 1].toInt()
            data = data.copyOfRange(0, data.size - padding)
            val inflater = Inflater()
            inflater.setInput(data)
            val output = ByteArrayOutputStream(data.size)
            val buf = ByteArray(1024)
            while (!inflater.finished()) {
                try {
                    val count = inflater.inflate(buf)
                    output.write(buf, 0, count)
                } catch (e: Exception) { }
            }
            try {
                output.close()
            } catch (e: Exception) { }
            data = output.toByteArray()
        }
        return data.inputStream()
    }

    private fun decipher(input: InputStream, drm: Drm): ByteArray? {
        val drmLicense = drm.license ?: return null
        val buffer = input.readBytes()
        return drmLicense.decipher(buffer)
    }

}