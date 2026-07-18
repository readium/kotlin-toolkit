/*
 * Copyright 2026 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.shared.util.data

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlinx.coroutines.test.runTest
import org.readium.r2.shared.Fixtures
import org.readium.r2.shared.util.ImageSize

class DecodeImageTest {

    // 500x714 PNG.
    private val coverBytes: ByteArray =
        Fixtures("resource").read("epub/EPUB/images/cover.png").toByteArray()

    @Test
    fun decodeImageAtFullSize() = runTest {
        val image = assertNotNull(coverBytes.decodeImage().getOrNull())
        assertEquals(500, image.width)
        assertEquals(714, image.height)
    }

    @Test
    fun decodeImageFittingABox() = runTest {
        val image = assertNotNull(coverBytes.decodeImage(maxSize = ImageSize(250, 250)).getOrNull())
        assertTrue(image.width <= 250, "width ${image.width} does not fit the box")
        assertTrue(image.height <= 250, "height ${image.height} does not fit the box")
        // Preserves the aspect ratio: the height is the limiting dimension.
        // Allow one pixel of leeway for platform rounding differences.
        assertTrue(image.height in 249..250, "unexpected height ${image.height}")
        assertTrue(image.width in 174..176, "unexpected width ${image.width}")
    }

    @Test
    fun decodeImageLargerBoxReturnsOriginalSize() = runTest {
        val image = assertNotNull(coverBytes.decodeImage(maxSize = ImageSize(1000, 1000)).getOrNull())
        assertEquals(500, image.width)
        assertEquals(714, image.height)
    }

    @Test
    fun decodeInvalidImageFails() = runTest {
        val result = "Not an image".encodeToByteArray().decodeImage()
        assertTrue(result.isFailure)
    }
}
