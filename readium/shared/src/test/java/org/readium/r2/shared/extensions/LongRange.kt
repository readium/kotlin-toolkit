package org.readium.r2.shared.extensions

import org.junit.Test
import kotlin.test.assertEquals

class LongRangeTest {

    @Test
    fun `coerceIn is correct`() {
        assertEquals((25L..30), (25L..30).coerceIn(22L..32))
        assertEquals((27L..28), (25L..30).coerceIn(27L..28))
        assertEquals((27L..30), (25L..30).coerceIn(27L..32))
        assertEquals((25L..28), (25L..30).coerceIn(22L..28))
        assertEquals(0L until 0, (25L..30).coerceIn(0L until 0))
        assertEquals(0L until 0, (25L..30).coerceIn(32L until 34))
        assertEquals(0L until 0, (25L..30).coerceIn(0L until 25))
    }
}