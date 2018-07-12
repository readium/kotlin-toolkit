/*
 * Copyright 2018 Readium Foundation. All rights reserved.
 */

package org.readium.r2.streamer.Fetcher

import android.util.Log
import org.readium.r2.shared.drm.Drm
import org.readium.r2.shared.Link
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.util.zip.Inflater


class DrmDecoder {

    private val TAG = this::class.java.simpleName

    fun decoding(input: InputStream, resourceLink: Link, drm: Drm?): InputStream {

        val encryption = resourceLink.properties.encryption?.let {
            return@let it
        } ?: run {
            return input
        }

        val scheme = resourceLink.properties.encryption?.scheme?.let {
            return@let it
        } ?: run {
            return input
        }

        drm?.let {

            if (scheme == drm.scheme) {

                var data = decipher(input, drm) ?: return input

                if (resourceLink.properties.encryption?.compression == "deflate") {

                    val padding = data[data.size - 1].toInt()
                    data = data.copyOfRange(0, data.size - padding)
                    val inflater = Inflater(true)
                    inflater.setInput(data)
                    val output = ByteArrayOutputStream(data.size)
                    val buf = ByteArray(1024)
                    while (!inflater.finished()) {
                        try {
                            val count = inflater.inflate(buf)
                            output.write(buf, 0, count)
                        } catch (e: Exception) {
                            Log.e("output.write", e.message)
                        }
                    }
                    try {
                        output.close()
                    } catch (e: Exception) {
                        Log.e("output.close", e.message)
                    }
                    data = output.toByteArray()
                    return ByteArrayInputStream(data)

                }
                return input
            }
            return input

        } ?: run {
            return input
        }

    }


    private fun decipher(input: InputStream, drm: Drm): ByteArray? {
        val drmLicense = drm.license ?: return null
        val buffer = input.readBytes()
        return drmLicense.decipher(buffer)
    }


}

