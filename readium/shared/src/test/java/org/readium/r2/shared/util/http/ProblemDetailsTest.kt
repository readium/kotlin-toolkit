package org.readium.r2.shared.util.http

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ProblemDetailsTest {

    @Test
    fun `parse minimal JSON`() {
        val json = JSONObject(
            """
            {"title": "You do not have enough credit."}
        """
        )

        assertEquals(ProblemDetails(title = "You do not have enough credit."), ProblemDetails.fromJSON(json))
    }

    @Test
    fun `parse full JSON`() {
        val json = JSONObject(
            """{
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
        }"""
        )

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
        val json = JSONObject(
            """
            {"type": "https://example.net/validation-error"}
        """
        )

        assertNull(ProblemDetails.fromJSON(json))
    }
}
