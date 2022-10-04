package org.readium.r2.navigator.epub.css

import org.junit.Test
import kotlin.test.assertEquals

class LayoutTest {

    @Test
    fun `Compute the HTML dir from the stylesheets`() {
        assertEquals(Layout.HtmlDir.Ltr, Layout.Stylesheets.Default.htmlDir)
        assertEquals(Layout.HtmlDir.Rtl, Layout.Stylesheets.Rtl.htmlDir)
        assertEquals(Layout.HtmlDir.Unspecified, Layout.Stylesheets.CjkVertical.htmlDir)
        assertEquals(Layout.HtmlDir.Ltr, Layout.Stylesheets.CjkHorizontal.htmlDir)
    }
}
