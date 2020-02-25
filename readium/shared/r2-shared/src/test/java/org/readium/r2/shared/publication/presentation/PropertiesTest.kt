/*
 * Module: r2-shared-kotlin
 * Developers: Mickaël Menu
 *
 * Copyright (c) 2020. Readium Foundation. All rights reserved.
 * Use of this source code is governed by a BSD-style license which is detailed in the
 * LICENSE file present in the project repository where this source code is maintained.
 */

package org.readium.r2.shared.publication.presentation

import org.junit.Assert.*
import org.junit.Test
import org.readium.r2.shared.publication.Properties

class PropertiesTest {

    @Test fun `get Properties {clipped} when available`() {
        assertEquals(true, Properties(otherProperties = mapOf("clipped" to true)).clipped)
    }

    @Test fun `get Properties {clipped} when missing`() {
        assertNull(Properties().clipped)
    }

    @Test fun `get Properties {fit} when available`() {
        assertEquals(
            Presentation.Fit.COVER,
            Properties(otherProperties = mapOf("fit" to "cover")).fit
        )
    }

    @Test fun `get Properties {fit} when missing`() {
        assertNull(Properties().fit)
    }

    @Test fun `get Properties {orientation} when available`() {
        assertEquals(
            Presentation.Orientation.LANDSCAPE,
            Properties(otherProperties = mapOf("orientation" to "landscape")).orientation
        )
    }

    @Test fun `get Properties {orientation} when missing`() {
        assertNull(Properties().orientation)
    }

    @Test fun `get Properties {overflow} when available`() {
        assertEquals(
            Presentation.Overflow.SCROLLED,
            Properties(otherProperties = mapOf("overflow" to "scrolled")).overflow
        )
    }

    @Test fun `get Properties {overflow} when missing`() {
        assertNull(Properties().overflow)
    }

    @Test fun `get Properties {page} when available`() {
        assertEquals(
            Presentation.Page.RIGHT,
            Properties(otherProperties = mapOf("page" to "right")).page
        )
    }

    @Test fun `get Properties {page} when missing`() {
        assertNull(Properties().page)
    }

    @Test fun `get Properties {spread} when available`() {
        assertEquals(
            Presentation.Spread.BOTH,
            Properties(otherProperties = mapOf("spread" to "both")).spread
        )
    }

    @Test fun `get Properties {spread} when missing`() {
        assertNull(Properties().spread)
    }

}
