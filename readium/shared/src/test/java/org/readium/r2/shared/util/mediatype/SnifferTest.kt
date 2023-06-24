package org.readium.r2.shared.util.mediatype

import android.webkit.MimeTypeMap
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.runner.RunWith
import org.readium.r2.shared.Fixtures
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf

@RunWith(RobolectricTestRunner::class)
class SnifferTest {

    val fixtures = Fixtures("format")

    private val mediaTypeRetriever: MediaTypeRetriever =
        MediaTypeRetriever()

    @Test
    fun `sniff ignores extension case`() = runBlocking {
        assertEquals(MediaType.EPUB, mediaTypeRetriever.retrieve(fileExtension = "EPUB"))
    }

    @Test
    fun `sniff ignores media type case`() = runBlocking {
        assertEquals(MediaType.EPUB, mediaTypeRetriever.retrieve(mediaType = "APPLICATION/EPUB+ZIP"))
    }

    @Test
    fun `sniff ignores media type extra parameters`() = runBlocking {
        assertEquals(MediaType.EPUB, mediaTypeRetriever.retrieve(mediaType = "application/epub+zip;param=value"))
    }

    @Test
    fun `sniff from metadata`() = runBlocking {
        assertNull(mediaTypeRetriever.retrieve(fileExtension = null))
        assertEquals(MediaType.READIUM_AUDIOBOOK, mediaTypeRetriever.retrieve(fileExtension = "audiobook"))
        assertNull(mediaTypeRetriever.retrieve(mediaType = null))
        assertEquals(MediaType.READIUM_AUDIOBOOK, mediaTypeRetriever.retrieve(mediaType = "application/audiobook+zip"))
        assertEquals(MediaType.READIUM_AUDIOBOOK, mediaTypeRetriever.retrieve(mediaType = "application/audiobook+zip"))
        assertEquals(MediaType.READIUM_AUDIOBOOK, mediaTypeRetriever.retrieve(mediaType = "application/audiobook+zip", fileExtension = "audiobook"))
        assertEquals(MediaType.READIUM_AUDIOBOOK, mediaTypeRetriever.retrieve(mediaTypes = listOf("application/audiobook+zip"), fileExtensions = listOf("audiobook")))
    }

    @Test
    fun `sniff from a file`() = runBlocking {
        assertEquals(MediaType.READIUM_AUDIOBOOK_MANIFEST, mediaTypeRetriever.retrieve(fixtures.fileAt("audiobook.json")))
    }

    @Test
    fun `sniff from bytes`() = runBlocking {
        assertEquals(MediaType.READIUM_AUDIOBOOK_MANIFEST, mediaTypeRetriever.retrieve({ fixtures.fileAt("audiobook.json").readBytes() }))
    }

    @Test
    fun `sniff unknown format`() = runBlocking {
        assertNull(mediaTypeRetriever.retrieve(mediaType = "invalid"))
        assertNull(mediaTypeRetriever.retrieve(fixtures.fileAt("unknown")))
    }

    @Test
    fun `sniff falls back on parsing the given media type if it's valid`() = runBlocking {
        val expected = MediaType.parse("fruit/grapes")!!
        assertEquals(expected, mediaTypeRetriever.retrieve(mediaType = "fruit/grapes"))
        assertEquals(expected, mediaTypeRetriever.retrieve(mediaType = "fruit/grapes"))
        assertEquals(expected, mediaTypeRetriever.retrieve(mediaTypes = listOf("invalid", "fruit/grapes"), fileExtensions = emptyList()))
        assertEquals(expected, mediaTypeRetriever.retrieve(mediaTypes = listOf("fruit/grapes", "vegetable/brocoli"), fileExtensions = emptyList()))
    }

    @Test
    fun `sniff audiobook`() = runBlocking {
        assertEquals(MediaType.READIUM_AUDIOBOOK, mediaTypeRetriever.retrieve(fileExtension = "audiobook"))
        assertEquals(MediaType.READIUM_AUDIOBOOK, mediaTypeRetriever.retrieve(mediaType = "application/audiobook+zip"))
        assertEquals(MediaType.READIUM_AUDIOBOOK, mediaTypeRetriever.retrieve(fixtures.fileAt("audiobook-package.unknown")))
    }

    @Test
    fun `sniff audiobook manifest`() = runBlocking {
        assertEquals(MediaType.READIUM_AUDIOBOOK_MANIFEST, mediaTypeRetriever.retrieve(mediaType = "application/audiobook+json"))
        assertEquals(MediaType.READIUM_AUDIOBOOK_MANIFEST, mediaTypeRetriever.retrieve(fixtures.fileAt("audiobook.json")))
        assertEquals(MediaType.READIUM_AUDIOBOOK_MANIFEST, mediaTypeRetriever.retrieve(fixtures.fileAt("audiobook-wrongtype.json")))
    }

    @Test
    fun `sniff BMP`() = runBlocking {
        assertEquals(MediaType.BMP, mediaTypeRetriever.retrieve(fileExtension = "bmp"))
        assertEquals(MediaType.BMP, mediaTypeRetriever.retrieve(fileExtension = "dib"))
        assertEquals(MediaType.BMP, mediaTypeRetriever.retrieve(mediaType = "image/bmp"))
        assertEquals(MediaType.BMP, mediaTypeRetriever.retrieve(mediaType = "image/x-bmp"))
    }

    @Test
    fun `sniff CBZ`() = runBlocking {
        assertEquals(MediaType.CBZ, mediaTypeRetriever.retrieve(fileExtension = "cbz"))
        assertEquals(MediaType.CBZ, mediaTypeRetriever.retrieve(mediaType = "application/vnd.comicbook+zip"))
        assertEquals(MediaType.CBZ, mediaTypeRetriever.retrieve(mediaType = "application/x-cbz"))
        assertEquals(MediaType.CBZ, mediaTypeRetriever.retrieve(mediaType = "application/x-cbr"))
        assertEquals(MediaType.CBZ, mediaTypeRetriever.retrieve(fixtures.fileAt("cbz.unknown")))
    }

    @Test
    fun `sniff DiViNa`() = runBlocking {
        assertEquals(MediaType.DIVINA, mediaTypeRetriever.retrieve(fileExtension = "divina"))
        assertEquals(MediaType.DIVINA, mediaTypeRetriever.retrieve(mediaType = "application/divina+zip"))
        assertEquals(MediaType.DIVINA, mediaTypeRetriever.retrieve(fixtures.fileAt("divina-package.unknown")))
    }

    @Test
    fun `sniff DiViNa manifest`() = runBlocking {
        assertEquals(MediaType.DIVINA_MANIFEST, mediaTypeRetriever.retrieve(mediaType = "application/divina+json"))
        assertEquals(MediaType.DIVINA_MANIFEST, mediaTypeRetriever.retrieve(fixtures.fileAt("divina.json")))
    }

    @Test
    fun `sniff EPUB`() = runBlocking {
        assertEquals(MediaType.EPUB, mediaTypeRetriever.retrieve(fileExtension = "epub"))
        assertEquals(MediaType.EPUB, mediaTypeRetriever.retrieve(mediaType = "application/epub+zip"))
        assertEquals(MediaType.EPUB, mediaTypeRetriever.retrieve(fixtures.fileAt("epub.unknown")))
    }

    @Test
    fun `sniff AVIF`() = runBlocking {
        assertEquals(MediaType.AVIF, mediaTypeRetriever.retrieve(fileExtension = "avif"))
        assertEquals(MediaType.AVIF, mediaTypeRetriever.retrieve(mediaType = "image/avif"))
    }

    @Test
    fun `sniff GIF`() = runBlocking {
        assertEquals(MediaType.GIF, mediaTypeRetriever.retrieve(fileExtension = "gif"))
        assertEquals(MediaType.GIF, mediaTypeRetriever.retrieve(mediaType = "image/gif"))
    }

    @Test
    fun `sniff HTML`() = runBlocking {
        assertEquals(MediaType.HTML, mediaTypeRetriever.retrieve(fileExtension = "htm"))
        assertEquals(MediaType.HTML, mediaTypeRetriever.retrieve(fileExtension = "html"))
        assertEquals(MediaType.HTML, mediaTypeRetriever.retrieve(mediaType = "text/html"))
        assertEquals(MediaType.HTML, mediaTypeRetriever.retrieve(fixtures.fileAt("html.unknown")))
        assertEquals(MediaType.HTML, mediaTypeRetriever.retrieve(fixtures.fileAt("html-doctype-case.unknown")))
    }

    @Test
    fun `sniff XHTML`() = runBlocking {
        assertEquals(MediaType.XHTML, mediaTypeRetriever.retrieve(fileExtension = "xht"))
        assertEquals(MediaType.XHTML, mediaTypeRetriever.retrieve(fileExtension = "xhtml"))
        assertEquals(MediaType.XHTML, mediaTypeRetriever.retrieve(mediaType = "application/xhtml+xml"))
        assertEquals(MediaType.XHTML, mediaTypeRetriever.retrieve(fixtures.fileAt("xhtml.unknown")))
    }

    @Test
    fun `sniff JPEG`() = runBlocking {
        assertEquals(MediaType.JPEG, mediaTypeRetriever.retrieve(fileExtension = "jpg"))
        assertEquals(MediaType.JPEG, mediaTypeRetriever.retrieve(fileExtension = "jpeg"))
        assertEquals(MediaType.JPEG, mediaTypeRetriever.retrieve(fileExtension = "jpe"))
        assertEquals(MediaType.JPEG, mediaTypeRetriever.retrieve(fileExtension = "jif"))
        assertEquals(MediaType.JPEG, mediaTypeRetriever.retrieve(fileExtension = "jfif"))
        assertEquals(MediaType.JPEG, mediaTypeRetriever.retrieve(fileExtension = "jfi"))
        assertEquals(MediaType.JPEG, mediaTypeRetriever.retrieve(mediaType = "image/jpeg"))
    }

    @Test
    fun `sniff JXL`() = runBlocking {
        assertEquals(MediaType.JXL, mediaTypeRetriever.retrieve(fileExtension = "jxl"))
        assertEquals(MediaType.JXL, mediaTypeRetriever.retrieve(mediaType = "image/jxl"))
    }

    @Test
    fun `sniff OPDS 1 feed`() = runBlocking {
        assertEquals(MediaType.OPDS1, mediaTypeRetriever.retrieve(mediaType = "application/atom+xml;profile=opds-catalog"))
        assertEquals(MediaType.OPDS1, mediaTypeRetriever.retrieve(fixtures.fileAt("opds1-feed.unknown")))
    }

    @Test
    fun `sniff OPDS 1 entry`() = runBlocking {
        assertEquals(MediaType.OPDS1_ENTRY, mediaTypeRetriever.retrieve(mediaType = "application/atom+xml;type=entry;profile=opds-catalog"))
        assertEquals(MediaType.OPDS1_ENTRY, mediaTypeRetriever.retrieve(fixtures.fileAt("opds1-entry.unknown")))
    }

    @Test
    fun `sniff OPDS 2 feed`() = runBlocking {
        assertEquals(MediaType.OPDS2, mediaTypeRetriever.retrieve(mediaType = "application/opds+json"))
        assertEquals(MediaType.OPDS2, mediaTypeRetriever.retrieve(fixtures.fileAt("opds2-feed.json")))
    }

    @Test
    fun `sniff OPDS 2 publication`() = runBlocking {
        assertEquals(MediaType.OPDS2_PUBLICATION, mediaTypeRetriever.retrieve(mediaType = "application/opds-publication+json"))
        assertEquals(MediaType.OPDS2_PUBLICATION, mediaTypeRetriever.retrieve(fixtures.fileAt("opds2-publication.json")))
    }

    @Test
    fun `sniff OPDS authentication document`() = runBlocking {
        assertEquals(MediaType.OPDS_AUTHENTICATION, mediaTypeRetriever.retrieve(mediaType = "application/opds-authentication+json"))
        assertEquals(MediaType.OPDS_AUTHENTICATION, mediaTypeRetriever.retrieve(mediaType = "application/vnd.opds.authentication.v1.0+json"))
        assertEquals(MediaType.OPDS_AUTHENTICATION, mediaTypeRetriever.retrieve(fixtures.fileAt("opds-authentication.json")))
    }

    @Test
    fun `sniff LCP protected audiobook`() = runBlocking {
        assertEquals(MediaType.LCP_PROTECTED_AUDIOBOOK, mediaTypeRetriever.retrieve(fileExtension = "lcpa"))
        assertEquals(MediaType.LCP_PROTECTED_AUDIOBOOK, mediaTypeRetriever.retrieve(mediaType = "application/audiobook+lcp"))
        assertEquals(MediaType.LCP_PROTECTED_AUDIOBOOK, mediaTypeRetriever.retrieve(fixtures.fileAt("audiobook-lcp.unknown")))
    }

    @Test
    fun `sniff LCP protected PDF`() = runBlocking {
        assertEquals(MediaType.LCP_PROTECTED_PDF, mediaTypeRetriever.retrieve(fileExtension = "lcpdf"))
        assertEquals(MediaType.LCP_PROTECTED_PDF, mediaTypeRetriever.retrieve(mediaType = "application/pdf+lcp"))
        assertEquals(MediaType.LCP_PROTECTED_PDF, mediaTypeRetriever.retrieve(fixtures.fileAt("pdf-lcp.unknown")))
    }

    @Test
    fun `sniff LCP license document`() = runBlocking {
        assertEquals(MediaType.LCP_LICENSE_DOCUMENT, mediaTypeRetriever.retrieve(fileExtension = "lcpl"))
        assertEquals(MediaType.LCP_LICENSE_DOCUMENT, mediaTypeRetriever.retrieve(mediaType = "application/vnd.readium.lcp.license.v1.0+json"))
        assertEquals(MediaType.LCP_LICENSE_DOCUMENT, mediaTypeRetriever.retrieve(fixtures.fileAt("lcpl.unknown")))
    }

    @Test
    fun `sniff LPF`() = runBlocking {
        assertEquals(MediaType.LPF, mediaTypeRetriever.retrieve(fileExtension = "lpf"))
        assertEquals(MediaType.LPF, mediaTypeRetriever.retrieve(mediaType = "application/lpf+zip"))
        assertEquals(MediaType.LPF, mediaTypeRetriever.retrieve(fixtures.fileAt("lpf.unknown")))
        assertEquals(MediaType.LPF, mediaTypeRetriever.retrieve(fixtures.fileAt("lpf-index-html.unknown")))
    }

    @Test
    fun `sniff PDF`() = runBlocking {
        assertEquals(MediaType.PDF, mediaTypeRetriever.retrieve(fileExtension = "pdf"))
        assertEquals(MediaType.PDF, mediaTypeRetriever.retrieve(mediaType = "application/pdf"))
        assertEquals(MediaType.PDF, mediaTypeRetriever.retrieve(fixtures.fileAt("pdf.unknown")))
    }

    @Test
    fun `sniff PNG`() = runBlocking {
        assertEquals(MediaType.PNG, mediaTypeRetriever.retrieve(fileExtension = "png"))
        assertEquals(MediaType.PNG, mediaTypeRetriever.retrieve(mediaType = "image/png"))
    }

    @Test
    fun `sniff TIFF`() = runBlocking {
        assertEquals(MediaType.TIFF, mediaTypeRetriever.retrieve(fileExtension = "tiff"))
        assertEquals(MediaType.TIFF, mediaTypeRetriever.retrieve(fileExtension = "tif"))
        assertEquals(MediaType.TIFF, mediaTypeRetriever.retrieve(mediaType = "image/tiff"))
        assertEquals(MediaType.TIFF, mediaTypeRetriever.retrieve(mediaType = "image/tiff-fx"))
    }

    @Test
    fun `sniff WebP`() = runBlocking {
        assertEquals(MediaType.WEBP, mediaTypeRetriever.retrieve(fileExtension = "webp"))
        assertEquals(MediaType.WEBP, mediaTypeRetriever.retrieve(mediaType = "image/webp"))
    }

    @Test
    fun `sniff WebPub`() = runBlocking {
        assertEquals(MediaType.READIUM_WEBPUB, mediaTypeRetriever.retrieve(fileExtension = "webpub"))
        assertEquals(MediaType.READIUM_WEBPUB, mediaTypeRetriever.retrieve(mediaType = "application/webpub+zip"))
        assertEquals(MediaType.READIUM_WEBPUB, mediaTypeRetriever.retrieve(fixtures.fileAt("webpub-package.unknown")))
    }

    @Test
    fun `sniff WebPub manifest`() = runBlocking {
        assertEquals(MediaType.READIUM_WEBPUB_MANIFEST, mediaTypeRetriever.retrieve(mediaType = "application/webpub+json"))
        assertEquals(MediaType.READIUM_WEBPUB_MANIFEST, mediaTypeRetriever.retrieve(fixtures.fileAt("webpub.json")))
    }

    @Test
    fun `sniff W3C WPUB manifest`() = runBlocking {
        assertEquals(MediaType.W3C_WPUB_MANIFEST, mediaTypeRetriever.retrieve(fixtures.fileAt("w3c-wpub.json")))
    }

    @Test
    fun `sniff ZAB`() = runBlocking {
        assertEquals(MediaType.ZAB, mediaTypeRetriever.retrieve(fileExtension = "zab"))
        assertEquals(MediaType.ZAB, mediaTypeRetriever.retrieve(fixtures.fileAt("zab.unknown")))
    }

    @Test
    fun `sniff JSON`() = runBlocking {
        assertEquals(MediaType.JSON, mediaTypeRetriever.retrieve(mediaType = "application/json"))
        assertEquals(MediaType.JSON, mediaTypeRetriever.retrieve(mediaType = "application/json; charset=utf-8"))
        assertEquals(MediaType.JSON, mediaTypeRetriever.retrieve(fixtures.fileAt("any.json")))
    }

    @Test
    fun `sniff JSON problem details`() = runBlocking {
        assertEquals(MediaType.JSON_PROBLEM_DETAILS, mediaTypeRetriever.retrieve(mediaType = "application/problem+json"))
        assertEquals(MediaType.JSON_PROBLEM_DETAILS, mediaTypeRetriever.retrieve(mediaType = "application/problem+json; charset=utf-8"))

        // The sniffing of a JSON document should not take precedence over the JSON problem details.
        assertEquals(MediaType.JSON_PROBLEM_DETAILS, mediaTypeRetriever.retrieve({ """{"title": "Message"}""".toByteArray() }, mediaType = "application/problem+json"))
    }

    @Test
    fun `sniff system media types`() = runBlocking {
        shadowOf(MimeTypeMap.getSingleton()).addExtensionMimeTypMapping("xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
        val xlsx = MediaType.parse(
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
            name = "XLSX",
            fileExtension = "xlsx"
        )!!
        assertEquals(xlsx, mediaTypeRetriever.retrieve(mediaTypes = emptyList(), fileExtensions = listOf("foobar", "xlsx")))
        assertEquals(xlsx, mediaTypeRetriever.retrieve(mediaTypes = listOf("applicaton/foobar", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"), fileExtensions = emptyList()))
    }

    @Test
    fun `sniff system media types from bytes`() = runBlocking {
        shadowOf(MimeTypeMap.getSingleton()).addExtensionMimeTypMapping("png", "image/png")
        val png = MediaType.parse(
            "image/png",
            name = "PNG",
            fileExtension = "png"
        )!!
        assertEquals(png, mediaTypeRetriever.retrieve(fixtures.fileAt("png.unknown")))
    }
}
