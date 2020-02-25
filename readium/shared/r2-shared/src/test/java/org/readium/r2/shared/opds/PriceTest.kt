/*
 * Module: r2-shared-kotlin
 * Developers: Mickaël Menu
 *
 * Copyright (c) 2020. Readium Foundation. All rights reserved.
 * Use of this source code is governed by a BSD-style license which is detailed in the
 * LICENSE file present in the project repository where this source code is maintained.
 */

package org.readium.r2.shared.opds

import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import org.readium.r2.shared.assertJSONEquals

class PriceTest {

    @Test fun `parse JSON price`() {
        assertEquals(
            Price(currency = "EUR", value = 4.65),
            Price.fromJSON(JSONObject("{'currency': 'EUR', 'value': 4.65}"))
        )
    }

    @Test fun `parse invalid JSON price`() {
        assertNull(Price.fromJSON(JSONObject("{}")))
    }

    @Test fun `parse null JSON price`() {
        assertNull(Price.fromJSON(null))
    }

    @Test fun `parse JSON price requires {currency}`() {
        assertNull(Price.fromJSON(JSONObject("{'value': 4.65}")))
    }

    @Test fun `parse JSON price requires {value}`() {
        assertNull(Price.fromJSON(JSONObject("{'currency': 'EUR'}")))
    }

    @Test fun `parse JSON price requires positive {value}`() {
        assertNull(Price.fromJSON(JSONObject("{'currency': 'EUR', 'value': -20}")))
    }

    @Test fun `get JSON price`() {
        assertJSONEquals(
            JSONObject("{'currency': 'EUR', 'value': 4.65}"),
            Price(currency = "EUR", value = 4.65).toJSON()
        )
    }

}
