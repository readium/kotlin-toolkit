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
import org.readium.r2.shared.util.Url
import org.readium.r2.shared.util.resource.FileZipArchiveFactory
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf

@RunWith(RobolectricTestRunner::class)
class MediaTypeRetrieverTest {

    val fixtures = Fixtures("util/mediatype")

    private val retriever = MediaTypeRetriever()

    @Test
    fun `sniff ignores extension case`() = runBlocking {
        assertEquals(MediaType.EPUB, retriever.retrieve(fileExtension = "EPUB"))
    }

    @Test
    fun `sniff ignores media type case`() = runBlocking {
        assertEquals(
            MediaType.EPUB,
            retriever.retrieve(mediaType = "APPLICATION/EPUB+ZIP")
        )
    }

    @Test
    fun `sniff ignores media type extra parameters`() = runBlocking {
        assertEquals(
            MediaType.EPUB,
            retriever.retrieve(mediaType = "application/epub+zip;param=value")
        )
    }

    @Test
    fun `sniff from metadata`() = runBlocking {
        assertNull(retriever.retrieve(fileExtension = null))
        assertEquals(
            MediaType.READIUM_AUDIOBOOK,
            retriever.retrieve(fileExtension = "audiobook")
        )
        assertNull(retriever.retrieve(mediaType = null))
        assertEquals(
            MediaType.READIUM_AUDIOBOOK,
            retriever.retrieve(mediaType = "application/audiobook+zip")
        )
        assertEquals(
            MediaType.READIUM_AUDIOBOOK,
            retriever.retrieve(mediaType = "application/audiobook+zip")
        )
        assertEquals(
            MediaType.READIUM_AUDIOBOOK,
            retriever.retrieve(
                mediaType = "application/audiobook+zip",
                fileExtension = "audiobook"
            )
        )
        assertEquals(
            MediaType.READIUM_AUDIOBOOK,
            retriever.retrieve(
                mediaTypes = listOf("application/audiobook+zip"),
                fileExtensions = listOf("audiobook")
            )
        )
    }

    @Test
    fun `sniff from bytes`() = runBlocking {
        assertEquals(
            MediaType.READIUM_AUDIOBOOK_MANIFEST,
            retriever.retrieveResource(fixtures.fileAt("audiobook.json"))
        )
    }

    @Test
    fun `sniff unknown format`() = runBlocking {
        assertNull(retriever.retrieve(mediaType = "invalid"))
        assertNull(retriever.retrieveResource(fixtures.fileAt("unknown")))
    }

    @Test
    fun `sniff audiobook`() = runBlocking {
        assertEquals(
            MediaType.READIUM_AUDIOBOOK,
            retriever.retrieve(fileExtension = "audiobook")
        )
        assertEquals(
            MediaType.READIUM_AUDIOBOOK,
            retriever.retrieve(mediaType = "application/audiobook+zip")
        )
        assertEquals(
            MediaType.READIUM_AUDIOBOOK,
            retriever.retrieveArchive(fixtures.fileAt("audiobook-package.unknown"))
        )
    }

    @Test
    fun `sniff audiobook manifest`() = runBlocking {
        assertEquals(
            MediaType.READIUM_AUDIOBOOK_MANIFEST,
            retriever.retrieve(mediaType = "application/audiobook+json")
        )
        assertEquals(
            MediaType.READIUM_AUDIOBOOK_MANIFEST,
            retriever.retrieveResource(fixtures.fileAt("audiobook.json"))
        )
        assertEquals(
            MediaType.READIUM_AUDIOBOOK_MANIFEST,
            retriever.retrieveResource(fixtures.fileAt("audiobook-wrongtype.json"))
        )
    }

    @Test
    fun `sniff BMP`() = runBlocking {
        assertEquals(MediaType.BMP, retriever.retrieve(fileExtension = "bmp"))
        assertEquals(MediaType.BMP, retriever.retrieve(fileExtension = "dib"))
        assertEquals(MediaType.BMP, retriever.retrieve(mediaType = "image/bmp"))
        assertEquals(MediaType.BMP, retriever.retrieve(mediaType = "image/x-bmp"))
    }

    @Test
    fun `sniff CBZ`() = runBlocking {
        assertEquals(MediaType.CBZ, retriever.retrieve(fileExtension = "cbz"))
        assertEquals(
            MediaType.CBZ,
            retriever.retrieve(mediaType = "application/vnd.comicbook+zip")
        )
        assertEquals(MediaType.CBZ, retriever.retrieve(mediaType = "application/x-cbz"))
        assertEquals(MediaType.CBZ, retriever.retrieve(mediaType = "application/x-cbr"))
        assertEquals(MediaType.CBZ, retriever.retrieveArchive(fixtures.fileAt("cbz.unknown")))
    }

    @Test
    fun `sniff DiViNa`() = runBlocking {
        assertEquals(MediaType.DIVINA, retriever.retrieve(fileExtension = "divina"))
        assertEquals(
            MediaType.DIVINA,
            retriever.retrieve(mediaType = "application/divina+zip")
        )
        assertEquals(
            MediaType.DIVINA,
            retriever.retrieveArchive(fixtures.fileAt("divina-package.unknown"))
        )
    }

    @Test
    fun `sniff DiViNa manifest`() = runBlocking {
        assertEquals(
            MediaType.DIVINA_MANIFEST,
            retriever.retrieve(mediaType = "application/divina+json")
        )
        assertEquals(
            MediaType.DIVINA_MANIFEST,
            retriever.retrieveResource(fixtures.fileAt("divina.json"))
        )
    }

    @Test
    fun `sniff EPUB`() = runBlocking {
        assertEquals(MediaType.EPUB, retriever.retrieve(fileExtension = "epub"))
        assertEquals(
            MediaType.EPUB,
            retriever.retrieve(mediaType = "application/epub+zip")
        )
        assertEquals(MediaType.EPUB, retriever.retrieveArchive(fixtures.fileAt("epub.unknown")))
    }

    @Test
    fun `sniff AVIF`() = runBlocking {
        assertEquals(MediaType.AVIF, retriever.retrieve(fileExtension = "avif"))
        assertEquals(MediaType.AVIF, retriever.retrieve(mediaType = "image/avif"))
    }

    @Test
    fun `sniff GIF`() = runBlocking {
        assertEquals(MediaType.GIF, retriever.retrieve(fileExtension = "gif"))
        assertEquals(MediaType.GIF, retriever.retrieve(mediaType = "image/gif"))
    }

    @Test
    fun `sniff HTML`() = runBlocking {
        assertEquals(MediaType.HTML, retriever.retrieve(fileExtension = "htm"))
        assertEquals(MediaType.HTML, retriever.retrieve(fileExtension = "html"))
        assertEquals(MediaType.HTML, retriever.retrieve(mediaType = "text/html"))
        assertEquals(MediaType.HTML, retriever.retrieveResource(fixtures.fileAt("html.unknown")))
        assertEquals(
            MediaType.HTML,
            retriever.retrieveResource(fixtures.fileAt("html-doctype-case.unknown"))
        )
    }

    @Test
    fun `sniff XHTML`() = runBlocking {
        assertEquals(MediaType.XHTML, retriever.retrieve(fileExtension = "xht"))
        assertEquals(MediaType.XHTML, retriever.retrieve(fileExtension = "xhtml"))
        assertEquals(
            MediaType.XHTML,
            retriever.retrieve(mediaType = "application/xhtml+xml")
        )
        assertEquals(MediaType.XHTML, retriever.retrieveResource(fixtures.fileAt("xhtml.unknown")))
    }

    @Test
    fun `sniff JPEG`() = runBlocking {
        assertEquals(MediaType.JPEG, retriever.retrieve(fileExtension = "jpg"))
        assertEquals(MediaType.JPEG, retriever.retrieve(fileExtension = "jpeg"))
        assertEquals(MediaType.JPEG, retriever.retrieve(fileExtension = "jpe"))
        assertEquals(MediaType.JPEG, retriever.retrieve(fileExtension = "jif"))
        assertEquals(MediaType.JPEG, retriever.retrieve(fileExtension = "jfif"))
        assertEquals(MediaType.JPEG, retriever.retrieve(fileExtension = "jfi"))
        assertEquals(MediaType.JPEG, retriever.retrieve(mediaType = "image/jpeg"))
    }

    @Test
    fun `sniff JXL`() = runBlocking {
        assertEquals(MediaType.JXL, retriever.retrieve(fileExtension = "jxl"))
        assertEquals(MediaType.JXL, retriever.retrieve(mediaType = "image/jxl"))
    }

    @Test
    fun `sniff OPDS 1 feed`() = runBlocking {
        assertEquals(
            MediaType.OPDS1,
            retriever.retrieve(mediaType = "application/atom+xml;profile=opds-catalog")
        )
        assertEquals(
            MediaType.OPDS1_NAVIGATION_FEED,
            retriever.retrieve("application/atom+xml;profile=opds-catalog;kind=navigation")
        )
        assertEquals(
            MediaType.OPDS1_ACQUISITION_FEED,
            retriever.retrieve("application/atom+xml;profile=opds-catalog;kind=acquisition")
        )
        assertEquals(
            MediaType.OPDS1,
            retriever.retrieveResource(fixtures.fileAt("opds1-feed.unknown"))
        )
    }

    @Test
    fun `sniff OPDS 1 entry`() = runBlocking {
        assertEquals(
            MediaType.OPDS1_ENTRY,
            retriever.retrieve(
                mediaType = "application/atom+xml;type=entry;profile=opds-catalog"
            )
        )
        assertEquals(
            MediaType.OPDS1_ENTRY,
            retriever.retrieveResource(fixtures.fileAt("opds1-entry.unknown"))
        )
    }

    @Test
    fun `sniff OPDS 2 feed`() = runBlocking {
        assertEquals(
            MediaType.OPDS2,
            retriever.retrieve(mediaType = "application/opds+json")
        )
        assertEquals(
            MediaType.OPDS2,
            retriever.retrieveResource(fixtures.fileAt("opds2-feed.json"))
        )
    }

    @Test
    fun `sniff OPDS 2 publication`() = runBlocking {
        assertEquals(
            MediaType.OPDS2_PUBLICATION,
            retriever.retrieve(mediaType = "application/opds-publication+json")
        )
        assertEquals(
            MediaType.OPDS2_PUBLICATION,
            retriever.retrieveResource(fixtures.fileAt("opds2-publication.json"))
        )
    }

    @Test
    fun `sniff OPDS authentication document`() = runBlocking {
        assertEquals(
            MediaType.OPDS_AUTHENTICATION,
            retriever.retrieve(mediaType = "application/opds-authentication+json")
        )
        assertEquals(
            MediaType.OPDS_AUTHENTICATION,
            retriever.retrieve(mediaType = "application/vnd.opds.authentication.v1.0+json")
        )
        assertEquals(
            MediaType.OPDS_AUTHENTICATION,
            retriever.retrieveResource(fixtures.fileAt("opds-authentication.json"))
        )
    }

    @Test
    fun `sniff LCP protected audiobook`() = runBlocking {
        assertEquals(
            MediaType.LCP_PROTECTED_AUDIOBOOK,
            retriever.retrieve(fileExtension = "lcpa")
        )
        assertEquals(
            MediaType.LCP_PROTECTED_AUDIOBOOK,
            retriever.retrieve(mediaType = "application/audiobook+lcp")
        )
        assertEquals(
            MediaType.LCP_PROTECTED_AUDIOBOOK,
            retriever.retrieveArchive(fixtures.fileAt("audiobook-lcp.unknown"))
        )
    }

    @Test
    fun `sniff LCP protected PDF`() = runBlocking {
        assertEquals(
            MediaType.LCP_PROTECTED_PDF,
            retriever.retrieve(fileExtension = "lcpdf")
        )
        assertEquals(
            MediaType.LCP_PROTECTED_PDF,
            retriever.retrieve(mediaType = "application/pdf+lcp")
        )
        assertEquals(
            MediaType.LCP_PROTECTED_PDF,
            retriever.retrieveArchive(fixtures.fileAt("pdf-lcp.unknown"))
        )
    }

    @Test
    fun `sniff LCP license document`() = runBlocking {
        assertEquals(
            MediaType.LCP_LICENSE_DOCUMENT,
            retriever.retrieve(fileExtension = "lcpl")
        )
        assertEquals(
            MediaType.LCP_LICENSE_DOCUMENT,
            retriever.retrieve(mediaType = "application/vnd.readium.lcp.license.v1.0+json")
        )
        assertEquals(
            MediaType.LCP_LICENSE_DOCUMENT,
            retriever.retrieveResource(fixtures.fileAt("lcpl.unknown"))
        )
    }

    @Test
    fun `sniff LPF`() = runBlocking {
        assertEquals(MediaType.LPF, retriever.retrieve(fileExtension = "lpf"))
        assertEquals(MediaType.LPF, retriever.retrieve(mediaType = "application/lpf+zip"))
        assertEquals(MediaType.LPF, retriever.retrieveArchive(fixtures.fileAt("lpf.unknown")))
        assertEquals(
            MediaType.LPF,
            retriever.retrieveArchive(fixtures.fileAt("lpf-index-html.unknown"))
        )
    }

    @Test
    fun `sniff PDF`() = runBlocking {
        assertEquals(MediaType.PDF, retriever.retrieve(fileExtension = "pdf"))
        assertEquals(MediaType.PDF, retriever.retrieve(mediaType = "application/pdf"))
        assertEquals(MediaType.PDF, retriever.retrieveResource(fixtures.fileAt("pdf.unknown")))
    }

    @Test
    fun `sniff PNG`() = runBlocking {
        assertEquals(MediaType.PNG, retriever.retrieve(fileExtension = "png"))
        assertEquals(MediaType.PNG, retriever.retrieve(mediaType = "image/png"))
    }

    @Test
    fun `sniff TIFF`() = runBlocking {
        assertEquals(MediaType.TIFF, retriever.retrieve(fileExtension = "tiff"))
        assertEquals(MediaType.TIFF, retriever.retrieve(fileExtension = "tif"))
        assertEquals(MediaType.TIFF, retriever.retrieve(mediaType = "image/tiff"))
        assertEquals(MediaType.TIFF, retriever.retrieve(mediaType = "image/tiff-fx"))
    }

    @Test
    fun `sniff WebP`() = runBlocking {
        assertEquals(MediaType.WEBP, retriever.retrieve(fileExtension = "webp"))
        assertEquals(MediaType.WEBP, retriever.retrieve(mediaType = "image/webp"))
    }

    @Test
    fun `sniff WebPub`() = runBlocking {
        assertEquals(
            MediaType.READIUM_WEBPUB,
            retriever.retrieve(fileExtension = "webpub")
        )
        assertEquals(
            MediaType.READIUM_WEBPUB,
            retriever.retrieve(mediaType = "application/webpub+zip")
        )
        assertEquals(
            MediaType.READIUM_WEBPUB,
            retriever.retrieveArchive(fixtures.fileAt("webpub-package.unknown"))
        )
    }

    @Test
    fun `sniff WebPub manifest`() = runBlocking {
        assertEquals(
            MediaType.READIUM_WEBPUB_MANIFEST,
            retriever.retrieve(mediaType = "application/webpub+json")
        )
        assertEquals(
            MediaType.READIUM_WEBPUB_MANIFEST,
            retriever.retrieveResource(fixtures.fileAt("webpub.json"))
        )
    }

    @Test
    fun `sniff W3C WPUB manifest`() = runBlocking {
        assertEquals(
            MediaType.W3C_WPUB_MANIFEST,
            retriever.retrieveResource(fixtures.fileAt("w3c-wpub.json"))
        )
    }

    @Test
    fun `sniff ZAB`() = runBlocking {
        assertEquals(MediaType.ZAB, retriever.retrieve(fileExtension = "zab"))
        assertEquals(MediaType.ZAB, retriever.retrieveArchive(fixtures.fileAt("zab.unknown")))
    }

    @Test
    fun `sniff JSON`() = runBlocking {
        assertEquals(MediaType.JSON, retriever.retrieveResource(fixtures.fileAt("any.json")))
    }

    @Test
    fun `sniff JSON problem details`() = runBlocking {
        assertEquals(
            MediaType.JSON_PROBLEM_DETAILS,
            retriever.retrieve(mediaType = "application/problem+json")
        )
        assertEquals(
            MediaType.JSON_PROBLEM_DETAILS,
            retriever.retrieve(mediaType = "application/problem+json; charset=utf-8")
        )

        // The sniffing of a JSON document should not take precedence over the JSON problem details.
        assertEquals(
            MediaType.JSON_PROBLEM_DETAILS,
            retriever.retrieve(
                hints = MediaTypeHints(mediaType = MediaType("application/problem+json")!!),
                content = BytesResourceMediaTypeSnifferContent { """{"title": "Message"}""".toByteArray() }
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
            retriever.retrieve(
                mediaTypes = emptyList(),
                fileExtensions = listOf("foobar", "xlsx")
            )
        )
        assertEquals(
            xlsx,
            retriever.retrieve(
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
        assertEquals(png, retriever.retrieveResource(fixtures.fileAt("png.unknown")))
    }

    // Convenience

    private suspend fun MediaTypeRetriever.retrieveResource(file: File): MediaType? =
        retrieve(content = BytesResourceMediaTypeSnifferContent { file.readBytes() })

    private suspend fun MediaTypeRetriever.retrieveArchive(
        file: File,
        hints: MediaTypeHints = MediaTypeHints()
    ): MediaType? {
        val archive = assertNotNull(FileZipArchiveFactory(this).open(file).getOrNull())

        return retrieve(
            hints,
            content = object : ContainerMediaTypeSnifferContent {
                override suspend fun entries(): Set<String>? =
                    archive.entries()?.map { it.url.toString() }?.toSet()

                override suspend fun read(path: String, range: LongRange?): ByteArray? =
                    archive.get(Url(path)!!).read(range).getOrNull()
            }
        )
    }
}
