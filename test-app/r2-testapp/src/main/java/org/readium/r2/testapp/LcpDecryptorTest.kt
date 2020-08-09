package org.readium.r2.testapp

import org.readium.r2.shared.extensions.coerceIn
import org.readium.r2.shared.fetcher.Resource
import org.readium.r2.shared.fetcher.mapCatching
import org.readium.r2.shared.publication.Publication
import org.readium.r2.shared.util.getOrElse
import timber.log.Timber
import java.lang.IllegalStateException
import kotlin.math.ceil

suspend fun Publication.checkDecryption() {

    checkResourcesAreReadableInOneBlock(this)

    checkLengthComputationIsCorrect(this)

    checkAllResourcesAreReadableByChunks(this)

    checkExceedingRangesAreAllowed(this)
}

private suspend fun checkResourcesAreReadableInOneBlock(publication: Publication) {
    Timber.d("checking resources are readable in one block")

    (publication.readingOrder + publication.resources)
        .forEach { link ->
            Timber.d("attempting to read ${link.href} in one block")
            publication.get(link).use { resource ->
                val bytes = resource.read()
                check(bytes.isSuccess) { "failed to read ${link.href} in one block" }
            }
        }
}

private suspend fun checkLengthComputationIsCorrect(publication: Publication) {
    Timber.d("checking length computation is correct")

    (publication.readingOrder + publication.resources)
        .forEach { link ->
            val trueLength = publication.get(link).use { it.read().getOrThrow().size.toLong() }
            publication.get(link).use { resource ->
                resource.length()
                    .onFailure {
                        throw IllegalStateException("failed to compute length of ${link.href}", it)
                    }.onSuccess {
                        check(it == trueLength) { "computed length of ${link.href} seems to be wrong" }
                    }
            }
        }
}

private suspend fun checkAllResourcesAreReadableByChunks(publication: Publication) {
    Timber.d("checking all resources are readable by chunks")

    (publication.readingOrder + publication.resources)
        .forEach { link ->
            Timber.d("attempting to read ${link.href} by chunks ")
            val groundTruth = publication.get(link).use { it.read() }.getOrThrow()
            for (chunkSize in listOf(4096L, 2050L)) {
                publication.get(link).use { resource ->
                    resource.readByChunks(chunkSize, groundTruth).onFailure {
                        throw IllegalStateException("failed to read ${link.href} by chunks of size $chunkSize", it)
                    }
                }
            }
        }
}

private suspend fun checkExceedingRangesAreAllowed(publication: Publication) {
    Timber.d("checking exceeding ranges are allowed")

    (publication.readingOrder + publication.resources)
        .forEach { link ->
            publication.get(link).use { resource ->
                val length = resource.length().getOrThrow()
                val fullTruth = resource.read().getOrThrow()
                for (range in listOf(
                    0 until length + 100,
                    0 until length + 2048,
                    length - 500 until length + 200,
                    length until length + 5028,
                    length + 200 until length + 500
                )) {
                    resource.read(range)
                        .onFailure {
                            throw IllegalStateException("unable to decrypt range $range from ${link.href}")
                        }.onSuccess {
                            val coercedRange = range.coerceIn(0L until fullTruth.size)
                            val truth = fullTruth.sliceArray(coercedRange.first.toInt()..coercedRange.last.toInt())
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

private suspend fun Resource.readByChunks(chunkSize: Long, groundTruth: ByteArray, shuffle: Boolean = true) =
    length().mapCatching { length ->
        val blockNb =  ceil(length / chunkSize.toDouble()).toInt()
        val blocks = (0 until blockNb)
            .map { Pair(it, it * chunkSize until kotlin.math.min(length, (it + 1)  * chunkSize)) }
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
                throw IllegalStateException("unable to decrypt chunk ${it.second} from ${link().href}", error)
            }
            check(decryptedBytes.isNotEmpty()) { "empty decrypted bytearray"}
            check(decryptedBytes.contentEquals(groundTruth.sliceArray(it.second.map(Long::toInt))))
            {   Timber.d("decrypted length: ${decryptedBytes.size}")
                Timber.d("expected length: ${groundTruth.sliceArray(it.second.map(Long::toInt)).size}")
                "decrypted chunk ${it.first}: ${it.second} seems to be wrong in ${link().href}"
            }
            Pair(it.first, decryptedBytes)
        }
    }