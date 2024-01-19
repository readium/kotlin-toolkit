package org.readium.r2.shared.util.mediatype

import kotlin.test.*
import org.junit.Test

class MediaTypeTest {

    @Test
    fun `returns null for invalid types`() {
        assertNull(MediaType("application"))
        assertNull(MediaType("application/atom+xml/extra"))
    }

    @Test
    fun `to string`() {
        assertEquals(
            "application/atom+xml;profile=opds-catalog",
            MediaType("application/atom+xml;profile=opds-catalog")?.toString()
        )
    }

    @Test
    fun `to string is normalized`() {
        assertEquals(
            "application/atom+xml;a=0;profile=OPDS-CATALOG",
            MediaType("APPLICATION/ATOM+XML;PROFILE=OPDS-CATALOG   ;   a=0")?.toString()
        )
        // Parameters are sorted by name
        assertEquals(
            "application/atom+xml;a=0;b=1",
            MediaType("application/atom+xml;a=0;b=1")?.toString()
        )
        assertEquals(
            "application/atom+xml;a=0;b=1",
            MediaType("application/atom+xml;b=1;a=0")?.toString()
        )
    }

    @Test
    fun `get type`() {
        assertEquals(
            "application",
            MediaType("application/atom+xml;profile=opds-catalog")?.type
        )
        assertEquals("*", MediaType("*/jpeg")?.type)
    }

    @Test
    fun `get subtype`() {
        assertEquals(
            "atom+xml",
            MediaType("application/atom+xml;profile=opds-catalog")?.subtype
        )
        assertEquals("*", MediaType("image/*")?.subtype)
    }

    @Test
    fun `get parameters`() {
        assertEquals(
            mapOf(
                "type" to "entry",
                "profile" to "opds-catalog"
            ),
            MediaType("application/atom+xml;type=entry;profile=opds-catalog")?.parameters
        )
    }

    @Test
    fun `get empty parameters`() {
        assertTrue(MediaType("application/atom+xml")!!.parameters.isEmpty())
    }

    @Test
    fun `get parameters with whitespaces`() {
        assertEquals(
            mapOf(
                "type" to "entry",
                "profile" to "opds-catalog"
            ),
            MediaType(
                "application/atom+xml    ;    type=entry   ;    profile=opds-catalog   "
            )?.parameters
        )
    }

    @Test
    fun `get structured syntax suffix`() {
        assertNull(MediaType("foo/bar")?.structuredSyntaxSuffix)
        assertNull(MediaType("application/zip")?.structuredSyntaxSuffix)
        assertEquals("+zip", MediaType("application/epub+zip")?.structuredSyntaxSuffix)
        assertEquals("+zip", MediaType("foo/bar+json+zip")?.structuredSyntaxSuffix)
    }

    @Test
    fun `get charset`() {
        assertNull(MediaType("text/html")?.charset)
        assertEquals(Charsets.UTF_8, MediaType("text/html;charset=utf-8")?.charset)
        assertEquals(Charsets.UTF_16, MediaType("text/html;charset=utf-16")?.charset)
    }

    @Test
    fun `type, subtype and parameter names are lowercased`() {
        val mediaType = MediaType("APPLICATION/ATOM+XML;PROFILE=OPDS-CATALOG")
        assertEquals("application", mediaType?.type)
        assertEquals("atom+xml", mediaType?.subtype)
        assertEquals(mapOf("profile" to "OPDS-CATALOG"), mediaType?.parameters)
    }

    @Test
    fun `charset value is uppercased`() {
        assertEquals(
            "UTF-8",
            MediaType("text/html;charset=utf-8")?.parameters?.get("charset")
        )
    }

    @Test
    fun `charset value is canonicalized`() {
        assertEquals(
            "US-ASCII",
            MediaType("text/html;charset=ascii")?.parameters?.get("charset")
        )
        assertEquals(
            "UNKNOWN",
            MediaType("text/html;charset=unknown")?.parameters?.get("charset")
        )
    }

    @Test
    fun equality() {
        assertEquals(
            MediaType("application/atom+xml")!!,
            MediaType("application/atom+xml")!!
        )
        assertEquals(
            MediaType("application/atom+xml;profile=opds-catalog")!!,
            MediaType("application/atom+xml;profile=opds-catalog")!!
        )
        assertNotEquals(
            MediaType("application/atom+xml")!!,
            MediaType("application/atom")!!
        )
        assertNotEquals(
            MediaType("application/atom+xml")!!,
            MediaType("text/atom+xml")!!
        )
        assertNotEquals(
            MediaType("application/atom+xml;profile=opds-catalog")!!,
            MediaType("application/atom+xml")!!
        )
    }

    @Test
    fun `equality ignores case of type, subtype and parameter names`() {
        assertEquals(
            MediaType("application/atom+xml;profile=opds-catalog")!!,
            MediaType("APPLICATION/ATOM+XML;PROFILE=opds-catalog")!!
        )
        assertNotEquals(
            MediaType("application/atom+xml;profile=opds-catalog")!!,
            MediaType("APPLICATION/ATOM+XML;PROFILE=OPDS-CATALOG")!!
        )
    }

    @Test
    fun `equality ignores parameters order`() {
        assertEquals(
            MediaType("application/atom+xml;type=entry;profile=opds-catalog")!!,
            MediaType("application/atom+xml;profile=opds-catalog;type=entry")!!
        )
    }

    @Test
    fun `equality ignores charset case`() {
        assertEquals(
            MediaType("application/atom+xml;charset=utf-8")!!,
            MediaType("application/atom+xml;charset=UTF-8")!!
        )
    }

    @Test
    fun `contains equal media type`() {
        assertTrue(
            MediaType("text/html;charset=utf-8")!!.contains(
                MediaType("text/html;charset=utf-8")
            )
        )
    }

    @Test
    fun `contains must match parameters`() {
        assertFalse(
            MediaType("text/html;charset=utf-8")!!.contains(
                MediaType("text/html;charset=ascii")
            )
        )
        assertFalse(
            MediaType("text/html;charset=utf-8")!!.contains(MediaType("text/html"))
        )
    }

    @Test
    fun `contains ignores parameters order`() {
        assertTrue(
            MediaType("text/html;charset=utf-8;type=entry")!!.contains(
                MediaType("text/html;type=entry;charset=utf-8")
            )
        )
    }

    @Test
    fun `contains ignore extra parameters`() {
        assertTrue(
            MediaType("text/html")!!.contains(MediaType("text/html;charset=utf-8"))
        )
    }

    @Test
    fun `contains supports wildcards`() {
        assertTrue(
            MediaType("*/*")!!.contains(MediaType("text/html;charset=utf-8"))
        )
        assertTrue(
            MediaType("text/*")!!.contains(MediaType("text/html;charset=utf-8"))
        )
        assertFalse(
            MediaType("text/*")!!.contains(MediaType("application/zip"))
        )
    }

    @Test
    fun `contains from string`() {
        assertTrue(
            MediaType("text/html;charset=utf-8")!!.contains("text/html;charset=utf-8")
        )
    }

    @Test
    fun `matches equal media type`() {
        assertTrue(
            MediaType("text/html;charset=utf-8")!!.matches(
                MediaType("text/html;charset=utf-8")
            )
        )
    }

    @Test
    fun `matches must match parameters`() {
        assertFalse(
            MediaType("text/html;charset=ascii")!!.matches(
                MediaType("text/html;charset=utf-8")
            )
        )
    }

    @Test
    fun `matches ignores parameters order`() {
        assertTrue(
            MediaType("text/html;charset=utf-8;type=entry")!!.matches(
                MediaType("text/html;type=entry;charset=utf-8")
            )
        )
    }

    @Test
    fun `matches ignores extra parameters`() {
        assertTrue(
            MediaType("text/html;charset=utf-8")!!.matches(
                MediaType("text/html;charset=utf-8;extra=param")
            )
        )
        assertTrue(
            MediaType("text/html;charset=utf-8;extra=param")!!.matches(
                MediaType("text/html;charset=utf-8")
            )
        )
    }

    @Test
    fun `matches supports wildcards`() {
        assertTrue(MediaType("text/html;charset=utf-8")!!.matches(MediaType("*/*")))
        assertTrue(MediaType("text/html;charset=utf-8")!!.matches(MediaType("text/*")))
        assertFalse(MediaType("application/zip")!!.matches(MediaType("text/*")))
        assertTrue(MediaType("*/*")!!.matches(MediaType("text/html;charset=utf-8")))
        assertTrue(MediaType("text/*")!!.matches(MediaType("text/html;charset=utf-8")))
        assertFalse(MediaType("text/*")!!.matches(MediaType("application/zip")))
    }

    @Test
    fun `matches from string`() {
        assertTrue(
            MediaType("text/html;charset=utf-8")!!.matches("text/html;charset=utf-8")
        )
    }

    @Test
    fun `matches any media type`() {
        assertTrue(
            MediaType("text/html")!!.matchesAny(
                MediaType("application/zip")!!,
                MediaType("text/html;charset=utf-8")!!
            )
        )
        assertFalse(
            MediaType("text/html")!!.matchesAny(
                MediaType("application/zip")!!,
                MediaType("text/plain;charset=utf-8")!!
            )
        )
        assertTrue(
            MediaType("text/html")!!.matchesAny("application/zip", "text/html;charset=utf-8")
        )
        assertFalse(
            MediaType("text/html")!!.matchesAny("application/zip", "text/plain;charset=utf-8")
        )
    }

    @Test
    fun `is ZIP`() {
        assertFalse(MediaType("text/plain")!!.isZip)
        assertTrue(MediaType("application/zip")!!.isZip)
        assertTrue(MediaType("application/zip;charset=utf-8")!!.isZip)
        assertTrue(MediaType("application/epub+zip")!!.isZip)
        // These media types must be explicitly matched since they don't have any ZIP hint
        assertTrue(MediaType("application/audiobook+lcp")!!.isZip)
        assertTrue(MediaType("application/pdf+lcp")!!.isZip)
    }

    @Test
    fun `is JSON`() {
        assertFalse(MediaType("text/plain")!!.isJson)
        assertTrue(MediaType("application/json")!!.isJson)
        assertTrue(MediaType("application/json;charset=utf-8")!!.isJson)
        assertTrue(MediaType("application/opds+json")!!.isJson)
    }

    @Test
    fun `is OPDS`() {
        assertFalse(MediaType("text/html")!!.isOpds)
        assertTrue(MediaType("application/atom+xml;profile=opds-catalog")!!.isOpds)
        assertTrue(MediaType("application/atom+xml;type=entry;profile=opds-catalog")!!.isOpds)
        assertTrue(MediaType("application/opds+json")!!.isOpds)
        assertTrue(MediaType("application/opds-publication+json")!!.isOpds)
        assertTrue(MediaType("application/opds+json;charset=utf-8")!!.isOpds)
        assertTrue(MediaType("application/opds-authentication+json")!!.isOpds)
    }

    @Test
    fun `is HTML`() {
        assertFalse(MediaType("application/opds+json")!!.isHtml)
        assertTrue(MediaType("text/html")!!.isHtml)
        assertTrue(MediaType("application/xhtml+xml")!!.isHtml)
        assertTrue(MediaType("text/html;charset=utf-8")!!.isHtml)
    }

    @Test
    fun `is bitmap`() {
        assertFalse(MediaType("text/html")!!.isBitmap)
        assertTrue(MediaType("image/bmp")!!.isBitmap)
        assertTrue(MediaType("image/gif")!!.isBitmap)
        assertTrue(MediaType("image/jpeg")!!.isBitmap)
        assertTrue(MediaType("image/png")!!.isBitmap)
        assertTrue(MediaType("image/tiff")!!.isBitmap)
        assertTrue(MediaType("image/tiff")!!.isBitmap)
        assertTrue(MediaType("image/tiff;charset=utf-8")!!.isBitmap)
    }

    @Test
    fun `is audio`() {
        assertFalse(MediaType("text/html")!!.isAudio)
        assertTrue(MediaType("audio/unknown")!!.isAudio)
        assertTrue(MediaType("audio/mpeg;param=value")!!.isAudio)
    }

    @Test
    fun `is video`() {
        assertFalse(MediaType("text/html")!!.isVideo)
        assertTrue(MediaType("video/unknown")!!.isVideo)
        assertTrue(MediaType("video/mpeg;param=value")!!.isVideo)
    }

    @Test
    fun `is RWPM`() {
        assertFalse(MediaType("text/html")!!.isRwpm)
        assertTrue(MediaType("application/audiobook+json")!!.isRwpm)
        assertTrue(MediaType("application/divina+json")!!.isRwpm)
        assertTrue(MediaType("application/webpub+json")!!.isRwpm)
        assertTrue(MediaType("application/webpub+json;charset=utf-8")!!.isRwpm)
    }

    @Test
    fun `is publication`() {
        assertFalse(MediaType("text/html")!!.isPublication)
        assertTrue(MediaType("application/audiobook+zip")!!.isPublication)
        assertTrue(MediaType("application/audiobook+json")!!.isPublication)
        assertTrue(MediaType("application/audiobook+lcp")!!.isPublication)
        assertTrue(MediaType("application/audiobook+json;charset=utf-8")!!.isPublication)
        assertTrue(MediaType("application/divina+zip")!!.isPublication)
        assertTrue(MediaType("application/divina+json")!!.isPublication)
        assertTrue(MediaType("application/webpub+zip")!!.isPublication)
        assertTrue(MediaType("application/webpub+json")!!.isPublication)
        assertTrue(MediaType("application/vnd.comicbook+zip")!!.isPublication)
        assertTrue(MediaType("application/epub+zip")!!.isPublication)
        assertTrue(MediaType("application/lpf+zip")!!.isPublication)
        assertTrue(MediaType("application/pdf")!!.isPublication)
        assertTrue(MediaType("application/pdf+lcp")!!.isPublication)
        assertTrue(MediaType("application/x.readium.w3c.wpub+json")!!.isPublication)
        assertTrue(MediaType("application/x.readium.zab+zip")!!.isPublication)
    }
}
