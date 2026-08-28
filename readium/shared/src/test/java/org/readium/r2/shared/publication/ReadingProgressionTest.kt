package org.readium.r2.shared.publication

import org.junit.Assert.*
import org.junit.Test

class ReadingProgressionTest {

    @Test
    fun `parse reading progression`() {
        assertEquals(ReadingProgression.LTR, ReadingProgression("LTR"))
        assertEquals(ReadingProgression.LTR, ReadingProgression("ltr"))
        assertEquals(ReadingProgression.RTL, ReadingProgression("rtl"))
        assertEquals(null, ReadingProgression("auto"))
        assertEquals(null, ReadingProgression("foobar"))
        assertEquals(null, ReadingProgression(null))
    }

    @Test fun `get reading progression value`() {
        assertEquals("ltr", ReadingProgression.LTR.value)
        assertEquals("rtl", ReadingProgression.RTL.value)
    }
}
