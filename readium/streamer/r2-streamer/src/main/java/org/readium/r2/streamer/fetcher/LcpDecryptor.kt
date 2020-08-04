/*
 * Module: r2-streamer-kotlin
 * Developers: Mickaël Menu
 *
 * Copyright (c) 2020. Readium Foundation. All rights reserved.
 * Use of this source code is governed by a BSD-style license which is detailed in the
 * LICENSE file present in the project repository where this source code is maintained.
 */

package org.readium.r2.streamer.fetcher

import org.readium.r2.shared.drm.DRM
import org.readium.r2.shared.drm.DRMLicense
import org.readium.r2.shared.extensions.inflate
import org.readium.r2.shared.fetcher.TransformingResource
import org.readium.r2.shared.fetcher.Resource
import org.readium.r2.shared.fetcher.ResourceTry
import org.readium.r2.shared.fetcher.LazyResource
import org.readium.r2.shared.fetcher.flatMapCatching
import org.readium.r2.shared.fetcher.mapCatching
import org.readium.r2.shared.publication.Link
import org.readium.r2.shared.publication.encryption.encryption
import org.readium.r2.shared.util.Try
import java.io.IOException

/**
 * Decrypts a resource protected with LCP.
 */
internal class LcpDecryptor(val drm: DRM) {

    fun transform(resource: Resource): Resource = LazyResource {
        // Checks if the resource is encrypted and whether the encryption schemes of the resource
        // and the DRM license are the same.
        val license = drm.license
        val link = resource.link()
        val encryption = link.properties.encryption
        if (license == null || encryption == null || encryption.scheme != drm.scheme.rawValue)
            return@LazyResource resource

        when {
            link.isDeflated || !link.isCbcEncrypted -> FullLcpResource(resource, license)
            else -> CbcLcpResource(resource, license)
        }
    }

    /**
     * A  LCP resource that is read, decrypted and cached fully before reading requested ranges.
     *
     * Can be used when it's impossible to map a read range (byte range request) to the encrypted
     * resource, for example when the resource is deflated before encryption.
     */
    private class FullLcpResource(
        resource: Resource,
        private val license: DRMLicense
    ) : TransformingResource(resource) {

        override suspend fun transform(data: ResourceTry<ByteArray>): ResourceTry<ByteArray> =
            license.decryptFully(data, resource.link().isDeflated)

        override suspend fun length(): ResourceTry<Long> =
            resource.link().properties.encryption?.originalLength
                ?.let { Try.success(it) }
                ?: super.length()
    }

    /**
     * A LCP resource used to read content encrypted with the CBC algorithm.
     *
     * Supports random access for byte range requests, but the resource MUST NOT be deflated.
     */
    private class CbcLcpResource(
        private val resource: Resource,
        private val license: DRMLicense
    ) : Resource {

        override suspend fun link(): Link = resource.link()

        /** Plain text size. */
        override suspend fun length(): ResourceTry<Long> =
            resource.length().flatMapCatching { length ->
                if (length < 2 * AES_BLOCK_SIZE) {
                    throw Exception("Invalid CBC-encrypted stream")
                }

                val readOffset = length - (2 * AES_BLOCK_SIZE)
                resource.read(readOffset..length)
                    .mapCatching { bytes ->
                        val decryptedBytes = license.decipher(bytes)
                            ?: throw Exception("Can't decrypt trailing size of CBC-encrypted stream")

                        return@mapCatching length -
                                AES_BLOCK_SIZE -  // Minus IV or previous block
                                (AES_BLOCK_SIZE - decryptedBytes.size) % AES_BLOCK_SIZE  // Minus padding part
                    }
            }

        override suspend fun read(range: LongRange?): ResourceTry<ByteArray> {
            return if (range == null) {
                license.decryptFully(resource.read(), isDeflated = false)
            } else {
                resource.length().flatMapCatching { length ->
                    val blockPosition = range.first % AES_BLOCK_SIZE

                    // For beginning of the cipher text, IV used for XOR.
                    // For cipher text in the middle, previous block used for XOR.
                    val readPosition = range.first - blockPosition

                    // Count blocks to read.
                    // First block for IV or previous block to perform XOR.
                    var blocksCount: Long = 1
                    var bytesInFirstBlock = (AES_BLOCK_SIZE - blockPosition) % AES_BLOCK_SIZE
                    if (length < bytesInFirstBlock) {
                        bytesInFirstBlock = 0
                    }
                    if (bytesInFirstBlock > 0) {
                        blocksCount += 1
                    }

                    blocksCount += (length - bytesInFirstBlock) / AES_BLOCK_SIZE
                    if ((length - bytesInFirstBlock) % AES_BLOCK_SIZE != 0L) {
                        blocksCount += 1
                    }

                    val readSize = blocksCount * AES_BLOCK_SIZE
                    resource.read(readPosition..(readPosition + readSize))
                        .mapCatching {
                            var bytes = license.decipher(it)
                                ?: throw IOException("Can't decrypt the content at: ${link().href}")

                            if (bytes.size > length) {
                                bytes = bytes.copyOfRange(0, length.toInt())
                            }

                            bytes
                        }
                }
            }
        }

        override suspend fun close() = resource.close()

        companion object {
            private const val AES_BLOCK_SIZE = 16 // bytes
        }

    }
}

private fun DRMLicense.decryptFully(data:  ResourceTry<ByteArray>, isDeflated: Boolean): ResourceTry<ByteArray> =
    data.mapCatching {
        // Decrypts the resource.
        var bytes = decipher(it)
            ?.takeIf { b -> b.isNotEmpty() }
            ?: throw Exception("Failed to decrypt the resource")

        // Removes the padding.
        val padding = bytes.last().toInt()
        bytes = bytes.copyOfRange(0, bytes.size - padding)

        // If the ressource was compressed using deflate, inflates it.
        if (isDeflated) {
            bytes = bytes.inflate(nowrap = true)
        }

        bytes
    }

private val Link.isDeflated: Boolean get() =
    properties.encryption?.compression?.toLowerCase(java.util.Locale.ROOT) == "deflate"

private val Link.isCbcEncrypted: Boolean get() =
    properties.encryption?.algorithm == "http://www.w3.org/2001/04/xmlenc#aes256-cbc"
