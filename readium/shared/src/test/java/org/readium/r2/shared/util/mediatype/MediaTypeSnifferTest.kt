package org.readium.r2.shared.util.mediatype

import android.webkit.MimeTypeMap
import java.io.File
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.runner.RunWith
import org.readium.r2.shared.Fixtures
import org.readium.r2.shared.resource.DefaultArchiveFactory
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf

@RunWith(RobolectricTestRunner::class)
class MediaTypeSnifferTest {

    val fixtures = Fixtures("format")

    private val sniffer = DefaultMediaTypeSniffer()

    @Test
    fun `sniff ignores extension case`() = runBlocking {
        assertEquals(MediaType.EPUB, sniffer.sniff(fileExtension = "EPUB"))
    }

    @Test
    fun `sniff ignores media type case`() = runBlocking {
        assertEquals(
            MediaType.EPUB,
            sniffer.sniff(mediaType = "APPLICATION/EPUB+ZIP")
        )
    }

    @Test
    fun `sniff ignores media type extra parameters`() = runBlocking {
        assertEquals(
            MediaType.EPUB,
            sniffer.sniff(mediaType = "application/epub+zip;param=value")
        )
    }

    @Test
    fun `sniff from metadata`() = runBlocking {
        assertNull(sniffer.sniff(fileExtension = null))
        assertEquals(
            MediaType.READIUM_AUDIOBOOK,
            sniffer.sniff(fileExtension = "audiobook")
        )
        assertNull(sniffer.sniff(mediaType = null))
        assertEquals(
            MediaType.READIUM_AUDIOBOOK,
            sniffer.sniff(mediaType = "application/audiobook+zip")
        )
        assertEquals(
            MediaType.READIUM_AUDIOBOOK,
            sniffer.sniff(mediaType = "application/audiobook+zip")
        )
        assertEquals(
            MediaType.READIUM_AUDIOBOOK,
            sniffer.sniff(
                mediaType = "application/audiobook+zip",
                fileExtension = "audiobook"
            )
        )
        assertEquals(
            MediaType.READIUM_AUDIOBOOK,
            sniffer.sniff(
                mediaTypes = listOf("application/audiobook+zip"),
                fileExtensions = listOf("audiobook")
            )
        )
    }

    @Test
    fun `sniff from bytes`() = runBlocking {
        assertEquals(
            MediaType.READIUM_AUDIOBOOK_MANIFEST,
            sniffer.sniffResource(fixtures.fileAt("audiobook.json"))
        )
    }

    @Test
    fun `sniff unknown format`() = runBlocking {
        assertNull(sniffer.sniff(mediaType = "invalid"))
        assertNull(sniffer.sniffResource(fixtures.fileAt("unknown")))
    }

    @Test
    fun `sniff audiobook`() = runBlocking {
        assertEquals(
            MediaType.READIUM_AUDIOBOOK,
            sniffer.sniff(fileExtension = "audiobook")
        )
        assertEquals(
            MediaType.READIUM_AUDIOBOOK,
            sniffer.sniff(mediaType = "application/audiobook+zip")
        )
        assertEquals(
            MediaType.READIUM_AUDIOBOOK,
            sniffer.sniffArchive(fixtures.fileAt("audiobook-package.unknown"))
        )
    }

    @Test
    fun `sniff audiobook manifest`() = runBlocking {
        assertEquals(
            MediaType.READIUM_AUDIOBOOK_MANIFEST,
            sniffer.sniff(mediaType = "application/audiobook+json")
        )
        assertEquals(
            MediaType.READIUM_AUDIOBOOK_MANIFEST,
            sniffer.sniffResource(fixtures.fileAt("audiobook.json"))
        )
        assertEquals(
            MediaType.READIUM_AUDIOBOOK_MANIFEST,
            sniffer.sniffResource(fixtures.fileAt("audiobook-wrongtype.json"))
        )
    }

    @Test
    fun `sniff BMP`() = runBlocking {
        assertEquals(MediaType.BMP, sniffer.sniff(fileExtension = "bmp"))
        assertEquals(MediaType.BMP, sniffer.sniff(fileExtension = "dib"))
        assertEquals(MediaType.BMP, sniffer.sniff(mediaType = "image/bmp"))
        assertEquals(MediaType.BMP, sniffer.sniff(mediaType = "image/x-bmp"))
    }

    @Test
    fun `sniff CBZ`() = runBlocking {
        assertEquals(MediaType.CBZ, sniffer.sniff(fileExtension = "cbz"))
        assertEquals(
            MediaType.CBZ,
            sniffer.sniff(mediaType = "application/vnd.comicbook+zip")
        )
        assertEquals(MediaType.CBZ, sniffer.sniff(mediaType = "application/x-cbz"))
        assertEquals(MediaType.CBZ, sniffer.sniff(mediaType = "application/x-cbr"))
        assertEquals(MediaType.CBZ, sniffer.sniffArchive(fixtures.fileAt("cbz.unknown")))
    }

    @Test
    fun `sniff DiViNa`() = runBlocking {
        assertEquals(MediaType.DIVINA, sniffer.sniff(fileExtension = "divina"))
        assertEquals(
            MediaType.DIVINA,
            sniffer.sniff(mediaType = "application/divina+zip")
        )
        assertEquals(
            MediaType.DIVINA,
            sniffer.sniffArchive(fixtures.fileAt("divina-package.unknown"))
        )
    }

    @Test
    fun `sniff DiViNa manifest`() = runBlocking {
        assertEquals(
            MediaType.DIVINA_MANIFEST,
            sniffer.sniff(mediaType = "application/divina+json")
        )
        assertEquals(
            MediaType.DIVINA_MANIFEST,
            sniffer.sniffResource(fixtures.fileAt("divina.json"))
        )
    }

    @Test
    fun `sniff EPUB`() = runBlocking {
        assertEquals(MediaType.EPUB, sniffer.sniff(fileExtension = "epub"))
        assertEquals(
            MediaType.EPUB,
            sniffer.sniff(mediaType = "application/epub+zip")
        )
        assertEquals(MediaType.EPUB, sniffer.sniffArchive(fixtures.fileAt("epub.unknown")))
    }

    @Test
    fun `sniff AVIF`() = runBlocking {
        assertEquals(MediaType.AVIF, sniffer.sniff(fileExtension = "avif"))
        assertEquals(MediaType.AVIF, sniffer.sniff(mediaType = "image/avif"))
    }

    @Test
    fun `sniff GIF`() = runBlocking {
        assertEquals(MediaType.GIF, sniffer.sniff(fileExtension = "gif"))
        assertEquals(MediaType.GIF, sniffer.sniff(mediaType = "image/gif"))
    }

    @Test
    fun `sniff HTML`() = runBlocking {
        assertEquals(MediaType.HTML, sniffer.sniff(fileExtension = "htm"))
        assertEquals(MediaType.HTML, sniffer.sniff(fileExtension = "html"))
        assertEquals(MediaType.HTML, sniffer.sniff(mediaType = "text/html"))
        assertEquals(MediaType.HTML, sniffer.sniffResource(fixtures.fileAt("html.unknown")))
        assertEquals(
            MediaType.HTML,
            sniffer.sniffResource(fixtures.fileAt("html-doctype-case.unknown"))
        )
    }

    @Test
    fun `sniff XHTML`() = runBlocking {
        assertEquals(MediaType.XHTML, sniffer.sniff(fileExtension = "xht"))
        assertEquals(MediaType.XHTML, sniffer.sniff(fileExtension = "xhtml"))
        assertEquals(
            MediaType.XHTML,
            sniffer.sniff(mediaType = "application/xhtml+xml")
        )
        assertEquals(MediaType.XHTML, sniffer.sniffResource(fixtures.fileAt("xhtml.unknown")))
    }

    @Test
    fun `sniff JPEG`() = runBlocking {
        assertEquals(MediaType.JPEG, sniffer.sniff(fileExtension = "jpg"))
        assertEquals(MediaType.JPEG, sniffer.sniff(fileExtension = "jpeg"))
        assertEquals(MediaType.JPEG, sniffer.sniff(fileExtension = "jpe"))
        assertEquals(MediaType.JPEG, sniffer.sniff(fileExtension = "jif"))
        assertEquals(MediaType.JPEG, sniffer.sniff(fileExtension = "jfif"))
        assertEquals(MediaType.JPEG, sniffer.sniff(fileExtension = "jfi"))
        assertEquals(MediaType.JPEG, sniffer.sniff(mediaType = "image/jpeg"))
    }

    @Test
    fun `sniff JXL`() = runBlocking {
        assertEquals(MediaType.JXL, sniffer.sniff(fileExtension = "jxl"))
        assertEquals(MediaType.JXL, sniffer.sniff(mediaType = "image/jxl"))
    }

    @Test
    fun `sniff OPDS 1 feed`() = runBlocking {
        assertEquals(
            MediaType.OPDS1,
            sniffer.sniff(mediaType = "application/atom+xml;profile=opds-catalog")
        )
        assertEquals(
            MediaType.OPDS1,
            sniffer.sniffResource(fixtures.fileAt("opds1-feed.unknown"))
        )
    }

    @Test
    fun `sniff OPDS 1 entry`() = runBlocking {
        assertEquals(
            MediaType.OPDS1_ENTRY,
            sniffer.sniff(
                mediaType = "application/atom+xml;type=entry;profile=opds-catalog"
            )
        )
        assertEquals(
            MediaType.OPDS1_ENTRY,
            sniffer.sniffResource(fixtures.fileAt("opds1-entry.unknown"))
        )
    }

    @Test
    fun `sniff OPDS 2 feed`() = runBlocking {
        assertEquals(
            MediaType.OPDS2,
            sniffer.sniff(mediaType = "application/opds+json")
        )
        assertEquals(
            MediaType.OPDS2,
            sniffer.sniffResource(fixtures.fileAt("opds2-feed.json"))
        )
    }

    @Test
    fun `sniff OPDS 2 publication`() = runBlocking {
        assertEquals(
            MediaType.OPDS2_PUBLICATION,
            sniffer.sniff(mediaType = "application/opds-publication+json")
        )
        assertEquals(
            MediaType.OPDS2_PUBLICATION,
            sniffer.sniffResource(fixtures.fileAt("opds2-publication.json"))
        )
    }

    @Test
    fun `sniff OPDS authentication document`() = runBlocking {
        assertEquals(
            MediaType.OPDS_AUTHENTICATION,
            sniffer.sniff(mediaType = "application/opds-authentication+json")
        )
        assertEquals(
            MediaType.OPDS_AUTHENTICATION,
            sniffer.sniff(mediaType = "application/vnd.opds.authentication.v1.0+json")
        )
        assertEquals(
            MediaType.OPDS_AUTHENTICATION,
            sniffer.sniffResource(fixtures.fileAt("opds-authentication.json"))
        )
    }

    @Test
    fun `sniff LCP protected audiobook`() = runBlocking {
        assertEquals(
            MediaType.LCP_PROTECTED_AUDIOBOOK,
            sniffer.sniff(fileExtension = "lcpa")
        )
        assertEquals(
            MediaType.LCP_PROTECTED_AUDIOBOOK,
            sniffer.sniff(mediaType = "application/audiobook+lcp")
        )
        assertEquals(
            MediaType.LCP_PROTECTED_AUDIOBOOK,
            sniffer.sniffArchive(fixtures.fileAt("audiobook-lcp.unknown"))
        )
    }

    @Test
    fun `sniff LCP protected PDF`() = runBlocking {
        assertEquals(
            MediaType.LCP_PROTECTED_PDF,
            sniffer.sniff(fileExtension = "lcpdf")
        )
        assertEquals(
            MediaType.LCP_PROTECTED_PDF,
            sniffer.sniff(mediaType = "application/pdf+lcp")
        )
        assertEquals(
            MediaType.LCP_PROTECTED_PDF,
            sniffer.sniffArchive(fixtures.fileAt("pdf-lcp.unknown"))
        )
    }

    @Test
    fun `sniff LCP license document`() = runBlocking {
        assertEquals(
            MediaType.LCP_LICENSE_DOCUMENT,
            sniffer.sniff(fileExtension = "lcpl")
        )
        assertEquals(
            MediaType.LCP_LICENSE_DOCUMENT,
            sniffer.sniff(mediaType = "application/vnd.readium.lcp.license.v1.0+json")
        )
        assertEquals(
            MediaType.LCP_LICENSE_DOCUMENT,
            sniffer.sniffResource(fixtures.fileAt("lcpl.unknown"))
        )
    }

    @Test
    fun `sniff LPF`() = runBlocking {
        assertEquals(MediaType.LPF, sniffer.sniff(fileExtension = "lpf"))
        assertEquals(MediaType.LPF, sniffer.sniff(mediaType = "application/lpf+zip"))
        assertEquals(MediaType.LPF, sniffer.sniffArchive(fixtures.fileAt("lpf.unknown")))
        assertEquals(
            MediaType.LPF,
            sniffer.sniffArchive(fixtures.fileAt("lpf-index-html.unknown"))
        )
    }

    @Test
    fun `sniff PDF`() = runBlocking {
        assertEquals(MediaType.PDF, sniffer.sniff(fileExtension = "pdf"))
        assertEquals(MediaType.PDF, sniffer.sniff(mediaType = "application/pdf"))
        assertEquals(MediaType.PDF, sniffer.sniffResource(fixtures.fileAt("pdf.unknown")))
    }

    @Test
    fun `sniff PNG`() = runBlocking {
        assertEquals(MediaType.PNG, sniffer.sniff(fileExtension = "png"))
        assertEquals(MediaType.PNG, sniffer.sniff(mediaType = "image/png"))
    }

    @Test
    fun `sniff TIFF`() = runBlocking {
        assertEquals(MediaType.TIFF, sniffer.sniff(fileExtension = "tiff"))
        assertEquals(MediaType.TIFF, sniffer.sniff(fileExtension = "tif"))
        assertEquals(MediaType.TIFF, sniffer.sniff(mediaType = "image/tiff"))
        assertEquals(MediaType.TIFF, sniffer.sniff(mediaType = "image/tiff-fx"))
    }

    @Test
    fun `sniff WebP`() = runBlocking {
        assertEquals(MediaType.WEBP, sniffer.sniff(fileExtension = "webp"))
        assertEquals(MediaType.WEBP, sniffer.sniff(mediaType = "image/webp"))
    }

    @Test
    fun `sniff WebPub`() = runBlocking {
        assertEquals(
            MediaType.READIUM_WEBPUB,
            sniffer.sniff(fileExtension = "webpub")
        )
        assertEquals(
            MediaType.READIUM_WEBPUB,
            sniffer.sniff(mediaType = "application/webpub+zip")
        )
        assertEquals(
            MediaType.READIUM_WEBPUB,
            sniffer.sniffArchive(fixtures.fileAt("webpub-package.unknown"))
        )
    }

    @Test
    fun `sniff WebPub manifest`() = runBlocking {
        assertEquals(
            MediaType.READIUM_WEBPUB_MANIFEST,
            sniffer.sniff(mediaType = "application/webpub+json")
        )
        assertEquals(
            MediaType.READIUM_WEBPUB_MANIFEST,
            sniffer.sniffResource(fixtures.fileAt("webpub.json"))
        )
    }

    @Test
    fun `sniff W3C WPUB manifest`() = runBlocking {
        assertEquals(
            MediaType.W3C_WPUB_MANIFEST,
            sniffer.sniffResource(fixtures.fileAt("w3c-wpub.json"))
        )
    }

    @Test
    fun `sniff ZAB`() = runBlocking {
        assertEquals(MediaType.ZAB, sniffer.sniff(fileExtension = "zab"))
        assertEquals(MediaType.ZAB, sniffer.sniffArchive(fixtures.fileAt("zab.unknown")))
    }

    @Test
    fun `sniff JSON`() = runBlocking {
        assertEquals(MediaType.JSON, sniffer.sniffResource(fixtures.fileAt("any.json")))
    }

    @Test
    fun `sniff JSON problem details`() = runBlocking {
        assertEquals(
            MediaType.JSON_PROBLEM_DETAILS,
            sniffer.sniff(mediaType = "application/problem+json")
        )
        assertEquals(
            MediaType.JSON_PROBLEM_DETAILS,
            sniffer.sniff(mediaType = "application/problem+json; charset=utf-8")
        )

        // The sniffing of a JSON document should not take precedence over the JSON problem details.
        assertEquals(
            MediaType.JSON_PROBLEM_DETAILS,
            sniffer.sniff(
                BytesContentMediaTypeSnifferContext(
                    hints = MediaTypeHints(mediaType = MediaType("application/problem+json")!!),
                    bytes = { """{"title": "Message"}""".toByteArray() }
                )
            )
        )
    }

    @Test
    fun `sniff system media types`() = runBlocking {
        shadowOf(MimeTypeMap.getSingleton()).addExtensionMimeTypMapping(
            "xlsx",
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
        )
        val xlsx = MediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")!!
        assertEquals(
            xlsx,
            sniffer.sniff(
                mediaTypes = emptyList(),
                fileExtensions = listOf("foobar", "xlsx")
            )
        )
        assertEquals(
            xlsx,
            sniffer.sniff(
                mediaTypes = listOf(
                    "applicaton/foobar",
                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
                ),
                fileExtensions = emptyList()
            )
        )
    }

    @Test
    fun `sniff system media types from bytes`() = runBlocking {
        shadowOf(MimeTypeMap.getSingleton()).addExtensionMimeTypMapping("png", "image/png")
        val png = MediaType("image/png")!!
        assertEquals(png, sniffer.sniffResource(fixtures.fileAt("png.unknown")))
    }

    // Convenience

    private suspend fun MediaTypeSniffer.sniff(
        mediaType: String? = null,
        fileExtension: String? = null
    ): MediaType? =
        sniff(
            HintMediaTypeSnifferContext(
                MediaTypeHints(
                    mediaType = mediaType?.let { MediaType(it) },
                    fileExtension = fileExtension
                )
            )
        )

    private suspend fun MediaTypeSniffer.sniff(
        mediaTypes: List<String> = emptyList(),
        fileExtensions: List<String> = emptyList()
    ): MediaType? =
        sniff(
            HintMediaTypeSnifferContext(
                MediaTypeHints(
                    mediaTypes = mediaTypes,
                    fileExtensions = fileExtensions
                )
            )
        )

    private suspend fun MediaTypeSniffer.sniffResource(file: File): MediaType? =
        sniff(BytesContentMediaTypeSnifferContext { file.readBytes() })

    private suspend fun MediaTypeSniffer.sniffArchive(
        file: File,
        hints: MediaTypeHints = MediaTypeHints()
    ): MediaType? {
        val archive = assertNotNull(DefaultArchiveFactory(this).open(file).getOrNull())

        return sniff(object : ContainerMediaTypeSnifferContext {
            override suspend fun entries(): Set<String>? =
                archive.entries()?.map { it.path }?.toSet()

            override suspend fun read(path: String): ByteArray? =
                archive.get(path).read().getOrNull()

            override val hints: MediaTypeHints = hints
        })
    }
}
