/*
 * Module: r2-shared-kotlin
 * Developers: Mickaël Menu
 *
 * Copyright (c) 2020. Readium Foundation. All rights reserved.
 * Use of this source code is governed by a BSD-style license which is detailed in the
 * LICENSE file present in the project repository where this source code is maintained.
 */

@file:OptIn(InternalReadiumApi::class)

package org.readium.r2.shared.opds

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import org.readium.r2.shared.InternalReadiumApi
import org.readium.r2.shared.assertJSONEquals
import org.readium.r2.shared.util.json.toJsonObjectOrNull

class PriceTest {

    @Test fun `parse JSON price`() {
        assertEquals(
            Price(currency = "EUR", value = 4.65),
            Price.fromJSON("{\"currency\": \"EUR\", \"value\": 4.65}".toJsonObjectOrNull()!!)
        )
    }

    @Test fun `parse invalid JSON price`() {
        assertNull(Price.fromJSON("{}".toJsonObjectOrNull()!!))
    }

    @Test fun `parse null JSON price`() {
        assertNull(Price.fromJSON(null))
    }

    @Test fun `parse JSON price requires currency`() {
        assertNull(Price.fromJSON("{\"value\": 4.65}".toJsonObjectOrNull()!!))
    }

    @Test fun `parse JSON price requires value`() {
        assertNull(Price.fromJSON("{\"currency\": \"EUR\"}".toJsonObjectOrNull()!!))
    }

    @Test fun `parse JSON price requires positive value`() {
        assertNull(Price.fromJSON("{\"currency\": \"EUR\", \"value\": -20}".toJsonObjectOrNull()!!))
    }

    @Test fun `get JSON price`() {
        assertJSONEquals(
            "{\"currency\": \"EUR\", \"value\": 4.65}".toJsonObjectOrNull()!!,
            Price(currency = "EUR", value = 4.65).toJSON()
        )
    }
}
