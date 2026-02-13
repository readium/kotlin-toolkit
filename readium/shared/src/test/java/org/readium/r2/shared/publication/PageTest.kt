/*
 * Module: r2-shared-kotlin
 * Developers: Mickaël Menu
 *
 * Copyright (c) 2020. Readium Foundation. All rights reserved.
 * Use of this source code is governed by a BSD-style license which is detailed in the
 * LICENSE file present in the project repository where this source code is maintained.
 */

package org.readium.r2.shared.publication

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class PageTest {

    @Test fun `get page JSON value`() {
        assertEquals("left", Page.LEFT.value)
        assertEquals("right", Page.RIGHT.value)
        assertEquals("center", Page.CENTER.value)
    }

    @Test fun `parse page from JSON value`() {
        assertEquals(Page.LEFT, Page("left"))
        assertEquals(Page.RIGHT, Page("right"))
        assertEquals(Page.CENTER, Page("center"))
        assertNull(Page("foobar"))
        assertNull(Page(null))
    }

    @Test fun `get Properties {page} when available`() {
        assertEquals(
            Page.RIGHT,
            Properties(otherProperties = mapOf("page" to "right")).page
        )
    }

    @Test fun `get Properties {page} when missing`() {
        assertNull(Properties().page)
    }
}
