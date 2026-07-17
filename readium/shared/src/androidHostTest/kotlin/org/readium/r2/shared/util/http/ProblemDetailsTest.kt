@file:OptIn(InternalReadiumApi::class)

package org.readium.r2.shared.util.http

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.readium.r2.shared.InternalReadiumApi
import org.readium.r2.shared.util.json.toJsonObjectOrNull
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ProblemDetailsTest {

    @Test
    fun `parse minimal JSON`() {
        val json = """
            {"title": "You do not have enough credit."}
        """.toJsonObjectOrNull()!!

        assertEquals(
            ProblemDetails(title = "You do not have enough credit."),
            ProblemDetails.fromJSON(json)
        )
    }

    @Test
    fun `parse full JSON`() {
        val json = """{
            "type": "https://example.net/validation-error",
            "title": "Your request parameters didn't validate.",
            "status": 400,
            "invalid-params": [
                {
                    "name": "age",
                    "reason": "must be a positive integer"
                },
                {
                    "name": "color",
                    "reason": "must be 'green', 'red' or 'blue'"
                }
            ]
        }""".toJsonObjectOrNull()!!

        assertEquals(
            ProblemDetails(
                title = "Your request parameters didn't validate.",
                type = "https://example.net/validation-error",
                status = 400
            ),
            ProblemDetails.fromJSON(json)
        )
    }

    @Test
    fun `parse without a title`() {
        val json = """
            {"type": "https://example.net/validation-error"}
        """.toJsonObjectOrNull()!!

        assertNull(ProblemDetails.fromJSON(json))
    }
}
