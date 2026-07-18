@file:OptIn(InternalReadiumApi::class)

package org.readium.r2.shared.util.http

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import org.readium.r2.shared.InternalReadiumApi
import org.readium.r2.shared.util.json.toJsonObjectOrNull

class ProblemDetailsTest {

    @Test
    fun parseMinimalJson() {
        val json = """
            {"title": "You do not have enough credit."}
        """.toJsonObjectOrNull()!!

        assertEquals(
            ProblemDetails(title = "You do not have enough credit."),
            ProblemDetails.fromJSON(json)
        )
    }

    @Test
    fun parseFullJson() {
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
    fun parseWithoutATitle() {
        val json = """
            {"type": "https://example.net/validation-error"}
        """.toJsonObjectOrNull()!!

        assertNull(ProblemDetails.fromJSON(json))
    }
}
