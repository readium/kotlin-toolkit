package org.readium.r2.shared.publication

import org.junit.Assert.*
import org.junit.Test
import org.readium.r2.shared.publication.ReadingProgression

class ReadingProgressionTest {

    @Test
    fun `parse reading progression`() {
        assertEquals(ReadingProgression.LTR, ReadingProgression("LTR"))
        assertEquals(ReadingProgression.LTR, ReadingProgression("ltr"))
        assertEquals(ReadingProgression.RTL, ReadingProgression("rtl"))
        assertEquals(ReadingProgression.TTB, ReadingProgression("ttb"))
        assertEquals(ReadingProgression.BTT, ReadingProgression("btt"))
        assertEquals(ReadingProgression.AUTO, ReadingProgression("auto"))
        assertEquals(ReadingProgression.AUTO, ReadingProgression("foobar"))
        assertEquals(ReadingProgression.AUTO, ReadingProgression(null))
    }

    @Test fun `get reading progression value`() {
        assertEquals("ltr", ReadingProgression.LTR.value)
        assertEquals("rtl", ReadingProgression.RTL.value)
        assertEquals("ttb", ReadingProgression.TTB.value)
        assertEquals("btt", ReadingProgression.BTT.value)
        assertEquals("auto", ReadingProgression.AUTO.value)
    }

}
