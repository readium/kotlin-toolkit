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
    val encryptionData: Map<Url, Encryption>,
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
                    CbcLcpResource(resource, license, encryption.originalLength)
            }
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
    private val license: LcpLicense,
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
internal class CbcLcpResource(
    resource: Resource,
    private val license: LcpLicense,
    private val originalLength: Long? = null,
) : Resource by resource {

    private val resource = CachingRangeTailResource(resource, 4 * AES_BLOCK_SIZE)

    private var builtinPaddingLength: Int? = null

    /** Plain text size. */
    override suspend fun length(): Try<Long, ReadError> {
        originalLength?.let { return Try.success(it) }

        val encryptedLength = resource.length()
            .getOrElse { return Try.failure(it) }

        if (encryptedLength == 0L) {
            return Try.success(0)
        }

        if (encryptedLength < 2 * AES_BLOCK_SIZE) {
            return Try.failure(
                ReadError.Decoding(
                    DebugError("Invalid CBC-encrypted stream.")
                )
            )
        }

        val paddingLength = builtinPaddingLength
            ?: readPaddingLength(encryptedLength)
                .onSuccess { builtinPaddingLength = it }
                .getOrElse { return Try.failure(it) }
            ?: 0 // encryptedLength is bullshit, we can't get paddingLength
        // Not sure if an inaccurate length is better than nothing,
        // but ExoPlayer can't work with no length at all.

        val originalLength = encryptedLength -
            AES_BLOCK_SIZE - // Minus IV
            paddingLength // Minus padding size

        return Try.success(originalLength)
    }

    private suspend fun readPaddingLength(
        encryptedSize: Long,
    ): Try<Int?, ReadError> {
        val readOffset = encryptedSize - (2 * AES_BLOCK_SIZE)

        // try to read one more byte past the end to check if this is the actual end
        val bytes = resource.read(readOffset until encryptedSize + 1)
            .getOrElse { return Try.failure(it) }

        if (bytes.size != 2 * AES_BLOCK_SIZE) {
            // encryptedSize must be bullshit, we cannot know the padding length
            return Try.success(null)
        }

        val decryptedBytes = license.decrypt(bytes)
            .getOrElse {
                return Try.failure(
                    ReadError.Decoding(
                        DebugError("Can't decrypt trailing size of CBC-encrypted stream")
                    )
                )
            }

        check(decryptedBytes.size == AES_BLOCK_SIZE)
        return Try.success(decryptedBytes.last().toInt())
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

        // range bounds must be multiple of AES_BLOCK_SIZE
        val startPadding = range.first - range.first.floorMultipleOf(AES_BLOCK_SIZE.toLong())
        val endPadding = (range.last + 1).ceilMultipleOf(AES_BLOCK_SIZE.toLong()) - (range.last + 1)

        // encrypted data is shifted by AES_BLOCK_SIZE because of IV and
        // the previous block must be provided to perform XOR on intermediate blocks
        // we also try to read one extra block to check if there is more data coming or we read the
        // last block
        val encryptedStart = range.first - startPadding
        val encryptedEndExclusive = range.last + 1 + endPadding + 2 * AES_BLOCK_SIZE
        val encryptedRangeSize = (encryptedEndExclusive - encryptedStart).toInt()

        val encryptedData = resource.read(encryptedStart until encryptedEndExclusive)
            .getOrElse { return Try.failure(it) }

        if (encryptedData.size % AES_BLOCK_SIZE != 0) {
            return Try.failure(
                ReadError.Decoding(
                    DebugError("Encrypted data size is not a multiple of AES block size.")
                )
            )
        }

        // Out of range request, there are no data to decrypt, only maybe a previous block.
        if (encryptedData.size < 2 * AES_BLOCK_SIZE) {
            return Try.success(ByteArray(0))
        }

        // Have we got all requested data with the extra block or less?
        val dataIncludesBuiltinPadding = encryptedData.size < encryptedRangeSize

        // We might not have got all data requested because range.last is allowed to be out of bounds.
        check(encryptedData.size <= encryptedRangeSize)
        val missingEndSize = encryptedRangeSize - encryptedData.size

        val bytes = license.decrypt(encryptedData)
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

        check(bytes.size == encryptedData.size - AES_BLOCK_SIZE)

        val builtinPaddingLength =
            if (dataIncludesBuiltinPadding) {
                bytes.last().toInt().also { this.builtinPaddingLength = it }
            } else {
                0
            }

        val correctedEndPadding = (endPadding.toInt() + AES_BLOCK_SIZE - missingEndSize)
            .coerceAtLeast(builtinPaddingLength)

        val dataSlice = startPadding.toInt() until bytes.size - correctedEndPadding

        return Try.success(bytes.sliceArray(dataSlice))
    }

    companion object {
        private const val AES_BLOCK_SIZE = 16 // bytes
    }
}

private suspend fun LcpLicense.decryptFully(
    data: Try<ByteArray, ReadError>,
    isDeflated: Boolean,
): Try<ByteArray, ReadError> =
    data.flatMap { encryptedData ->
        if (encryptedData.isEmpty()) {
            return Try.success(encryptedData)
        }

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

/**
 * A resource caching the last bytes read on range requests.
 *
 * Decryption needs to look one block back into the ciphered data. This means that in case of
 * contiguous read requests such as those you can expect from audio players, the data fetched from
 * the underlying resource are not contiguous: every range request to the underlying resource starts
 * one block before the end of the previous one.
 *
 * This is an issue with remote publications as you have to make a new HTTP request for each range
 * instead of reusing the previous one. To alleviate this, we cache the last bytes read in each
 * request and reuse them in the next one if possible.
 */
private class CachingRangeTailResource(
    private val resource: Resource,
    private val cacheLength: Int,
) : Resource by resource {

    private class Cache(
        var startIndex: Int?,
        val data: ByteArray,
    )

    private val cache: Cache = Cache(null, ByteArray(cacheLength))

    override suspend fun read(range: LongRange?): Try<ByteArray, ReadError> {
        if (range == null) {
            return resource.read(null)
        }

        @Suppress("NAME_SHADOWING")
        val range = range
            .coerceFirstNonNegative()
            .requireLengthFitInt()

        if (range.isEmpty()) {
            return Try.success(ByteArray(0))
        }

        // cache start index or null if we can't read from the cache
        val cacheStartIndex = cache.startIndex
            ?.takeIf { cacheStart ->
                val cacheEndExclusive = cacheStart + cacheLength
                val rangeBeginsInCache = range.first in cacheStart until cacheEndExclusive
                val rangeGoesBeyondCache = cacheEndExclusive <= range.last + 1
                rangeBeginsInCache && rangeGoesBeyondCache
            }

        val result =
            if (cacheStartIndex == null) {
                resource.read(range)
            } else {
                readWithCache(cacheStartIndex, cache.data, range)
            }

        // Cache the end of result if it's big enough
        if (result is Try.Success && result.value.size >= cacheLength) {
            val offsetInResult = result.value.size - cacheLength
            cache.startIndex = (range.last + 1 - cacheLength).toInt()
            result.value.copyInto(cache.data, 0, offsetInResult)
        }

        return result
    }

    private suspend fun readWithCache(
        cacheStartIndex: Int,
        cachedData: ByteArray,
        range: LongRange,
    ): Try<ByteArray, ReadError> {
        require(range.first >= cacheStartIndex)
        require(range.last + 1 >= cacheStartIndex + cachedData.size)

        val offsetInCache = (range.first - cacheStartIndex).toInt()
        val fromCacheLength = cachedData.size - offsetInCache

        val newData = resource.read(range.first + fromCacheLength..range.last)
            .getOrElse { return Try.failure((it)) }

        val result = ByteArray(fromCacheLength + newData.size)
        cachedData.copyInto(result, 0, offsetInCache)
        newData.copyInto(result, fromCacheLength)

        return Try.success(result)
    }
}

private val Encryption.isDeflated: Boolean get() =
    compression?.lowercase(java.util.Locale.ROOT) == "deflate"

private val Encryption.isCbcEncrypted: Boolean get() =
    algorithm == "http://www.w3.org/2001/04/xmlenc#aes256-cbc"

private fun Long.ceilMultipleOf(divisor: Long) =
    divisor * (this / divisor + if (this % divisor == 0L) 0 else 1)

private fun Long.floorMultipleOf(divisor: Long) =
    divisor * (this / divisor)
