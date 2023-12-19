/*
 * Copyright 2023 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.shared.util.format

import java.util.Locale
import org.json.JSONObject
import org.readium.r2.shared.extensions.tryOrNull
import org.readium.r2.shared.publication.Link
import org.readium.r2.shared.publication.Manifest
import org.readium.r2.shared.publication.Publication
import org.readium.r2.shared.publication.encryption.encryption
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
import org.readium.r2.shared.util.flatMap
import org.readium.r2.shared.util.format.Format.Companion.orEmpty
import org.readium.r2.shared.util.getOrDefault
import org.readium.r2.shared.util.getOrElse

public class DefaultFormatSniffer :
    FormatSniffer by CompositeFormatSniffer(
        ZipSniffer,
        RarSniffer,
        EpubSniffer,
        LpfSniffer,
        ArchiveSniffer,
        RpfSniffer,
        PdfSniffer,
        HtmlSniffer,
        BitmapSniffer,
        AudioSniffer,
        JsonSniffer,
        OpdsSniffer,
        LcpLicenseSniffer,
        LcpSniffer,
        AdeptSniffer,
        W3cWpubSniffer,
        RwpmSniffer
    )

/** Sniffs an HTML or XHTML document. */
public object HtmlSniffer : FormatSniffer {
    override fun sniffHints(
        format: Format?,
        hints: FormatHints
    ): Format? {
        if (
            hints.hasFileExtension("htm", "html") ||
            hints.hasMediaType("text/html")
        ) {
            return format.orEmpty() + Format.HTML
        }

        if (
            hints.hasFileExtension("xht", "xhtml") ||
            hints.hasMediaType("application/xhtml+xml")
        ) {
            return format.orEmpty() + Format.XHTML
        }

        return format
    }

    override suspend fun sniffBlob(
        format: Format?,
        source: Readable
    ): Try<Format?, ReadError> {
        if (format != null && format != Format.XML || !source.canReadWholeBlob()) {
            return Try.success(format)
        }

        // decodeXml will fail if the HTML is not a proper XML document, hence the doctype check.
        source.readDecodeOrElse(
            decode = { it.decodeXml() },
            recoverRead = { return Try.failure(it) },
            recoverDecode = { null }
        )
            ?.takeIf { it.name.lowercase(Locale.ROOT) == "html" }
            ?.let {
                return Try.success(
                    if (it.namespace.lowercase(Locale.ROOT).contains("xhtml")) {
                        Format.XHTML
                    } else {
                        Format.HTML
                    }
                )
            }

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
public object OpdsSniffer : FormatSniffer {

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
            return format.orEmpty() + Format.OPDS1_ENTRY
        }
        if (hints.hasMediaType("application/atom+xml;profile=opds-catalog;kind=navigation")) {
            return format.orEmpty() + Format.OPDS1_NAVIGATION_FEED
        }
        if (hints.hasMediaType("application/atom+xml;profile=opds-catalog;kind=acquisition")) {
            return format.orEmpty() + Format.OPDS1_ACQUISITION_FEED
        }
        if (hints.hasMediaType("application/atom+xml;profile=opds-catalog")) {
            return format.orEmpty() + Format.OPDS1_CATALOG
        }

        return format
    }

    private fun sniffHintsJson(format: Format?, hints: FormatHints): Format? {
        // OPDS 2
        if (hints.hasMediaType("application/opds+json")) {
            return format.orEmpty() + Format.OPDS2_CATALOG
        }
        if (hints.hasMediaType("application/opds-publication+json")) {
            return format.orEmpty() + Format.OPDS2_PUBLICATION
        }

        // OPDS Authentication Document.
        if (
            hints.hasMediaType("application/opds-authentication+json") ||
            hints.hasMediaType("application/vnd.opds.authentication.v1.0+json")
        ) {
            return format.orEmpty() + Format.OPDS_AUTHENTICATION
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

        sniffBlobXml(format, source)
            .flatMap { sniffBlobJson(it, source) }
            .getOrElse { return Try.failure(it) }
            ?.let { return Try.success(it) }

        return Try.success(format)
    }

    private suspend fun sniffBlobXml(format: Format?, source: Readable): Try<Format?, ReadError> {
        if (format != null && format != Format.XML) {
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
                    return Try.success(Format.OPDS1_CATALOG)
                } else if (xml.name == "entry") {
                    return Try.success(Format.OPDS1_ENTRY)
                }
            }

        return Try.success(format)
    }

    private suspend fun sniffBlobJson(format: Format?, source: Readable): Try<Format?, ReadError> {
        if (format != null && format != Format.JSON) {
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
                    return Try.success(Format.OPDS2_CATALOG)
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
public object LcpLicenseSniffer : FormatSniffer {
    override fun sniffHints(
        format: Format?,
        hints: FormatHints
    ): Format? {
        if (
            hints.hasFileExtension("lcpl") ||
            hints.hasMediaType("application/vnd.readium.lcp.license.v1.0+json")
        ) {
            return format.orEmpty() + Format.LCP_LICENSE_DOCUMENT
        }

        return format
    }

    override suspend fun sniffBlob(
        format: Format?,
        source: Readable
    ): Try<Format?, ReadError> {
        if (
            format != null && format != Format.JSON ||
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
public object BitmapSniffer : FormatSniffer {

    override fun sniffHints(
        format: Format?,
        hints: FormatHints
    ): Format? {
        if (
            hints.hasFileExtension("avif") ||
            hints.hasMediaType("image/avif")
        ) {
            return format.orEmpty() + Format(setOf(Trait.BITMAP, Trait.AVIF))
        }
        if (
            hints.hasFileExtension("bmp", "dib") ||
            hints.hasMediaType("image/bmp", "image/x-bmp")
        ) {
            return format.orEmpty() + Format(setOf(Trait.BITMAP, Trait.BMP))
        }
        if (
            hints.hasFileExtension("gif") ||
            hints.hasMediaType("image/gif")
        ) {
            return format.orEmpty() + Format(setOf(Trait.BITMAP, Trait.GIF))
        }
        if (
            hints.hasFileExtension("jpg", "jpeg", "jpe", "jif", "jfif", "jfi") ||
            hints.hasMediaType("image/jpeg")
        ) {
            return format.orEmpty() + Format(setOf(Trait.BITMAP, Trait.JPEG))
        }
        if (
            hints.hasFileExtension("jxl") ||
            hints.hasMediaType("image/jxl")
        ) {
            return format.orEmpty() + Format(setOf(Trait.BITMAP, Trait.JXL))
        }
        if (
            hints.hasFileExtension("png") ||
            hints.hasMediaType("image/png")
        ) {
            return format.orEmpty() + Format(setOf(Trait.BITMAP, Trait.PNG))
        }
        if (
            hints.hasFileExtension("tiff", "tif") ||
            hints.hasMediaType("image/tiff", "image/tiff-fx")
        ) {
            return format.orEmpty() + Format(setOf(Trait.BITMAP, Trait.TIFF))
        }
        if (
            hints.hasFileExtension("webp") ||
            hints.hasMediaType("image/webp")
        ) {
            return format.orEmpty() + Format(setOf(Trait.BITMAP, Trait.WEBP))
        }

        return format
    }
}

/** Sniffs audio files. */
public object AudioSniffer : FormatSniffer {
    override fun sniffHints(format: Format?, hints: FormatHints): Format? {
        if (
            hints.hasFileExtension(
                "aac", "aiff", "alac", "flac", "m4a", "m4b", "mp3",
                "ogg", "oga", "mogg", "opus", "wav", "webm"
            )
        ) {
            return format.orEmpty() + Format(setOf(Trait.AUDIO))
        }

        return format
    }
}

/** Sniffs a Readium Web Manifest. */
public object RwpmSniffer : FormatSniffer {
    override fun sniffHints(
        format: Format?,
        hints: FormatHints
    ): Format? {
        if (hints.hasMediaType("application/audiobook+json")) {
            return format.orEmpty() + Format.READIUM_AUDIOBOOK_MANIFEST
        }

        if (hints.hasMediaType("application/divina+json")) {
            return format.orEmpty() + Format.READIUM_COMICS_MANIFEST
        }

        if (hints.hasMediaType("application/webpub+json")) {
            return format.orEmpty() + Format.READIUM_WEBPUB_MANIFEST
        }

        return format
    }

    public override suspend fun sniffBlob(
        format: Format?,
        source: Readable
    ): Try<Format?, ReadError> {
        if (
            format != null && format != Format.JSON ||
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
            return Try.success(Format.READIUM_AUDIOBOOK_MANIFEST)
        }

        if (manifest.conformsTo(Publication.Profile.DIVINA)) {
            return Try.success(Format.READIUM_COMICS_MANIFEST)
        }
        if (manifest.linkWithRel("self")?.mediaType?.matches("application/webpub+json") == true) {
            return Try.success(Format.READIUM_WEBPUB_MANIFEST)
        }

        return Try.success(format)
    }
}

/** Sniffs a Readium Web Publication, protected or not by LCP. */
public object RpfSniffer : FormatSniffer {

    override fun sniffHints(
        format: Format?,
        hints: FormatHints
    ): Format? {
        if (
            hints.hasFileExtension("audiobook") ||
            hints.hasMediaType("application/audiobook+zip")
        ) {
            return format.orEmpty() + Format.READIUM_AUDIOBOOK
        }

        if (
            hints.hasFileExtension("divina") ||
            hints.hasMediaType("application/divina+zip")
        ) {
            return format.orEmpty() + Format.READIUM_COMICS
        }

        if (
            hints.hasFileExtension("webpub") ||
            hints.hasMediaType("application/webpub+zip")
        ) {
            return format.orEmpty() + Format.READIUM_WEBPUB
        }

        if (
            hints.hasFileExtension("lcpa") ||
            hints.hasMediaType("application/audiobook+lcp")
        ) {
            return format.orEmpty() + Format.READIUM_AUDIOBOOK + Trait.LCP_PROTECTED
        }
        if (
            hints.hasFileExtension("lcpdf") ||
            hints.hasMediaType("application/pdf+lcp")
        ) {
            return format.orEmpty() + Format.READIUM_PDF + Trait.LCP_PROTECTED
        }

        return format
    }

    override suspend fun sniffContainer(
        format: Format?,
        container: Container<Readable>
    ): Try<Format?, ReadError> {
        if (
            format != null && format != Format.ZIP
        ) {
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
            return Try.success(Format.READIUM_AUDIOBOOK)
        }
        if (manifest.conformsTo(Publication.Profile.DIVINA)) {
            return Try.success(Format.READIUM_COMICS)
        }
        if (manifest.conformsTo(Publication.Profile.PDF)) {
            return Try.success(Format.READIUM_PDF)
        }

        return Try.success(Format.READIUM_WEBPUB)
    }
}

/** Sniffs a W3C Web Publication Manifest. */
public object W3cWpubSniffer : FormatSniffer {

    override suspend fun sniffBlob(
        format: Format?,
        source: Readable
    ): Try<Format?, ReadError> {
        if (format != null && format != Format.JSON || !source.canReadWholeBlob()) {
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
            return Try.success(
                if (string.contains("https://www.w3.org/TR/audiobooks/")) {
                    Format(setOf(Trait.JSON, Trait.W3C_AUDIOBOOK_MANIFEST))
                } else {
                    Format(setOf(Trait.JSON, Trait.W3C_PUB_MANIFEST))
                }
            )
        }

        return Try.success(format)
    }
}

/**
 * Sniffs an EPUB publication.
 *
 * Reference: https://www.w3.org/publishing/epub3/epub-ocf.html#sec-zip-container-mime
 */
public object EpubSniffer : FormatSniffer {
    override fun sniffHints(
        format: Format?,
        hints: FormatHints
    ): Format? {
        if (
            hints.hasFileExtension("epub") ||
            hints.hasMediaType("application/epub+zip")
        ) {
            return format.orEmpty() + Format.EPUB
        }

        return format
    }

    override suspend fun sniffContainer(
        format: Format?,
        container: Container<Readable>
    ): Try<Format?, ReadError> {
        if (format != null && format != Format.ZIP) {
            return Try.success(format)
        }

        val mimetype = container[RelativeUrl("mimetype")!!]
            ?.readDecodeOrElse(
                decode = { it.decodeString() },
                recoverRead = { return Try.failure(it) },
                recoverDecode = { null }
            )?.trim()

        if (mimetype == "application/epub+zip") {
            return Try.success(format.orEmpty() + Trait.EPUB)
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
public object LpfSniffer : FormatSniffer {
    override fun sniffHints(
        format: Format?,
        hints: FormatHints
    ): Format? {
        if (
            hints.hasFileExtension("lpf") ||
            hints.hasMediaType("application/lpf+zip")
        ) {
            return format.orEmpty() + Trait.ZIP + Trait.LPF
        }

        return format
    }

    override suspend fun sniffContainer(
        format: Format?,
        container: Container<Readable>
    ): Try<Format?, ReadError> {
        if (format != null && format != Format.ZIP) {
            return Try.success(format)
        }

        if (RelativeUrl("index.html")!! in container) {
            return Try.success(format.orEmpty() + Trait.LPF)
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
                    return Try.success(
                        if (manifest.contains("https://www.w3.org/TR/audiobooks/")) {
                            format.orEmpty() + Trait.LPF + Trait.AUDIOBOOK
                        } else {
                            format.orEmpty() + Trait.LPF
                        }
                    )
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
public object RarSniffer : FormatSniffer {

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
            return format.orEmpty() + Format.RAR
        }

        return format
    }
}

/**
 * Sniffs a ZIP archive.
 */
public object ZipSniffer : FormatSniffer {

    override fun sniffHints(
        format: Format?,
        hints: FormatHints
    ): Format? {
        if (hints.hasMediaType("application/zip") ||
            hints.hasFileExtension("zip")
        ) {
            return format.orEmpty() + Format.ZIP
        }

        return format
    }
}

/**
 * Sniffs a simple Archive-based publication format, like Comic Book Archive or Zipped Audio Book.
 *
 * Reference: https://wiki.mobileread.com/wiki/CBR_and_CBZ
 */
public object ArchiveSniffer : FormatSniffer {

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

        if (container.entries.isEmpty()) {
            return Try.success(format)
        }

        if (archiveContainsOnlyExtensions(cbzExtensions)) {
            return Try.success(format.orEmpty() + Trait.COMICS)
        }

        if (archiveContainsOnlyExtensions(zabExtensions)) {
            return Try.success(format.orEmpty() + Trait.AUDIOBOOK)
        }

        return Try.success(format)
    }
}

/**
 * Sniffs a PDF document.
 *
 * Reference: https://www.loc.gov/preservation/digital/formats/fdd/fdd000123.shtml
 */
public object PdfSniffer : FormatSniffer {
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
        if (format != null) {
            return Try.success(format)
        }

        source.read(0L until 5L)
            .getOrElse { return Try.failure(it) }
            .let { tryOrNull { it.toString(Charsets.UTF_8) } }
            .takeIf { it == "%PDF-" }
            ?.let { return Try.success(Format.PDF) }

        return Try.success(format)
    }
}

/** Sniffs a JSON document. */
public object JsonSniffer : FormatSniffer {
    override fun sniffHints(
        format: Format?,
        hints: FormatHints
    ): Format? {
        if (hints.hasFileExtension("json") ||
            hints.hasMediaType("application/json")
        ) {
            return Format.JSON
        }

        if (hints.hasMediaType("application/problem+json")) {
            return Format.JSON + Trait.JSON_PROBLEM_DETAILS
        }

        return format
    }

    override suspend fun sniffBlob(
        format: Format?,
        source: Readable
    ): Try<Format?, ReadError> {
        if (format != null || !source.canReadWholeBlob()) {
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
public object AdeptSniffer : FormatSniffer {

    override suspend fun sniffContainer(
        format: Format?,
        container: Container<Readable>
    ): Try<Format?, ReadError> {
        if (format != Format.EPUB) {
            return Try.success(format)
        }

        container[Url("META-INF/encryption.xml")!!]
            ?.readDecodeOrElse(
                decode = { it.decodeXml() },
                recover = { return Try.failure(it) }
            )
            ?.get("EncryptedData", EpubEncryption.ENC)
            ?.flatMap { it.get("KeyInfo", EpubEncryption.SIG) }
            ?.flatMap { it.get("resource", "http://ns.adobe.com/adept") }
            ?.takeIf { it.isNotEmpty() }
            ?.let { return Try.success(format + Trait.ADEPT_PROTECTED) }

        container[Url("META-INF/rights.xml")!!]
            ?.readDecodeOrElse(
                decode = { it.decodeXml() },
                recover = { null }
            )
            ?.takeIf { it.namespace == "http://ns.adobe.com/adept" }
            ?.let { return Try.success(format + Trait.ADEPT_PROTECTED) }

        return Try.success(format)
    }
}

/**
 * Sniffs LCP protected packages.
 */
public object LcpSniffer : FormatSniffer {

    override suspend fun sniffContainer(
        format: Format?,
        container: Container<Readable>
    ): Try<Format?, ReadError> {
        return when {
            format?.conformsTo(Trait.RPF) == true -> {
                val isLcpProtected = RelativeUrl("license.lcpl")!! in container ||
                    hasLcpSchemeInManifest(container)
                        .getOrElse { return Try.failure(it) }

                Try.success(
                    if (isLcpProtected) {
                        format + Trait.LCP_PROTECTED
                    } else {
                        format
                    }
                )
            }

            format == Format.EPUB -> {
                val isLcpProtected = RelativeUrl("META-INF/license.lcpl")!! in container ||
                    hasLcpSchemeInEncryptionXml(container)
                        .getOrElse { return Try.failure(it) }

                Try.success(
                    if (isLcpProtected) {
                        format + Trait.LCP_PROTECTED
                    } else {
                        format
                    }
                )
            }
            else ->
                Try.success(format)
        }
    }

    private suspend fun hasLcpSchemeInManifest(container: Container<Readable>): Try<Boolean, ReadError> {
        val manifest = container[Url("manifest.json")!!]
            ?.readDecodeOrElse(
                decode = { it.decodeRwpm() },
                recover = { return Try.failure(it) }
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

private object EpubEncryption {
    const val ENC = "http://www.w3.org/2001/04/xmlenc#"
    const val SIG = "http://www.w3.org/2000/09/xmldsig#"
}
