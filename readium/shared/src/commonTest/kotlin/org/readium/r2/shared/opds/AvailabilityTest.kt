@file:OptIn(InternalReadiumApi::class)

package org.readium.r2.shared.opds

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import org.readium.r2.shared.InternalReadiumApi
import org.readium.r2.shared.assertJSONEquals
import org.readium.r2.shared.extensions.toInstant
import org.readium.r2.shared.util.json.toJsonObjectOrNull

class AvailabilityTest {

    @Test fun `parse JSON availability state`() {
        assertEquals(Availability.State.AVAILABLE, Availability.State("available"))
        assertEquals(Availability.State.READY, Availability.State("ready"))
        assertEquals(Availability.State.RESERVED, Availability.State("reserved"))
        assertEquals(Availability.State.UNAVAILABLE, Availability.State("unavailable"))
        assertNull(Availability.State("foobar"))
        assertNull(Availability.State(null))
    }

    @Test fun `get JSON availability state`() {
        assertEquals("available", Availability.State.AVAILABLE.value)
        assertEquals("ready", Availability.State.READY.value)
        assertEquals("reserved", Availability.State.RESERVED.value)
        assertEquals("unavailable", Availability.State.UNAVAILABLE.value)
    }

    @Test fun `parse minimal JSON availability`() {
        assertEquals(
            Availability(state = Availability.State.AVAILABLE),
            Availability.fromJSON("{\"state\": \"available\"}".toJsonObjectOrNull()!!)
        )
    }

    @Test fun `parse full JSON availability`() {
        assertEquals(
            Availability(
                state = Availability.State.AVAILABLE,
                since = ("2001-01-01T12:36:27.000Z").toInstant(),
                until = ("2001-02-01T12:36:27.000Z").toInstant()
            ),
            Availability.fromJSON(
                """{
                "state": "available",
                "since": "2001-01-01T12:36:27.000Z",
                "until": "2001-02-01T12:36:27.000Z"
            }""".toJsonObjectOrNull()!!
            )
        )
    }

    @Test fun `parse null JSON availability`() {
        assertNull(Availability.fromJSON(null))
    }

    @Test fun `parse JSON availability requires state`() {
        assertNull(Availability.fromJSON("{ \"since\": \"2001-01-01T12:36:27+0000\" }".toJsonObjectOrNull()!!))
    }

    @Test fun `get minimal JSON availability`() {
        assertEquals(
            Availability.fromJSON("{\"state\": \"available\"}".toJsonObjectOrNull()!!),
            Availability(state = Availability.State.AVAILABLE)
        )
    }

    @Test fun `get full JSON availability`() {
        assertJSONEquals(
            """{
                "state": "available",
                "since": "2001-02-01T13:36:27Z",
                "until": "2001-02-01T13:36:27Z"
            }""".toJsonObjectOrNull()!!,
            Availability(
                state = Availability.State.AVAILABLE,
                since = ("2001-02-01T13:36:27Z").toInstant(),
                until = ("2001-02-01T13:36:27Z").toInstant()
            ).toJSON()
        )
    }
}
