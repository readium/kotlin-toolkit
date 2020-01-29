/*
 * Module: r2-shared-kotlin
 * Developers: Mickaël Menu
 *
 * Copyright (c) 2020. Readium Foundation. All rights reserved.
 * Use of this source code is governed by a BSD-style license which is detailed in the
 * LICENSE file present in the project repository where this source code is maintained.
 */

package org.readium.r2.shared.publication

import org.junit.Assert.*
import org.junit.Test

class ContentLayoutTest {

    @Test fun `parse fallbacks on LTR`() {
        assertEquals(ContentLayout.LTR, ContentLayout.from("foobar"))
    }

    @Test fun `parse fallbacks on the provided {readingProgression}`() {
        assertEquals(ContentLayout.RTL, ContentLayout.from("foobar", ReadingProgression.RTL))
    }

    @Test fun `parse from RTL language`() {
        assertEquals(ContentLayout.RTL, ContentLayout.from("AR"))
        assertEquals(ContentLayout.RTL, ContentLayout.from("FA"))
        assertEquals(ContentLayout.RTL, ContentLayout.from("HE"))
    }

    @Test fun `parse from CJK language`() {
        assertEquals(ContentLayout.CJK_HORIZONTAL, ContentLayout.from("ZH"))
        assertEquals(ContentLayout.CJK_HORIZONTAL, ContentLayout.from("JA"))
        assertEquals(ContentLayout.CJK_HORIZONTAL, ContentLayout.from("KO"))
        assertEquals(ContentLayout.CJK_HORIZONTAL, ContentLayout.from("ZH", ReadingProgression.LTR))
        assertEquals(ContentLayout.CJK_HORIZONTAL, ContentLayout.from("JA", ReadingProgression.LTR))
        assertEquals(ContentLayout.CJK_HORIZONTAL, ContentLayout.from("KO", ReadingProgression.LTR))
        assertEquals(ContentLayout.CJK_VERTICAL, ContentLayout.from("ZH", ReadingProgression.RTL))
        assertEquals(ContentLayout.CJK_VERTICAL, ContentLayout.from("JA", ReadingProgression.RTL))
        assertEquals(ContentLayout.CJK_VERTICAL, ContentLayout.from("KO", ReadingProgression.RTL))
    }

    @Test fun `parse ignores case`() {
        assertEquals(ContentLayout.RTL, ContentLayout.from("ar"))
    }

    @Test fun `parse ignores region`() {
        assertEquals(ContentLayout.RTL, ContentLayout.from("AR-FOOBAR"))
    }

    @Test fun `get the {readingProgression}`() {
        assertEquals(ReadingProgression.LTR, ContentLayout.LTR.readingProgression)
        assertEquals(ReadingProgression.LTR, ContentLayout.CJK_HORIZONTAL.readingProgression)
        assertEquals(ReadingProgression.RTL, ContentLayout.RTL.readingProgression)
        assertEquals(ReadingProgression.RTL, ContentLayout.CJK_VERTICAL.readingProgression)
    }

}