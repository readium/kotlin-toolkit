/*
 * Module: r2-shared-kotlin
 * Developers: Mickaël Menu
 *
 * Copyright (c) 2020. Readium Foundation. All rights reserved.
 * Use of this source code is governed by a BSD-style license which is detailed in the
 * LICENSE file present in the project repository where this source code is maintained.
 */

package org.readium.r2.shared.format

import org.junit.Assert.*
import org.junit.Test

class FormatTest {

    @Test
    fun `equals checks only media type`() {
        assertEquals(
            Format(name = "B", mediaType = MediaType.PNG, fileExtension = "b"),
            Format(name = "A", mediaType = MediaType.PNG, fileExtension = "a")
        )
        assertNotEquals(
            Format(name = "A", mediaType = MediaType.JPEG, fileExtension = "a"),
            Format(name = "A", mediaType = MediaType.PNG, fileExtension = "a")
        )
    }

}