/*
 * Module: r2-shared-kotlin
 * Developers: Quentin Gliosca
 *
 * Copyright (c) 2020. Readium Foundation. All rights reserved.
 * Use of this source code is governed by a BSD-style license which is detailed in the
 * LICENSE file present in the project repository where this source code is maintained.
 */

package org.readium.r2.shared.extensions

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Size
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

@RunWith(AndroidJUnit4::class)
@Config(sdk = [28])
class BitmapTest {

    private val bitmap: Bitmap

    init {
        val url = BitmapTest::class.java.getResource("image.jpg")
        assertNotNull(url)
        val bytes = url.readBytes()
        bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
    }


    @Test
    fun `scaleToFit returns the same Bitmap when its size is ok`() {
        assertTrue(bitmap.scaleToFit(bitmap.size).sameAs(bitmap))
    }

    @Test
    fun `scaled bitmap has correct size`() {
        val scaled = bitmap.scaleToFit(Size(300, 400))
        assertEquals(400, scaled.height)
        assertEquals(299, scaled.width)
    }
}

