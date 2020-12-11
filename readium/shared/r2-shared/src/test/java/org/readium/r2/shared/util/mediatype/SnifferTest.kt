package org.readium.r2.shared.util.mediatype

import android.webkit.MimeTypeMap
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.runner.RunWith
import org.readium.r2.shared.Fixtures
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import kotlin.test.assertEquals
import kotlin.test.assertNull

@RunWith(AndroidJUnit4::class)
@Config(sdk = [28])
class SnifferTest {

    val fixtures = Fixtures("format")
    
    @Test
    fun `sniff ignores extension case`() = runBlocking {
        assertEquals(MediaType.EPUB, MediaType.of(fileExtension = "EPUB"))
    }

    @Test
    fun `sniff ignores media type case`() = runBlocking {
        assertEquals(MediaType.EPUB, MediaType.of(mediaType = "APPLICATION/EPUB+ZIP"))
    }

    @Test
    fun `sniff ignores media type extra parameters`() = runBlocking {
        assertEquals(MediaType.EPUB, MediaType.of(mediaType = "application/epub+zip;param=value"))
    }

    @Test
    fun `sniff from metadata`() = runBlocking {
        assertNull(MediaType.of(fileExtension = null))
        assertEquals(MediaType.READIUM_AUDIOBOOK, MediaType.of(fileExtension = "audiobook"))
        assertNull(MediaType.of(mediaType = null))
        assertEquals(MediaType.READIUM_AUDIOBOOK, MediaType.of(mediaType = "application/audiobook+zip"))
        assertEquals(MediaType.READIUM_AUDIOBOOK, MediaType.of(mediaType = "application/audiobook+zip"))
        assertEquals(MediaType.READIUM_AUDIOBOOK, MediaType.of(mediaType = "application/audiobook+zip", fileExtension = "audiobook"))
        assertEquals(MediaType.READIUM_AUDIOBOOK, MediaType.of(mediaTypes = listOf("application/audiobook+zip"), fileExtensions = listOf("audiobook")))
    }

    @Test
    fun `sniff from a file`() = runBlocking {
        assertEquals(MediaType.READIUM_AUDIOBOOK_MANIFEST, MediaType.ofFile(fixtures.fileAt("audiobook.json")))
    }

    @Test
    fun `sniff from bytes`() = runBlocking {
        assertEquals(MediaType.READIUM_AUDIOBOOK_MANIFEST, MediaType.ofBytes({ fixtures.fileAt("audiobook.json").readBytes() }))
    }

    @Test
    fun `sniff unknown format`() = runBlocking {
        assertNull(MediaType.of(mediaType = "invalid"))
        assertNull(MediaType.ofFile(fixtures.fileAt("unknown")))
    }

    @Test
    fun `sniff falls back on parsing the given media type if it's valid`() = runBlocking {
        val expected = MediaType.parse("fruit/grapes")!!
        assertEquals(expected, MediaType.of(mediaType = "fruit/grapes"))
        assertEquals(expected, MediaType.of(mediaType = "fruit/grapes"))
        assertEquals(expected, MediaType.of(mediaTypes = listOf("invalid", "fruit/grapes"), fileExtensions = emptyList()))
        assertEquals(expected, MediaType.of(mediaTypes = listOf("fruit/grapes", "vegetable/brocoli"), fileExtensions = emptyList()))
    }

    @Test
    fun `sniff audiobook`() = runBlocking {
        assertEquals(MediaType.READIUM_AUDIOBOOK, MediaType.of(fileExtension = "audiobook"))
        assertEquals(MediaType.READIUM_AUDIOBOOK, MediaType.of(mediaType = "application/audiobook+zip"))
        assertEquals(MediaType.READIUM_AUDIOBOOK, MediaType.ofFile(fixtures.fileAt("audiobook-package.unknown")))
    }

    @Test
    fun `sniff audiobook manifest`() = runBlocking {
        assertEquals(MediaType.READIUM_AUDIOBOOK_MANIFEST, MediaType.of(mediaType = "application/audiobook+json"))
        assertEquals(MediaType.READIUM_AUDIOBOOK_MANIFEST, MediaType.ofFile(fixtures.fileAt("audiobook.json")))
        assertEquals(MediaType.READIUM_AUDIOBOOK_MANIFEST, MediaType.ofFile(fixtures.fileAt("audiobook-wrongtype.json")))
    }

    @Test
    fun `sniff BMP`() = runBlocking {
        assertEquals(MediaType.BMP, MediaType.of(fileExtension = "bmp"))
        assertEquals(MediaType.BMP, MediaType.of(fileExtension = "dib"))
        assertEquals(MediaType.BMP, MediaType.of(mediaType = "image/bmp"))
        assertEquals(MediaType.BMP, MediaType.of(mediaType = "image/x-bmp"))
    }

    @Test
    fun `sniff CBZ`() = runBlocking {
        assertEquals(MediaType.CBZ, MediaType.of(fileExtension = "cbz"))
        assertEquals(MediaType.CBZ, MediaType.of(mediaType = "application/vnd.comicbook+zip"))
        assertEquals(MediaType.CBZ, MediaType.of(mediaType = "application/x-cbz"))
        assertEquals(MediaType.CBZ, MediaType.of(mediaType = "application/x-cbr"))
        assertEquals(MediaType.CBZ, MediaType.ofFile(fixtures.fileAt("cbz.unknown")))
    }

    @Test
    fun `sniff DiViNa`() = runBlocking {
        assertEquals(MediaType.DIVINA, MediaType.of(fileExtension = "divina"))
        assertEquals(MediaType.DIVINA, MediaType.of(mediaType = "application/divina+zip"))
        assertEquals(MediaType.DIVINA, MediaType.ofFile(fixtures.fileAt("divina-package.unknown")))
    }

    @Test
    fun `sniff DiViNa manifest`() = runBlocking {
        assertEquals(MediaType.DIVINA_MANIFEST, MediaType.of(mediaType = "application/divina+json"))
        assertEquals(MediaType.DIVINA_MANIFEST, MediaType.ofFile(fixtures.fileAt("divina.json")))
    }

    @Test
    fun `sniff EPUB`() = runBlocking {
        assertEquals(MediaType.EPUB, MediaType.of(fileExtension = "epub"))
        assertEquals(MediaType.EPUB, MediaType.of(mediaType = "application/epub+zip"))
        assertEquals(MediaType.EPUB, MediaType.ofFile(fixtures.fileAt("epub.unknown")))
    }

    @Test
    fun `sniff GIF`() = runBlocking {
        assertEquals(MediaType.GIF, MediaType.of(fileExtension = "gif"))
        assertEquals(MediaType.GIF, MediaType.of(mediaType = "image/gif"))
    }

    @Test
    fun `sniff HTML`() = runBlocking {
        assertEquals(MediaType.HTML, MediaType.of(fileExtension = "htm"))
        assertEquals(MediaType.HTML, MediaType.of(fileExtension = "html"))
        assertEquals(MediaType.HTML, MediaType.of(fileExtension = "xht"))
        assertEquals(MediaType.HTML, MediaType.of(fileExtension = "xhtml"))
        assertEquals(MediaType.HTML, MediaType.of(mediaType = "text/html"))
        assertEquals(MediaType.HTML, MediaType.of(mediaType = "application/xhtml+xml"))
        assertEquals(MediaType.HTML, MediaType.ofFile(fixtures.fileAt("html.unknown")))
        assertEquals(MediaType.HTML, MediaType.ofFile(fixtures.fileAt("xhtml.unknown")))
    }

    @Test
    fun `sniff JPEG`() = runBlocking {
        assertEquals(MediaType.JPEG, MediaType.of(fileExtension = "jpg"))
        assertEquals(MediaType.JPEG, MediaType.of(fileExtension = "jpeg"))
        assertEquals(MediaType.JPEG, MediaType.of(fileExtension = "jpe"))
        assertEquals(MediaType.JPEG, MediaType.of(fileExtension = "jif"))
        assertEquals(MediaType.JPEG, MediaType.of(fileExtension = "jfif"))
        assertEquals(MediaType.JPEG, MediaType.of(fileExtension = "jfi"))
        assertEquals(MediaType.JPEG, MediaType.of(mediaType = "image/jpeg"))
    }

    @Test
    fun `sniff OPDS 1 feed`() = runBlocking {
        assertEquals(MediaType.OPDS1, MediaType.of(mediaType = "application/atom+xml;profile=opds-catalog"))
        assertEquals(MediaType.OPDS1, MediaType.ofFile(fixtures.fileAt("opds1-feed.unknown")))
    }

    @Test
    fun `sniff OPDS 1 entry`() = runBlocking {
        assertEquals(MediaType.OPDS1_ENTRY, MediaType.of(mediaType = "application/atom+xml;type=entry;profile=opds-catalog"))
        assertEquals(MediaType.OPDS1_ENTRY, MediaType.ofFile(fixtures.fileAt("opds1-entry.unknown")))
    }

    @Test
    fun `sniff OPDS 2 feed`() = runBlocking {
        assertEquals(MediaType.OPDS2, MediaType.of(mediaType = "application/opds+json"))
        assertEquals(MediaType.OPDS2, MediaType.ofFile(fixtures.fileAt("opds2-feed.json")))
    }

    @Test
    fun `sniff OPDS 2 publication`() = runBlocking {
        assertEquals(MediaType.OPDS2_PUBLICATION, MediaType.of(mediaType = "application/opds-publication+json"))
        assertEquals(MediaType.OPDS2_PUBLICATION, MediaType.ofFile(fixtures.fileAt("opds2-publication.json")))
    }

    @Test
    fun `sniff OPDS authentication document`() = runBlocking {
        assertEquals(MediaType.OPDS_AUTHENTICATION, MediaType.of(mediaType = "application/opds-authentication+json"))
        assertEquals(MediaType.OPDS_AUTHENTICATION, MediaType.of(mediaType = "application/vnd.opds.authentication.v1.0+json"))
        assertEquals(MediaType.OPDS_AUTHENTICATION, MediaType.ofFile(fixtures.fileAt("opds-authentication.json")))
    }

    @Test
    fun `sniff LCP protected audiobook`() = runBlocking {
        assertEquals(MediaType.LCP_PROTECTED_AUDIOBOOK, MediaType.of(fileExtension = "lcpa"))
        assertEquals(MediaType.LCP_PROTECTED_AUDIOBOOK, MediaType.of(mediaType = "application/audiobook+lcp"))
        assertEquals(MediaType.LCP_PROTECTED_AUDIOBOOK, MediaType.ofFile(fixtures.fileAt("audiobook-lcp.unknown")))
    }

    @Test
    fun `sniff LCP protected PDF`() = runBlocking {
        assertEquals(MediaType.LCP_PROTECTED_PDF, MediaType.of(fileExtension = "lcpdf"))
        assertEquals(MediaType.LCP_PROTECTED_PDF, MediaType.of(mediaType = "application/pdf+lcp"))
        assertEquals(MediaType.LCP_PROTECTED_PDF, MediaType.ofFile(fixtures.fileAt("pdf-lcp.unknown")))
    }

    @Test
    fun `sniff LCP license document`() = runBlocking {
        assertEquals(MediaType.LCP_LICENSE_DOCUMENT, MediaType.of(fileExtension = "lcpl"))
        assertEquals(MediaType.LCP_LICENSE_DOCUMENT, MediaType.of(mediaType = "application/vnd.readium.lcp.license.v1.0+json"))
        assertEquals(MediaType.LCP_LICENSE_DOCUMENT, MediaType.ofFile(fixtures.fileAt("lcpl.unknown")))
    }

    @Test
    fun `sniff LPF`() = runBlocking {
        assertEquals(MediaType.LPF, MediaType.of(fileExtension = "lpf"))
        assertEquals(MediaType.LPF, MediaType.of(mediaType = "application/lpf+zip"))
        assertEquals(MediaType.LPF, MediaType.ofFile(fixtures.fileAt("lpf.unknown")))
        assertEquals(MediaType.LPF, MediaType.ofFile(fixtures.fileAt("lpf-index-html.unknown")))
    }

    @Test
    fun `sniff PDF`() = runBlocking {
        assertEquals(MediaType.PDF, MediaType.of(fileExtension = "pdf"))
        assertEquals(MediaType.PDF, MediaType.of(mediaType = "application/pdf"))
        assertEquals(MediaType.PDF, MediaType.ofFile(fixtures.fileAt("pdf.unknown")))
    }

    @Test
    fun `sniff PNG`() = runBlocking {
        assertEquals(MediaType.PNG, MediaType.of(fileExtension = "png"))
        assertEquals(MediaType.PNG, MediaType.of(mediaType = "image/png"))
    }

    @Test
    fun `sniff TIFF`() = runBlocking {
        assertEquals(MediaType.TIFF, MediaType.of(fileExtension = "tiff"))
        assertEquals(MediaType.TIFF, MediaType.of(fileExtension = "tif"))
        assertEquals(MediaType.TIFF, MediaType.of(mediaType = "image/tiff"))
        assertEquals(MediaType.TIFF, MediaType.of(mediaType = "image/tiff-fx"))
    }

    @Test
    fun `sniff WebP`() = runBlocking {
        assertEquals(MediaType.WEBP, MediaType.of(fileExtension = "webp"))
        assertEquals(MediaType.WEBP, MediaType.of(mediaType = "image/webp"))
    }

    @Test
    fun `sniff WebPub`() = runBlocking {
        assertEquals(MediaType.READIUM_WEBPUB, MediaType.of(fileExtension = "webpub"))
        assertEquals(MediaType.READIUM_WEBPUB, MediaType.of(mediaType = "application/webpub+zip"))
        assertEquals(MediaType.READIUM_WEBPUB, MediaType.ofFile(fixtures.fileAt("webpub-package.unknown")))
    }

    @Test
    fun `sniff WebPub manifest`() = runBlocking {
        assertEquals(MediaType.READIUM_WEBPUB_MANIFEST, MediaType.of(mediaType = "application/webpub+json"))
        assertEquals(MediaType.READIUM_WEBPUB_MANIFEST, MediaType.ofFile(fixtures.fileAt("webpub.json")))
    }

    @Test
    fun `sniff W3C WPUB manifest`() = runBlocking {
        assertEquals(MediaType.W3C_WPUB_MANIFEST, MediaType.ofFile(fixtures.fileAt("w3c-wpub.json")))
    }

    @Test
    fun `sniff ZAB`() = runBlocking {
        assertEquals(MediaType.ZAB, MediaType.of(fileExtension = "zab"))
        assertEquals(MediaType.ZAB, MediaType.ofFile(fixtures.fileAt("zab.unknown")))
    }

    @Test
    fun `sniff system media types`() = runBlocking {
        shadowOf(MimeTypeMap.getSingleton()).addExtensionMimeTypMapping("xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
        val xlsx = MediaType.parse(
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
            name = "XLSX",
            fileExtension = "xlsx"
        )!!
        assertEquals(xlsx, MediaType.of(mediaTypes = emptyList(), fileExtensions = listOf("foobar", "xlsx")))
        assertEquals(xlsx, MediaType.of(mediaTypes = listOf("applicaton/foobar", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"), fileExtensions = emptyList()))
    }

    @Test
    fun `sniff system media types from bytes`() = runBlocking {
        shadowOf(MimeTypeMap.getSingleton()).addExtensionMimeTypMapping("png", "image/png")
        val png = MediaType.parse(
            "image/png",
            name = "PNG",
            fileExtension = "png"
        )!!
        assertEquals(png, MediaType.ofFile(fixtures.fileAt("png.unknown")))
    }

}