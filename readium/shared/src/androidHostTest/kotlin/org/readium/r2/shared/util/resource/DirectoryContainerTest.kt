/*
 * Module: r2-shared-kotlin
 * Developers: Quentin Gliosca
 *
 * Copyright (c) 2020. Readium Foundation. All rights reserved.
 * Use of this source code is governed by a BSD-style license which is detailed in the
 * LICENSE file present in the project repository where this source code is maintained.
 */

package org.readium.r2.shared.util.resource

import java.nio.charset.StandardCharsets
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.readium.r2.shared.lengthBlocking
import org.readium.r2.shared.readBlocking
import org.readium.r2.shared.util.Url
import org.readium.r2.shared.util.checkSuccess
import org.readium.r2.shared.util.data.Container
import org.readium.r2.shared.util.file.DirectoryContainer
import org.readium.r2.shared.util.toAbsoluteUrl
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class DirectoryContainerTest {

    private val directory = assertNotNull(
        DirectoryContainerTest::class.java.getResource("directory")?.toAbsoluteUrl()?.toFile()
    )

    private fun sut(): Container<Resource> = runBlocking {
        assertNotNull(
            DirectoryContainer(directory).checkSuccess()
        )
    }

    @Test
    fun `Reading a missing file returns null`() {
        assertNull(sut()[Url("unknown")!!])
    }

    @Test
    fun `Reading a file at the root works well`() {
        val resource = assertNotNull(sut()[Url("text1.txt")!!])
        val result = resource.readBlocking().getOrNull()
        assertEquals("text1", result?.toString(StandardCharsets.UTF_8))
    }

    @Test
    fun `Reading a file in a subdirectory works well`() {
        val resource = assertNotNull(sut()[Url("subdirectory/text2.txt")!!])
        val result = resource.readBlocking().getOrNull()
        assertEquals("text2", result?.toString(StandardCharsets.UTF_8))
    }

    @Test
    fun `Reading a directory returns null`() {
        assertNull(sut()[Url("subdirectory")!!])
    }

    @Test
    fun `Reading a file outside the allowed directory returns null`() {
        assertNull(sut()[Url("../epub.epub")!!])
    }

    @Test
    fun `Reading a range works well`() {
        val resource = assertNotNull(sut()[Url("text1.txt")!!])
        val result = assertNotNull(resource.readBlocking(0..2L).getOrNull())
        assertEquals("tex", result.toString(StandardCharsets.UTF_8))
    }

    @Test
    fun `Reading two ranges with the same resource work well`() {
        val resource = assertNotNull(sut()[Url("text1.txt")!!])
        val result1 = resource.readBlocking(0..1L).getOrNull()
        assertEquals("te", result1?.toString(StandardCharsets.UTF_8))
        val result2 = resource.readBlocking(1..3L).getOrNull()
        assertEquals("ext", result2?.toString(StandardCharsets.UTF_8))
    }

    @Test
    fun `Out of range indexes are clamped to the available length`() {
        val resource = assertNotNull(sut()[Url("text1.txt")!!])
        val result = resource.readBlocking(-5..60L).getOrNull()
        assertEquals("text1", result?.toString(StandardCharsets.UTF_8))
        assertEquals(5, result?.size)
    }

    @Test
    @Suppress("EmptyRange")
    fun `Decreasing ranges are understood as empty ones`() {
        val resource = assertNotNull(sut()[Url("text1.txt")!!])
        val result = resource.readBlocking(60..20L).getOrNull()
        assertEquals("", result?.toString(StandardCharsets.UTF_8))
        assertEquals(0, result?.size)
    }

    @Test
    fun `Computing length works well`() {
        val resource = assertNotNull(sut().get(Url("text1.txt")!!))
        val result = resource.lengthBlocking().getOrNull()
        assertEquals(5L, result)
    }

    @Test
    fun `Computing entries works well`() {
        runBlocking {
            // FIXME: Test media types
            val entries = sut().entries
            assertThat(entries).contains(
                Url("subdirectory/hello.mp3")!!,
                Url("subdirectory/text2.txt")!!,
                Url("text1.txt")!!
            )
        }
    }
}
