/*
 * Copyright 2022 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.navigator.epub.css

import kotlin.test.assertEquals
import org.junit.Test
import org.readium.r2.shared.ExperimentalReadiumApi

@OptIn(ExperimentalReadiumApi::class)
class LayoutTest {

    @Test
    fun `Compute the HTML dir from the stylesheets`() {
        assertEquals(Layout.HtmlDir.Ltr, Layout.Stylesheets.Default.htmlDir)
        assertEquals(Layout.HtmlDir.Rtl, Layout.Stylesheets.Rtl.htmlDir)
        assertEquals(Layout.HtmlDir.Unspecified, Layout.Stylesheets.CjkVertical.htmlDir)
        assertEquals(Layout.HtmlDir.Ltr, Layout.Stylesheets.CjkHorizontal.htmlDir)
    }
}
