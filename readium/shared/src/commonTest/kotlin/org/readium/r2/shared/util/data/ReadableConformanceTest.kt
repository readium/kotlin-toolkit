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
import org.readium.r2.shared.util.FileExtension
import org.readium.r2.shared.util.Url
import org.readium.r2.shared.util.file.FileResource
import org.readium.r2.shared.util.format.Format
import org.readium.r2.shared.util.format.FormatSpecification
import org.readium.r2.shared.util.format.Specification
import org.readium.r2.shared.util.mediatype.MediaType
import org.readium.r2.shared.util.zip.FileZipArchiveProvider
import org.readium.r2.shared.util.zip.StreamingZipArchiveProvider
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
        AbsoluteUrl.fromFilePath(Fixtures("zip").path("epub.epub").toString())
    )

    private val bytes = ByteArray(5000) { (it % 251).toByte() }

    // 342 KB, deflated: large enough to require several chunks and to skip the entry cache.
    private val zipEntryHref = "EPUB/s04.xhtml"

    // 392 bytes: small enough to hit the whole-entry cache branch.
    private val smallEntryHref = "EPUB/cover.xhtml"

    /** Flips every bit, so untransformed bytes are detectable. */
    private fun flip(data: ByteArray): ByteArray =
        ByteArray(data.size) { (data[it].toInt().inv() and 0xFF).toByte() }

    private fun transform(resource: Resource): Resource =
        TransformingResource(resource) { Try.success(flip(it)) }

    // Streaming and file-based ZIP entries, which have the most intricate `stream()` of the set:
    // forward-seek reuse, a container-wide mutex, and an optional whole-entry cache.
    private suspend fun zipEntry(streaming: Boolean, href: String): Resource {
        val container = if (streaming) {
            StreamingZipArchiveProvider().openFile(fileUrl)
        } else {
            requireNotNull(FileZipArchiveProvider().open(epubFormat, fileUrl).getOrNull())
        }
        return requireNotNull(container[requireNotNull(Url(href))])
    }

    private val epubFormat = Format(
        specification = FormatSpecification(Specification.Zip, Specification.Epub),
        mediaType = MediaType.EPUB,
        fileExtension = FileExtension("epub")
    )

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

    // ZIP entries need a suspending factory, so they are exercised separately.
    private val zipSubjects: Map<String, suspend () -> Resource> = mapOf(
        "StreamingZipContainer.Entry" to { zipEntry(streaming = true, href = zipEntryHref) },
        "FileZipContainer.Entry" to { zipEntry(streaming = false, href = zipEntryHref) },
        // Small enough to exercise the whole-entry cache branch.
        "StreamingZipContainer.Entry(cached)" to { zipEntry(streaming = true, href = smallEntryHref) }
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
    fun zipEntriesAgreeBetweenStreamAndRead() = runTest {
        for ((name, factory) in zipSubjects) {
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
