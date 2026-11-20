/*
 * Copyright 2026 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

@file:OptIn(ExperimentalReadiumApi::class)

package org.readium.r2.shared.extensibility

import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.readium.r2.shared.ExperimentalReadiumApi

class ValueTest {

    @Test
    fun testAccessors() {
        val nullVal: Value = NullValue
        assertTrue(nullVal.isNull)
        assertNull(nullVal.string)

        val boolVal: Value = BooleanValue(true)
        assertEquals(true, boolVal.boolean)
        assertFalse(boolVal.isNull)

        val intVal: Value = IntValue(42L)
        assertEquals(42L, intVal.long)
        assertEquals(42, intVal.int)
        assertEquals(42.0, intVal.double!!, 0.001)

        val doubleVal: Value = DoubleValue(3.14)
        assertEquals(3.14, doubleVal.double!!, 0.001)

        val strVal: Value = StringValue("hello")
        assertEquals("hello", strVal.string)

        val listVal: Value = ListValue(StringValue("a"), IntValue(1))
        assertEquals(2, listVal.list?.size)
        assertEquals("a", listVal[0]?.string)

        val mapVal: Value = MapValue("key" to StringValue("value"), "num" to IntValue(10))
        assertEquals("value", mapVal["key"]?.string)
        assertEquals(10, mapVal["num"]?.int)
    }

    @Test
    fun testJsonSerialization() {
        val json = Json { isLenient = true }

        val map = MapValue(
            "title" to StringValue("Book"),
            "count" to IntValue(5),
            "price" to DoubleValue(9.99),
            "available" to BooleanValue(true),
            "tags" to ListValue(StringValue("fiction"), StringValue("classic")),
            "details" to NullValue
        )

        val serialized = json.encodeToString<Value>(map)
        val deserialized = json.decodeFromString<Value>(serialized)

        assertEquals(map, deserialized)
    }
}
