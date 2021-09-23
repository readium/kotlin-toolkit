/*
 * Module: r2-streamer-kotlin
 * Developers: Quentin Gliosca
 *
 * Copyright (c) 2020. Readium Foundation. All rights reserved.
 * Use of this source code is governed by a BSD-style license which is detailed in the
 * LICENSE file present in the project repository where this source code is maintained.
 */

package org.readium.r2.streamer.extensions

import org.junit.Test
import org.readium.r2.shared.publication.Link
import kotlin.test.assertEquals
import kotlin.test.assertNull

class LinkTest {

    @Test
    fun `hrefCommonFirstComponent is null when files are in the root`() {
        assertNull(
            listOf(Link("/im1.jpg"), Link("/im2.jpg"), Link("/toc.xml"))
                .hrefCommonFirstComponent()
        )
    }

    @Test
    fun `hrefCommonFirstComponent is null when files are in different directories`() {
           assertNull(
            listOf(Link("/dir1/im1.jpg"), Link("/dir2/im2.jpg"), Link("/toc.xml"))
                .hrefCommonFirstComponent()
        )
    }


    @Test
    fun `hrefCommonFirstComponent is correct when there is only one file in the root`() {
        assertEquals(
            "im1.jpg",
            listOf(Link("/im1.jpg")).hrefCommonFirstComponent()?.name
        )
    }

    @Test
    fun `hrefCommonFirstComponent is correct when all files are in the same directory`() {
        assertEquals(
            "root",
            listOf(
                Link("/root/im1.jpg"),
                Link("/root/im2.jpg"),
                Link("/root/xml/toc.xml")
            ).hrefCommonFirstComponent()?.name
        )
    }

}
