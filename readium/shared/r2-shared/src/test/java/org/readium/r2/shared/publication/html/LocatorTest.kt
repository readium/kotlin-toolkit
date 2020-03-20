/*
 * Module: r2-shared-kotlin
 * Developers: Mickaël Menu
 *
 * Copyright (c) 2020. Readium Foundation. All rights reserved.
 * Use of this source code is governed by a BSD-style license which is detailed in the
 * LICENSE file present in the project repository where this source code is maintained.
 */

package org.readium.r2.shared.publication.html

import org.junit.Assert.*
import org.junit.Test
import org.readium.r2.shared.publication.Locator

class LocatorTest {

    @Test fun `get Locations {cssSelector} when available`() {
        assertEquals(
            "p",
            Locator.Locations(otherLocations = mapOf("cssSelector" to "p")).cssSelector
        )
    }

    @Test fun `get Locations {cssSelector} when missing`() {
        assertNull(Locator.Locations().cssSelector)
    }

    @Test fun `get Locations {partialCfi} when available`() {
        assertEquals(
            "epubcfi(/4)",
            Locator.Locations(otherLocations = mapOf("partialCfi" to "epubcfi(/4)")).partialCfi
        )
    }

    @Test fun `get Locations {partialCfi} when missing`() {
        assertNull(Locator.Locations().partialCfi)
    }

    @Test fun `get Locations {domRange} when available`() {
        assertEquals(
            DomRange(start = DomRange.Point(cssSelector = "p", textNodeIndex = 4)),
            Locator.Locations(otherLocations = mapOf(
                "domRange" to mapOf(
                    "start" to mapOf(
                        "cssSelector" to "p",
                        "textNodeIndex" to 4
                    )
                )
            )).domRange
        )
    }

    @Test fun `get Locations {domRange} when missing`() {
        assertNull(Locator.Locations().domRange)
    }

}