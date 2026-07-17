package org.readium.r2.shared.publication

import org.junit.Assert
import org.junit.Test

class LayoutTest {

    @Test
    fun `parse layout`() {
        Assert.assertEquals(Layout.SCROLLED, Layout("scrolled"))
        Assert.assertEquals(Layout.FIXED, Layout("fixed"))
        Assert.assertEquals(Layout.REFLOWABLE, Layout("reflowable"))
        Assert.assertNull(Layout("foobar"))
        Assert.assertNull(Layout(null))
    }

    @Test
    fun `get layout value`() {
        Assert.assertEquals("scrolled", Layout.SCROLLED.value)

        Assert.assertEquals("fixed", Layout.FIXED.value)
        Assert.assertEquals("reflowable", Layout.REFLOWABLE.value)
    }
}
