package org.readium.r2.shared.publication.webpub.extensions.epub

import org.junit.Assert.*
import org.junit.Test

class EpubLayoutTest {

    @Test
    fun `parse layout`() {
        assertEquals(EpubLayout.FIXED, EpubLayout.from("fixed"))
        assertEquals(EpubLayout.REFLOWABLE, EpubLayout.from("reflowable"))
        assertNull(EpubLayout.from("foobar"))
        assertNull(EpubLayout.from(null))
    }

    @Test
    fun `parse layout from EPUB rendition property`() {
        assertEquals(EpubLayout.REFLOWABLE, EpubLayout.fromEpub("reflowable"))
        assertEquals(EpubLayout.FIXED, EpubLayout.fromEpub("pre-paginated"))
        assertEquals(EpubLayout.REFLOWABLE, EpubLayout.fromEpub("foobar"))
        assertEquals(EpubLayout.FIXED, EpubLayout.fromEpub("foobar", fallback = EpubLayout.FIXED))
    }

}