/*
 * Module: r2-streamer-kotlin
 * Developers: Aferdita Muriqi, Clément Baumannn, Quentin Gliosca
 *
 * Copyright (c) 2020. Readium Foundation. All rights reserved.
 * Use of this source code is governed by a BSD-style license which is detailed in the
 * LICENSE file present in the project repository where this source code is maintained.
 */

package org.readium.r2.streamer.parser.epub

import com.mcxiaoke.koi.HASH
import com.mcxiaoke.koi.ext.toHexBytes
import org.readium.r2.shared.fetcher.ProxyResource
import org.readium.r2.shared.fetcher.Resource
import org.readium.r2.shared.fetcher.ResourceTry
import org.readium.r2.shared.fetcher.mapCatching
import org.readium.r2.shared.publication.encryption.encryption
import kotlin.experimental.xor

internal class EpubDeobfuscator(private val pubId: String) {

    fun transform(resource: Resource): Resource = DeobfuscatingResource(resource)

    inner class DeobfuscatingResource(resource: Resource): ProxyResource(resource) {

        override suspend fun read(range: LongRange?): ResourceTry<ByteArray> {
            val algorithm = resource.link().properties.encryption?.algorithm

            if (algorithm !in algorithm2length.keys)
                return resource.read(range)

            return resource.read(range).mapCatching {
                val obfuscationLength: Int = algorithm2length[algorithm]!!
                val obfuscationKey: ByteArray = when (algorithm) {
                    "http://ns.adobe.com/pdf/enc#RC" -> getHashKeyAdobe(pubId)
                    else -> HASH.sha1(pubId).toHexBytes()
                }

                deobfuscate(it, range, obfuscationKey, obfuscationLength)
                it
            }
        }
    }

    private val algorithm2length: Map<String, Int> = mapOf(
        "http://www.idpf.org/2008/embedding" to 1040,
        "http://ns.adobe.com/pdf/enc#RC" to 1024
    )

    private fun deobfuscate(bytes: ByteArray, range: LongRange?, obfuscationKey: ByteArray, obfuscationLength: Int) {
        @Suppress("NAME_SHADOWING")
        val range = range ?: (0L until bytes.size)
        val toDeobfuscate = Math.max(range.start, 0L) .. Math.min(range.last, obfuscationLength - 1L)
        for (i in toDeobfuscate.map { it.toInt() })
            bytes[i] = bytes[i].xor(obfuscationKey[i % obfuscationKey.size])
    }

    private fun getHashKeyAdobe(pubId: String) =
        pubId.replace("urn:uuid:", "")
            .replace("-", "")
            .toHexBytes()
}