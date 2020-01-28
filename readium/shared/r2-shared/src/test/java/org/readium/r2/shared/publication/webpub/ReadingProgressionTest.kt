package org.readium.r2.shared.publication.webpub

import org.junit.Assert.*
import org.junit.Test

class ReadingProgressionTest {

    @Test
    fun `parse reading progression`() {
        assertEquals(ReadingProgression.LTR, ReadingProgression.from("ltr"))
        assertEquals(ReadingProgression.RTL, ReadingProgression.from("rtl"))
        assertEquals(ReadingProgression.TTB, ReadingProgression.from("ttb"))
        assertEquals(ReadingProgression.BTT, ReadingProgression.from("btt"))
        assertEquals(ReadingProgression.AUTO, ReadingProgression.from("auto"))
        assertEquals(ReadingProgression.AUTO, ReadingProgression.from("foobar"))
        assertEquals(ReadingProgression.AUTO, ReadingProgression.from(null))
    }

    @Test fun `get reading progression value`() {
        assertEquals("ltr", ReadingProgression.LTR.value)
        assertEquals("rtl", ReadingProgression.RTL.value)
        assertEquals("ttb", ReadingProgression.TTB.value)
        assertEquals("btt", ReadingProgression.BTT.value)
        assertEquals("auto", ReadingProgression.AUTO.value)
    }

}