/*
 * Module: r2-shared-kotlin
 * Developers: Mickaël Menu
 *
 * Copyright (c) 2020. Readium Foundation. All rights reserved.
 * Use of this source code is governed by a BSD-style license which is detailed in the
 * LICENSE file present in the project repository where this source code is maintained.
 */

@file:OptIn(InternalReadiumApi::class)

package org.readium.r2.shared.publication

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.serialization.json.JsonObject
import org.readium.r2.shared.InternalReadiumApi
import org.readium.r2.shared.assertJSONEquals
import org.readium.r2.shared.util.json.toJsonObjectOrNull

class PropertiesTest {

    @Test fun `parse null JSON`() {
        assertEquals(Properties(), Properties.fromJSON(null))
    }

    @Test fun `parse minimal JSON`() {
        assertEquals(Properties(), Properties.fromJSON("{}".toJsonObjectOrNull()!!))
    }

    @Test fun `parse full JSON`() {
        assertEquals(
            Properties(
                otherProperties = mapOf<String, Any>(
                    "other-property1" to "value",
                    "other-property2" to listOf(42)
                )
            ),
            Properties.fromJSON(
                """{
                "other-property1": "value",
                "other-property2": [42]
            }""".toJsonObjectOrNull()!!
            )
        )
    }

    @Test fun `get minimal JSON`() {
        assertJSONEquals(JsonObject(emptyMap()), Properties().toJSON())
    }

    @Test fun `get full JSON`() {
        assertJSONEquals(
            """{
                "other-property1": "value",
                "other-property2": [42]
            }""".toJsonObjectOrNull()!!,
            Properties(
                otherProperties = mapOf<String, Any>(
                    "other-property1" to "value",
                    "other-property2" to listOf(42)
                )
            ).toJSON()
        )
    }

    @Test fun `copy after adding the given properties`() {
        val properties = Properties(
            otherProperties = mapOf<String, Any>(
                "other-property1" to "value",
                "other-property2" to listOf(42)
            )
        )

        assertJSONEquals(
            """{
                "other-property1": "value",
                "other-property2": [42],
                "additional": "property"
            }""".toJsonObjectOrNull()!!,
            properties.add(mapOf("additional" to "property")).toJSON()
        )
    }
}
