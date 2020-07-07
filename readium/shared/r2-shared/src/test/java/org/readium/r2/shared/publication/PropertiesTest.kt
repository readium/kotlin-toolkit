/*
 * Module: r2-shared-kotlin
 * Developers: Mickaël Menu
 *
 * Copyright (c) 2020. Readium Foundation. All rights reserved.
 * Use of this source code is governed by a BSD-style license which is detailed in the
 * LICENSE file present in the project repository where this source code is maintained.
 */

package org.readium.r2.shared.publication

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Test
import org.readium.r2.shared.assertJSONEquals

class PropertiesTest {

    @Test fun `parse null JSON`() {
        assertEquals(Properties(), Properties.fromJSON(null))
    }

    @Test fun `parse minimal JSON`() {
        assertEquals(Properties(), Properties.fromJSON(JSONObject("{}")))
    }

    @Test fun `parse full JSON`() {
        assertEquals(
            Properties(
                otherProperties = mapOf<String, Any>(
                    "other-property1" to "value",
                    "other-property2" to listOf(42)
                )
            ),
            Properties.fromJSON(JSONObject("""{
                "other-property1": "value",
                "other-property2": [42]
            }"""))
        )
    }

    @Test fun `get minimal JSON`() {
        assertJSONEquals(JSONObject(), Properties().toJSON())
    }

    @Test fun `get full JSON`() {
        assertJSONEquals(
            JSONObject("""{
                "other-property1": "value",
                "other-property2": [42]
            }"""),
            Properties(
                otherProperties = mapOf<String, Any>(
                    "other-property1" to "value",
                    "other-property2" to listOf(42)
                )
            ).toJSON()
        )
    }

    @Test fun `copy after adding the given {properties}`() {
        val properties = Properties(otherProperties = mapOf<String, Any>(
            "other-property1" to "value",
            "other-property2" to listOf(42)
        ))

        assertJSONEquals(
            JSONObject("""{
                "other-property1": "value",
                "other-property2": [42],
                "additional": "property"
            }"""),
            properties.add(mapOf("additional" to "property")).toJSON()
        )
    }

}
