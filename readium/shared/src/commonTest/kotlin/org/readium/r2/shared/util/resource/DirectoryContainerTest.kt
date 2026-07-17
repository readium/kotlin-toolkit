/*
 * Module: r2-shared-kotlin
 * Developers: Quentin Gliosca
 *
 * Copyright (c) 2020. Readium Foundation. All rights reserved.
 * Use of this source code is governed by a BSD-style license which is detailed in the
 * LICENSE file present in the project repository where this source code is maintained.
 */

package org.readium.r2.shared.util.resource

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.coroutines.test.runTest
import org.readium.r2.shared.Fixtures
import org.readium.r2.shared.util.AbsoluteUrl
import org.readium.r2.shared.util.Url
import org.readium.r2.shared.util.checkSuccess
import org.readium.r2.shared.util.data.Container
import org.readium.r2.shared.util.file.DirectoryContainer
import org.readium.r2.shared.util.file.FileSystemError
import org.readium.r2.shared.util.fromFilePath

class DirectoryContainerTest {

    private val directory: AbsoluteUrl = assertNotNull(
        AbsoluteUrl.fromFilePath(
            Fixtures("resource").path("directory").toString(),
            isDirectory = true
        )
    )

    private suspend fun sut(): Container<Resource> =
        assertNotNull(
            DirectoryContainer(directory).checkSuccess()
        )

    @Test
    fun `Reading a missing file returns null`() = runTest {
        assertNull(sut()[Url("unknown")!!])
    }

    @Test
    fun `Reading a file at the root works well`() = runTest {
        val resource = assertNotNull(sut()[Url("text1.txt")!!])
        val result = resource.read().getOrNull()
        assertEquals("text1", result?.decodeToString())
    }

    @Test
    fun `Reading a file in a subdirectory works well`() = runTest {
        val resource = assertNotNull(sut()[Url("subdirectory/text2.txt")!!])
        val result = resource.read().getOrNull()
        assertEquals("text2", result?.decodeToString())
    }

    @Test
    fun `Reading a directory returns null`() = runTest {
        assertNull(sut()[Url("subdirectory")!!])
    }

    @Test
    fun `Reading a file outside the allowed directory returns null`() = runTest {
        assertNull(sut()[Url("../epub.epub")!!])
    }

    @Test
    fun `Reading a range works well`() = runTest {
        val resource = assertNotNull(sut()[Url("text1.txt")!!])
        val result = assertNotNull(resource.read(0..2L).getOrNull())
        assertEquals("tex", result.decodeToString())
    }

    @Test
    fun `Reading two ranges with the same resource work well`() = runTest {
        val resource = assertNotNull(sut()[Url("text1.txt")!!])
        val result1 = resource.read(0..1L).getOrNull()
        assertEquals("te", result1?.decodeToString())
        val result2 = resource.read(1..3L).getOrNull()
        assertEquals("ext", result2?.decodeToString())
    }

    @Test
    fun `Out of range indexes are clamped to the available length`() = runTest {
        val resource = assertNotNull(sut()[Url("text1.txt")!!])
        val result = resource.read(-5..60L).getOrNull()
        assertEquals("text1", result?.decodeToString())
        assertEquals(5, result?.size)
    }

    @Test
    @Suppress("EmptyRange")
    fun `Decreasing ranges are understood as empty ones`() = runTest {
        val resource = assertNotNull(sut()[Url("text1.txt")!!])
        val result = resource.read(60..20L).getOrNull()
        assertEquals("", result?.decodeToString())
        assertEquals(0, result?.size)
    }

    @Test
    fun `Computing length works well`() = runTest {
        val resource = assertNotNull(sut().get(Url("text1.txt")!!))
        val result = resource.length().getOrNull()
        assertEquals(5L, result)
    }

    @Test
    fun `Opening a missing root directory fails with FileNotFound`() = runTest {
        val missingRoot = assertNotNull(
            AbsoluteUrl.fromFilePath(
                Fixtures("resource").path("missing-directory").toString(),
                isDirectory = true
            )
        )
        val error = DirectoryContainer(missingRoot).failureOrNull()
        assertIs<FileSystemError.FileNotFound>(error)
    }

    @Test
    fun `Computing entries works well`() = runTest {
        val entries = sut().entries
        assertTrue(
            entries.containsAll(
                listOf(
                    Url("subdirectory/hello.mp3")!!,
                    Url("subdirectory/text2.txt")!!,
                    Url("text1.txt")!!
                )
            )
        )
    }
}
