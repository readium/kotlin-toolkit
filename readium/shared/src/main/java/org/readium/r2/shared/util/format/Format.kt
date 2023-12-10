/*
 * Copyright 2023 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.shared.util.format

@JvmInline
public value class Format(public val id: String) {

    public fun conformsTo(other: Format): Boolean {
        val thisComponents = id.split(".")
        val otherComponents = other.id.split(".")
        return thisComponents.containsAll(otherComponents)
    }

    public companion object {

        public val RAR: Format = Format("rar")
        public val CBR: Format = Format("rar.image")

        public val ZIP: Format = Format("zip")
        public val CBZ: Format = Format("zip.image")
        public val ZAB: Format = Format("zip.audio")
        public val LPF: Format = Format("zip.lpf")
        public val EPUB: Format = Format("zip.epub")
        public val EPUB_LCP: Format = Format("zip.epub.lcp")
        public val EPUB_ADEPT: Format = Format("zip.epub.adept")

        public val RPF: Format = Format("zip.rpf")
        public val RPF_AUDIO: Format = Format("zip.rpf.audio")
        public val RPF_AUDIO_LCP: Format = Format("zip.rpf.audio.lcp")
        public val RPF_IMAGE: Format = Format("zip.rpf.image")
        public val RPF_IMAGE_LCP: Format = Format("zip.rpf.image.lcp")
        public val RPF_PDF: Format = Format("zip.rpf.pdf")
        public val RPF_PDF_LCP: Format = Format("zip.rpf.pdf.lcp")
        public val RPF_LCP: Format = Format("zip.rpf.lcp")

        public val JSON: Format = Format("json")
        public val JSON_PROBLEM_DETAILS: Format = Format("json.problem_details")
        public val LCP_LICENSE_DOCUMENT: Format = Format("json.lcpl")
        public val W3C_WPUB_MANIFEST: Format = Format("json.w3c_wp_manifest")
        public val RWPM: Format = Format("json.rwpm")
        public val RWPM_AUDIO: Format = Format("json.rwpm.audio")
        public val RWPM_IMAGE: Format = Format("json.rwpm.image")
        public val OPDS2: Format = Format("json.opds")
        public val OPDS2_PUBLICATION: Format = Format("json.opds_publication")
        public val OPDS_AUTHENTICATION: Format = Format("json.opds_authentication")

        public val PDF: Format = Format("pdf")
        public val HTML: Format = Format("html")

        public val AVIF: Format = Format("avif")
        public val BMP: Format = Format("bmp")
        public val GIF: Format = Format("gif")
        public val JPEG: Format = Format("jpeg")
        public val JXL: Format = Format("jxl")
        public val PNG: Format = Format("png")
        public val TIFF: Format = Format("tiff")
        public val WEBP: Format = Format("webp")


        public val XML: Format = Format("xml")
        public val XHTML: Format = Format("xml.html")
        public val ATOM: Format = Format("xml.atom")
        public val OPDS1: Format = Format("xml.atom.opds")
        public val OPDS1_ENTRY: Format = Format("xml.atom.opds_entry")
        public val OPDS1_NAVIGATION_FEED: Format = Format("xml.atom.opds_navigation_feed")
        public val OPDS1_ACQUISITION_FEED: Format = Format("xml.atom.opds_acquisition_feed")
    }
}
