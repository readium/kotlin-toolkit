/*
 * Module: r2-streamer-kotlin
 * Developers: Quentin Gliosca
 *
 * Copyright (c) 2020. Readium Foundation. All rights reserved.
 * Use of this source code is governed by a BSD-style license which is detailed in the
 * LICENSE file present in the project repository where this source code is maintained.
 */

package org.readium.r2.streamer.extensions

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.readium.r2.shared.util.Url
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ContainerTest {

    @Test
    fun `pathCommonFirstComponent is null when files are in the root`() {
        assertNull(
            listOf(Url("im1.jpg")!!, Url("im2.jpg")!!, Url("toc.xml")!!)
                .pathCommonFirstComponent()
        )
    }

    @Test
    fun `pathCommonFirstComponent is null when files are in different directories`() {
        assertNull(
            listOf(Url("dir1/im1.jpg")!!, Url("dir2/im2.jpg")!!, Url("toc.xml")!!)
                .pathCommonFirstComponent()
        )
    }

    @Test
    fun `pathCommonFirstComponent is correct when there is only one file in the root`() {
        assertEquals(
            "im1.jpg",
            listOf(Url("im1.jpg")!!).pathCommonFirstComponent()?.name
        )
    }

    @Test
    fun `pathCommonFirstComponent is correct when all files are in the same directory`() {
        assertEquals(
            "root",
            listOf(
                Url("root/im1.jpg")!!,
                Url("root/im2.jpg")!!,
                Url("root/xml/toc.xml")!!
            ).pathCommonFirstComponent()?.name
        )
    }
}
