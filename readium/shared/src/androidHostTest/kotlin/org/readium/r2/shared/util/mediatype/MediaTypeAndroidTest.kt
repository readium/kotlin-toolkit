package org.readium.r2.shared.util.mediatype

import kotlin.test.assertEquals
import kotlin.test.assertNull
import org.junit.Test

class MediaTypeAndroidTest {

    @Test
    fun `get charset`() {
        assertNull(MediaType("text/html")?.charset)
        assertEquals(Charsets.UTF_8, MediaType("text/html;charset=utf-8")?.charset)
        assertEquals(Charsets.UTF_16, MediaType("text/html;charset=utf-16")?.charset)
    }

    @Test
    fun `get unknown charset`() {
        assertNull(MediaType("text/html;charset=unknown")?.charset)
    }
}
