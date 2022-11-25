/*
 * Module: r2-streamer-kotlin
 * Developers: Quentin Gliosca
 *
 * Copyright (c) 2020. Readium Foundation. All rights reserved.
 * Use of this source code is governed by a BSD-style license which is detailed in the
 * LICENSE file present in the project repository where this source code is maintained.
 */

package org.readium.r2.streamer.extensions

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Test

class FileTest {

    @Test
    fun `firstComponent works well when {File} is a directory`() {
        assertEquals("dir", File("/dir").firstComponent.name)
    }

    @Test
    fun `firstComponent works well when {File} is a file at root`() {
        assertEquals("image.jpg", File("/image.jpg").firstComponent.name)
    }

    @Test
    fun `firstComponent works well when {File} is a file inside a directory`() {
        assertEquals("dir1", File("/dir1/dir2/image.jpg").firstComponent.name)
    }
}
