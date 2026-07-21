/*
 * Copyright 2026 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.shared.util.data

import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertTrue
import kotlinx.coroutines.test.runTest
import org.readium.r2.shared.Fixtures
import org.readium.r2.shared.util.AbsoluteUrl
import org.readium.r2.shared.util.Try
import org.readium.r2.shared.util.checkSuccess
import org.readium.r2.shared.util.file.FileResource
import org.readium.r2.shared.util.fromFilePath
import org.readium.r2.shared.util.resource.BufferingResource
import org.readium.r2.shared.util.resource.FallbackResource
import org.readium.r2.shared.util.resource.InMemoryResource
import org.readium.r2.shared.util.resource.LazyResource
import org.readium.r2.shared.util.resource.Resource
import org.readium.r2.shared.util.resource.SynchronizedResource
import org.readium.r2.shared.util.resource.TransformingResource
import org.readium.r2.shared.util.resource.borrow
import org.readium.r2.shared.util.use

/**
 * `stream()` and `read()` must agree, for every [Readable] implementation.
 *
 * This is the guard against the delegation hazard: Kotlin's `by` satisfies an abstract member
 * automatically, so a decorator that transforms bytes can silently forward `stream()` to its
 * wrapped source and serve untransformed data with no compile error and no runtime signal.
 */
class ReadableConformanceTest {

    private val fileUrl: AbsoluteUrl = requireNotNull(
        AbsoluteUrl.fromFilePath(Fixtures("resource").path("epub.epub").toString())
    )

    private val bytes = ByteArray(5000) { (it % 251).toByte() }

    /** Flips every bit, so untransformed bytes are detectable. */
    private fun flip(data: ByteArray): ByteArray =
        ByteArray(data.size) { (data[it].toInt().inv() and 0xFF).toByte() }

    private fun transform(resource: Resource): Resource =
        TransformingResource(resource) { Try.success(flip(it)) }

    private val subjects: Map<String, () -> Resource> = mapOf(
        "FileResource" to { FileResource(fileUrl) },
        "InMemoryResource" to { InMemoryResource(bytes) },
        "BufferingResource" to { BufferingResource(FileResource(fileUrl), bufferSize = 1024) },
        "SynchronizedResource" to { SynchronizedResource(FileResource(fileUrl)) },
        "LazyResource" to { LazyResource { FileResource(fileUrl) } },
        "BorrowedResource" to { FileResource(fileUrl).borrow() },
        "FallbackResource" to { FallbackResource(FileResource(fileUrl)) { null } },
        "TransformingResource" to { transform(FileResource(fileUrl)) },
        "TransformingResource(buffered)" to {
            transform(BufferingResource(FileResource(fileUrl), bufferSize = 1024))
        },
        "TransformingResource(in-memory)" to { transform(InMemoryResource(bytes)) }
    )

    private val ranges: List<LongRange?> = listOf(
        null,
        0L..0L,
        0L..99L,
        100L..1099L,
        1L..2L,
        // Deliberately past the end: out-of-range indexes are clamped, not an error.
        4900L..6000L,
        0L..100_000L
    )

    private suspend fun Readable.streamed(range: LongRange?): ByteArray {
        val chunks = mutableListOf<ByteArray>()
        stream(range) { chunks.add(it) }.checkSuccess()
        return chunks.join()
    }

    @Test
    fun streamAndReadAgree() = runTest {
        for ((name, factory) in subjects) {
            for (range in ranges) {
                val read = factory().use { it.read(range).checkSuccess() }
                val streamed = factory().use { it.streamed(range) }

                assertContentEquals(
                    read,
                    streamed,
                    "$name disagrees between read() and stream() for range $range"
                )
            }
        }
    }

    @Test
    fun transformingResourceStreamsTransformedBytes() = runTest {
        // The specific regression the abstract `stream()` exists to prevent: a `Resource by`
        // decorator serving the wrapped source's bytes.
        val original = InMemoryResource(bytes).use { it.read().checkSuccess() }
        val streamed = transform(InMemoryResource(bytes)).use { it.streamed(null) }

        assertTrue(
            !original.contentEquals(streamed),
            "TransformingResource streamed untransformed bytes"
        )
        assertContentEquals(flip(bytes), streamed)
    }

    @Test
    fun streamEmitsNothingForAnEmptyRange() = runTest {
        for ((name, factory) in subjects) {
            val chunks = mutableListOf<ByteArray>()
            factory().use { it.stream(10L..9L) { chunk -> chunks.add(chunk) }.checkSuccess() }

            assertTrue(
                chunks.sumOf { it.size } == 0,
                "$name emitted bytes for an empty range"
            )
        }
    }
}
