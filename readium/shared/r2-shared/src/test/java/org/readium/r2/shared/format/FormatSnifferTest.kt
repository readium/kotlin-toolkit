/*
 * Module: r2-shared-kotlin
 * Developers: Mickaël Menu
 *
 * Copyright (c) 2020. Readium Foundation. All rights reserved.
 * Use of this source code is governed by a BSD-style license which is detailed in the
 * LICENSE file present in the project repository where this source code is maintained.
 */

package org.readium.r2.shared.format

import android.webkit.MimeTypeMap
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.readium.r2.shared.Fixtures
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [28])
class FormatSnifferTest {

    val fixtures = Fixtures("format")

    @Test
    fun `sniff ignores extension case`() {
        assertEquals(Format.EPUB, Format.of(fileExtension = "EPUB"))
    }

    @Test
    fun `sniff ignores media type case`() {
        assertEquals(Format.EPUB, Format.of(mediaType = "APPLICATION/EPUB+ZIP"))
    }

    @Test
    fun `sniff ignores media type extra parameters`() {
        assertEquals(Format.EPUB, Format.of(mediaType = "application/epub+zip;param=value"))
    }

    @Test
    fun `sniff from metadata`() {
        assertNull(Format.of(fileExtension = null))
        assertEquals(Format.AUDIOBOOK, Format.of(fileExtension = "audiobook"))
        assertNull(Format.of(mediaType = null))
        assertEquals(Format.AUDIOBOOK, Format.of(mediaType = "application/audiobook+zip"))
        assertEquals(Format.AUDIOBOOK, Format.of(mediaType = "application/audiobook+zip"))
        assertEquals(Format.AUDIOBOOK, Format.of(mediaType = "application/audiobook+zip", fileExtension = "audiobook"))
        assertEquals(Format.AUDIOBOOK, Format.of(mediaTypes = listOf("application/audiobook+zip"), fileExtensions = listOf("audiobook")))
    }

    @Test
    fun `sniff from a file`() = runBlocking {
        assertEquals(Format.AUDIOBOOK_MANIFEST, Format.ofFile(fixtures.fileAt("audiobook.json")))
    }

    @Test
    fun `sniff from bytes`() = runBlocking {
        assertEquals(Format.AUDIOBOOK_MANIFEST, Format.ofBytes({ fixtures.fileAt("audiobook.json").readBytes() }))
    }

    @Test
    fun `sniff unknown format`() = runBlocking {
        assertNull(Format.of(mediaType = "unknown/type"))
        assertNull(Format.ofFile(fixtures.fileAt("unknown")))
    }

    @Test
    fun `sniff audiobook`() = runBlocking {
        assertEquals(Format.AUDIOBOOK, Format.of(fileExtension = "audiobook"))
        assertEquals(Format.AUDIOBOOK, Format.of(mediaType = "application/audiobook+zip"))
        assertEquals(Format.AUDIOBOOK, Format.ofFile(fixtures.fileAt("audiobook-package.unknown")))
    }

    @Test
    fun `sniff audiobook manifest`() = runBlocking {
        assertEquals(Format.AUDIOBOOK_MANIFEST, Format.of(mediaType = "application/audiobook+json"))
        assertEquals(Format.AUDIOBOOK_MANIFEST, Format.ofFile(fixtures.fileAt("audiobook.json")))
        assertEquals(Format.AUDIOBOOK_MANIFEST, Format.ofFile(fixtures.fileAt("audiobook-wrongtype.json")))
    }

    @Test
    fun `sniff BMP`() {
        assertEquals(Format.BMP, Format.of(fileExtension = "bmp"))
        assertEquals(Format.BMP, Format.of(fileExtension = "dib"))
        assertEquals(Format.BMP, Format.of(mediaType = "image/bmp"))
        assertEquals(Format.BMP, Format.of(mediaType = "image/x-bmp"))
    }

    @Test
    fun `sniff CBZ`() = runBlocking {
        assertEquals(Format.CBZ, Format.of(fileExtension = "cbz"))
        assertEquals(Format.CBZ, Format.of(mediaType = "application/vnd.comicbook+zip"))
        assertEquals(Format.CBZ, Format.of(mediaType = "application/x-cbz"))
        assertEquals(Format.CBZ, Format.of(mediaType = "application/x-cbr"))
        assertEquals(Format.CBZ, Format.ofFile(fixtures.fileAt("cbz.unknown")))
    }

    @Test
    fun `sniff DiViNa`() = runBlocking {
        assertEquals(Format.DIVINA, Format.of(fileExtension = "divina"))
        assertEquals(Format.DIVINA, Format.of(mediaType = "application/divina+zip"))
        assertEquals(Format.DIVINA, Format.ofFile(fixtures.fileAt("divina-package.unknown")))
    }

    @Test
    fun `sniff DiViNa manifest`() = runBlocking {
        assertEquals(Format.DIVINA_MANIFEST, Format.of(mediaType = "application/divina+json"))
        assertEquals(Format.DIVINA_MANIFEST, Format.ofFile(fixtures.fileAt("divina.json")))
    }

    @Test
    fun `sniff EPUB`() = runBlocking {
        assertEquals(Format.EPUB, Format.of(fileExtension = "epub"))
        assertEquals(Format.EPUB, Format.of(mediaType = "application/epub+zip"))
        assertEquals(Format.EPUB, Format.ofFile(fixtures.fileAt("epub.unknown")))
    }

    @Test
    fun `sniff GIF`() {
        assertEquals(Format.GIF, Format.of(fileExtension = "gif"))
        assertEquals(Format.GIF, Format.of(mediaType = "image/gif"))
    }

    @Test
    fun `sniff HTML`() = runBlocking {
        assertEquals(Format.HTML, Format.of(fileExtension = "htm"))
        assertEquals(Format.HTML, Format.of(fileExtension = "html"))
        assertEquals(Format.HTML, Format.of(fileExtension = "xht"))
        assertEquals(Format.HTML, Format.of(fileExtension = "xhtml"))
        assertEquals(Format.HTML, Format.of(mediaType = "text/html"))
        assertEquals(Format.HTML, Format.of(mediaType = "application/xhtml+xml"))
        assertEquals(Format.HTML, Format.ofFile(fixtures.fileAt("html.unknown")))
        assertEquals(Format.HTML, Format.ofFile(fixtures.fileAt("xhtml.unknown")))
    }

    @Test
    fun `sniff JPEG`() {
        assertEquals(Format.JPEG, Format.of(fileExtension = "jpg"))
        assertEquals(Format.JPEG, Format.of(fileExtension = "jpeg"))
        assertEquals(Format.JPEG, Format.of(fileExtension = "jpe"))
        assertEquals(Format.JPEG, Format.of(fileExtension = "jif"))
        assertEquals(Format.JPEG, Format.of(fileExtension = "jfif"))
        assertEquals(Format.JPEG, Format.of(fileExtension = "jfi"))
        assertEquals(Format.JPEG, Format.of(mediaType = "image/jpeg"))
    }

    @Test
    fun `sniff OPDS 1 feed`() = runBlocking {
        assertEquals(Format.OPDS1_FEED, Format.of(mediaType = "application/atom+xml;profile=opds-catalog"))
        assertEquals(Format.OPDS1_FEED, Format.ofFile(fixtures.fileAt("opds1-feed.unknown")))
    }

    @Test
    fun `sniff OPDS 1 entry`() = runBlocking {
        assertEquals(Format.OPDS1_ENTRY, Format.of(mediaType = "application/atom+xml;type=entry;profile=opds-catalog"))
        assertEquals(Format.OPDS1_ENTRY, Format.ofFile(fixtures.fileAt("opds1-entry.unknown")))
    }

    @Test
    fun `sniff OPDS 2 feed`() = runBlocking {
        assertEquals(Format.OPDS2_FEED, Format.of(mediaType = "application/opds+json"))
        assertEquals(Format.OPDS2_FEED, Format.ofFile(fixtures.fileAt("opds2-feed.json")))
    }

    @Test
    fun `sniff OPDS 2 publication`() = runBlocking {
        assertEquals(Format.OPDS2_PUBLICATION, Format.of(mediaType = "application/opds-publication+json"))
        assertEquals(Format.OPDS2_PUBLICATION, Format.ofFile(fixtures.fileAt("opds2-publication.json")))
    }

    @Test
    fun `sniff OPDS authentication document`() = runBlocking {
        assertEquals(Format.OPDS_AUTHENTICATION, Format.of(mediaType = "application/opds-authentication+json"))
        assertEquals(Format.OPDS_AUTHENTICATION, Format.of(mediaType = "application/vnd.opds.authentication.v1.0+json"))
        assertEquals(Format.OPDS_AUTHENTICATION, Format.ofFile(fixtures.fileAt("opds-authentication.json")))
    }

    @Test
    fun `sniff LCP protected audiobook`() = runBlocking {
        assertEquals(Format.LCP_PROTECTED_AUDIOBOOK, Format.of(fileExtension = "lcpa"))
        assertEquals(Format.LCP_PROTECTED_AUDIOBOOK, Format.of(mediaType = "application/audiobook+lcp"))
        assertEquals(Format.LCP_PROTECTED_AUDIOBOOK, Format.ofFile(fixtures.fileAt("audiobook-lcp.unknown")))
    }

    @Test
    fun `sniff LCP protected PDF`() = runBlocking {
        assertEquals(Format.LCP_PROTECTED_PDF, Format.of(fileExtension = "lcpdf"))
        assertEquals(Format.LCP_PROTECTED_PDF, Format.of(mediaType = "application/pdf+lcp"))
        assertEquals(Format.LCP_PROTECTED_PDF, Format.ofFile(fixtures.fileAt("pdf-lcp.unknown")))
    }

    @Test
    fun `sniff LCP license document`() = runBlocking {
        assertEquals(Format.LCP_LICENSE, Format.of(fileExtension = "lcpl"))
        assertEquals(Format.LCP_LICENSE, Format.of(mediaType = "application/vnd.readium.lcp.license.v1.0+json"))
        assertEquals(Format.LCP_LICENSE, Format.ofFile(fixtures.fileAt("lcpl.unknown")))
    }

    @Test
    fun `sniff LPF`() = runBlocking {
        assertEquals(Format.LPF, Format.of(fileExtension = "lpf"))
        assertEquals(Format.LPF, Format.of(mediaType = "application/lpf+zip"))
        assertEquals(Format.LPF, Format.ofFile(fixtures.fileAt("lpf.unknown")))
        assertEquals(Format.LPF, Format.ofFile(fixtures.fileAt("lpf-index-html.unknown")))
    }

    @Test
    fun `sniff PDF`() = runBlocking {
        assertEquals(Format.PDF, Format.of(fileExtension = "pdf"))
        assertEquals(Format.PDF, Format.of(mediaType = "application/pdf"))
        assertEquals(Format.PDF, Format.ofFile(fixtures.fileAt("pdf.unknown")))
    }

    @Test
    fun `sniff PNG`() {
        assertEquals(Format.PNG, Format.of(fileExtension = "png"))
        assertEquals(Format.PNG, Format.of(mediaType = "image/png"))
    }

    @Test
    fun `sniff TIFF`() {
        assertEquals(Format.TIFF, Format.of(fileExtension = "tiff"))
        assertEquals(Format.TIFF, Format.of(fileExtension = "tif"))
        assertEquals(Format.TIFF, Format.of(mediaType = "image/tiff"))
        assertEquals(Format.TIFF, Format.of(mediaType = "image/tiff-fx"))
    }

    @Test
    fun `sniff WebP`() {
        assertEquals(Format.WEBP, Format.of(fileExtension = "webp"))
        assertEquals(Format.WEBP, Format.of(mediaType = "image/webp"))
    }

    @Test
    fun `sniff WebPub`() = runBlocking {
        assertEquals(Format.WEBPUB, Format.of(fileExtension = "webpub"))
        assertEquals(Format.WEBPUB, Format.of(mediaType = "application/webpub+zip"))
        assertEquals(Format.WEBPUB, Format.ofFile(fixtures.fileAt("webpub-package.unknown")))
    }

    @Test
    fun `sniff WebPub manifest`() = runBlocking {
        assertEquals(Format.WEBPUB_MANIFEST, Format.of(mediaType = "application/webpub+json"))
        assertEquals(Format.WEBPUB_MANIFEST, Format.ofFile(fixtures.fileAt("webpub.json")))
    }

    @Test
    fun `sniff W3C WPUB manifest`() = runBlocking {
        assertEquals(Format.W3C_WPUB_MANIFEST, Format.ofFile(fixtures.fileAt("w3c-wpub.json")))
    }

    @Test
    fun `sniff ZAB`() = runBlocking {
        assertEquals(Format.ZAB, Format.of(fileExtension = "zab"))
        assertEquals(Format.ZAB, Format.ofFile(fixtures.fileAt("zab.unknown")))
    }

    @Test
    fun `sniff system media types`() {
        shadowOf(MimeTypeMap.getSingleton()).addExtensionMimeTypMapping("xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
        val xlsx = Format(
            name = "XLSX",
            mediaType = MediaType.parse("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")!!,
            fileExtension = "xlsx"
        )
        assertEquals(xlsx, Format.of(mediaTypes = emptyList(), fileExtensions = listOf("foobar", "xlsx")))
        assertEquals(xlsx, Format.of(mediaTypes = listOf("applicaton/foobar", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"), fileExtensions = emptyList()))
    }

    @Test
    fun `sniff system media types from bytes`() = runBlocking {
        shadowOf(MimeTypeMap.getSingleton()).addExtensionMimeTypMapping("png", "image/png")
        val png = Format(
            name = "PNG",
            mediaType = MediaType.parse("image/png")!!,
            fileExtension = "png"
        )
        assertEquals(png, Format.ofFile(fixtures.fileAt("png.unknown")))
    }

}
