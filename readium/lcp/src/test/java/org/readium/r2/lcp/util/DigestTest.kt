/*
 * Copyright 2023 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.lcp.util

import java.io.File
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import org.junit.Test

class DigestTest {

    private val file: File =
        File(DigestTest::class.java.getResource("a-fc.jpg")!!.path)

    @OptIn(ExperimentalEncodingApi::class, ExperimentalStdlibApi::class)
    @Test
    fun `sha256 is correct`() {
        val digest = assertNotNull(file.sha256())
        assertEquals("GI42TOamBYJ4q4KKBcmMzlkfvld8bTVRcbjjQ20OvLI=", Base64.encode(digest))
        assertEquals(
            "188e364ce6a6058278ab828a05c98cce591fbe577c6d355171b8e3436d0ebcb2",
            digest.toHexString()
        )
    }
}
