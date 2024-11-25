/*
 * Module: r2-lcp-kotlin
 * Developers: Mickaël Menu
 *
 * Copyright (c) 2020. Readium Foundation. All rights reserved.
 * Use of this source code is governed by a BSD-style license which is detailed in the
 * LICENSE file present in the project repository where this source code is maintained.
 */

@file:OptIn(InternalReadiumApi::class)

package org.readium.r2.lcp

import org.readium.r2.shared.InternalReadiumApi
import org.readium.r2.shared.extensions.coerceFirstNonNegative
import org.readium.r2.shared.extensions.inflate
import org.readium.r2.shared.extensions.requireLengthFitInt
import org.readium.r2.shared.publication.encryption.Encryption
import org.readium.r2.shared.util.DebugError
import org.readium.r2.shared.util.ThrowableError
import org.readium.r2.shared.util.Try
import org.readium.r2.shared.util.Url
import org.readium.r2.shared.util.data.ReadError
import org.readium.r2.shared.util.flatMap
import org.readium.r2.shared.util.getEquivalent
import org.readium.r2.shared.util.getOrElse
import org.readium.r2.shared.util.resource.FailureResource
import org.readium.r2.shared.util.resource.Resource
import org.readium.r2.shared.util.resource.TransformingResource
import org.readium.r2.shared.util.resource.flatMap

/**
 * Decrypts a resource protected with LCP.
 */
internal class LcpDecryptor(
    val license: LcpLicense?,
    val encryptionData: Map<Url, Encryption>
) {

    fun transform(url: Url, resource: Resource): Resource {
        return resource.flatMap {
            val encryption = encryptionData.getEquivalent(url)

            // Checks if the resource is encrypted and whether the encryption schemes of the resource
            // and the DRM license are the same.
            if (encryption == null || encryption.scheme != "http://readium.org/2014/01/lcp") {
                return@flatMap resource
            }

            when {
                license == null ->
                    FailureResource(
                        ReadError.Decoding(
                            DebugError(
                                "Cannot decipher content because the publication is locked."
                            )
                        )
                    )
                encryption.isDeflated || !encryption.isCbcEncrypted ->
                    FullLcpResource(resource, encryption, license)
                else ->
                    CbcLcpResource(resource, license)
            }
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

        override suspend fun transform(data: Try<ByteArray, ReadError>): Try<ByteArray, ReadError> =
            license.decryptFully(data, encryption.isDeflated)

        override suspend fun length(): Try<Long, ReadError> =
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
        private val license: LcpLicense
    ) : Resource by resource {

        private class Cache(
            var startIndex: Int? = null,
            val data: ByteArray = ByteArray(3 * AES_BLOCK_SIZE)
        )

        private lateinit var _length: Try<Long, ReadError>

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
        override suspend fun length(): Try<Long, ReadError> {
            if (::_length.isInitialized) {
                return _length
            }

            // Unfortunately, encryption.originalLength is not reliable.
            _length = lengthFromPadding()

            return _length
        }

        private suspend fun lengthFromPadding(): Try<Long, ReadError> {
            val length = resource.length()
                .getOrElse { return Try.failure(it) }

            if (length < 2 * AES_BLOCK_SIZE) {
                return Try.failure(
                    ReadError.Decoding(
                        DebugError("Invalid CBC-encrypted stream.")
                    )
                )
            }

            val readOffset = length - (2 * AES_BLOCK_SIZE)
            val bytes = resource.read(readOffset..length)
                .getOrElse { return Try.failure(it) }

            return lengthFromLastTwoBlocks(length, bytes)
        }

        private suspend fun lengthFromLastTwoBlocks(
            cipheredLength: Long,
            lastTwoBlocks: ByteArray
        ): Try<Long, ReadError> {
            require(lastTwoBlocks.size == 2 * AES_BLOCK_SIZE)

            val decryptedBytes = license.decrypt(lastTwoBlocks)
                .getOrElse {
                    return Try.failure(
                        ReadError.Decoding(
                            DebugError("Can't decrypt trailing size of CBC-encrypted stream")
                        )
                    )
                }

            check(decryptedBytes.size == AES_BLOCK_SIZE)

            val adjustedLength = cipheredLength -
                AES_BLOCK_SIZE - // Minus IV
                decryptedBytes.last().toInt() // Minus padding size

            return if (adjustedLength >= 0) {
                Try.success(adjustedLength)
            } else {
                Try.failure(
                    ReadError.Decoding(
                        DebugError("Padding length seems invalid.")
                    )
                )
            }
        }

        override suspend fun read(range: LongRange?): Try<ByteArray, ReadError> {
            if (range == null) {
                return license.decryptFully(resource.read(), isDeflated = false)
            }

            @Suppress("NAME_SHADOWING")
            val range = range
                .coerceFirstNonNegative()
                .requireLengthFitInt()

            if (range.isEmpty()) {
                return Try.success(ByteArray(0))
            }

            val rangeSize = range.last + 1 - range.first

            val encryptedLength = resource.length()
                .getOrElse { return Try.failure(it) }

            // range bounds must be multiple of AES_BLOCK_SIZE and
            val startPadding = range.first - range.first.floorMultipleOf(AES_BLOCK_SIZE.toLong())
            val endPadding = (range.last + 1).ceilMultipleOf(AES_BLOCK_SIZE.toLong()) - range.last - 1

            // encrypted data is shifted by AES_BLOCK_SIZE because of IV and
            // the previous block must be provided to perform XOR on intermediate blocks
            val encryptedStart = range.first - startPadding
            val encryptedEndExclusive = range.last + 1 + endPadding + AES_BLOCK_SIZE

            val encryptedData = getEncryptedData(encryptedStart until encryptedEndExclusive)
                .getOrElse { return Try.failure(it) }

            val bytes = license.decrypt(encryptedData)
                .onSuccess {
                    check(it.isEmpty() || it.size == encryptedData.size - AES_BLOCK_SIZE)
                }
                .getOrElse {
                    return Try.failure(
                        ReadError.Decoding(
                            DebugError(
                                "Can't decrypt the content for resource with key: ${resource.sourceUrl}",
                                it
                            )
                        )
                    )
                }

            // was the last block read to provide the desired range
            val lastBlockRead = encryptedLength - encryptedEndExclusive <= AES_BLOCK_SIZE

            val dataSlice =
                if (lastBlockRead) {
                    val decryptedLength =
                        if (::_length.isInitialized) {
                            _length
                        } else {
                            val lastTwoBlocks = encryptedData.sliceArray(
                                encryptedData.size - 2 until encryptedData.size
                            )
                            lengthFromLastTwoBlocks(encryptedLength, lastTwoBlocks)
                                .onSuccess { _length = Try.success(it) }
                        }.getOrElse { return Try.failure(it) }

                    // use decrypted length to ensure range.last doesn't exceed decrypted length - 1
                    val dataLength = (range.last + 1).coerceAtMost(decryptedLength) - range.first

                    // keep only enough bytes to fit the length corrected request in order to never include padding
                    val sliceEnd = startPadding + dataLength.toInt()

                    startPadding.toInt() until sliceEnd.toInt()
                } else {
                    // the last block was not read, so there's no need to compute decrypted length

                    // bytes contains deciphered data for startPadding, then for the requested
                    // range, and then for endPadding
                    // the requested range might have been far too large, in which case bytes doesn't
                    // content all of that data
                    // if there are any data for endPadding, it begins at endPaddingStartIndex.
                    val endPaddingStartIndex = (startPadding + rangeSize).coerceAtMost(
                        bytes.size.toLong()
                    )
                    startPadding.toInt() until endPaddingStartIndex.toInt()
                }

            return Try.success(bytes.sliceArray(dataSlice))
        }

        /**
         * Reads encrypted data using the cache when suitable.
         */
        private suspend fun getEncryptedData(range: LongRange): Try<ByteArray, ReadError> {
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
            }.onSuccess { result ->
                if (result.size >= _cache.data.size) {
                    // cache the three last encrypted blocks that have been read for future use
                    val cacheStart = result.size - _cache.data.size
                    _cache.startIndex = (range.last + 1 - _cache.data.size).toInt()
                    result.copyInto(_cache.data, 0, cacheStart)
                }
            }
        }

        companion object {
            private const val AES_BLOCK_SIZE = 16 // bytes
        }
    }
}

private suspend fun LcpLicense.decryptFully(
    data: Try<ByteArray, ReadError>,
    isDeflated: Boolean
): Try<ByteArray, ReadError> =
    data.flatMap { encryptedData ->
        // Decrypts the resource.
        var bytes = decrypt(encryptedData)
            .getOrElse {
                return Try.failure(
                    ReadError.Decoding(
                        DebugError("Failed to decrypt the resource", it)
                    )
                )
            }

        if (bytes.isEmpty()) {
            throw IllegalStateException("Lcp.nativeDecrypt returned an empty ByteArray")
        }

        // Removes the padding.
        val padding = bytes.last().toInt()
        if (padding !in bytes.indices) {
            return Try.failure(
                ReadError.Decoding(
                    DebugError(
                        "The padding length of the encrypted resource is incorrect: $padding / ${bytes.size}"
                    )
                )
            )
        }
        bytes = bytes.copyOfRange(0, bytes.size - padding)

        // If the resource was compressed using deflate, inflates it.
        if (isDeflated) {
            bytes = bytes.inflate(nowrap = true)
                .getOrElse {
                    return Try.failure(
                        ReadError.Decoding(
                            DebugError("Cannot deflate the decrypted resource", ThrowableError(it))
                        )
                    )
                }
        }

        Try.success(bytes)
    }

private val Encryption.isDeflated: Boolean get() =
    compression?.lowercase(java.util.Locale.ROOT) == "deflate"

private val Encryption.isCbcEncrypted: Boolean get() =
    algorithm == "http://www.w3.org/2001/04/xmlenc#aes256-cbc"

private fun Long.ceilMultipleOf(divisor: Long) =
    divisor * (this / divisor + if (this % divisor == 0L) 0 else 1)

private fun Long.floorMultipleOf(divisor: Long) =
    divisor * (this / divisor)
