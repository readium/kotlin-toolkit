package org.readium.r2.shared.extensions

import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class JSONTest {

    @Test fun `unpack an empty JSONObject`() {
        val sut = JSONObject("{}")

        assertEquals(sut.toMap(), mapOf<String, Any>())
    }

    @Test fun `unpack a JSONObject`() {
        val sut = JSONObject("""{
            "a": 1,
            "b": "hello",
            "c": true
        }""")

        assertEquals(sut.toMap(), mapOf(
            "a" to 1,
            "b" to "hello",
            "c" to true
        ))
    }

    @Test fun `unpack a nested JSONObject`() {
        val sut = JSONObject("""{
            "a": 1,
            "b": { "b.1": "hello" },
            "c": [true, 42, "world"]
        }""")

        assertEquals(sut.toMap(), mapOf(
            "a" to 1,
            "b" to mapOf("b.1" to "hello"),
            "c" to listOf(true, 42, "world")
        ))
    }

    @Test fun `unpack an empty JSONArray`() {
        val sut = JSONArray("[]")

        assertEquals(sut.toList(), listOf<Any>())
    }

    @Test fun `unpack a JSONArray`() {
        val sut = JSONArray("[1, 'hello', true]")

        assertEquals(sut.toList(), listOf(1, "hello", true))
    }

    @Test fun `unpack a nested JSONArray`() {
        val sut = JSONArray("""[
            1,
            { "b.1": "hello" },
            [true, 42, "world"]
        ]""")

        assertEquals(sut.toList(), listOf(
            1,
            mapOf("b.1" to "hello"),
            listOf(true, 42, "world")
        ))
    }

}

@RunWith(RobolectricTestRunner::class)
class JSONAndroidTest {

    // This test requires `RunWith(AndroidJUnit4::class)`, because the behavior is not buggy on the
    // local JVM.
    @Test
    fun `optNullableString() handles null values`() {
        // optString() returns "null" if the key exists but contains the `null` value.
        // https://stackoverflow.com/questions/18226288/json-jsonobject-optstring-returns-string-null
        val sut = JSONObject("""{ "key": null }""")

        assertNull(sut.optNullableString("key"))
    }

}