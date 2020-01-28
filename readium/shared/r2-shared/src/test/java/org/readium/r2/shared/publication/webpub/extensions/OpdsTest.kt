/*
 * Module: r2-shared-kotlin
 * Developers: Mickaël Menu
 *
 * Copyright (c) 2020. Readium Foundation. All rights reserved.
 * Use of this source code is governed by a BSD-style license which is detailed in the
 * LICENSE file present in the project repository where this source code is maintained.
 */

package org.readium.r2.shared.publication.webpub.extensions

import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import org.readium.r2.shared.assertJSONEquals
import org.readium.r2.shared.extensions.toIso8601Date
import org.readium.r2.shared.publication.webpub.link.Properties
import org.readium.r2.shared.toJSON

class OpdsTest {

    // OpdsAcquisition

    @Test fun `parse minimal JSON acquisition`() {
        assertEquals(
            OpdsAcquisition(type = "acquisition-type"),
            OpdsAcquisition.fromJSON(JSONObject("{'type': 'acquisition-type'}"))
        )
    }

    @Test fun `parse full JSON acquisition`() {
        assertEquals(
            OpdsAcquisition(
                type = "acquisition-type",
                children = listOf(
                    OpdsAcquisition(
                        type = "sub-acquisition",
                        children = listOf(
                            OpdsAcquisition(type = "sub-sub1"),
                            OpdsAcquisition(type = "sub-sub2")
                        )
                    )
                )
            ),
            OpdsAcquisition.fromJSON(JSONObject("""{
                "type": "acquisition-type",
                "child": [
                    {
                        "type": "sub-acquisition",
                        "child": [
                            { "type": "sub-sub1" },
                            { "type": "sub-sub2" }
                        ]
                    }
                ]
            }"""))
        )
    }

    @Test fun `parse invalid JSON acquisition`() {
        assertNull(OpdsAcquisition.fromJSON(JSONObject("{}")))
    }

    @Test fun `parse null JSON acquisition`() {
        assertNull(OpdsAcquisition.fromJSON(null))
    }

    @Test fun `parse JSON acquisition requires {type}`() {
        assertNull(OpdsAcquisition.fromJSON(JSONObject("{'child': []}")))
    }

    @Test fun `parse JSON acquisition array`() {
        assertEquals(
            listOf(
                OpdsAcquisition(type = "acq1"),
                OpdsAcquisition(type = "acq2")
            ),
            OpdsAcquisition.fromJSONArray(JSONArray("""[
                { "type": "acq1" },
                { "type": "acq2" }
            ]"""))
        )
    }

    @Test fun `parse JSON acquisition array ignores invalid acquisitions`() {
        assertEquals(
            listOf(
                OpdsAcquisition(type = "acq1")
            ),
            OpdsAcquisition.fromJSONArray(JSONArray("""[
                { "type": "acq1" },
                { "invalid": "acq2" }
            ]"""))
        )
    }

    @Test fun `parse null JSON acquisition array`() {
        assertEquals(
            emptyList<OpdsAcquisition>(),
            OpdsAcquisition.fromJSONArray(null)
        )
    }

    @Test fun `get minimal JSON acquisition`() {
        assertJSONEquals(
            JSONObject("{'type': 'acquisition-type'}"),
            OpdsAcquisition(type = "acquisition-type").toJSON()
        )
    }

    @Test fun `get full JSON acquisition`() {
        assertJSONEquals(
            JSONObject("""{
                "type": "acquisition-type",
                "child": [
                    {
                        "type": "sub-acquisition",
                        "child": [
                            { "type": "sub-sub1" },
                            { "type": "sub-sub2" }
                        ]
                    }
                ]
            }"""),
            OpdsAcquisition(
                type = "acquisition-type",
                children = listOf(
                    OpdsAcquisition(
                        type = "sub-acquisition",
                        children = listOf(
                            OpdsAcquisition(type = "sub-sub1"),
                            OpdsAcquisition(type = "sub-sub2")
                        )
                    )
                )
            ).toJSON()
        )
    }

    @Test fun `get JSON acquisition array`() {
        assertJSONEquals(
            JSONArray("""[
                { "type": "acq1" },
                { "type": "acq2" }
            ]"""),
            listOf(
                OpdsAcquisition(type = "acq1"),
                OpdsAcquisition(type = "acq2")
            ).toJSON()
        )
    }


    // OpdsPrice

    @Test fun `parse JSON price`() {
        assertEquals(
            OpdsPrice(currency = "EUR", value = 4.65),
            OpdsPrice.fromJSON(JSONObject("{'currency': 'EUR', 'value': 4.65}"))
        )
    }

    @Test fun `parse invalid JSON price`() {
        assertNull(OpdsPrice.fromJSON(JSONObject("{}")))
    }

    @Test fun `parse null JSON price`() {
        assertNull(OpdsPrice.fromJSON(null))
    }

    @Test fun `parse JSON price requires {currency}`() {
        assertNull(OpdsPrice.fromJSON(JSONObject("{'value': 4.65}")))
    }

    @Test fun `parse JSON price requires {value}`() {
        assertNull(OpdsPrice.fromJSON(JSONObject("{'currency': 'EUR'}")))
    }

    @Test fun `parse JSON price requires positive {value}`() {
        assertNull(OpdsPrice.fromJSON(JSONObject("{'currency': 'EUR', 'value': -20}")))
    }

    @Test fun `get JSON price`() {
        assertJSONEquals(
            JSONObject("{'currency': 'EUR', 'value': 4.65}"),
            OpdsPrice(currency = "EUR", value = 4.65).toJSON()
        )
    }


    // OpdsHolds

    @Test fun `parse minimal JSON holds`() {
        assertEquals(
            OpdsHolds(total = null, position = null),
            OpdsHolds.fromJSON(JSONObject("{}"))
        )
    }

    @Test fun `parse full JSON holds`() {
        assertEquals(
            OpdsHolds(total = 5, position = 6),
            OpdsHolds.fromJSON(JSONObject("{'total': 5, 'position': 6}"))
        )
    }

    @Test fun `parse null JSON holds`() {
        assertNull(OpdsHolds.fromJSON(null))
    }

    @Test fun `parse JSON holds requires positive {total}`() {
        assertEquals(
            OpdsHolds(total = null, position = 6),
            OpdsHolds.fromJSON(JSONObject("{'total': -5, 'position': 6}"))
        )
    }

    @Test fun `parse JSON holds requires positive {position}`() {
        assertEquals(
            OpdsHolds(total = 5, position = null),
            OpdsHolds.fromJSON(JSONObject("{'total': 5, 'position': -6}"))
        )
    }

    @Test fun `get minimal JSON holds`() {
        assertJSONEquals(
            JSONObject("{}"),
            OpdsHolds(total = null, position = null).toJSON()
        )
    }

    @Test fun `get full JSON holds`() {
        assertJSONEquals(
            JSONObject("{'total': 5, 'position': 6}"),
            OpdsHolds(total = 5, position = 6).toJSON()
        )
    }


    // OpdsCopies

    @Test fun `parse minimal JSON copies`() {
        assertEquals(
            OpdsCopies(total = null, available = null),
            OpdsCopies.fromJSON(JSONObject("{}"))
        )
    }

    @Test fun `parse full JSON copies`() {
        assertEquals(
            OpdsCopies(total = 5, available = 6),
            OpdsCopies.fromJSON(JSONObject("{'total': 5, 'available': 6}"))
        )
    }

    @Test fun `parse null JSON copies`() {
        assertNull(OpdsCopies.fromJSON(null))
    }

    @Test fun `parse JSON copies requires positive {total}`() {
        assertEquals(
            OpdsCopies(total = null, available = 6),
            OpdsCopies.fromJSON(JSONObject("{'total': -5, 'available': 6}"))
        )
    }

    @Test fun `parse JSON copies requires positive {available}`() {
        assertEquals(
            OpdsCopies(total = 5, available = null),
            OpdsCopies.fromJSON(JSONObject("{'total': 5, 'available': -6}"))
        )
    }

    @Test fun `get minimal JSON copies`() {
        assertJSONEquals(
            JSONObject("{}"),
            OpdsCopies(total = null, available = null).toJSON()
        )
    }

    @Test fun `get full JSON copies`() {
        assertJSONEquals(
            JSONObject("{'total': 5, 'available': 6}"),
            OpdsCopies(total = 5, available = 6).toJSON()
        )
    }


    // OpdsAvailability

    @Test fun `parse minimal JSON availability`() {
        assertEquals(
            OpdsAvailability(state = OpdsAvailability.State.AVAILABLE),
            OpdsAvailability.fromJSON(JSONObject("{'state': 'available'}"))
        )
    }

    @Test fun `parse full JSON availability`() {
        assertEquals(
            OpdsAvailability(
                state = OpdsAvailability.State.AVAILABLE,
                since = "2001-01-01T12:36:27.000Z".toIso8601Date(),
                until = "2001-02-01T12:36:27.000Z".toIso8601Date()
            ),
            OpdsAvailability.fromJSON(JSONObject("""{
                'state': 'available',
                'since': '2001-01-01T12:36:27.000Z',
                'until': '2001-02-01T12:36:27.000Z'
            }"""))
        )
    }

    @Test fun `parse null JSON availability`() {
        assertNull(OpdsAvailability.fromJSON(null))
    }

    @Test fun `parse JSON availability requires {state}`() {
        assertNull(OpdsAvailability.fromJSON(JSONObject("{ 'since': '2001-01-01T12:36:27+0000' }")))
    }

    @Test fun `get minimal JSON availability`() {
        assertEquals(
            OpdsAvailability.fromJSON(JSONObject("{'state': 'available'}")),
            OpdsAvailability(state = OpdsAvailability.State.AVAILABLE)
        )
    }

    @Test fun `get full JSON availability`() {
        assertJSONEquals(
            JSONObject("""{
                'state': 'available',
                'since': '2001-01-01T12:36:27.000Z',
                'until': '2001-02-01T12:36:27.000Z'
            }"""),
            OpdsAvailability(
                state = OpdsAvailability.State.AVAILABLE,
                since = "2001-01-01T12:36:27.000Z".toIso8601Date(),
                until = "2001-02-01T12:36:27.000Z".toIso8601Date()
            ).toJSON()
        )
    }


    // OpdsAvailability.State

    @Test fun `parse JSON availability state`() {
        assertEquals(OpdsAvailability.State.AVAILABLE, OpdsAvailability.State.from("available"))
        assertEquals(OpdsAvailability.State.READY, OpdsAvailability.State.from("ready"))
        assertEquals(OpdsAvailability.State.RESERVED, OpdsAvailability.State.from("reserved"))
        assertEquals(OpdsAvailability.State.UNAVAILABLE, OpdsAvailability.State.from("unavailable"))
        assertNull(OpdsAvailability.State.from("foobar"))
        assertNull(OpdsAvailability.State.from(null))
    }

    @Test fun `get JSON availability state`() {
        assertEquals("available", OpdsAvailability.State.AVAILABLE.value)
        assertEquals("ready", OpdsAvailability.State.READY.value)
        assertEquals("reserved", OpdsAvailability.State.RESERVED.value)
        assertEquals("unavailable", OpdsAvailability.State.UNAVAILABLE.value)
    }


    // OPDS extensions for link [Properties].

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
            OpdsPrice(currency = "EUR", value = 4.36),
            Properties(otherProperties = mapOf("price" to mapOf("currency" to "EUR", "value" to 4.36))).price
        )
    }

    @Test fun `get Properties {price} when missing`() {
        assertNull(Properties().price)
    }

    @Test fun `get Properties {indirectAcquisitions} when available`() {
        assertEquals(
            listOf(
                OpdsAcquisition(type = "acq1"),
                OpdsAcquisition(type = "acq2")
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
            OpdsHolds(total = 5),
            Properties(otherProperties = mapOf("holds" to mapOf("total" to 5))).holds
        )
    }

    @Test fun `get Properties {holds} when missing`() {
        assertNull(Properties().holds)
    }

    @Test fun `get Properties {copies} when available`() {
        assertEquals(
            OpdsCopies(total = 5),
            Properties(otherProperties = mapOf("copies" to mapOf("total" to 5))).copies
        )
    }

    @Test fun `get Properties {copies} when missing`() {
        assertNull(Properties().copies)
    }

    @Test fun `get Properties {availability} when available`() {
        assertEquals(
            OpdsAvailability(state = OpdsAvailability.State.AVAILABLE),
            Properties(otherProperties = mapOf("availability" to mapOf("state" to "available"))).availability
        )
    }

    @Test fun `get Properties {availability} when missing`() {
        assertNull(Properties().availability)
    }

}
