package org.readium.r2.shared.util

import kotlin.test.assertEquals
import kotlin.test.assertNull
import org.junit.Test

class InstantTest {

    @Test
    fun `parses an ISO-8601 string to an Instant`() {
        assertNull(Instant.parse("invalid"))

        assertEquals(Instant.fromEpochMilliseconds(1712707200000), Instant.parse("2024-04-10"))
        assertEquals(
            Instant.fromEpochMilliseconds(1712746680000),
            Instant.parse("2024-04-10T10:58")
        )
        assertEquals(
            Instant.fromEpochMilliseconds(1712746724000),
            Instant.parse("2024-04-10T10:58:44")
        )
        assertEquals(
            Instant.fromEpochMilliseconds(1712746724000),
            Instant.parse("2024-04-10T10:58:44Z")
        )
        assertEquals(
            Instant.fromEpochMilliseconds(1712746724000),
            Instant.parse("2024-04-10T10:58:44.000Z")
        )
    }

    @Test
    fun `serializes an Instant to an ISO-8601 string`() {
        assertEquals(
            "2024-04-10T00:00:00Z",
            Instant.fromEpochMilliseconds(1712707200000).toString()
        )
        assertEquals(
            "2024-04-10T10:58:00Z",
            Instant.fromEpochMilliseconds(1712746680000).toString()
        )
        assertEquals(
            "2024-04-10T10:58:44Z",
            Instant.fromEpochMilliseconds(1712746724000).toString()
        )
        assertEquals(
            "2024-04-10T10:58:44Z",
            Instant.fromEpochMilliseconds(1712746724000).toString()
        )
        assertEquals(
            "2024-04-10T10:58:44Z",
            Instant.fromEpochMilliseconds(1712746724000).toString()
        )
    }
}
