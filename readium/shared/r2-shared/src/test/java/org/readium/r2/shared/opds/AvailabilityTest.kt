package org.readium.r2.shared.opds

import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import org.readium.r2.shared.assertJSONEquals
import org.readium.r2.shared.extensions.iso8601ToDate

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
            Availability.fromJSON(JSONObject("{'state': 'available'}"))
        )
    }

    @Test fun `parse full JSON availability`() {
        assertEquals(
            Availability(
                state = Availability.State.AVAILABLE,
                since = "2001-01-01T12:36:27.000Z".iso8601ToDate(),
                until = "2001-02-01T12:36:27.000Z".iso8601ToDate()
            ),
            Availability.fromJSON(JSONObject("""{
                'state': 'available',
                'since': '2001-01-01T12:36:27.000Z',
                'until': '2001-02-01T12:36:27.000Z'
            }"""))
        )
    }

    @Test fun `parse null JSON availability`() {
        assertNull(Availability.fromJSON(null))
    }

    @Test fun `parse JSON availability requires {state}`() {
        assertNull(Availability.fromJSON(JSONObject("{ 'since': '2001-01-01T12:36:27+0000' }")))
    }

    @Test fun `get minimal JSON availability`() {
        assertEquals(
            Availability.fromJSON(JSONObject("{'state': 'available'}")),
            Availability(state = Availability.State.AVAILABLE)
        )
    }

    @Test fun `get full JSON availability`() {
        assertJSONEquals(
            JSONObject("""{
                'state': 'available',
                'since': '2001-02-01T13:36:27.000Z',
                'until': '2001-02-01T13:36:27.000Z'
            }"""),
            Availability(
                state = Availability.State.AVAILABLE,
                since = "2001-02-01T13:36:27.000Z".iso8601ToDate(),
                until = "2001-02-01T13:36:27.000Z".iso8601ToDate()
            ).toJSON()
        )
    }

}