/*
 * Module: r2-shared-kotlin
 * Developers: Mickaël Menu
 *
 * Copyright (c) 2020. Readium Foundation. All rights reserved.
 * Use of this source code is governed by a BSD-style license which is detailed in the
 * LICENSE file present in the project repository where this source code is maintained.
 */

package org.readium.r2.shared.format

import org.junit.Assert.*
import org.junit.Test
import org.readium.r2.shared.Fixtures

class FormatSnifferTest {

    val fixtures = Fixtures("format")

    @Test
    fun testSniffIgnoresExtensionCase() {
        assertEquals(Format.EPUB, Format.of(fileExtension = "EPUB"))
    }

    @Test
    fun testSniffIgnoresMediaTypeCase() {
        assertEquals(Format.EPUB, Format.of(mediaType = "APPLICATION/EPUB+ZIP"))
    }

    @Test
    fun testSniffIgnoresMediaTypeExtraParameters() {
        assertEquals(Format.EPUB, Format.of(mediaType = "application/epub+zip;param=value"))
    }

    @Test
    fun testSniffFromMetadata() {
        assertNull(Format.of(fileExtension = null))
        assertEquals(Format.AUDIOBOOK, Format.of(fileExtension = "audiobook"))
        assertNull(Format.of(mediaType = null))
        assertEquals(Format.AUDIOBOOK, Format.of(mediaType = "application/audiobook+zip"))
        assertEquals(Format.AUDIOBOOK, Format.of(mediaType = "application/audiobook+zip"))
        assertEquals(Format.AUDIOBOOK, Format.of(mediaType = "application/audiobook+zip", fileExtension = "audiobook"))
        assertEquals(Format.AUDIOBOOK, Format.of(mediaTypes = listOf("application/audiobook+zip"), fileExtensions = listOf("audiobook")))
    }

    @Test
    fun testSniffFromAFile() {
        assertEquals(Format.AUDIOBOOK_MANIFEST, Format.of(fixtures.fileAt("audiobook.json")))
    }

    @Test
    fun testSniffFromBytes() {
        assertEquals(Format.AUDIOBOOK_MANIFEST, Format.of({ fixtures.fileAt("audiobook.json")!!.readBytes() }))
    }

    @Test
    fun testSniffUnknownFormat() {
        assertNull(Format.of(mediaType = "unknown/type"))
        assertNull(Format.of(fixtures.fileAt("unknown")))
    }

    @Test
    fun testSniffAudiobook() {
        assertEquals(Format.AUDIOBOOK, Format.of(fileExtension = "audiobook"))
        assertEquals(Format.AUDIOBOOK, Format.of(mediaType = "application/audiobook+zip"))
        assertEquals(Format.AUDIOBOOK, Format.of(fixtures.fileAt("audiobook-package.unknown")))
    }

    @Test
    fun testSniffAudiobookManifest() {
        assertEquals(Format.AUDIOBOOK_MANIFEST, Format.of(mediaType = "application/audiobook+json"))
        assertEquals(Format.AUDIOBOOK_MANIFEST, Format.of(fixtures.fileAt("audiobook.json")))
        assertEquals(Format.AUDIOBOOK_MANIFEST, Format.of(fixtures.fileAt("audiobook-wrongtype.json")))
    }

    @Test
    fun testSniffBMP() {
        assertEquals(Format.BMP, Format.of(fileExtension = "bmp"))
        assertEquals(Format.BMP, Format.of(fileExtension = "dib"))
        assertEquals(Format.BMP, Format.of(mediaType = "image/bmp"))
        assertEquals(Format.BMP, Format.of(mediaType = "image/x-bmp"))
    }

    @Test
    fun testSniffCBZ() {
        assertEquals(Format.CBZ, Format.of(fileExtension = "cbz"))
        assertEquals(Format.CBZ, Format.of(mediaType = "application/vnd.comicbook+zip"))
        assertEquals(Format.CBZ, Format.of(mediaType = "application/x-cbz"))
        assertEquals(Format.CBZ, Format.of(mediaType = "application/x-cbr"))
        assertEquals(Format.CBZ, Format.of(fixtures.fileAt("cbz.unknown")))
    }

    @Test
    fun testSniffDiViNa() {
        assertEquals(Format.DIVINA, Format.of(fileExtension = "divina"))
        assertEquals(Format.DIVINA, Format.of(mediaType = "application/divina+zip"))
        assertEquals(Format.DIVINA, Format.of(fixtures.fileAt("divina-package.unknown")))
    }

    @Test
    fun testSniffDiViNaManifest() {
        assertEquals(Format.DIVINA_MANIFEST, Format.of(mediaType = "application/divina+json"))
        assertEquals(Format.DIVINA_MANIFEST, Format.of(fixtures.fileAt("divina.json")))
    }

    @Test
    fun testSniffEPUB() {
        assertEquals(Format.EPUB, Format.of(fileExtension = "epub"))
        assertEquals(Format.EPUB, Format.of(mediaType = "application/epub+zip"))
        assertEquals(Format.EPUB, Format.of(fixtures.fileAt("epub.unknown")))
    }

    @Test
    fun testSniffGIF() {
        assertEquals(Format.GIF, Format.of(fileExtension = "gif"))
        assertEquals(Format.GIF, Format.of(mediaType = "image/gif"))
    }

    @Test
    fun testSniffHTML() {
        assertEquals(Format.HTML, Format.of(fileExtension = "htm"))
        assertEquals(Format.HTML, Format.of(fileExtension = "html"))
        assertEquals(Format.HTML, Format.of(fileExtension = "xht"))
        assertEquals(Format.HTML, Format.of(fileExtension = "xhtml"))
        assertEquals(Format.HTML, Format.of(mediaType = "text/html"))
        assertEquals(Format.HTML, Format.of(mediaType = "application/xhtml+xml"))
        assertEquals(Format.HTML, Format.of(fixtures.fileAt("html.unknown")))
        assertEquals(Format.HTML, Format.of(fixtures.fileAt("xhtml.unknown")))
    }

    @Test
    fun testSniffJPEG() {
        assertEquals(Format.JPEG, Format.of(fileExtension = "jpg"))
        assertEquals(Format.JPEG, Format.of(fileExtension = "jpeg"))
        assertEquals(Format.JPEG, Format.of(fileExtension = "jpe"))
        assertEquals(Format.JPEG, Format.of(fileExtension = "jif"))
        assertEquals(Format.JPEG, Format.of(fileExtension = "jfif"))
        assertEquals(Format.JPEG, Format.of(fileExtension = "jfi"))
        assertEquals(Format.JPEG, Format.of(mediaType = "image/jpeg"))
    }

    @Test
    fun testSniffOPDS1Feed() {
        assertEquals(Format.OPDS1_FEED, Format.of(mediaType = "application/atom+xml;profile=opds-catalog"))
        assertEquals(Format.OPDS1_FEED, Format.of(fixtures.fileAt("opds1-feed.unknown")))
    }

    @Test
    fun testSniffOPDS1Entry() {
        assertEquals(Format.OPDS1_ENTRY, Format.of(mediaType = "application/atom+xml;type=entry;profile=opds-catalog"))
        assertEquals(Format.OPDS1_ENTRY, Format.of(fixtures.fileAt("opds1-entry.unknown")))
    }

    @Test
    fun testSniffOPDS2Feed() {
        assertEquals(Format.OPDS2_FEED, Format.of(mediaType = "application/opds+json"))
        assertEquals(Format.OPDS2_FEED, Format.of(fixtures.fileAt("opds2-feed.json")))
    }

    @Test
    fun testSniffOPDS2Publication() {
        assertEquals(Format.OPDS2_PUBLICATION, Format.of(mediaType = "application/opds-publication+json"))
        assertEquals(Format.OPDS2_PUBLICATION, Format.of(fixtures.fileAt("opds2-publication.json")))
    }

    @Test
    fun testSniffOPDSAuthentication() {
        assertEquals(Format.OPDS_AUTHENTICATION, Format.of(mediaType = "application/opds-authentication+json"))
        assertEquals(Format.OPDS_AUTHENTICATION, Format.of(mediaType = "application/vnd.opds.authentication.v1.0+json"))
        assertEquals(Format.OPDS_AUTHENTICATION, Format.of(fixtures.fileAt("opds-authentication.json")))
    }

    @Test
    fun testSniffLCPProtectedAudiobook() {
        assertEquals(Format.LCP_PROTECTED_AUDIOBOOK, Format.of(fileExtension = "lcpa"))
        assertEquals(Format.LCP_PROTECTED_AUDIOBOOK, Format.of(mediaType = "application/audiobook+lcp"))
        assertEquals(Format.LCP_PROTECTED_AUDIOBOOK, Format.of(fixtures.fileAt("audiobook-lcp.unknown")))
    }

    @Test
    fun testSniffLCPProtectedPDF() {
        assertEquals(Format.LCP_PROTECTED_PDF, Format.of(fileExtension = "lcpdf"))
        assertEquals(Format.LCP_PROTECTED_PDF, Format.of(mediaType = "application/pdf+lcp"))
        assertEquals(Format.LCP_PROTECTED_PDF, Format.of(fixtures.fileAt("pdf-lcp.unknown")))
    }

    @Test
    fun testSniffLCPLicenseDocument() {
        assertEquals(Format.LCP_LICENSE, Format.of(fileExtension = "lcpl"))
        assertEquals(Format.LCP_LICENSE, Format.of(mediaType = "application/vnd.readium.lcp.license.v1.0+json"))
        assertEquals(Format.LCP_LICENSE, Format.of(fixtures.fileAt("lcpl.unknown")))
    }

    @Test
    fun testSniffLPF() {
        assertEquals(Format.LPF, Format.of(fileExtension = "lpf"))
        assertEquals(Format.LPF, Format.of(mediaType = "application/lpf+zip"))
        assertEquals(Format.LPF, Format.of(fixtures.fileAt("lpf.unknown")))
        assertEquals(Format.LPF, Format.of(fixtures.fileAt("lpf-index-html.unknown")))
    }

    @Test
    fun testSniffPDF() {
        assertEquals(Format.PDF, Format.of(fileExtension = "pdf"))
        assertEquals(Format.PDF, Format.of(mediaType = "application/pdf"))
        assertEquals(Format.PDF, Format.of(fixtures.fileAt("pdf.unknown")))
    }

    @Test
    fun testSniffPNG() {
        assertEquals(Format.PNG, Format.of(fileExtension = "png"))
        assertEquals(Format.PNG, Format.of(mediaType = "image/png"))
    }

    @Test
    fun testSniffTIFF() {
        assertEquals(Format.TIFF, Format.of(fileExtension = "tiff"))
        assertEquals(Format.TIFF, Format.of(fileExtension = "tif"))
        assertEquals(Format.TIFF, Format.of(mediaType = "image/tiff"))
        assertEquals(Format.TIFF, Format.of(mediaType = "image/tiff-fx"))
    }

    @Test
    fun testSniffWebP() {
        assertEquals(Format.WEBP, Format.of(fileExtension = "webp"))
        assertEquals(Format.WEBP, Format.of(mediaType = "image/webp"))
    }

    @Test
    fun testSniffWebPub() {
        assertEquals(Format.WEBPUB, Format.of(fileExtension = "webpub"))
        assertEquals(Format.WEBPUB, Format.of(mediaType = "application/webpub+zip"))
        assertEquals(Format.WEBPUB, Format.of(fixtures.fileAt("webpub-package.unknown")))
    }

    @Test
    fun testSniffWebPubManifest() {
        assertEquals(Format.WEBPUB_MANIFEST, Format.of(mediaType = "application/webpub+json"))
        assertEquals(Format.WEBPUB_MANIFEST, Format.of(fixtures.fileAt("webpub.json")))
    }

    @Test
    fun testSniffW3CWPUBManifest() {
        assertEquals(Format.W3C_WPUB_MANIFEST, Format.of(fixtures.fileAt("w3c-wpub.json")))
    }

    @Test
    fun testSniffZAB() {
        assertEquals(Format.ZAB, Format.of(fileExtension = "zab"))
        assertEquals(Format.ZAB, Format.of(fixtures.fileAt("zab.unknown")))
    }

}
