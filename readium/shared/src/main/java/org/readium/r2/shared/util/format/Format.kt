/*
 * Copyright 2023 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.shared.util.format

@JvmInline
public value class Trait(private val id: String) {

    override fun toString(): String = id

    public companion object {

        public val ZIP: Trait = Trait("zip")
        public val RAR: Trait = Trait("rar")

        public val JSON: Trait = Trait("json")
        public val JSON_PROBLEM_DETAILS: Trait = Trait("json_problem_details")
        public val LCP_LICENSE_DOCUMENT: Trait = Trait("lcp_license_document")

        public val W3C_PUB_MANIFEST: Trait = Trait("w3c_pub_manifest")
        public val W3C_AUDIOBOOK_MANIFEST: Trait = Trait("w3c_audiobook_manifest")

        public val READIUM_WEBPUB_MANIFEST: Trait = Trait("readium_webpub_manifest")
        public val READIUM_AUDIOBOOK_MANIFEST: Trait = Trait("readium_audiobook_manifest")
        public val READIUM_COMICS_MANIFEST: Trait = Trait("readium_comics_manifest")
        public val READIUM_PDF_MANIFEST: Trait = Trait("readium_pdf_manifest")

        public val XML: Trait = Trait("xml")

        public val LCP_PROTECTED: Trait = Trait("lcp")
        public val ADEPT_PROTECTED: Trait = Trait("adept")

        public val PDF: Trait = Trait("pdf")
        public val HTML: Trait = Trait("html")
        public val AUDIO: Trait = Trait("audio")
        public val BITMAP: Trait = Trait("bitmap")

        public val AVIF: Trait = Trait("avif")
        public val BMP: Trait = Trait("bmp")
        public val GIF: Trait = Trait("gif")
        public val JPEG: Trait = Trait("jpeg")
        public val JXL: Trait = Trait("jxl")
        public val PNG: Trait = Trait("png")
        public val TIFF: Trait = Trait("tiff")
        public val WEBP: Trait = Trait("webp")

        public val EPUB: Trait = Trait("epub")
        public val RPF: Trait = Trait("rpf")
        public val LPF: Trait = Trait("lpf")
        public val AUDIOBOOK: Trait = Trait("audiobook")
        public val COMICS: Trait = Trait("comics")
        public val PDFBOOK: Trait = Trait("pdfbook")
        public val WEBPUB: Trait = Trait("webpub")

        public val OPDS1_CATALOG: Trait = Trait("opds1_catalog")
        public val OPDS1_ENTRY: Trait = Trait("opds1_entry")
        public val OPDS1_NAVIGATION_FEED: Trait = Trait("opds1_navigation_feed")
        public val OPDS1_ACQUISITION_FEED: Trait = Trait("opds1_acquisition_feed")

        public val OPDS2_CATALOG: Trait = Trait("opds2_catalog")
        public val OPDS2_PUBLICATION: Trait = Trait("opds2_publication")
        public val OPDS_AUTHENTICATION: Trait = Trait("opds_authentication")
    }
}

@JvmInline
public value class Format(private val traits: Set<Trait>) {

    public operator fun plus(trait: Trait): Format =
        Format(traits + trait)

    public operator fun minus(trait: Trait): Format =
        Format(traits - trait)

    public fun conformsTo(trait: Trait): Boolean =
        trait in traits

    public fun conformsTo(format: Format): Boolean =
        format.traits.all { it in this.traits }

    public override fun toString(): String {
        return traits.joinToString(";") { it.toString() }
    }

    public companion object {

        public operator fun invoke(string: String): Format =
            Format(string.split(";").map { Trait(it) }.toSet())

        internal fun Format?.orEmpty() = this ?: Format(setOf())

        public val ZIP: Format = Format(setOf(Trait.ZIP))
        public val RAR: Format = Format(setOf(Trait.RAR))
        public val LCP_LICENSE_DOCUMENT: Format = Format(
            setOf(Trait.JSON, Trait.LCP_LICENSE_DOCUMENT)
        )

        public val READIUM_WEBPUB_MANIFEST: Format = Format(
            setOf(Trait.JSON, Trait.READIUM_WEBPUB_MANIFEST)
        )
        public val READIUM_AUDIOBOOK_MANIFEST: Format = Format(
            setOf(Trait.JSON, Trait.READIUM_AUDIOBOOK_MANIFEST)
        )
        public val READIUM_COMICS_MANIFEST: Format = Format(
            setOf(Trait.JSON, Trait.READIUM_COMICS_MANIFEST)
        )
        public val READIUM_PDF_MANIFEST: Format = Format(
            setOf(Trait.JSON, Trait.READIUM_PDF_MANIFEST)
        )

        public val READIUM_WEBPUB: Format = Format(setOf(Trait.ZIP, Trait.RPF))
        public val READIUM_AUDIOBOOK: Format = Format(setOf(Trait.ZIP, Trait.RPF, Trait.AUDIOBOOK))
        public val READIUM_COMICS: Format = Format(setOf(Trait.ZIP, Trait.RPF, Trait.COMICS))
        public val READIUM_PDF: Format = Format(setOf(Trait.ZIP, Trait.RPF, Trait.PDFBOOK))

        public val PDF: Format = Format(setOf(Trait.PDF))
        public val EPUB: Format = Format(setOf(Trait.ZIP, Trait.EPUB))
        public val CBZ: Format = Format(setOf(Trait.ZIP, Trait.COMICS))
        public val CBR: Format = Format(setOf(Trait.RAR, Trait.COMICS))
        public val ZAB: Format = Format(setOf(Trait.ZIP, Trait.AUDIOBOOK))

        public val XML: Format = Format(setOf(Trait.XML))
        public val XHTML: Format = Format(setOf(Trait.XML, Trait.HTML))
        public val HTML: Format = Format(setOf(Trait.HTML))

        public val AVIF: Format = Format(setOf(Trait.BITMAP, Trait.AVIF))
        public val BMP: Format = Format(setOf(Trait.BITMAP, Trait.BMP))
        public val GIF: Format = Format(setOf(Trait.BITMAP, Trait.GIF))
        public val JPEG: Format = Format(setOf(Trait.BITMAP, Trait.JPEG))
        public val JXL: Format = Format(setOf(Trait.BITMAP, Trait.JXL))
        public val PNG: Format = Format(setOf(Trait.BITMAP, Trait.PNG))
        public val TIFF: Format = Format(setOf(Trait.BITMAP, Trait.TIFF))
        public val WEBP: Format = Format(setOf(Trait.BITMAP, Trait.WEBP))

        public val JSON: Format = Format(setOf(Trait.JSON))
        public val JSON_PROBLEM_DETAILS: Format = Format(
            setOf(Trait.JSON, Trait.JSON_PROBLEM_DETAILS)
        )

        public val W3C_PUB_MANIFEST: Format = Format(setOf(Trait.JSON, Trait.W3C_PUB_MANIFEST))
        public val W3C_AUDIOBOOK_MANIFEST: Format = Format(
            setOf(Trait.JSON, Trait.W3C_AUDIOBOOK_MANIFEST)
        )

        public val OPDS1_CATALOG: Format = Format(setOf(Trait.XML, Trait.OPDS1_CATALOG))
        public val OPDS1_ENTRY: Format = Format(setOf(Trait.XML, Trait.OPDS1_ENTRY))
        public val OPDS1_NAVIGATION_FEED: Format = Format(
            setOf(Trait.XML, Trait.OPDS1_NAVIGATION_FEED)
        )
        public val OPDS1_ACQUISITION_FEED: Format = Format(
            setOf(Trait.XML, Trait.OPDS1_ACQUISITION_FEED)
        )

        public val OPDS2_CATALOG: Format = Format(setOf(Trait.JSON, Trait.OPDS2_CATALOG))
        public val OPDS2_PUBLICATION: Format = Format(setOf(Trait.JSON, Trait.OPDS2_PUBLICATION))
        public val OPDS_AUTHENTICATION: Format = Format(
            setOf(Trait.JSON, Trait.OPDS_AUTHENTICATION)
        )
    }
}
