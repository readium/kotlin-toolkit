/*
 * Module: r2-shared-kotlin
 * Developers: Mickaël Menu
 *
 * Copyright (c) 2020. Readium Foundation. All rights reserved.
 * Use of this source code is governed by a BSD-style license which is detailed in the
 * LICENSE file present in the project repository where this source code is maintained.
 */

package org.readium.r2.shared.publication.epub

import org.junit.Assert.*
import org.junit.Test

class EpubLayoutTest {

    @Test fun `parse layout`() {
        assertEquals(EpubLayout.FIXED, EpubLayout("fixed"))
        assertEquals(EpubLayout.REFLOWABLE, EpubLayout("reflowable"))
        assertNull(EpubLayout("foobar"))
        assertNull(EpubLayout(null))
    }

    @Test fun `get layout value`() {
        assertEquals("fixed", EpubLayout.FIXED.value)
        assertEquals("reflowable", EpubLayout.REFLOWABLE.value)
    }

}
