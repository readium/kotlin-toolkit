/*
 * Module: r2-streamer-kotlin
 * Developers: Aferdita Muriqi, Clément Baumann
 *
 * Copyright (c) 2018. Readium Foundation. All rights reserved.
 * Use of this source code is governed by a BSD-style license which is detailed in the
 * LICENSE file present in the project repository where this source code is maintained.
 */

package org.readium.r2.streamer.fetcher

import org.readium.r2.shared.Link
import org.readium.r2.shared.drm.DRM
import timber.log.Timber
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.util.zip.Inflater


class DrmDecoder {


    fun decoding(input: InputStream, resourceLink: Link, drm: DRM?): InputStream {

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
                            Timber.e(e)
                        }
                    }
                    try {
                        output.close()
                    } catch (e: Exception) {
                        Timber.e(e)
                    }
                    data = output.toByteArray()
                    
                }
                return ByteArrayInputStream(data)
            }
            return input

        } ?: run {
            return input
        }

    }


    private fun decipher(input: InputStream, drm: DRM): ByteArray? {
        val drmLicense = drm.license ?: return null
        val buffer = input.readBytes()
        return drmLicense.decipher(buffer)
    }


}

