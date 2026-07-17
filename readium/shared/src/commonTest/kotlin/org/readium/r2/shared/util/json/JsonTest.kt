/*
 * Copyright 2026 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

@file:OptIn(InternalReadiumApi::class)

package org.readium.r2.shared.util.json

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.readium.r2.shared.InternalReadiumApi

class JsonTest {

    private fun jsonObject(string: String): JsonObject =
        string.toJsonObjectOrNull()!!

    private fun jsonArray(string: String): JsonArray =
        string.toJsonArrayOrNull()!!

    // Parsing

    @Test
    fun parseAnInvalidDocumentReturnsNull() {
        assertNull("invalid".toJsonObjectOrNull())
        assertNull("[1, 2]".toJsonObjectOrNull())
        assertNull("{}".toJsonArrayOrNull())
    }

    // toMap / toList, ported from the legacy JSONTest.

    @Test
    fun unpackAnEmptyJsonObject() {
        assertEquals(emptyMap(), jsonObject("{}").toMap())
    }

    @Test
    fun unpackAJsonObject() {
        val sut = jsonObject(
            """{
                "a": 1,
                "b": "hello",
                "c": true
            }"""
        )

        assertEquals(
            mapOf(
                "a" to 1,
                "b" to "hello",
                "c" to true
            ),
            sut.toMap()
        )
    }

    @Test
    fun unpackANestedJsonObject() {
        val sut = jsonObject(
            """{
                "a": 1,
                "b": { "b.1": "hello" },
                "c": [true, 42, "world"]
            }"""
        )

        assertEquals(
            mapOf(
                "a" to 1,
                "b" to mapOf("b.1" to "hello"),
                "c" to listOf(true, 42, "world")
            ),
            sut.toMap()
        )
    }

    @Test
    fun unpackSkipsNullValues() {
        val sut = jsonObject("""{ "a": null, "b": 1 }""")

        assertEquals(mapOf<String, Any>("b" to 1), sut.toMap())
    }

    @Test
    fun unpackNumbers() {
        val sut = jsonObject("""{ "int": 1, "long": 10000000000, "double": 1.5 }""")

        assertEquals(
            mapOf(
                "int" to 1,
                "long" to 10000000000L,
                "double" to 1.5
            ),
            sut.toMap()
        )
    }

    @Test
    fun unpackAnEmptyJsonArray() {
        assertEquals(emptyList(), jsonArray("[]").toList())
    }

    @Test
    fun unpackAJsonArray() {
        assertEquals(
            listOf(1, "hello", true),
            jsonArray("""[1, "hello", true]""").toList()
        )
    }

    @Test
    fun unpackANestedJsonArray() {
        val sut = jsonArray(
            """[
                1,
                { "b.1": "hello" },
                [true, 42, "world"]
            ]"""
        )

        assertEquals(
            listOf(
                1,
                mapOf("b.1" to "hello"),
                listOf(true, 42, "world")
            ),
            sut.toList()
        )
    }

    // optString

    @Test
    fun optStringWithAbsentKey() {
        assertEquals("", jsonObject("{}").optString("key"))
        assertEquals("fallback", jsonObject("{}").optString("key", fallback = "fallback"))
    }

    @Test
    fun optStringWithNullValue() {
        assertEquals("", jsonObject("""{ "key": null }""").optString("key"))
    }

    @Test
    fun optStringCoercesOtherTypes() {
        assertEquals("42", jsonObject("""{ "key": 42 }""").optString("key"))
        assertEquals("true", jsonObject("""{ "key": true }""").optString("key"))
    }

    // optNullableString

    @Test
    fun optNullableStringWithAbsentKey() {
        assertNull(jsonObject("{}").optNullableString("key"))
    }

    @Test
    fun optNullableStringWithNullValue() {
        assertNull(jsonObject("""{ "key": null }""").optNullableString("key"))
    }

    @Test
    fun optNullableStringWithEmptyString() {
        assertNull(jsonObject("""{ "key": "" }""").optNullableString("key"))
    }

    @Test
    fun optNullableStringHandlesIllTypedValues() {
        assertNull(jsonObject("""{ "key": ["jhhh", "mlkk"] }""").optNullableString("key"))
        assertNull(jsonObject("""{ "key": 42 }""").optNullableString("key"))
    }

    @Test
    fun optNullableStringWithString() {
        assertEquals("hello", jsonObject("""{ "key": "hello" }""").optNullableString("key"))
    }

    @Test
    fun optNullableStringWithRemove() {
        val sut = jsonObject("""{ "key": "hello", "other": 1 }""").toMutableMap()

        assertEquals("hello", sut.optNullableString("key", remove = true))
        assertEquals(setOf("other"), sut.keys)
    }

    // optBoolean

    @Test
    fun optBooleanWithAbsentKey() {
        assertFalse(jsonObject("{}").optBoolean("key"))
        assertTrue(jsonObject("{}").optBoolean("key", fallback = true))
    }

    @Test
    fun optBooleanWithNullValue() {
        assertFalse(jsonObject("""{ "key": null }""").optBoolean("key"))
    }

    @Test
    fun optBooleanCoercesStrings() {
        assertTrue(jsonObject("""{ "key": "true" }""").optBoolean("key"))
        assertTrue(jsonObject("""{ "key": "TRUE" }""").optBoolean("key"))
        assertFalse(jsonObject("""{ "key": "false" }""").optBoolean("key"))
        assertFalse(jsonObject("""{ "key": "invalid" }""").optBoolean("key"))
    }

    @Test
    fun optNullableBooleanWithAbsentKey() {
        assertNull(jsonObject("{}").optNullableBoolean("key"))
    }

    @Test
    fun optNullableBooleanWithValue() {
        assertEquals(true, jsonObject("""{ "key": true }""").optNullableBoolean("key"))
        assertEquals(false, jsonObject("""{ "key": "invalid" }""").optNullableBoolean("key"))
    }

    // optInt / optLong / optDouble

    @Test
    fun optIntWithAbsentKey() {
        assertEquals(0, jsonObject("{}").optInt("key"))
        assertEquals(-1, jsonObject("{}").optInt("key", fallback = -1))
        assertNull(jsonObject("{}").optNullableInt("key"))
    }

    @Test
    fun optIntWithWrongType() {
        assertEquals(0, jsonObject("""{ "key": "hello" }""").optInt("key"))
        assertEquals(0, jsonObject("""{ "key": [1] }""").optInt("key"))
    }

    @Test
    fun optIntWithStringEncodedNumbers() {
        assertEquals(42, jsonObject("""{ "key": "42" }""").optInt("key"))
        // Like `org.json`, a string-encoded decimal number is truncated.
        assertEquals(4, jsonObject("""{ "key": "4.5" }""").optInt("key"))
    }

    @Test
    fun optNullableIntWithValue() {
        assertEquals(42, jsonObject("""{ "key": 42 }""").optNullableInt("key"))
        // The key exists but has the wrong type: coerced to the 0 fallback, like `org.json`.
        assertEquals(0, jsonObject("""{ "key": "hello" }""").optNullableInt("key"))
    }

    @Test
    fun optNullableIntWithRemove() {
        val sut = jsonObject("""{ "key": 42 }""").toMutableMap()

        assertEquals(42, sut.optNullableInt("key", remove = true))
        assertTrue(sut.isEmpty())
    }

    @Test
    fun optLongValues() {
        assertEquals(10000000000L, jsonObject("""{ "key": 10000000000 }""").optLong("key"))
        assertEquals(0L, jsonObject("{}").optLong("key"))
        assertNull(jsonObject("{}").optNullableLong("key"))
        assertEquals(42L, jsonObject("""{ "key": "42" }""").optNullableLong("key"))
    }

    @Test
    fun optNullableDoubleWithWrongTypeFallsBackToNaN() {
        // Like org.json, a present but uncoercible value yields NaN, not 0.0, so that range
        // checks like `it in 0.0..1.0` filter it out.
        assertTrue(jsonObject("""{ "key": "abc" }""").optNullableDouble("key")!!.isNaN())
        assertTrue(jsonObject("""{ "key": [1] }""").optNullableDouble("key")!!.isNaN())
    }

    @Test
    fun stringAccessorsAcceptUnquotedLenientLiterals() {
        // org.json parsed unquoted literals as strings; kotlinx marks them as non-string
        // primitives, so the accessors coerce them back, except for booleans and numbers.
        val sut = jsonObject("{ href: foo, count: 42, flag: true }")
        assertEquals("foo", sut.optNullableString("href"))
        assertEquals("foo", sut["href"].stringOrNull)
        assertNull(sut.optNullableString("count"))
        assertNull(sut.optNullableString("flag"))
        assertEquals(listOf("foo"), sut.optStringsFromArrayOrSingle("href"))
    }

    @Test
    fun optDoubleValues() {
        assertEquals(4.5, jsonObject("""{ "key": 4.5 }""").optDouble("key"))
        assertTrue(jsonObject("{}").optDouble("key").isNaN())
        assertEquals(2.0, jsonObject("{}").optDouble("key", fallback = 2.0))
        assertNull(jsonObject("{}").optNullableDouble("key"))
        assertEquals(4.5, jsonObject("""{ "key": "4.5" }""").optNullableDouble("key"))
    }

    // optPositiveInt / optPositiveDouble

    @Test
    fun optPositiveInt() {
        assertEquals(42, jsonObject("""{ "key": 42 }""").optPositiveInt("key"))
        assertEquals(0, jsonObject("""{ "key": 0 }""").optPositiveInt("key"))
        assertNull(jsonObject("""{ "key": -1 }""").optPositiveInt("key"))
        assertNull(jsonObject("{}").optPositiveInt("key"))
        assertNull(jsonObject("""{ "key": "hello" }""").optPositiveInt("key"))
    }

    @Test
    fun optPositiveIntWithRemove() {
        val sut = jsonObject("""{ "key": 42 }""").toMutableMap()

        assertEquals(42, sut.optPositiveInt("key", remove = true))
        assertTrue(sut.isEmpty())
    }

    @Test
    fun optPositiveDouble() {
        assertEquals(4.5, jsonObject("""{ "key": 4.5 }""").optPositiveDouble("key"))
        assertNull(jsonObject("""{ "key": -1.5 }""").optPositiveDouble("key"))
        assertNull(jsonObject("{}").optPositiveDouble("key"))
    }

    // optStringsFromArrayOrSingle

    @Test
    fun optStringsFromArrayOrSingle() {
        assertEquals(
            listOf("a", "b"),
            jsonObject("""{ "key": ["a", "b"] }""").optStringsFromArrayOrSingle("key")
        )
        assertEquals(
            listOf("a"),
            jsonObject("""{ "key": "a" }""").optStringsFromArrayOrSingle("key")
        )
        assertEquals(
            emptyList(),
            jsonObject("{}").optStringsFromArrayOrSingle("key")
        )
        assertEquals(
            emptyList(),
            jsonObject("""{ "key": 42 }""").optStringsFromArrayOrSingle("key")
        )
        // Non-string elements are dropped.
        assertEquals(
            listOf("a"),
            jsonObject("""{ "key": ["a", 42, true] }""").optStringsFromArrayOrSingle("key")
        )
    }

    @Test
    fun optStringsFromArrayOrSingleWithRemove() {
        val sut = jsonObject("""{ "key": ["a"] }""").toMutableMap()

        assertEquals(listOf("a"), sut.optStringsFromArrayOrSingle("key", remove = true))
        assertTrue(sut.isEmpty())
    }

    // optJsonObject / optJsonArray

    @Test
    fun optJsonObject() {
        assertEquals(
            jsonObject("""{ "a": 1 }"""),
            jsonObject("""{ "key": { "a": 1 } }""").optJsonObject("key")
        )
        assertNull(jsonObject("{}").optJsonObject("key"))
        assertNull(jsonObject("""{ "key": [1] }""").optJsonObject("key"))
        assertNull(jsonObject("""{ "key": null }""").optJsonObject("key"))
    }

    @Test
    fun optJsonArray() {
        assertEquals(
            jsonArray("[1, 2]"),
            jsonObject("""{ "key": [1, 2] }""").optJsonArray("key")
        )
        assertNull(jsonObject("{}").optJsonArray("key"))
        assertNull(jsonObject("""{ "key": { "a": 1 } }""").optJsonArray("key"))
    }

    // Builders

    @Test
    fun putIfNotNullDropsNullValues() {
        val sut = buildJsonObject {
            putIfNotNull("string", "hello")
            putIfNotNull("nullString", null as String?)
            putIfNotNull("boolean", true)
            putIfNotNull("nullBoolean", null as Boolean?)
            putIfNotNull("number", 42)
            putIfNotNull("nullNumber", null as Number?)
        }

        assertEquals(
            jsonObject("""{ "string": "hello", "boolean": true, "number": 42 }"""),
            sut
        )
    }

    @Test
    fun putIfNotEmptyDropsEmptyObjectsAndArrays() {
        val sut = buildJsonObject {
            putIfNotEmpty("object", jsonObject("""{ "a": 1 }"""))
            putIfNotEmpty("emptyObject", jsonObject("{}"))
            putIfNotEmpty("nullObject", null as JsonObject?)
            putIfNotEmpty("array", jsonArray("[1]"))
            putIfNotEmpty("emptyArray", jsonArray("[]"))
            putIfNotEmpty("nullArray", null as JsonArray?)
        }

        assertEquals(
            jsonObject("""{ "object": { "a": 1 }, "array": [1] }"""),
            sut
        )
    }

    @Test
    fun putIfNotEmptyWithCollections() {
        val sut = buildJsonObject {
            putIfNotEmpty("strings", listOf("a", "b"))
            putIfNotEmpty("empty", emptyList<String>())
            putIfNotEmpty("nested", listOf(mapOf("a" to 1), emptyMap<String, Any>()))
        }

        assertEquals(
            jsonObject("""{ "strings": ["a", "b"], "nested": [{ "a": 1 }] }"""),
            sut
        )
    }

    @Test
    fun putIfNotEmptyWithMaps() {
        val sut = buildJsonObject {
            putIfNotEmpty("map", mapOf("a" to 1, "b" to listOf("x")))
            putIfNotEmpty("empty", emptyMap<String, Any>())
        }

        assertEquals(
            jsonObject("""{ "map": { "a": 1, "b": ["x"] } }"""),
            sut
        )
    }

    @Test
    fun putAllPreservesEmptyNestedValues() {
        // Like the legacy JSONObject(Map) conversion, and unlike putIfNotEmpty.
        val sut = buildJsonObject {
            putAll(mapOf("object" to emptyMap<String, Any>(), "array" to emptyList<Any>()))
        }

        assertEquals(jsonObject("""{ "object": {}, "array": [] }"""), sut)
    }

    @Test
    fun putAllWrapsValues() {
        val sut = buildJsonObject {
            putAll(mapOf("a" to 1, "b" to "hello", "c" to mapOf("d" to true)))
            put("b", "overwritten")
        }

        assertEquals(
            jsonObject("""{ "a": 1, "b": "overwritten", "c": { "d": true } }"""),
            sut
        )
    }

    @Test
    fun wrapJsonValues() {
        assertNull(wrapJson(null))
        assertEquals(JsonPrimitive(1), wrapJson(1))
        assertEquals(JsonPrimitive("a"), wrapJson("a"))
        assertEquals(JsonPrimitive(true), wrapJson(true))
        assertEquals(JsonPrimitive(1.5), wrapJson(1.5))
        assertNull(wrapJson(emptyMap<String, Any>()))
        assertNull(wrapJson(emptyList<Any>()))
        assertEquals(
            jsonObject("""{ "a": [1] }"""),
            wrapJson(mapOf("a" to listOf(1)))
        )
    }

    // parseObjects

    @Test
    fun parseObjectsSkipsFailures() {
        val sut = jsonArray("""[{ "a": 1 }, "invalid", { "a": 2 }]""")

        assertEquals(
            listOf(1, 2),
            sut.parseObjects { (it as? JsonObject)?.optNullableInt("a") }
        )

        assertEquals(emptyList<Int>(), (null as JsonArray?).parseObjects { (it as? JsonObject)?.optNullableInt("a") })
    }

    @Test
    fun buildersInteroperateWithKotlinxBuilders() {
        val sut = buildJsonObject {
            put("a", 1)
            putIfNotEmpty(
                "b",
                buildJsonArray {
                    add(JsonPrimitive("x"))
                }
            )
        }

        assertEquals(jsonObject("""{ "a": 1, "b": ["x"] }"""), sut)
    }
}
