package org.readium.r2.shared.publication

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class LayoutTest {

    @Test
    fun `parse layout`() {
        assertEquals(Layout.SCROLLED, Layout("scrolled"))
        assertEquals(Layout.FIXED, Layout("fixed"))
        assertEquals(Layout.REFLOWABLE, Layout("reflowable"))
        assertNull(Layout("foobar"))
        assertNull(Layout(null))
    }

    @Test
    fun `get layout value`() {
        assertEquals("scrolled", Layout.SCROLLED.value)

        assertEquals("fixed", Layout.FIXED.value)
        assertEquals("reflowable", Layout.REFLOWABLE.value)
    }
}
