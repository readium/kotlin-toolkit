package org.readium.r2.shared.util.mediatype

import kotlinx.coroutines.runBlocking
import org.junit.Test
import kotlin.test.*

class MediaTypeTest {

    @Test
    fun `returns null for invalid types`() {
        assertNull(MediaType.parse("application"))
        assertNull(MediaType.parse("application/atom+xml/extra"))
    }

    @Test
    fun `to string`() {
        assertEquals(
            "application/atom+xml;profile=opds-catalog",
            MediaType.parse("application/atom+xml;profile=opds-catalog")?.toString()
        )
    }

    @Test
    fun `to string is normalized`() {
        assertEquals(
            "application/atom+xml;a=0;profile=OPDS-CATALOG",
            MediaType.parse("APPLICATION/ATOM+XML;PROFILE=OPDS-CATALOG   ;   a=0")?.toString()
        )
        // Parameters are sorted by name
        assertEquals(
            "application/atom+xml;a=0;b=1",
            MediaType.parse("application/atom+xml;a=0;b=1")?.toString()
        )
        assertEquals(
            "application/atom+xml;a=0;b=1",
            MediaType.parse("application/atom+xml;b=1;a=0")?.toString()
        )
    }

    @Test
    fun `get type`() {
        assertEquals(
            "application",
            MediaType.parse("application/atom+xml;profile=opds-catalog")?.type
        )
        assertEquals("*", MediaType.parse("*/jpeg")?.type)
    }

    @Test
    fun `get subtype`() {
        assertEquals(
            "atom+xml",
            MediaType.parse("application/atom+xml;profile=opds-catalog")?.subtype
        )
        assertEquals("*", MediaType.parse("image/*")?.subtype)
    }

    @Test
    fun `get parameters`() {
        assertEquals(
            mapOf(
                "type" to "entry",
                "profile" to "opds-catalog"
            ),
            MediaType.parse("application/atom+xml;type=entry;profile=opds-catalog")?.parameters
        )
    }

    @Test
    fun `get empty parameters`() {
        assertTrue(MediaType.parse("application/atom+xml")!!.parameters.isEmpty())
    }

    @Test
    fun `get parameters with whitespaces`() {
        assertEquals(
            mapOf(
                "type" to "entry",
                "profile" to "opds-catalog"
            ),
            MediaType.parse("application/atom+xml    ;    type=entry   ;    profile=opds-catalog   ")?.parameters
        )
    }

    @Test
    fun `get structured syntax suffix`() {
        assertNull(MediaType.parse("foo/bar")?.structuredSyntaxSuffix)
        assertNull(MediaType.parse("application/zip")?.structuredSyntaxSuffix)
        assertEquals("+zip", MediaType.parse("application/epub+zip")?.structuredSyntaxSuffix)
        assertEquals("+zip", MediaType.parse("foo/bar+json+zip")?.structuredSyntaxSuffix)
    }

    @Test
    fun `get charset`() {
        assertNull(MediaType.parse("text/html")?.charset)
        assertEquals(Charsets.UTF_8, MediaType.parse("text/html;charset=utf-8")?.charset)
        assertEquals(Charsets.UTF_16, MediaType.parse("text/html;charset=utf-16")?.charset)
    }

    @Test
    fun `type, subtype and parameter names are lowercased`() {
        val mediaType = MediaType.parse("APPLICATION/ATOM+XML;PROFILE=OPDS-CATALOG")
        assertEquals("application", mediaType?.type)
        assertEquals("atom+xml", mediaType?.subtype)
        assertEquals(mapOf("profile" to "OPDS-CATALOG"), mediaType?.parameters)
    }

    @Test
    fun `charset value is uppercased`() {
        assertEquals("UTF-8", MediaType.parse("text/html;charset=utf-8")?.parameters?.get("charset"))
    }

    @Test
    fun `charset value is canonicalized`() {
        assertEquals("US-ASCII", MediaType.parse("text/html;charset=ascii")?.parameters?.get("charset"))
        assertEquals("UNKNOWN", MediaType.parse("text/html;charset=unknown")?.parameters?.get("charset"))
    }

    @Test
    fun `canonicalize media type`() = runBlocking {
        assertEquals(MediaType.parse("text/html", fileExtension = "html")!!, MediaType.parse("text/html;charset=utf-8")!!.canonicalMediaType())
        assertEquals(MediaType.parse("application/atom+xml;profile=opds-catalog")!!, MediaType.parse("application/atom+xml;profile=opds-catalog;charset=utf-8")!!.canonicalMediaType())
        assertEquals(MediaType.parse("application/unknown;charset=utf-8")!!, MediaType.parse("application/unknown;charset=utf-8")!!.canonicalMediaType())
    }

    @Test
    fun equality() {
        assertEquals(MediaType.parse("application/atom+xml")!!, MediaType.parse("application/atom+xml")!!)
        assertEquals(MediaType.parse("application/atom+xml;profile=opds-catalog")!!, MediaType.parse("application/atom+xml;profile=opds-catalog")!!)
        assertNotEquals(MediaType.parse("application/atom+xml")!!, MediaType.parse("application/atom")!!)
        assertNotEquals(MediaType.parse("application/atom+xml")!!, MediaType.parse("text/atom+xml")!!)
        assertNotEquals(MediaType.parse("application/atom+xml;profile=opds-catalog")!!, MediaType.parse("application/atom+xml")!!)
    }

    @Test
    fun `equality ignores case of type, subtype and parameter names`() {
        assertEquals(
            MediaType.parse("application/atom+xml;profile=opds-catalog")!!,
            MediaType.parse("APPLICATION/ATOM+XML;PROFILE=opds-catalog")!!
        )
        assertNotEquals(
            MediaType.parse("application/atom+xml;profile=opds-catalog")!!,
            MediaType.parse("APPLICATION/ATOM+XML;PROFILE=OPDS-CATALOG")!!
        )
    }

    @Test
    fun `equality ignores parameters order`() {
        assertEquals(
            MediaType.parse("application/atom+xml;type=entry;profile=opds-catalog")!!,
            MediaType.parse("application/atom+xml;profile=opds-catalog;type=entry")!!
        )
    }

    @Test
    fun `equality ignores charset case`() {
        assertEquals(
            MediaType.parse("application/atom+xml;charset=utf-8")!!,
            MediaType.parse("application/atom+xml;charset=UTF-8")!!
        )
    }

    @Test
    fun `contains equal media type`() {
        assertTrue(MediaType.parse("text/html;charset=utf-8")
            !!.contains(MediaType.parse("text/html;charset=utf-8")))
    }

    @Test
    fun `contains must match parameters`() {
        assertFalse(MediaType.parse("text/html;charset=utf-8")
            !!.contains(MediaType.parse("text/html;charset=ascii")))
        assertFalse(MediaType.parse("text/html;charset=utf-8")
            !!.contains(MediaType.parse("text/html")))
    }

    @Test
    fun `contains ignores parameters order`() {
        assertTrue(MediaType.parse("text/html;charset=utf-8;type=entry")
            !!.contains(MediaType.parse("text/html;type=entry;charset=utf-8")))
    }

    @Test
    fun `contains ignore extra parameters`() {
        assertTrue(MediaType.parse("text/html")
            !!.contains(MediaType.parse("text/html;charset=utf-8")))
    }

    @Test
    fun `contains supports wildcards`() {
        assertTrue(MediaType.parse("*/*")
            !!.contains(MediaType.parse("text/html;charset=utf-8")))
        assertTrue(MediaType.parse("text/*")
            !!.contains(MediaType.parse("text/html;charset=utf-8")))
        assertFalse(MediaType.parse("text/*")
            !!.contains(MediaType.parse("application/zip")))
    }

    @Test
    fun `contains from string`() {
        assertTrue(MediaType.parse("text/html;charset=utf-8")
            !!.contains("text/html;charset=utf-8"))
    }

    @Test
    fun `matches equal media type`() {
        assertTrue(MediaType.parse("text/html;charset=utf-8")
            !!.matches(MediaType.parse("text/html;charset=utf-8")))
    }

    @Test
    fun `matches must match parameters`() {
        assertFalse(MediaType.parse("text/html;charset=ascii")
            !!.matches(MediaType.parse("text/html;charset=utf-8")))
    }

    @Test
    fun `matches ignores parameters order`() {
        assertTrue(MediaType.parse("text/html;charset=utf-8;type=entry")
            !!.matches(MediaType.parse("text/html;type=entry;charset=utf-8")))
    }

    @Test
    fun `matches ignores extra parameters`() {
        assertTrue(MediaType.parse("text/html;charset=utf-8")
            !!.matches(MediaType.parse("text/html;charset=utf-8;extra=param")))
        assertTrue(MediaType.parse("text/html;charset=utf-8;extra=param")
            !!.matches(MediaType.parse("text/html;charset=utf-8")))
    }

    @Test
    fun `matches supports wildcards`() {
        assertTrue(MediaType.parse("text/html;charset=utf-8")!!.matches(MediaType.parse("*/*")))
        assertTrue(MediaType.parse("text/html;charset=utf-8")!!.matches(MediaType.parse("text/*")))
        assertFalse(MediaType.parse("application/zip")!!.matches(MediaType.parse("text/*")))
        assertTrue(MediaType.parse("*/*")!!.matches(MediaType.parse("text/html;charset=utf-8")))
        assertTrue(MediaType.parse("text/*")!!.matches(MediaType.parse("text/html;charset=utf-8")))
        assertFalse(MediaType.parse("text/*")!!.matches(MediaType.parse("application/zip")))
    }

    @Test
    fun `matches from string`() {
        assertTrue(MediaType.parse("text/html;charset=utf-8")
            !!.matches("text/html;charset=utf-8"))
    }

    @Test
    fun `matches any media type`() {
        assertTrue(MediaType.parse("text/html")
            !!.matchesAny(MediaType.parse("application/zip")!!, MediaType.parse("text/html;charset=utf-8")!!))
        assertFalse(MediaType.parse("text/html")
            !!.matchesAny(MediaType.parse("application/zip")!!, MediaType.parse("text/plain;charset=utf-8")!!))
        assertTrue(MediaType.parse("text/html")
            !!.matchesAny("application/zip", "text/html;charset=utf-8"))
        assertFalse(MediaType.parse("text/html")
            !!.matchesAny("application/zip", "text/plain;charset=utf-8"))
    }

    @Test
    fun `is ZIP`() {
        assertFalse(MediaType.parse("text/plain")!!.isZip)
        assertTrue(MediaType.parse("application/zip")!!.isZip)
        assertTrue(MediaType.parse("application/zip;charset=utf-8")!!.isZip)
        assertTrue(MediaType.parse("application/epub+zip")!!.isZip)
        // These media types must be explicitly matched since they don't have any ZIP hint
        assertTrue(MediaType.parse("application/audiobook+lcp")!!.isZip)
        assertTrue(MediaType.parse("application/pdf+lcp")!!.isZip)
    }

    @Test
    fun `is JSON`() {
        assertFalse(MediaType.parse("text/plain")!!.isJson)
        assertTrue(MediaType.parse("application/json")!!.isJson)
        assertTrue(MediaType.parse("application/json;charset=utf-8")!!.isJson)
        assertTrue(MediaType.parse("application/opds+json")!!.isJson)
    }

    @Test
    fun `is OPDS`() {
        assertFalse(MediaType.parse("text/html")!!.isOpds)
        assertTrue(MediaType.parse("application/atom+xml;profile=opds-catalog")!!.isOpds)
        assertTrue(MediaType.parse("application/atom+xml;type=entry;profile=opds-catalog")!!.isOpds)
        assertTrue(MediaType.parse("application/opds+json")!!.isOpds)
        assertTrue(MediaType.parse("application/opds-publication+json")!!.isOpds)
        assertTrue(MediaType.parse("application/opds+json;charset=utf-8")!!.isOpds)
        assertTrue(MediaType.parse("application/opds-authentication+json")!!.isOpds)
    }

    @Test
    fun `is HTML`() {
        assertFalse(MediaType.parse("application/opds+json")!!.isHtml)
        assertTrue(MediaType.parse("text/html")!!.isHtml)
        assertTrue(MediaType.parse("application/xhtml+xml")!!.isHtml)
        assertTrue(MediaType.parse("text/html;charset=utf-8")!!.isHtml)
    }

    @Test
    fun `is bitmap`() {
        assertFalse(MediaType.parse("text/html")!!.isBitmap)
        assertTrue(MediaType.parse("image/bmp")!!.isBitmap)
        assertTrue(MediaType.parse("image/gif")!!.isBitmap)
        assertTrue(MediaType.parse("image/jpeg")!!.isBitmap)
        assertTrue(MediaType.parse("image/png")!!.isBitmap)
        assertTrue(MediaType.parse("image/tiff")!!.isBitmap)
        assertTrue(MediaType.parse("image/tiff")!!.isBitmap)
        assertTrue(MediaType.parse("image/tiff;charset=utf-8")!!.isBitmap)
    }

    @Test
    fun `is audio`() {
        assertFalse(MediaType.parse("text/html")!!.isAudio)
        assertTrue(MediaType.parse("audio/unknown")!!.isAudio)
        assertTrue(MediaType.parse("audio/mpeg;param=value")!!.isAudio)
    }

    @Test
    fun `is video`() {
        assertFalse(MediaType.parse("text/html")!!.isVideo)
        assertTrue(MediaType.parse("video/unknown")!!.isVideo)
        assertTrue(MediaType.parse("video/mpeg;param=value")!!.isVideo)
    }

    @Test
    fun `is RWPM`() {
        assertFalse(MediaType.parse("text/html")!!.isRwpm)
        assertTrue(MediaType.parse("application/audiobook+json")!!.isRwpm)
        assertTrue(MediaType.parse("application/divina+json")!!.isRwpm)
        assertTrue(MediaType.parse("application/webpub+json")!!.isRwpm)
        assertTrue(MediaType.parse("application/webpub+json;charset=utf-8")!!.isRwpm)
    }

    @Test
    fun `is publication`() {
        assertFalse(MediaType.parse("text/html")!!.isPublication)
        assertTrue(MediaType.parse("application/audiobook+zip")!!.isPublication)
        assertTrue(MediaType.parse("application/audiobook+json")!!.isPublication)
        assertTrue(MediaType.parse("application/audiobook+lcp")!!.isPublication)
        assertTrue(MediaType.parse("application/audiobook+json;charset=utf-8")!!.isPublication)
        assertTrue(MediaType.parse("application/divina+zip")!!.isPublication)
        assertTrue(MediaType.parse("application/divina+json")!!.isPublication)
        assertTrue(MediaType.parse("application/webpub+zip")!!.isPublication)
        assertTrue(MediaType.parse("application/webpub+json")!!.isPublication)
        assertTrue(MediaType.parse("application/vnd.comicbook+zip")!!.isPublication)
        assertTrue(MediaType.parse("application/epub+zip")!!.isPublication)
        assertTrue(MediaType.parse("application/lpf+zip")!!.isPublication)
        assertTrue(MediaType.parse("application/pdf")!!.isPublication)
        assertTrue(MediaType.parse("application/pdf+lcp")!!.isPublication)
        assertTrue(MediaType.parse("application/x.readium.w3c.wpub+json")!!.isPublication)
        assertTrue(MediaType.parse("application/x.readium.zab+zip")!!.isPublication)
    }

}