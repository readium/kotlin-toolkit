/*
 * Module: r2-lcp-kotlin
 * Developers: Quentin Gliosca
 *
 * Copyright (c) 2020. Readium Foundation. All rights reserved.
 * Use of this source code is governed by a BSD-style license which is detailed in the
 * LICENSE file present in the project repository where this source code is maintained.
 */

@file:Suppress("unused")
@file:OptIn(InternalReadiumApi::class)

package org.readium.r2.lcp

import kotlin.math.ceil
import org.readium.r2.shared.InternalReadiumApi
import org.readium.r2.shared.extensions.coerceIn
import org.readium.r2.shared.publication.Publication
import org.readium.r2.shared.util.ErrorException
import org.readium.r2.shared.util.Try
import org.readium.r2.shared.util.checkSuccess
import org.readium.r2.shared.util.getOrElse
import org.readium.r2.shared.util.getOrThrow
import org.readium.r2.shared.util.resource.Resource
import org.readium.r2.shared.util.use
import timber.log.Timber

internal suspend fun Publication.checkDecryption() {
    checkResourcesAreReadableInOneBlock(this)

    checkLengthComputationIsCorrect(this)

    checkAllResourcesAreReadableByChunks(this)

    checkExceedingRangesAreAllowed(this)
}

internal suspend fun checkResourcesAreReadableInOneBlock(publication: Publication) {
    Timber.d("checking resources are readable in one block")

    (publication.readingOrder + publication.resources)
        .forEach { link ->
            Timber.d("attempting to read ${link.href} in one block")
            publication.get(link)!!.use { resource ->
                val bytes = resource.read()
                check(bytes.isSuccess) { "failed to read ${link.href} in one block" }
            }
        }
}

internal suspend fun checkLengthComputationIsCorrect(publication: Publication) {
    Timber.d("checking length computation is correct")

    (publication.readingOrder + publication.resources)
        .forEach { link ->
            val trueLength = publication.get(link)!!.use { it.read().checkSuccess().size.toLong() }
            publication.get(link)!!.use { resource ->
                resource.length()
                    .onFailure {
                        throw IllegalStateException(
                            "failed to compute length of ${link.href}",
                            ErrorException(it)
                        )
                    }.onSuccess {
                        check(it == trueLength) { "computed length of ${link.href} seems to be wrong" }
                    }
            }
        }
}

internal suspend fun checkAllResourcesAreReadableByChunks(publication: Publication) {
    Timber.d("checking all resources are readable by chunks")

    (publication.readingOrder + publication.resources)
        .forEach { link ->
            Timber.d("attempting to read ${link.href} by chunks ")
            val groundTruth = publication.get(link)!!.use { it.read() }.checkSuccess()
            for (chunkSize in listOf(4096L, 2050L)) {
                publication.get(link).use { resource ->
                    resource!!.readByChunks(chunkSize, groundTruth).onFailure {
                        throw IllegalStateException(
                            "failed to read ${link.href} by chunks of size $chunkSize",
                            it
                        )
                    }
                }
            }
        }
}

internal suspend fun checkExceedingRangesAreAllowed(publication: Publication) {
    Timber.d("checking exceeding ranges are allowed")

    (publication.readingOrder + publication.resources)
        .forEach { link ->
            publication.get(link).use { resource ->
                val length = resource!!.length().checkSuccess()
                val fullTruth = resource.read().checkSuccess()
                for (
                range in listOf(
                    0 until length + 100,
                    0 until length + 2048,
                    length - 500 until length + 200,
                    length until length + 5028,
                    length + 200 until length + 500
                )
                ) {
                    resource.read(range)
                        .onFailure {
                            throw IllegalStateException(
                                "unable to decrypt range $range from ${link.href}"
                            )
                        }.onSuccess {
                            val coercedRange = range.coerceIn(0L until fullTruth.size)
                            val truth = fullTruth.sliceArray(
                                coercedRange.first.toInt()..coercedRange.last.toInt()
                            )
                            check(it.contentEquals(truth)) {
                                Timber.d("decrypted length: ${it.size}")
                                Timber.d("expected length: ${truth.size}")
                                "decrypted range $range of ${link.href} whose length is $length invalid"
                            }
                        }
                }
            }
        }
}

internal suspend fun Resource.readByChunks(
    chunkSize: Long,
    groundTruth: ByteArray,
    shuffle: Boolean = true,
) =
    try {
        val length = length()
            .mapFailure { ErrorException(it) }
            .getOrThrow()

        val blockNb = ceil(length / chunkSize.toDouble()).toInt()
        val blocks = (0 until blockNb)
            .map { Pair(it, it * chunkSize until kotlin.math.min(length, (it + 1) * chunkSize)) }
            .toMutableList()

        if (blocks.size > 1 && shuffle) {
            // Forbid the true order
            while (blocks.map(Pair<Int, LongRange>::first) == (0 until blockNb).toList())
                blocks.shuffle()
        }

        Timber.d("blocks $blocks")
        blocks.forEach {
            Timber.d("block index ${it.first}: ${it.second}")
            val decryptedBytes = read(it.second).getOrElse { error ->
                throw IllegalStateException(
                    "unable to decrypt chunk ${it.second} from $sourceUrl",
                    ErrorException(error)
                )
            }
            check(decryptedBytes.isNotEmpty()) { "empty decrypted bytearray" }
            check(decryptedBytes.contentEquals(groundTruth.sliceArray(it.second.map(Long::toInt)))) {
                Timber.d("decrypted length: ${decryptedBytes.size}")
                Timber.d(
                    "expected length: ${groundTruth.sliceArray(it.second.map(Long::toInt)).size}"
                )
                "decrypted chunk ${it.first}: ${it.second} seems to be wrong in $sourceUrl"
            }
            Pair(it.first, decryptedBytes)
        }
        Try.success(Unit)
    } catch (e: Exception) {
        Try.failure(e)
    }
