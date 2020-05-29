/*
 * Module: r2-streamer-kotlin
 * Developers: Aferdita Muriqi, Clément Baumannn, Quentin Gliosca
 *
 * Copyright (c) 2020. Readium Foundation. All rights reserved.
 * Use of this source code is governed by a BSD-style license which is detailed in the
 * LICENSE file present in the project repository where this source code is maintained.
 */

package org.readium.r2.streamer.fetcher

import com.mcxiaoke.koi.HASH
import com.mcxiaoke.koi.ext.toHexBytes
import org.readium.r2.shared.fetcher.Resource
import org.readium.r2.shared.publication.Link
import org.readium.r2.shared.publication.encryption.encryption
import org.readium.r2.shared.util.Try
import kotlin.experimental.xor

internal class DeobfuscationTransformer(private val pubId: String) {

    private val algorithm2length = mapOf(
        "http://www.idpf.org/2008/embedding" to 1040,
        "http://ns.adobe.com/pdf/enc#RC" to 1024
    )


    fun transform(resource: Resource): Resource {
        val link = resource.link
        val encryption = link.properties.encryption ?: return resource
        val algorithm = encryption.algorithm
        if (algorithm !in algorithm2length.keys) return resource

        val obfuscationLength: Int = algorithm2length[algorithm]!!
        val obfuscationKey: ByteArray = when (algorithm) {
            "http://ns.adobe.com/pdf/enc#RC" -> getHashKeyAdobe(pubId)
                else -> HASH.sha1(pubId).toHexBytes()
        }

        return deobfuscate(resource, obfuscationKey, obfuscationLength)
    }

    private fun deobfuscate(resource: Resource, obfuscationKey: ByteArray, obfuscationLength: Int): Resource =
        object: Resource by resource {
            override val length: Try<Long, Resource.Error> = resource.length

            override fun read(range: LongRange?): Try<ByteArray, Resource.Error> = resource.read(range).map {
                @Suppress("NAME_SHADOWING")
                val range = range ?: (0L until it.size)
                val toDeobfuscate = Math.max(range.start, 0L) .. Math.min(range.last, obfuscationLength - 1L)
                for (i in toDeobfuscate.map { it.toInt() })
                    it[i] = it[i].xor(obfuscationKey[i % obfuscationKey.size])
                it
            }
        }

    private fun getHashKeyAdobe(pubId: String) =
        pubId.replace("urn:uuid:", "")
            .replace("-", "")
            .toHexBytes()

}