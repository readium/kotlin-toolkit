package org.readium.r2.navigator.extensions

import kotlin.test.assertEquals
import kotlin.time.Duration.Companion.seconds
import org.junit.Assert.assertNull
import org.junit.Test
import org.readium.r2.shared.InternalReadiumApi
import org.readium.r2.shared.publication.Locator

class LocatorExtensionsTest {
    @OptIn(InternalReadiumApi::class)
    @Test
    fun `get time from fragment`() {
        assertNull(Locator.Locations(fragments = listOf("t=asd")).time)
        assertNull(Locator.Locations(fragments = listOf("t=NaN")).time)
        assertNull(Locator.Locations(fragments = listOf("t=Infinity")).time)
        assertNull(Locator.Locations(fragments = listOf("t=-1.0")).time)

        assertEquals(0.0.seconds, Locator.Locations(fragments = listOf("#t=0.0")).time)
        assertEquals(0.0.seconds, Locator.Locations(fragments = listOf("t=0.0")).time)
        assertEquals(0.seconds, Locator.Locations(fragments = listOf("t=0")).time)
        assertEquals(1.seconds, Locator.Locations(fragments = listOf("t=1")).time)
        assertEquals(1.seconds, Locator.Locations(fragments = listOf("t=1.0")).time)
        assertEquals(1.5.seconds, Locator.Locations(fragments = listOf("t=1.5")).time)
        assertEquals(1800.seconds, Locator.Locations(fragments = listOf("t=1800")).time)
        assertEquals(20.seconds, Locator.Locations(fragments = listOf("t=20")).time)
    }

    @Test
    fun `htmlId from fragment`() {
        assertNull(Locator.Locations().htmlId)
        assertNull(Locator.Locations(fragments = listOf("t=0.0")).htmlId)
        assertNull(Locator.Locations(fragments = listOf("page=1")).htmlId)

        assertEquals("chapter1", Locator.Locations(fragments = listOf("chapter1")).htmlId)
    }

    @Test
    fun `page from fragment`() {
        assertNull(Locator.Locations(fragments = listOf("t=0.0")).page)
        assertNull(Locator.Locations(fragments = listOf("chapter1")).page)
        assertNull(Locator.Locations(fragments = listOf("page=NaN")).page)

        assertEquals(1, Locator.Locations(fragments = listOf("page=1")).page)
        assertEquals(10, Locator.Locations(fragments = listOf("page=10")).page)
    }
}
