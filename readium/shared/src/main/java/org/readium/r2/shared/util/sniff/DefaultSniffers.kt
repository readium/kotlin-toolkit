/*
 * Copyright 2023 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.shared.util.sniff

import java.util.Locale
import org.json.JSONObject
import org.readium.r2.shared.extensions.tryOrNull
import org.readium.r2.shared.publication.Link
import org.readium.r2.shared.publication.Manifest
import org.readium.r2.shared.publication.Publication
import org.readium.r2.shared.publication.encryption.encryption
import org.readium.r2.shared.publication.protection.EpubEncryption
import org.readium.r2.shared.util.RelativeUrl
import org.readium.r2.shared.util.Try
import org.readium.r2.shared.util.Url
import org.readium.r2.shared.util.data.Container
import org.readium.r2.shared.util.data.ReadError
import org.readium.r2.shared.util.data.Readable
import org.readium.r2.shared.util.data.decodeJson
import org.readium.r2.shared.util.data.decodeRwpm
import org.readium.r2.shared.util.data.decodeString
import org.readium.r2.shared.util.data.decodeXml
import org.readium.r2.shared.util.data.readDecodeOrElse
import org.readium.r2.shared.util.format.Format
import org.readium.r2.shared.util.getOrDefault
import org.readium.r2.shared.util.getOrElse

public object DefaultContentSniffer
    : ContentSniffer by CompositeContentSniffer(
    listOf(
        RpfSniffer,
        EpubSniffer,
        LpfSniffer,
        ArchiveSniffer,
        PdfSniffer,
        BitmapSniffer,
        XhtmlSniffer,
        HtmlSniffer,
        OpdsSniffer,
        LcpLicenseSniffer,
        LcpSniffer,
        W3cWpubSniffer,
        RwpmSniffer,
        JsonSniffer,
        ZipSniffer,
        RarSniffer
    )
)

/**
 * Sniffs an XHTML document.
 *
 * Must precede the HTML sniffer.
 */
public object XhtmlSniffer : ContentSniffer {
    override fun sniffHints(
        format: Format?,
        hints: FormatHints
    ): Format? {
        if (
            hints.hasFileExtension("xht", "xhtml") ||
            hints.hasMediaType("application/xhtml+xml")
        ) {
            return Format.XHTML
        }

        return format
    }

    override suspend fun sniffBlob(
        format: Format?,
        source: Readable
    ): Try<Format?, ReadError> {
        if (format?.conformsTo(Format.XML) == false || !source.canReadWholeBlob()) {
                return Try.success(format)
        }

        source.readDecodeOrElse(
            decode = { it.decodeXml() },
            recoverRead = { return Try.failure(it) },
            recoverDecode = { null }
        )?.takeIf {
            it.name.lowercase(Locale.ROOT) == "html" &&
                it.namespace.lowercase(Locale.ROOT).contains("xhtml")
        }?.let {
            return Try.success(Format.XHTML)
        }

        return Try.success(format)
    }
}

/** Sniffs an HTML document. */
public object HtmlSniffer : ContentSniffer {
    override fun sniffHints(
        format: Format?,
        hints: FormatHints
    ): Format? {
        if (
            hints.hasFileExtension("htm", "html") ||
            hints.hasMediaType("text/html")
        ) {
            return Format.HTML
        }

        return format
    }

    override suspend fun sniffBlob(
        format: Format?,
        source: Readable
    ): Try<Format?, ReadError> {
        if (!source.canReadWholeBlob()) {
            return Try.success(format)
        }

        // decodeXml will fail if the HTML is not a proper XML document, hence the doctype check.
        source.readDecodeOrElse(
            decode = { it.decodeXml() },
            recoverRead = { return Try.failure(it) },
            recoverDecode = { null }
        )
            ?.takeIf { it.name.lowercase(Locale.ROOT) == "html" }
            ?.let { return Try.success(Format.HTML) }

        source.readDecodeOrElse(
            decode = { it.decodeString() },
            recoverRead = { return Try.failure(it) },
            recoverDecode = { null }
        )
            ?.takeIf { it.trimStart().take(15).lowercase() == "<!doctype html>" }
            ?.let { return Try.success(Format.HTML) }

        return Try.success(format)
    }
}

/** Sniffs an OPDS1 document. */
public object OpdsSniffer : ContentSniffer {

    override fun sniffHints(
        format: Format?,
        hints: FormatHints
    ): Format? {
        return sniffHintsXml(format, hints)
            ?: sniffHintsJson(format, hints)
    }

    private fun sniffHintsXml(format: Format?, hints: FormatHints): Format? {
        // OPDS 1
        if (hints.hasMediaType("application/atom+xml;type=entry;profile=opds-catalog")) {
            return Format.OPDS1_ENTRY
        }
        if (hints.hasMediaType("application/atom+xml;profile=opds-catalog;kind=navigation")) {
            return Format.OPDS1_NAVIGATION_FEED
        }
        if (hints.hasMediaType("application/atom+xml;profile=opds-catalog;kind=acquisition")) {
            return Format.OPDS1_ACQUISITION_FEED
        }
        if (hints.hasMediaType("application/atom+xml;profile=opds-catalog")) {
            return Format.OPDS1
        }

        return format
    }

    private fun sniffHintsJson(format: Format?, hints: FormatHints): Format? {
        // OPDS 2
        if (hints.hasMediaType("application/opds+json")) {
            return Format.OPDS2
        }
        if (hints.hasMediaType("application/opds-publication+json")) {
            return Format.OPDS2_PUBLICATION
        }

        // OPDS Authentication Document.
        if (
            hints.hasMediaType("application/opds-authentication+json") ||
            hints.hasMediaType("application/vnd.opds.authentication.v1.0+json")
        ) {
            return Format.OPDS_AUTHENTICATION
        }

        return format
    }

    override suspend fun sniffBlob(
        format: Format?,
        source: Readable
    ): Try<Format?, ReadError> {
        if (!source.canReadWholeBlob() ) {
            return Try.success(format)
        }

        sniffBlobXml(format, source)
            .getOrElse { return Try.failure(it) }
            ?.let { return Try.success(it) }

        sniffBlobJson(format, source)
            .getOrElse { return Try.failure(it) }
            ?.let { return Try.success(it) }

        return Try.success(format)
    }

    private suspend fun sniffBlobXml(format: Format?, source: Readable): Try<Format?, ReadError> {
        if (format?.conformsTo(Format.XML) == false) {
            return Try.success(format)
        }

        // OPDS 1
        source.readDecodeOrElse(
            decode = { it.decodeXml() },
            recoverRead = { return Try.failure(it) },
            recoverDecode = { null }
        )?.takeIf { it.namespace == "http://www.w3.org/2005/Atom" }
            ?.let { xml ->
                if (xml.name == "feed") {
                    return Try.success(Format.OPDS1)
                } else if (xml.name == "entry") {
                    return Try.success(Format.OPDS1_ENTRY)
                }
            }

        return Try.success(format)
    }

    private suspend fun sniffBlobJson(format: Format?, source: Readable): Try<Format?, ReadError> {
        if (format?.conformsTo(Format.JSON) == false) {
            return Try.success(format)
        }

        // OPDS 2
        source.readDecodeOrElse(
            decode = { it.decodeRwpm() },
            recoverRead = { return Try.failure(it) },
            recoverDecode = { null }
        )
            ?.let { rwpm ->
                if (rwpm.linkWithRel("self")?.mediaType?.matches("application/opds+json") == true
                ) {
                    return Try.success(Format.OPDS2)
                }

                /**
                 * Finds the first [Link] having a relation matching the given [predicate].
                 */
                fun List<Link>.firstWithRelMatching(predicate: (String) -> Boolean): Link? =
                    firstOrNull { it.rels.any(predicate) }

                if (rwpm.links.firstWithRelMatching {
                        it.startsWith(
                            "http://opds-spec.org/acquisition"
                        )
                    } != null
                ) {
                    return Try.success(Format.OPDS2_PUBLICATION)
                }
            }

        // OPDS Authentication Document.
        source.containsJsonKeys("id", "title", "authentication")
            .getOrElse { return Try.failure(it) }
            .takeIf { it }
            ?.let { return Try.success(Format.OPDS_AUTHENTICATION) }

        return Try.success(format)
    }
}

/** Sniffs an LCP License Document. */
public object LcpLicenseSniffer : ContentSniffer {
    override fun sniffHints(
        format: Format?,
        hints: FormatHints
    ): Format? {
        if (
            hints.hasFileExtension("lcpl") ||
            hints.hasMediaType("application/vnd.readium.lcp.license.v1.0+json")
        ) {
            return Format.LCP_LICENSE_DOCUMENT
        }

        return format
    }

    override suspend fun sniffBlob(
        format: Format?,
        source: Readable
    ): Try<Format?, ReadError> {
        if (
            format?.conformsTo(Format.JSON) == false ||
            !source.canReadWholeBlob()
            ) {
                return Try.success(format)
        }

        source.containsJsonKeys("id", "issued", "provider", "encryption")
            .getOrElse { return Try.failure(it) }
            .takeIf { it }
            ?.let { return Try.success(Format.LCP_LICENSE_DOCUMENT) }

        return Try.success(format)
    }
}

/** Sniffs a bitmap image. */
public object BitmapSniffer : ContentSniffer {
    override fun sniffHints(
        format: Format?,
        hints: FormatHints
    ): Format? {
        if (
            hints.hasFileExtension("avif") ||
            hints.hasMediaType("image/avif")
        ) {
            return Format.AVIF
        }
        if (
            hints.hasFileExtension("bmp", "dib") ||
            hints.hasMediaType("image/bmp", "image/x-bmp")
        ) {
            return Format.BMP
        }
        if (
            hints.hasFileExtension("gif") ||
            hints.hasMediaType("image/gif")
        ) {
            return Format.GIF
        }
        if (
            hints.hasFileExtension("jpg", "jpeg", "jpe", "jif", "jfif", "jfi") ||
            hints.hasMediaType("image/jpeg")
        ) {
            return Format.JPEG
        }
        if (
            hints.hasFileExtension("jxl") ||
            hints.hasMediaType("image/jxl")
        ) {
            return Format.JXL
        }
        if (
            hints.hasFileExtension("png") ||
            hints.hasMediaType("image/png")
        ) {
            return Format.PNG
        }
        if (
            hints.hasFileExtension("tiff", "tif") ||
            hints.hasMediaType("image/tiff", "image/tiff-fx")
        ) {
            return Format.TIFF
        }
        if (
            hints.hasFileExtension("webp") ||
            hints.hasMediaType("image/webp")
        ) {
            return Format.WEBP
        }
        return format
    }
}

/** Sniffs a Readium Web Manifest. */
public object RwpmSniffer : ContentSniffer {
    override fun sniffHints(
        format: Format?,
        hints: FormatHints
    ): Format? {
        if (hints.hasMediaType("application/audiobook+json")) {
            return Format.RWPM_AUDIO
        }

        if (hints.hasMediaType("application/divina+json")) {
            return Format.RWPM_IMAGE
        }

        if (hints.hasMediaType("application/webpub+json")) {
            return Format.RWPM
        }

        return format
    }

    public override suspend fun sniffBlob(
        format: Format?,
        source: Readable
    ): Try<Format?, ReadError> {
        if (
            format?.conformsTo(Format.JSON) == false ||
            !source.canReadWholeBlob()
            ) {
                return Try.success(format)
        }

        val manifest: Manifest =
            source.readDecodeOrElse(
                decode = { it.decodeRwpm() },
                recoverRead = { return Try.failure(it) },
                recoverDecode = { null }
            ) ?: return Try.success(format)

        if (manifest.conformsTo(Publication.Profile.AUDIOBOOK)) {
            return Try.success(Format.RWPM_AUDIO)
        }

        if (manifest.conformsTo(Publication.Profile.DIVINA)) {
            return Try.success(Format.RWPM_IMAGE)
        }
        if (manifest.linkWithRel("self")?.mediaType?.matches("application/webpub+json") == true) {
            return Try.success(Format.RWPM)
        }

        return Try.success(format)
    }
}

/** Sniffs a Readium Web Publication, protected or not by LCP. */
public object RpfSniffer : ContentSniffer {
    override fun sniffHints(
        format: Format?,
        hints: FormatHints
    ): Format? {
        if (
            hints.hasFileExtension("audiobook") ||
            hints.hasMediaType("application/audiobook+zip")
        ) {
            return Format.RPF_AUDIO
        }

        if (
            hints.hasFileExtension("divina") ||
            hints.hasMediaType("application/divina+zip")
        ) {
            return Format.RPF_IMAGE
        }

        if (
            hints.hasFileExtension("webpub") ||
            hints.hasMediaType("application/webpub+zip")
        ) {
            return Format.RPF
        }

        if (
            hints.hasFileExtension("lcpa") ||
            hints.hasMediaType("application/audiobook+lcp")
        ) {
            return Format.RPF_AUDIO_LCP
        }
        if (
            hints.hasFileExtension("lcpdf") ||
            hints.hasMediaType("application/pdf+lcp")
        ) {
            return Format.RPF_PDF_LCP
        }

        return format
    }

    override suspend fun sniffContainer(
        format: Format?,
        container: Container<Readable>
    ): Try<Format?, ReadError> {
        // Recognize exploded RPF.
        if (format?.conformsTo(Format.ZIP) == false) {
            return Try.success(format)
        }

        // Reads a RWPM from a manifest.json archive entry.
        val manifest: Manifest =
            container[RelativeUrl("manifest.json")!!]
                ?.read()
                ?.getOrElse { return Try.failure(it) }
                ?.let { tryOrNull { Manifest.fromJSON(JSONObject(String(it))) } }
                ?: return Try.success(format)

        if (manifest.conformsTo(Publication.Profile.AUDIOBOOK)) {
            return Try.success(Format.RPF_AUDIO)
        }
        if (manifest.conformsTo(Publication.Profile.DIVINA)) {
            return Try.success(Format.RPF_IMAGE)
        }
        if (manifest.conformsTo(Publication.Profile.PDF)) {
            return Try.success(Format.RPF_PDF)
        }
        if (manifest.linkWithRel("self")?.mediaType?.matches("application/webpub+json") == true) {
            Try.success(Format.RPF)
        }

        return Try.success(format)
    }
}

/** Sniffs a W3C Web Publication Manifest. */
public object W3cWpubSniffer : ContentSniffer {

    override suspend fun sniffBlob(
        format: Format?,
        source: Readable
    ): Try<Format?, ReadError> {
        if (!source.canReadWholeBlob() || format?.conformsTo(Format.JSON) == false) {
            return Try.success(format)
        }

        // Somehow, [JSONObject] can't access JSON-LD keys such as `@content`.
        val string = source.readDecodeOrElse(
            decode = { it.decodeString() },
            recoverRead = { return Try.failure(it) },
            recoverDecode = { "" }
        )
        if (
            string.contains("@context") &&
            string.contains("https://www.w3.org/ns/wp-context")
        ) {
            return Try.success(Format.W3C_WPUB_MANIFEST)
        }

        return Try.success(format)
    }
}

/**
 * Sniffs an EPUB publication.
 *
 * Reference: https://www.w3.org/publishing/epub3/epub-ocf.html#sec-zip-container-mime
 */
public object EpubSniffer : ContentSniffer {
    override fun sniffHints(
        format: Format?,
        hints: FormatHints
    ): Format? {
        if (
            hints.hasFileExtension("epub") ||
            hints.hasMediaType("application/epub+zip")
        ) {
            return Format.EPUB
        }

        return format
    }

    override suspend fun sniffContainer(
        format: Format?,
        container: Container<Readable>
    ): Try<Format?, ReadError> {
        // Recognize exploded EPUBs.
        if (format?.conformsTo(Format.ZIP) == false) {
            return Try.success(format)
        }

        val mimetype = container[RelativeUrl("mimetype")!!]
            ?.readDecodeOrElse(
                decode = { it.decodeString() },
                recoverRead = { return Try.failure(it) },
                recoverDecode = { null }
            )?.trim()

        if (mimetype == "application/epub+zip") {
            return Try.success(Format.EPUB)
        }

        return Try.success(format)
    }
}

/**
 * Sniffs a Lightweight Packaging Format (LPF).
 *
 * References:
 *  - https://www.w3.org/TR/lpf/
 *  - https://www.w3.org/TR/pub-manifest/
 */
public object LpfSniffer : ContentSniffer {
    override fun sniffHints(
        format: Format?,
        hints: FormatHints
    ): Format? {
        if (
            hints.hasFileExtension("lpf") ||
            hints.hasMediaType("application/lpf+zip")
        ) {
            return Format.LPF
        }

        return format
    }

    override suspend fun sniffContainer(
        format: Format?,
        container: Container<Readable>
    ): Try<Format?, ReadError> {
        // Recognize exploded LPFs.
        if (format?.conformsTo(Format.ZIP) == false) {
            return Try.success(format)
        }

        if (RelativeUrl("index.html")!! in container) {
            return Try.success(Format.LPF)
        }

        // Somehow, [JSONObject] can't access JSON-LD keys such as `@content`.
        container[RelativeUrl("publication.json")!!]
            ?.read()
            ?.getOrElse { return Try.failure(it) }
            ?.let { tryOrNull { String(it) } }
            ?.let { manifest ->
                if (
                    manifest.contains("@context") &&
                    manifest.contains("https://www.w3.org/ns/pub-context")
                ) {
                    return Try.success(Format.LPF)
                }
            }

        return Try.success(format)
    }
}

/**
 * Sniffs a RAR archive.
 *
 * At the moment, only hints are supported.
 */
public object RarSniffer : ContentSniffer {

    override fun sniffHints(
        format: Format?,
        hints: FormatHints
    ): Format? {
        if (
            hints.hasFileExtension("rar") ||
            hints.hasMediaType("application/vnd.rar") ||
            hints.hasMediaType("application/x-rar") ||
            hints.hasMediaType("application/x-rar-compressed")
        ) {
            return Format.RAR
        }

        return format
    }
}

/**
 * Sniffs a ZIP archive.
 */
public object ZipSniffer : ContentSniffer {

    override fun sniffHints(
        format: Format?,
        hints: FormatHints
    ): Format? {
        if (hints.hasMediaType("application/zip") ||
            hints.hasFileExtension("zip")
        ) {
            return Format.ZIP
        }

        return format
    }
}

/**
 * Sniffs a simple Archive-based publication format, like Comic Book Archive or Zipped Audio Book.
 *
 * Reference: https://wiki.mobileread.com/wiki/CBR_and_CBZ
 */
public object ArchiveSniffer : ContentSniffer {

    /**
     * Authorized extensions for resources in a CBZ archive.
     * Reference: https://wiki.mobileread.com/wiki/CBR_and_CBZ
     */
    private val cbzExtensions = listOf(
        // bitmap
        "bmp", "dib", "gif", "jif", "jfi", "jfif", "jpg", "jpeg", "png", "tif", "tiff", "webp",
        // metadata
        "acbf", "xml"
    )

    /**
     * Authorized extensions for resources in a ZAB archive (Zipped Audio Book).
     */
    private val zabExtensions = listOf(
        // audio
        "aac",
        "aiff",
        "alac",
        "flac",
        "m4a",
        "m4b",
        "mp3",
        "ogg",
        "oga",
        "mogg",
        "opus",
        "wav",
        "webm",
        // playlist
        "asx",
        "bio",
        "m3u",
        "m3u8",
        "pla",
        "pls",
        "smil",
        "vlc",
        "wpl",
        "xspf",
        "zpl"
    )

    override fun sniffHints(
        format: Format?,
        hints: FormatHints
    ): Format? {
        if (
            hints.hasFileExtension("cbz") ||
            hints.hasMediaType(
                "application/vnd.comicbook+zip",
                "application/x-cbz"
            )
        ) {
            return Format.CBZ
        }

        if (
            hints.hasFileExtension("cbr") ||
            hints.hasMediaType("application/vnd.comicbook-rar") ||
            hints.hasMediaType("application/x-cbr")
        ) {
            return Format.CBR
        }

        if (hints.hasFileExtension("zab")) {
            return Format.ZAB
        }

        return format
    }

    override suspend fun sniffContainer(
        format: Format?,
        container: Container<Readable>
    ): Try<Format?, ReadError> {
        fun isIgnored(url: Url): Boolean =
            url.filename?.startsWith(".") == true || url.filename == "Thumbs.db"

        fun archiveContainsOnlyExtensions(fileExtensions: List<String>): Boolean =
            container.all { url ->
                isIgnored(url) || url.extension?.let {
                    fileExtensions.contains(
                        it.lowercase(Locale.ROOT)
                    )
                } == true
            }

        if (
            archiveContainsOnlyExtensions(cbzExtensions) &&
            format?.conformsTo(Format.ZIP) != false // Recognize exploded CBZ/CBR
        ) {
            return Try.success(Format.CBZ)
        }

        if (
            archiveContainsOnlyExtensions(cbzExtensions) &&
            format?.conformsTo(Format.RAR) == true
        ) {
            return Try.success(Format.CBR)
        }

        if (
            archiveContainsOnlyExtensions(zabExtensions) &&
            format?.conformsTo(Format.ZIP) != false // Recognize exploded ZAB
        ) {
            return Try.success(Format.ZAB)
        }

        return Try.success(format)
    }
}

/**
 * Sniffs a PDF document.
 *
 * Reference: https://www.loc.gov/preservation/digital/formats/fdd/fdd000123.shtml
 */
public object PdfSniffer : ContentSniffer {
    override fun sniffHints(
        format: Format?,
        hints: FormatHints
    ): Format? {
        if (
            hints.hasFileExtension("pdf") ||
            hints.hasMediaType("application/pdf")
        ) {
            return Format.PDF
        }

        return format
    }

    override suspend fun sniffBlob(
        format: Format?,
        source: Readable
    ): Try<Format?, ReadError> {
        source.read(0L until 5L)
            .getOrElse { return Try.failure(it) }
            .let { tryOrNull { it.toString(Charsets.UTF_8) } }
            .takeIf { it == "%PDF-" }
            ?.let { return Try.success(Format.PDF) }

        return Try.success(format)
    }
}

/** Sniffs a JSON document. */
public object JsonSniffer : ContentSniffer {
    override fun sniffHints(
        format: Format?,
        hints: FormatHints
    ): Format? {
        if (hints.hasFileExtension("json") ||
            hints.hasMediaType("application/json")) {
            return Format.JSON
        }

        if (hints.hasMediaType("application/problem+json")) {
            return Format.JSON_PROBLEM_DETAILS
        }

        return format
    }

    override suspend fun sniffBlob(
        format: Format?,
        source: Readable
    ): Try<Format?, ReadError> {
        if (!source.canReadWholeBlob()) {
            return Try.success(format)
        }

        source.readDecodeOrElse(
            decode = { it.decodeJson() },
            recoverRead = { return Try.failure(it) },
            recoverDecode = { null }
        )?.let { return Try.success(Format.JSON) }

        return Try.success(format)
    }
}

/**
 * Sniffs Adept protection on EPUBs.
 */
public object AdeptSniffer : ContentSniffer {

    override suspend fun sniffContainer(
        format: Format?,
        container: Container<Readable>
    ): Try<Format?, ReadError> {
        if (format?.conformsTo(Format.EPUB) != true) {
            return Try.success(format)
        }

        container[Url("META-INF/encryption.xml")!!]
            ?.readDecodeOrElse(
                decode = { it.decodeXml() },
                recover = { null }
            )
            ?.get("EncryptedData", EpubEncryption.ENC)
            ?.flatMap { it.get("KeyInfo", EpubEncryption.SIG) }
            ?.flatMap { it.get("resource", "http://ns.adobe.com/adept") }
            ?.takeIf { it.isNotEmpty() }
            ?.let { return Try.success(Format.EPUB_ADEPT) }

        container[Url("META-INF/rights.xml")!!]
            ?.readDecodeOrElse(
                decode = { it.decodeXml() },
                recover = { null }
            )
            ?.takeIf { it.namespace == "http://ns.adobe.com/adept" }
            ?.let { return Try.success(Format.EPUB_ADEPT) }

        return Try.success(format)
    }
}

/**
 * Sniffs LCP protected packages.
 */
public object LcpSniffer : ContentSniffer {

    override suspend fun sniffContainer(
        format: Format?,
        container: Container<Readable>
    ): Try<Format?, ReadError> {
       when {
           format?.conformsTo(Format.RPF) == true -> {
               val isLcpProtected = RelativeUrl("license.lcpl")!! in container ||
                   hasLcpSchemeInManifest(container)
                       .getOrElse { return Try.failure(it) }

               if (isLcpProtected) {
                   val newFormat = when (format) {
                       Format.RPF_IMAGE -> Format.RPF_IMAGE_LCP
                       Format.RPF_AUDIO -> Format.RPF_AUDIO_LCP
                       Format.RPF_PDF -> Format.RPF_PDF_LCP
                       Format.RPF -> Format.RPF_LCP
                       else -> null
                   }
                   newFormat?.let { return Try.success(it) }
               }
           }

           format?.conformsTo(Format.EPUB) == true -> {
               val isLcpProtected = RelativeUrl("META-INF/license.lcpl")!! in container ||
                   hasLcpSchemeInEncryptionXml(container)
                       .getOrElse { return Try.failure(it) }

               if (isLcpProtected) {
                   return Try.success(Format.EPUB_LCP)
               }
           }
       }

        return Try.success(format)
    }

    private suspend fun hasLcpSchemeInManifest(container: Container<Readable>): Try<Boolean, ReadError> {
        val manifest = container[Url("manifest.json")!!]
            ?.readDecodeOrElse(
                decode = { it.decodeRwpm() },
                recoverRead = { return Try.success(false) },
                recoverDecode = { return Try.success(false) }
            ) ?: return Try.success(false)

        val manifestHasLcpScheme = manifest
            .readingOrder
            .any { it.properties.encryption?.scheme == "http://readium.org/2014/01/lcp" }

        return Try.success(manifestHasLcpScheme)
    }

    private suspend fun hasLcpSchemeInEncryptionXml(container: Container<Readable>): Try<Boolean, ReadError> {
        val encryptionXml = container[Url("META-INF/encryption.xml")!!]
            ?.readDecodeOrElse(
                decode = { it.decodeXml() },
                recover = { return Try.failure(it) }
            ) ?: return Try.success(false)

        val hasLcpScheme = encryptionXml
            .get("EncryptedData", EpubEncryption.ENC)
            .flatMap { it.get("KeyInfo", EpubEncryption.SIG) }
            .flatMap { it.get("RetrievalMethod", EpubEncryption.SIG) }
            .any { it.getAttr("URI") == "license.lcpl#/encryption/content_key" }

        return Try.success(hasLcpScheme)
    }
}

private suspend fun Readable.canReadWholeBlob() =
    length().getOrDefault(0) < 5 * 1000 * 1000

/**
 * Returns whether the content is a JSON object containing all of the given root keys.
 */
@Suppress("SameParameterValue")
private suspend fun Readable.containsJsonKeys(
    vararg keys: String
): Try<Boolean, ReadError> {
    val json = readDecodeOrElse(
        decode = { it.decodeJson() },
        recoverRead = { return Try.failure(it) },
        recoverDecode = { return Try.success(false) }
    )
    return Try.success(json.keys().asSequence().toSet().containsAll(keys.toList()))
}
