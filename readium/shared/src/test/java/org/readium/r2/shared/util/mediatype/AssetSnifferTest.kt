/*
 * Copyright 2023 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.shared.util.mediatype

import kotlin.test.assertEquals
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.runner.RunWith
import org.readium.r2.shared.Fixtures
import org.readium.r2.shared.util.Try
import org.readium.r2.shared.util.asset.AssetSniffer
import org.readium.r2.shared.util.asset.SniffError
import org.readium.r2.shared.util.checkSuccess
import org.readium.r2.shared.util.data.EmptyContainer
import org.readium.r2.shared.util.format.FileExtension
import org.readium.r2.shared.util.format.Format
import org.readium.r2.shared.util.format.FormatHints
import org.readium.r2.shared.util.format.Trait
import org.readium.r2.shared.util.resource.StringResource
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class AssetSnifferTest {

    private val fixtures = Fixtures("util/asset")

    private val sniffer = AssetSniffer()

    private suspend fun AssetSniffer.sniffHints(formatHints: FormatHints): Try<Format, SniffError> =
        sniff(
            hints = formatHints,
            container = EmptyContainer()
        )

    private suspend fun AssetSniffer.sniffFileExtension(extension: String?): Try<Format, SniffError> =
        sniffHints(FormatHints(fileExtension = extension?.let { FileExtension((it)) }))

    private suspend fun AssetSniffer.sniffMediaType(mediaType: String?): Try<Format, SniffError> =
        sniffHints(FormatHints(mediaType = mediaType?.let { MediaType(it) }))

    @Test
    fun `sniff ignores extension case`() = runBlocking {
        assertEquals(
            Format.EPUB,
            sniffer.sniffFileExtension("EPUB").checkSuccess()
        )
    }

    @Test
    fun `sniff ignores media type case`() = runBlocking {
        assertEquals(
            Format.EPUB,
            sniffer.sniffMediaType("APPLICATION/EPUB+ZIP").checkSuccess()
        )
    }

    @Test
    fun `sniff ignores media type extra parameters`() = runBlocking {
        assertEquals(
            Format.EPUB,
            sniffer.sniffMediaType("application/epub+zip;param=value").checkSuccess()
        )
    }

    @Test
    fun `sniff from metadata`() = runBlocking {
        assertEquals(
            sniffer.sniffFileExtension(null).failureOrNull(),
            SniffError.NotRecognized
        )
        assertEquals(
            Format.READIUM_AUDIOBOOK,
            sniffer.sniffFileExtension("audiobook").checkSuccess()
        )
        assertEquals(
            sniffer.sniffMediaType(null).failureOrNull(),
            SniffError.NotRecognized
        )
        assertEquals(
            Format.READIUM_AUDIOBOOK,
            sniffer.sniffMediaType("application/audiobook+zip").checkSuccess()
        )
        assertEquals(
            Format.READIUM_AUDIOBOOK,
            sniffer.sniffHints(
                FormatHints(
                    mediaTypes = listOf("application/audiobook+zip"),
                    fileExtensions = listOf("audiobook")
                )
            ).checkSuccess()
        )
    }

    @Test
    fun `sniff from bytes`() = runBlocking {
        assertEquals(
            Format.READIUM_AUDIOBOOK_MANIFEST,
            sniffer.sniff(fixtures.fileAt("audiobook.json")).checkSuccess()
        )
    }

    @Test
    fun `sniff unknown format`() = runBlocking {
        assertEquals(
            SniffError.NotRecognized,
            sniffer.sniffMediaType(mediaType = "invalid").failureOrNull()
        )
        assertEquals(
            SniffError.NotRecognized,
            sniffer.sniff(fixtures.fileAt("unknown")).failureOrNull()
        )
    }

    @Test
    fun `sniff audiobook`() = runBlocking {
        assertEquals(
            Format.READIUM_AUDIOBOOK,
            sniffer.sniffFileExtension("audiobook").checkSuccess()
        )
        assertEquals(
            Format.READIUM_AUDIOBOOK,
            sniffer.sniffMediaType("application/audiobook+zip").checkSuccess()
        )
        assertEquals(
            Format.READIUM_AUDIOBOOK,
            sniffer.sniff(fixtures.fileAt("audiobook-package.unknown")).checkSuccess()
        )
    }

    @Test
    fun `sniff audiobook manifest`() = runBlocking {
        assertEquals(
            Format.READIUM_AUDIOBOOK_MANIFEST,
            sniffer.sniffMediaType("application/audiobook+json").checkSuccess()
        )
        assertEquals(
            Format.READIUM_AUDIOBOOK_MANIFEST,
            sniffer.sniff(fixtures.fileAt("audiobook.json")).checkSuccess()
        )
        assertEquals(
            Format.READIUM_AUDIOBOOK_MANIFEST,
            sniffer.sniff(fixtures.fileAt("audiobook-wrongtype.json")).checkSuccess()
        )
    }

    @Test
    fun `sniff BMP`() = runBlocking {
        assertEquals(Format.BMP, sniffer.sniffFileExtension("bmp").checkSuccess())
        assertEquals(Format.BMP, sniffer.sniffFileExtension("dib").checkSuccess())
        assertEquals(Format.BMP, sniffer.sniffMediaType("image/bmp").checkSuccess())
        assertEquals(Format.BMP, sniffer.sniffMediaType("image/x-bmp").checkSuccess())
    }

    @Test
    fun `sniff CBZ`() = runBlocking {
        assertEquals(
            Format.CBZ,
            sniffer.sniffFileExtension("cbz").checkSuccess()
        )
        assertEquals(
            Format.CBZ,
            sniffer.sniffMediaType("application/vnd.comicbook+zip").checkSuccess()
        )
        assertEquals(
            Format.CBZ,
            sniffer.sniffMediaType("application/x-cbz").checkSuccess()
        )
        assertEquals(
            Format.CBR,
            sniffer.sniffMediaType("application/x-cbr").checkSuccess()
        )
        assertEquals(
            Format.CBZ,
            sniffer.sniff(fixtures.fileAt("cbz.unknown")).checkSuccess()
        )
    }

    @Test
    fun `sniff DiViNa`() = runBlocking {
        assertEquals(
            Format.READIUM_COMICS,
            sniffer.sniffFileExtension("divina").checkSuccess()
        )
        assertEquals(
            Format.READIUM_COMICS,
            sniffer.sniffMediaType("application/divina+zip").checkSuccess()
        )
        assertEquals(
            Format.READIUM_COMICS,
            sniffer.sniff(fixtures.fileAt("divina-package.unknown")).checkSuccess()
        )
    }

    @Test
    fun `sniff DiViNa manifest`() = runBlocking {
        assertEquals(
            Format.READIUM_COMICS_MANIFEST,
            sniffer.sniffMediaType("application/divina+json").checkSuccess()
        )
        assertEquals(
            Format.READIUM_COMICS_MANIFEST,
            sniffer.sniff(fixtures.fileAt("divina.json")).checkSuccess()
        )
    }

    @Test
    fun `sniff EPUB`() = runBlocking {
        assertEquals(
            Format.EPUB,
            sniffer.sniffFileExtension("epub").checkSuccess()
        )
        assertEquals(
            Format.EPUB,
            sniffer.sniffMediaType("application/epub+zip").checkSuccess()
        )
        assertEquals(
            Format.EPUB,
            sniffer.sniff(fixtures.fileAt("epub.unknown")).checkSuccess()
        )
    }

    @Test
    fun `sniff AVIF`() = runBlocking {
        assertEquals(Format.AVIF, sniffer.sniffFileExtension("avif").checkSuccess())
        assertEquals(Format.AVIF, sniffer.sniffMediaType("image/avif").checkSuccess())
    }

    @Test
    fun `sniff GIF`() = runBlocking {
        assertEquals(Format.GIF, sniffer.sniffFileExtension("gif").checkSuccess())
        assertEquals(Format.GIF, sniffer.sniffMediaType("image/gif").checkSuccess())
    }

    @Test
    fun `sniff HTML`() = runBlocking {
        assertEquals(
            Format.HTML,
            sniffer.sniffFileExtension("htm").checkSuccess()
        )
        assertEquals(
            Format.HTML,
            sniffer.sniffFileExtension("html").checkSuccess()
        )
        assertEquals(
            Format.HTML,
            sniffer.sniffMediaType("text/html").checkSuccess()
        )
        assertEquals(
            Format.HTML,
            sniffer.sniff(fixtures.fileAt("html.unknown")).checkSuccess()
        )
        assertEquals(
            Format.HTML,
            sniffer.sniff(fixtures.fileAt("html-doctype-case.unknown")).checkSuccess()
        )
    }

    @Test
    fun `sniff XHTML`() = runBlocking {
        assertEquals(
            Format.XHTML,
            sniffer.sniffFileExtension("xht").checkSuccess()
        )
        assertEquals(
            Format.XHTML,
            sniffer.sniffFileExtension("xhtml").checkSuccess()
        )
        assertEquals(
            Format.XHTML,
            sniffer.sniffMediaType("application/xhtml+xml").checkSuccess()
        )
        assertEquals(
            Format.XHTML,
            sniffer.sniff(fixtures.fileAt("xhtml.unknown")).checkSuccess()
        )
    }

    @Test
    fun `sniff JPEG`() = runBlocking {
        assertEquals(Format.JPEG, sniffer.sniffFileExtension("jpg").checkSuccess())
        assertEquals(Format.JPEG, sniffer.sniffFileExtension("jpeg").checkSuccess())
        assertEquals(Format.JPEG, sniffer.sniffFileExtension("jpe").checkSuccess())
        assertEquals(Format.JPEG, sniffer.sniffFileExtension("jif").checkSuccess())
        assertEquals(Format.JPEG, sniffer.sniffFileExtension("jfif").checkSuccess())
        assertEquals(Format.JPEG, sniffer.sniffFileExtension("jfi").checkSuccess())
        assertEquals(Format.JPEG, sniffer.sniffMediaType("image/jpeg").checkSuccess())
    }

    @Test
    fun `sniff JXL`() = runBlocking {
        assertEquals(Format.JXL, sniffer.sniffFileExtension("jxl").checkSuccess())
        assertEquals(Format.JXL, sniffer.sniffMediaType("image/jxl").checkSuccess())
    }

    @Test
    fun `sniff RAR`() = runBlocking {
        assertEquals(
            Format.RAR,
            sniffer.sniffFileExtension("rar").checkSuccess()
        )
        assertEquals(
            Format.RAR,
            sniffer.sniffMediaType("application/vnd.rar").checkSuccess()
        )
        assertEquals(
            Format.RAR,
            sniffer.sniffMediaType("application/x-rar").checkSuccess()
        )
        assertEquals(
            Format.RAR,
            sniffer.sniffMediaType("application/x-rar-compressed").checkSuccess()
        )
    }

    @Test
    fun `sniff OPDS 1 feed`() = runBlocking {
        assertEquals(
            Format.OPDS1_CATALOG,
            sniffer.sniffMediaType("application/atom+xml;profile=opds-catalog").checkSuccess()
        )
        assertEquals(
            Format.OPDS1_NAVIGATION_FEED,
            sniffer.sniffMediaType("application/atom+xml;profile=opds-catalog;kind=navigation").checkSuccess()
        )
        assertEquals(
            Format.OPDS1_ACQUISITION_FEED,
            sniffer.sniffMediaType("application/atom+xml;profile=opds-catalog;kind=acquisition").checkSuccess()
        )
        assertEquals(
            Format.OPDS1_CATALOG,
            sniffer.sniff(fixtures.fileAt("opds1-feed.unknown")).checkSuccess()
        )
    }

    @Test
    fun `sniff OPDS 1 entry`() = runBlocking {
        assertEquals(
            Format.OPDS1_ENTRY,
            sniffer.sniffMediaType("application/atom+xml;type=entry;profile=opds-catalog").checkSuccess()
        )
        assertEquals(
            Format.OPDS1_ENTRY,
            sniffer.sniff(fixtures.fileAt("opds1-entry.unknown")).checkSuccess()
        )
    }

    @Test
    fun `sniff OPDS 2 feed`() = runBlocking {
        assertEquals(
            Format.OPDS2_CATALOG,
            sniffer.sniffMediaType("application/opds+json").checkSuccess()
        )
        assertEquals(
            Format.OPDS2_CATALOG,
            sniffer.sniff(fixtures.fileAt("opds2-feed.json")).checkSuccess()
        )
    }

    @Test
    fun `sniff OPDS 2 publication`() = runBlocking {
        assertEquals(
            Format.OPDS2_PUBLICATION,
            sniffer.sniffMediaType("application/opds-publication+json").checkSuccess()
        )
        assertEquals(
            Format.OPDS2_PUBLICATION,
            sniffer.sniff(fixtures.fileAt("opds2-publication.json")).checkSuccess()
        )
    }

    @Test
    fun `sniff OPDS authentication document`() = runBlocking {
        assertEquals(
            Format.OPDS_AUTHENTICATION,
            sniffer.sniffMediaType("application/opds-authentication+json").checkSuccess()
        )
        assertEquals(
            Format.OPDS_AUTHENTICATION,
            sniffer.sniffMediaType("application/vnd.opds.authentication.v1.0+json").checkSuccess()
        )
        assertEquals(
            Format.OPDS_AUTHENTICATION,
            sniffer.sniff(fixtures.fileAt("opds-authentication.json")).checkSuccess()
        )
    }

    @Test
    fun `sniff LCP protected audiobook`() = runBlocking {
        assertEquals(
            Format.READIUM_AUDIOBOOK + Trait.LCP_PROTECTED,
            sniffer.sniffFileExtension("lcpa").checkSuccess()
        )
        assertEquals(
            Format.READIUM_AUDIOBOOK + Trait.LCP_PROTECTED,
            sniffer.sniffMediaType("application/audiobook+lcp").checkSuccess()
        )
        assertEquals(
            Format.READIUM_AUDIOBOOK + Trait.LCP_PROTECTED,
            sniffer.sniff(fixtures.fileAt("audiobook-lcp.unknown")).checkSuccess()
        )
    }

    @Test
    fun `sniff LCP protected PDF`() = runBlocking {
        assertEquals(
            Format.READIUM_PDF + Trait.LCP_PROTECTED,
            sniffer.sniffFileExtension("lcpdf").checkSuccess()
        )
        assertEquals(
            Format.READIUM_PDF + Trait.LCP_PROTECTED,
            sniffer.sniffMediaType("application/pdf+lcp").checkSuccess()
        )
        assertEquals(
            Format.READIUM_PDF + Trait.LCP_PROTECTED,
            sniffer.sniff(fixtures.fileAt("pdf-lcp.unknown")).checkSuccess()
        )
    }

    @Test
    fun `sniff LCP license document`() = runBlocking {
        assertEquals(
            Format.LCP_LICENSE_DOCUMENT,
            sniffer.sniffFileExtension("lcpl").checkSuccess()
        )
        assertEquals(
            Format.LCP_LICENSE_DOCUMENT,
            sniffer.sniffMediaType("application/vnd.readium.lcp.license.v1.0+json").checkSuccess()
        )
        assertEquals(
            Format.LCP_LICENSE_DOCUMENT,
            sniffer.sniff(fixtures.fileAt("lcpl.unknown")).checkSuccess()
        )
    }

    @Test
    fun `sniff LPF`() = runBlocking {
        assertEquals(
            Format(setOf(Trait.ZIP, Trait.LPF)),
            sniffer.sniffFileExtension("lpf").checkSuccess()
        )
        assertEquals(
            Format(setOf(Trait.ZIP, Trait.LPF)),
            sniffer.sniffMediaType("application/lpf+zip").checkSuccess()
        )
        assertEquals(
            Format(setOf(Trait.ZIP, Trait.LPF)),
            sniffer.sniff(fixtures.fileAt("lpf.unknown")).checkSuccess()
        )
        assertEquals(
            Format(setOf(Trait.ZIP, Trait.LPF)),
            sniffer.sniff(fixtures.fileAt("lpf-index-html.unknown")).checkSuccess()
        )
    }

    @Test
    fun `sniff PDF`() = runBlocking {
        assertEquals(
            Format.PDF,
            sniffer.sniffFileExtension("pdf").checkSuccess()
        )
        assertEquals(
            Format.PDF,
            sniffer.sniffMediaType("application/pdf").checkSuccess()
        )
        assertEquals(
            Format.PDF,
            sniffer.sniff(fixtures.fileAt("pdf.unknown")).checkSuccess()
        )
    }

    @Test
    fun `sniff PNG`() = runBlocking {
        assertEquals(Format.PNG, sniffer.sniffFileExtension("png").checkSuccess())
        assertEquals(Format.PNG, sniffer.sniffMediaType("image/png").checkSuccess())
    }

    @Test
    fun `sniff TIFF`() = runBlocking {
        assertEquals(Format.TIFF, sniffer.sniffFileExtension("tiff").checkSuccess())
        assertEquals(Format.TIFF, sniffer.sniffFileExtension("tif").checkSuccess())
        assertEquals(Format.TIFF, sniffer.sniffMediaType("image/tiff").checkSuccess())
        assertEquals(Format.TIFF, sniffer.sniffMediaType("image/tiff-fx").checkSuccess())
    }

    @Test
    fun `sniff WebP`() = runBlocking {
        assertEquals(Format.WEBP, sniffer.sniffFileExtension("webp").checkSuccess())
        assertEquals(Format.WEBP, sniffer.sniffMediaType("image/webp").checkSuccess())
    }

    @Test
    fun `sniff WebPub`() = runBlocking {
        assertEquals(
            Format.READIUM_WEBPUB,
            sniffer.sniffFileExtension("webpub").checkSuccess()
        )
        assertEquals(
            Format.READIUM_WEBPUB,
            sniffer.sniffMediaType("application/webpub+zip").checkSuccess()
        )
        assertEquals(
            Format.READIUM_WEBPUB,
            sniffer.sniff(fixtures.fileAt("webpub-package.unknown")).checkSuccess()
        )
    }

    @Test
    fun `sniff WebPub manifest`() = runBlocking {
        assertEquals(
            Format.READIUM_WEBPUB_MANIFEST,
            sniffer.sniffMediaType("application/webpub+json").checkSuccess()
        )
        assertEquals(
            Format.READIUM_WEBPUB_MANIFEST,
            sniffer.sniff(fixtures.fileAt("webpub.json")).checkSuccess()
        )
    }

    @Test
    fun `sniff W3C WPUB manifest`() = runBlocking {
        assertEquals(
            Format(setOf(Trait.JSON, Trait.W3C_PUB_MANIFEST)),
            sniffer.sniff(fixtures.fileAt("w3c-wpub.json")).checkSuccess()
        )
    }

    @Test
    fun `sniff ZAB`() = runBlocking {
        assertEquals(
            Format.ZAB,
            sniffer.sniffFileExtension("zab").checkSuccess()
        )
        assertEquals(
            Format.ZAB,
            sniffer.sniff(fixtures.fileAt("zab.unknown")).checkSuccess()
        )
    }

    @Test
    fun `sniff JSON`() = runBlocking {
        assertEquals(
            Format.JSON,
            sniffer.sniff(fixtures.fileAt("any.json")).checkSuccess()
        )
    }

    @Test
    fun `sniff JSON problem details`() = runBlocking {
        assertEquals(
            Format.JSON_PROBLEM_DETAILS,
            sniffer.sniffMediaType("application/problem+json").checkSuccess()
        )
        assertEquals(
            Format.JSON_PROBLEM_DETAILS,
            sniffer.sniffMediaType("application/problem+json; charset=utf-8").checkSuccess()
        )

        // The sniffing of a JSON document should not take precedence over the JSON problem details.
        assertEquals(
            Format.JSON_PROBLEM_DETAILS,
            sniffer.sniff(
                source = StringResource("""{"title": "Message"}"""),
                hints = FormatHints(mediaType = MediaType("application/problem+json")!!)
            ).checkSuccess()
        )
    }
}
