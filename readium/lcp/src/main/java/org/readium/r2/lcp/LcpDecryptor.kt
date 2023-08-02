/*
 * Module: r2-lcp-kotlin
 * Developers: Mickaël Menu
 *
 * Copyright (c) 2020. Readium Foundation. All rights reserved.
 * Use of this source code is governed by a BSD-style license which is detailed in the
 * LICENSE file present in the project repository where this source code is maintained.
 */

package org.readium.r2.lcp

import java.io.IOException
import org.readium.r2.shared.error.Try
import org.readium.r2.shared.error.getOrElse
import org.readium.r2.shared.error.getOrThrow
import org.readium.r2.shared.extensions.coerceFirstNonNegative
import org.readium.r2.shared.extensions.inflate
import org.readium.r2.shared.extensions.requireLengthFitInt
import org.readium.r2.shared.publication.encryption.Encryption
import org.readium.r2.shared.resource.FailureResource
import org.readium.r2.shared.resource.Resource
import org.readium.r2.shared.resource.ResourceTry
import org.readium.r2.shared.resource.TransformingResource
import org.readium.r2.shared.resource.flatMap
import org.readium.r2.shared.resource.flatMapCatching
import org.readium.r2.shared.resource.mapCatching
import org.readium.r2.shared.util.Url

/**
 * Decrypts a resource protected with LCP.
 */
internal class LcpDecryptor(
    val license: LcpLicense?,
    var retrieveEncryption: (Url) -> Encryption? = { null }
) {

    fun transform(resource: Resource): Resource =
        resource.flatMap {
            val encryption = resource.source?.let(retrieveEncryption)

            // Checks if the resource is encrypted and whether the encryption schemes of the resource
            // and the DRM license are the same.
            if (encryption == null || encryption.scheme != "http://readium.org/2014/01/lcp") {
                return@flatMap resource
            }

            when {
                license == null -> FailureResource(Resource.Exception.Forbidden())
                encryption.isDeflated || !encryption.isCbcEncrypted -> FullLcpResource(resource, encryption, license)
                else -> CbcLcpResource(resource, encryption, license)
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
        private val encryption: Encryption,
        private val license: LcpLicense
    ) : TransformingResource(resource) {

        override suspend fun transform(data: ResourceTry<ByteArray>): ResourceTry<ByteArray> =
            license.decryptFully(data, encryption.isDeflated)

        override suspend fun length(): ResourceTry<Long> =
            encryption.originalLength?.let { Try.success(it) }
                ?: super.length()
    }

    /**
     * A LCP resource used to read content encrypted with the CBC algorithm.
     *
     * Supports random access for byte range requests, but the resource MUST NOT be deflated.
     */
    private class CbcLcpResource(
        private val resource: Resource,
        private val encryption: Encryption,
        private val license: LcpLicense
    ) : Resource by resource {

        override val source: Url? = null

        private class Cache(
            var startIndex: Int? = null,
            val data: ByteArray = ByteArray(3 * AES_BLOCK_SIZE)
        )

        private lateinit var _length: ResourceTry<Long>

        /*
        * Decryption needs to look around the data strictly matching the content to decipher.
        * That means that in case of contiguous read requests, data fetched from the underlying
        * resource are not contiguous. Every request to the underlying resource starts slightly
        * before the end of the previous one. This is an issue with remote publications because
        * you have to make a new HTTP request every time instead of reusing the previous one.
        * To alleviate this, we cache the three last bytes read in each call and reuse them
        * in the next call if possible.
        */
        private val _cache: Cache = Cache()

        /** Plain text size. */
        override suspend fun length(): ResourceTry<Long> {
            if (::_length.isInitialized)
                return _length

            _length = encryption.originalLength?.let { Try.success(it) }
                ?: lengthFromPadding()

            return _length
        }

        private suspend fun lengthFromPadding(): ResourceTry<Long> =
            resource.length().flatMapCatching { length ->
                if (length < 2 * AES_BLOCK_SIZE) {
                    throw Exception("Invalid CBC-encrypted stream")
                }

                val readOffset = length - (2 * AES_BLOCK_SIZE)
                resource.read(readOffset..length)
                    .mapCatching { bytes ->
                        val decryptedBytes = license.decrypt(bytes)
                            .getOrElse { throw Exception("Can't decrypt trailing size of CBC-encrypted stream", it) }
                        check(decryptedBytes.size == AES_BLOCK_SIZE)

                        return@mapCatching length -
                            AES_BLOCK_SIZE - // Minus IV
                            decryptedBytes.last().toInt() // Minus padding size
                    }
            }

        override suspend fun read(range: LongRange?): ResourceTry<ByteArray> {
            if (range == null)
                return license.decryptFully(resource.read(), isDeflated = false)

            @Suppress("NAME_SHADOWING")
            val range = range
                .coerceFirstNonNegative()
                .requireLengthFitInt()

            if (range.isEmpty())
                return Try.success(ByteArray(0))

            return resource.length().flatMapCatching { encryptedLength ->
                // encrypted data is shifted by AES_BLOCK_SIZE because of IV and
                // the previous block must be provided to perform XOR on intermediate blocks
                val encryptedStart = range.first.floorMultipleOf(AES_BLOCK_SIZE.toLong())
                val encryptedEndExclusive = (range.last + 1).ceilMultipleOf(AES_BLOCK_SIZE.toLong()) + AES_BLOCK_SIZE

                getEncryptedData(encryptedStart until encryptedEndExclusive).mapCatching { encryptedData ->
                    if (encryptedData.size >= _cache.data.size) {
                        // cache the three last encrypted blocks that have been read for future use
                        val cacheStart = encryptedData.size - _cache.data.size
                        _cache.startIndex = (encryptedEndExclusive - _cache.data.size).toInt()
                        encryptedData.copyInto(_cache.data, 0, cacheStart)
                    }

                    val bytes = license.decrypt(encryptedData)
                        .getOrElse { throw IOException("Can't decrypt the content for resource with key: ${resource.source}", it) }

                    // exclude the bytes added to match a multiple of AES_BLOCK_SIZE
                    val sliceStart = (range.first - encryptedStart).toInt()

                    // was the last block read to provide the desired range
                    val lastBlockRead = encryptedLength - encryptedEndExclusive <= AES_BLOCK_SIZE

                    val rangeLength =
                        if (lastBlockRead)
                        // use decrypted length to ensure range.last doesn't exceed decrypted length - 1
                            range.last.coerceAtMost(length().getOrThrow() - 1) - range.first + 1
                        else
                        // the last block won't be read, so there's no need to compute length
                            range.last - range.first + 1

                    // keep only enough bytes to fit the length corrected request in order to never include padding
                    val sliceEnd = sliceStart + rangeLength.toInt()

                    bytes.sliceArray(sliceStart until sliceEnd)
                }
            }
        }

        private suspend fun getEncryptedData(range: LongRange): ResourceTry<ByteArray> {
            val cacheStartIndex = _cache.startIndex
                ?.takeIf { cacheStart ->
                    val cacheEnd = cacheStart + _cache.data.size
                    range.first in cacheStart until cacheEnd && cacheEnd <= range.last + 1
                } ?: return resource.read(range)

            val bytes = ByteArray(range.last.toInt() - range.first.toInt() + 1)
            val offsetInCache = (range.first - cacheStartIndex).toInt()
            val fromCacheLength = _cache.data.size - offsetInCache

            return resource.read(range.first + fromCacheLength..range.last).map {
                _cache.data.copyInto(bytes, 0, offsetInCache)
                it.copyInto(bytes, fromCacheLength)
                bytes
            }
        }

        companion object {
            private const val AES_BLOCK_SIZE = 16 // bytes
        }
    }
}

private suspend fun LcpLicense.decryptFully(data: ResourceTry<ByteArray>, isDeflated: Boolean): ResourceTry<ByteArray> =
    data.mapCatching { encryptedData ->
        // Decrypts the resource.
        var bytes = decrypt(encryptedData)
            .getOrElse { throw Exception("Failed to decrypt the resource", it) }

        if (bytes.isEmpty())
            throw IllegalStateException("Lcp.nativeDecrypt returned an empty ByteArray")

        // Removes the padding.
        val padding = bytes.last().toInt()
        bytes = bytes.copyOfRange(0, bytes.size - padding)

        // If the ressource was compressed using deflate, inflates it.
        if (isDeflated) {
            bytes = bytes.inflate(nowrap = true)
        }

        bytes
    }

private val Encryption.isDeflated: Boolean get() =
    compression?.lowercase(java.util.Locale.ROOT) == "deflate"

private val Encryption.isCbcEncrypted: Boolean get() =
    algorithm == "http://www.w3.org/2001/04/xmlenc#aes256-cbc"

private fun Long.ceilMultipleOf(divisor: Long) =
    divisor * (this / divisor + if (this % divisor == 0L) 0 else 1)

private fun Long.floorMultipleOf(divisor: Long) =
    divisor * (this / divisor)
