/*
 * Module: r2-streamer-kotlin
 * Developers: Aferdita Muriqi, Clément Baumann, Quentin Gliosca
 *
 * Copyright (c) 2020. Readium Foundation. All rights reserved.
 * Use of this source code is governed by a BSD-style license which is detailed in the
 * LICENSE file present in the project repository where this source code is maintained.
 */

package org.readium.r2.streamer.fetcher

import org.readium.r2.shared.drm.DRM
import org.readium.r2.shared.fetcher.Resource
import org.readium.r2.shared.publication.Link
import org.readium.r2.shared.publication.encryption.encryption
import org.readium.r2.shared.util.Try
import org.readium.r2.streamer.BuildConfig
import timber.log.Timber
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.util.zip.Inflater

internal class DecryptionTransformer(val drm: DRM) {

    fun transform(resource: Resource): Resource {
        val encryption = resource.link.properties.encryption ?: return resource
        if (encryption.scheme != drm.scheme.rawValue) return resource
        return decipher(resource)
    }

    private fun decipher(resource: Resource): Resource =
        object: Resource {
            override val link: Link = resource.link

            override val length: Try<Long, Resource.Error>
                get () {
                    resource.link.properties.encryption?.originalLength?.let { return Try.success(it) }
                    return read().map { it.size.toLong() }
                }

            override fun read(range: LongRange?): Try<ByteArray, Resource.Error> {
                val link = resource.link
                val cipheredBytes = resource.read().let { it.successOrNull() ?: return it }
                val stream = ByteArrayInputStream(cipheredBytes)
                val unciphered = decipher(stream, drm)
                return if (unciphered == null)
                    Try.failure(Resource.Error.Forbidden)
                else {
                    var data = unciphered
                    val padding = data[data.size - 1].toInt()
                    data.copyOfRange(0, data.size - padding)
                    val compression = link.properties.encryption?.compression
                    if (compression == "deflate")
                        data = deflate(data)
                    Try.success(data)
                }
            }

            override fun close() = resource.close()

        }

    private fun decipher(input: InputStream, drm: DRM): ByteArray? {
        val drmLicense = drm.license ?: return null
        val buffer = input.readBytes()
        return drmLicense.decipher(buffer)
    }

    private fun deflate(bytes: ByteArray): ByteArray {
        val inflater = Inflater(true)
        inflater.setInput(bytes)
        val output = ByteArrayOutputStream(bytes.size)
        val buf = ByteArray(1024)
        while (!inflater.finished()) {
            try {
                val count = inflater.inflate(buf)
                output.write(buf, 0, count)
            } catch (e: Exception) {
                if (BuildConfig.DEBUG) Timber.e(e)
            }
        }
        try {
            output.close()
        } catch (e: Exception) {
            if (BuildConfig.DEBUG) Timber.e(e)
        }

        return output.toByteArray()
    }

}