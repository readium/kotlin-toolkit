/*
 * Module: r2-shared-kotlin
 * Developers: Mickaël Menu
 *
 * Copyright (c) 2020. Readium Foundation. All rights reserved.
 * Use of this source code is governed by a BSD-style license which is detailed in the
 * LICENSE file present in the project repository where this source code is maintained.
 */

package org.readium.r2.shared.publication.opds

import org.junit.Assert.*
import org.junit.Test
import org.readium.r2.shared.opds.*
import org.readium.r2.shared.publication.Properties

class PropertiesTest {

    @Test fun `get Properties {numberOfItems} when available`() {
        assertEquals(
            42,
            Properties(otherProperties = mapOf("numberOfItems" to 42)).numberOfItems
        )
    }

    @Test fun `get Properties {numberOfItems} when missing`() {
        assertNull(Properties().numberOfItems)
    }

    @Test fun `Properties {numberOfItems} must be positive`() {
        assertNull(Properties(otherProperties = mapOf("numberOfItems" to -20)).numberOfItems)
    }

    @Test fun `get Properties {price} when available`() {
        assertEquals(
            Price(currency = "EUR", value = 4.36),
            Properties(otherProperties = mapOf("price" to mapOf("currency" to "EUR", "value" to 4.36))).price
        )
    }

    @Test fun `get Properties {price} when missing`() {
        assertNull(Properties().price)
    }

    @Test fun `get Properties {indirectAcquisitions} when available`() {
        assertEquals(
            listOf(
                Acquisition(type = "acq1"),
                Acquisition(type = "acq2")
            ),
            Properties(otherProperties = mapOf("indirectAcquisition" to listOf(
                mapOf("type" to "acq1"),
                mapOf("type" to "acq2")
            ))).indirectAcquisitions
        )
    }

    @Test fun `get Properties {indirectAcquisitions} when missing`() {
        assertEquals(0, Properties().indirectAcquisitions.size)
    }

    @Test fun `get Properties {holds} when available`() {
        assertEquals(
            Holds(total = 5),
            Properties(otherProperties = mapOf("holds" to mapOf("total" to 5))).holds
        )
    }

    @Test fun `get Properties {holds} when missing`() {
        assertNull(Properties().holds)
    }

    @Test fun `get Properties {copies} when available`() {
        assertEquals(
            Copies(total = 5),
            Properties(otherProperties = mapOf("copies" to mapOf("total" to 5))).copies
        )
    }

    @Test fun `get Properties {copies} when missing`() {
        assertNull(Properties().copies)
    }

    @Test fun `get Properties {availability} when available`() {
        assertEquals(
            Availability(state = Availability.State.AVAILABLE),
            Properties(otherProperties = mapOf("availability" to mapOf("state" to "available"))).availability
        )
    }

    @Test fun `get Properties {availability} when missing`() {
        assertNull(Properties().availability)
    }

}
