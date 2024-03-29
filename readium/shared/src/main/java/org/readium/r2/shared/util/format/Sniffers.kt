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
import org.readium.r2.shared.util.FileExtension
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
import org.readium.r2.shared.util.getOrDefault
import org.readium.r2.shared.util.getOrElse
import org.readium.r2.shared.util.mediatype.MediaType

/** Sniffs an HTML or XHTML document. */
public object HtmlSniffer : FormatSniffer {
    override fun sniffHints(
        hints: FormatHints
    ): Format? {
        if (
            hints.hasFileExtension("htm", "html") ||
            hints.hasMediaType("text/html")
        ) {
            return htmlFormat
        }

        if (
            hints.hasFileExtension("xht", "xhtml") ||
            hints.hasMediaType("application/xhtml+xml")
        ) {
            return xhtmlFormat
        }

        return null
    }

    override suspend fun sniffBlob(
        format: Format,
        source: Readable
    ): Try<Format, ReadError> {
        if (format.hasMoreThan(XmlSpecification) || !source.canReadWholeBlob()) {
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
                        xhtmlFormat
                    } else {
                        htmlFormat
                    }
                )
            }

        source.readDecodeOrElse(
            decode = { it.decodeString() },
            recoverRead = { return Try.failure(it) },
            recoverDecode = { null }
        )
            ?.takeIf { it.trimStart().take(15).lowercase() == "<!doctype html>" }
            ?.let { return Try.success(htmlFormat) }

        return Try.success(format)
    }

    private val htmlFormat = Format(
        specification = FormatSpecification(HtmlSpecification),
        fileExtension = FileExtension("html"),
        mediaType = MediaType.HTML
    )

    private val xhtmlFormat = Format(
        specification = FormatSpecification(XmlSpecification, HtmlSpecification),
        fileExtension = FileExtension("xhtml"),
        mediaType = MediaType.XHTML
    )
}

/** Sniffs an OPDS1 document. */
public object Opds1Sniffer : FormatSniffer {

    override fun sniffHints(
        hints: FormatHints
    ): Format? {
        // OPDS 1
        if (hints.hasMediaType("application/atom+xml;type=entry;profile=opds-catalog")) {
            return opds1EntryFormat
        }
        if (hints.hasMediaType("application/atom+xml;profile=opds-catalog;kind=navigation")) {
            return opds1NavigationFeedFormat
        }
        if (hints.hasMediaType("application/atom+xml;profile=opds-catalog;kind=acquisition")) {
            return opds1AcquisitionFeedFormat
        }
        if (hints.hasMediaType("application/atom+xml;profile=opds-catalog")) {
            return opds1CatalogFormat
        }

        return null
    }

    override suspend fun sniffBlob(
        format: Format,
        source: Readable
    ): Try<Format, ReadError> {
        if (format.hasMoreThan(XmlSpecification) || !source.canReadWholeBlob()) {
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
                    return Try.success(opds1CatalogFormat)
                } else if (xml.name == "entry") {
                    return Try.success(opds1EntryFormat)
                }
            }

        return Try.success(format)
    }

    private val opds1CatalogFormat = Format(
        specification = FormatSpecification(XmlSpecification, Opds1CatalogSpecification),
        mediaType = MediaType.OPDS1,
        fileExtension = FileExtension("xml")
    )

    private val opds1NavigationFeedFormat = Format(
        specification = FormatSpecification(XmlSpecification, Opds1CatalogSpecification),
        mediaType = MediaType.OPDS1_NAVIGATION_FEED,
        fileExtension = FileExtension("xml")
    )

    private val opds1AcquisitionFeedFormat = Format(
        specification = FormatSpecification(XmlSpecification, Opds1CatalogSpecification),
        mediaType = MediaType.OPDS1_ACQUISITION_FEED,
        fileExtension = FileExtension("xml")
    )

    private val opds1EntryFormat = Format(
        specification = FormatSpecification(XmlSpecification, Opds1EntrySpecification),
        mediaType = MediaType.OPDS1_ENTRY,
        fileExtension = FileExtension("xml")
    )
}

/**
 * Sniffs an OPDS 2 document.
 */
public object Opds2Sniffer : FormatSniffer {

    override fun sniffHints(hints: FormatHints): Format? {
        // OPDS 2
        if (hints.hasMediaType("application/opds+json")) {
            return opds2CatalogFormat
        }
        if (hints.hasMediaType("application/opds-publication+json")) {
            return opds2PublicationFormat
        }

        // OPDS Authentication Document.
        if (
            hints.hasMediaType("application/opds-authentication+json") ||
            hints.hasMediaType("application/vnd.opds.authentication.v1.0+json")
        ) {
            return opdsAuthenticationFormat
        }

        return null
    }

    override suspend fun sniffBlob(
        format: Format,
        source: Readable
    ): Try<Format, ReadError> {
        if (format.hasMoreThan(JsonSpecification) || !source.canReadWholeBlob()) {
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
                    return Try.success(opds2CatalogFormat)
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
                    return Try.success(opds2PublicationFormat)
                }
            }

        // OPDS Authentication Document.
        source.containsJsonKeys("id", "title", "authentication")
            .getOrElse { return Try.failure(it) }
            .takeIf { it }
            ?.let { return Try.success(opdsAuthenticationFormat) }

        return Try.success(format)
    }

    private val opdsAuthenticationFormat = Format(
        specification = FormatSpecification(JsonSpecification, OpdsAuthenticationSpecification),
        mediaType = MediaType.OPDS_AUTHENTICATION,
        fileExtension = FileExtension("json")
    )

    private val opds2CatalogFormat = Format(
        specification = FormatSpecification(JsonSpecification, Opds2CatalogSpecification),
        mediaType = MediaType.OPDS2,
        fileExtension = FileExtension("json")
    )

    private val opds2PublicationFormat = Format(
        specification = FormatSpecification(JsonSpecification, Opds2PublicationSpecification),
        mediaType = MediaType.OPDS2_PUBLICATION,
        fileExtension = FileExtension("json")
    )
}

/** Sniffs an LCP License Document. */
public object LcpLicenseSniffer : FormatSniffer {
    override fun sniffHints(
        hints: FormatHints
    ): Format? {
        if (
            hints.hasFileExtension("lcpl") ||
            hints.hasMediaType("application/vnd.readium.lcp.license.v1.0+json")
        ) {
            return lcplFormat
        }

        return null
    }

    override suspend fun sniffBlob(
        format: Format,
        source: Readable
    ): Try<Format, ReadError> {
        if (
            format.hasMoreThan(JsonSpecification) ||
            !source.canReadWholeBlob()
        ) {
            return Try.success(format)
        }

        source.containsJsonKeys("id", "issued", "provider", "encryption")
            .getOrElse { return Try.failure(it) }
            .takeIf { it }
            ?.let { return Try.success(lcplFormat) }

        return Try.success(format)
    }

    private val lcplFormat = Format(
        specification = FormatSpecification(JsonSpecification, LcpLicenseSpecification),
        mediaType = MediaType.LCP_LICENSE_DOCUMENT,
        fileExtension = FileExtension("lcpl")
    )
}

/** Sniffs a bitmap image. */
public object BitmapSniffer : FormatSniffer {

    override fun sniffHints(
        hints: FormatHints
    ): Format? {
        if (
            hints.hasFileExtension("avif") ||
            hints.hasMediaType("image/avif")
        ) {
            return Format(
                specification = FormatSpecification(AvifSpecification),
                mediaType = MediaType.AVIF,
                fileExtension = FileExtension("avif")
            )
        }
        if (
            hints.hasFileExtension("bmp", "dib") ||
            hints.hasMediaType("image/bmp", "image/x-bmp")
        ) {
            return Format(
                specification = FormatSpecification(BmpSpecification),
                mediaType = MediaType.BMP,
                fileExtension = FileExtension("bmp")
            )
        }
        if (
            hints.hasFileExtension("gif") ||
            hints.hasMediaType("image/gif")
        ) {
            return Format(
                specification = FormatSpecification(GifSpecification),
                mediaType = MediaType.GIF,
                fileExtension = FileExtension("gif")
            )
        }
        if (
            hints.hasFileExtension("jpg", "jpeg", "jpe", "jif", "jfif", "jfi") ||
            hints.hasMediaType("image/jpeg")
        ) {
            return Format(
                specification = FormatSpecification(JpegSpecification),
                mediaType = MediaType.JPEG,
                fileExtension = FileExtension("jpg")
            )
        }
        if (
            hints.hasFileExtension("jxl") ||
            hints.hasMediaType("image/jxl")
        ) {
            return Format(
                specification = FormatSpecification(JxlSpecification),
                mediaType = MediaType.JXL,
                fileExtension = FileExtension("jxl")
            )
        }
        if (
            hints.hasFileExtension("png") ||
            hints.hasMediaType("image/png")
        ) {
            return Format(
                specification = FormatSpecification(PngSpecification),
                mediaType = MediaType.PNG,
                fileExtension = FileExtension("png")
            )
        }
        if (
            hints.hasFileExtension("tiff", "tif") ||
            hints.hasMediaType("image/tiff", "image/tiff-fx")
        ) {
            return Format(
                specification = FormatSpecification(TiffSpecification),
                mediaType = MediaType.TIFF,
                fileExtension = FileExtension("tiff")
            )
        }
        if (
            hints.hasFileExtension("webp") ||
            hints.hasMediaType("image/webp")
        ) {
            return Format(
                specification = FormatSpecification(WebpSpecification),
                mediaType = MediaType.WEBP,
                fileExtension = FileExtension("webp")
            )
        }

        return null
    }
}

/** Sniffs audio files. */
public object AudioSniffer : FormatSniffer {
    override fun sniffHints(hints: FormatHints): Format? {
        if (
            hints.hasFileExtension("aac")
        ) {
            return Format(
                specification = FormatSpecification(AacSpecification),
                mediaType = MediaType.AAC,
                fileExtension = FileExtension("aac")
            )
        }

        if (
            hints.hasFileExtension("aiff")
        ) {
            return Format(
                specification = FormatSpecification(AiffSpecification),
                mediaType = MediaType.AIFF,
                fileExtension = FileExtension("aiff")
            )
        }

        if (
            hints.hasFileExtension("flac")
        ) {
            return Format(
                specification = FormatSpecification(FlacSpecification),
                mediaType = MediaType.FLAC,
                fileExtension = FileExtension("flac")
            )
        }

        if (
            hints.hasFileExtension("m4a", "m4b", "alac")
        ) {
            return Format(
                specification = FormatSpecification(Mp4Specification),
                mediaType = MediaType.MP4,
                fileExtension = FileExtension("m4a")
            )
        }

        if (
            hints.hasFileExtension("mp3")
        ) {
            return Format(
                specification = FormatSpecification(Mp3Specification),
                mediaType = MediaType.MP3,
                fileExtension = FileExtension("mp3")
            )
        }

        if (
            hints.hasFileExtension("ogg", "oga")
        ) {
            return Format(
                specification = FormatSpecification(OggSpecification),
                mediaType = MediaType.OGG,
                fileExtension = FileExtension("oga")
            )
        }

        if (
            hints.hasFileExtension("opus")
        ) {
            return Format(
                specification = FormatSpecification(OpusSpecification),
                mediaType = MediaType.OPUS,
                fileExtension = FileExtension("opus")
            )
        }

        if (
            hints.hasFileExtension("wav")
        ) {
            return Format(
                specification = FormatSpecification(WavSpecification),
                mediaType = MediaType.WAV,
                fileExtension = FileExtension("wav")
            )
        }

        if (
            hints.hasFileExtension("webm")
        ) {
            return Format(
                specification = FormatSpecification(WebmSpecification),
                mediaType = MediaType.WEBM_AUDIO,
                fileExtension = FileExtension("webm")
            )
        }

        return null
    }
}

/** Sniffs a Readium Web Manifest. */
public object RwpmSniffer : FormatSniffer {
    override fun sniffHints(
        hints: FormatHints
    ): Format? {
        if (hints.hasMediaType("application/audiobook+json")) {
            return rwpmAudioFormat
        }

        if (hints.hasMediaType("application/divina+json")) {
            return rwpmDivinaFormat
        }

        if (hints.hasMediaType("application/webpub+json")) {
            return rwpmFormat
        }

        return null
    }

    public override suspend fun sniffBlob(
        format: Format,
        source: Readable
    ): Try<Format, ReadError> {
        if (
            format.hasMoreThan(JsonSpecification) ||
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
            return Try.success(rwpmAudioFormat)
        }

        if (manifest.conformsTo(Publication.Profile.DIVINA)) {
            return Try.success(rwpmDivinaFormat)
        }
        if (manifest.linkWithRel("self")?.mediaType?.matches("application/webpub+json") == true) {
            return Try.success(rwpmFormat)
        }

        return Try.success(format)
    }

    private val rwpmFormat = Format(
        specification = FormatSpecification(JsonSpecification, RwpmSpecification),
        mediaType = MediaType.READIUM_WEBPUB_MANIFEST,
        fileExtension = FileExtension("json")
    )

    private val rwpmAudioFormat = Format(
        specification = FormatSpecification(JsonSpecification, RwpmSpecification),
        mediaType = MediaType.READIUM_AUDIOBOOK_MANIFEST,
        fileExtension = FileExtension("json")
    )

    private val rwpmDivinaFormat = Format(
        specification = FormatSpecification(JsonSpecification, RwpmSpecification),
        mediaType = MediaType.DIVINA_MANIFEST,
        fileExtension = FileExtension("json")
    )
}

/** Sniffs a Readium Web Publication, protected or not by LCP. */
public object RpfSniffer : FormatSniffer {

    override fun sniffHints(
        hints: FormatHints
    ): Format? {
        if (
            hints.hasFileExtension("audiobook") ||
            hints.hasMediaType("application/audiobook+zip")
        ) {
            return rpfAudioFormat
        }

        if (
            hints.hasFileExtension("divina") ||
            hints.hasMediaType("application/divina+zip")
        ) {
            return rpfDivinaFormat
        }

        if (
            hints.hasFileExtension("webpub") ||
            hints.hasMediaType("application/webpub+zip")
        ) {
            return rpfFormat
        }

        if (
            hints.hasFileExtension("lcpa") ||
            hints.hasMediaType("application/audiobook+lcp")
        ) {
            return lcpaFormat
        }
        if (
            hints.hasFileExtension("lcpdf") ||
            hints.hasMediaType("application/pdf+lcp")
        ) {
            return lcpdfFormat
        }

        return null
    }

    override suspend fun sniffContainer(
        format: Format,
        container: Container<Readable>
    ): Try<Format, ReadError> {
        if (
            format.hasMoreThan(ZipSpecification, RpfSpecification, LcpSpecification)
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

        val isLcpProtected = RelativeUrl("license.lcpl")!! in container ||
            hasLcpSchemeInManifest(manifest)

        val newFormat = when {
            manifest.conformsTo(Publication.Profile.AUDIOBOOK) -> {
                if (isLcpProtected) {
                    lcpaFormat
                } else {
                    rpfAudioFormat
                }
            }
            manifest.conformsTo(Publication.Profile.DIVINA) -> {
                if (isLcpProtected) {
                    rpfDivinaFormat.addSpecifications(LcpSpecification)
                } else {
                    rpfDivinaFormat
                }
            }
            manifest.conformsTo(Publication.Profile.PDF) -> {
                if (isLcpProtected) {
                    lcpdfFormat
                } else {
                    rpfFormat
                }
            }
            else ->
                if (isLcpProtected) {
                    rpfFormat.addSpecifications(LcpSpecification)
                } else {
                    rpfFormat
                }
        }

        return Try.success(newFormat)
    }

    private fun hasLcpSchemeInManifest(manifest: Manifest): Boolean = manifest
        .readingOrder
        .any { it.properties.encryption?.scheme == "http://readium.org/2014/01/lcp" }

    private val rpfFormat = Format(
        specification = FormatSpecification(ZipSpecification, RpfSpecification),
        mediaType = MediaType.READIUM_WEBPUB,
        fileExtension = FileExtension("webpub")
    )

    private val rpfAudioFormat = Format(
        specification = FormatSpecification(ZipSpecification, RpfSpecification),
        mediaType = MediaType.READIUM_AUDIOBOOK,
        fileExtension = FileExtension("audiobook")
    )

    private val rpfDivinaFormat = Format(
        specification = FormatSpecification(ZipSpecification, RpfSpecification),
        mediaType = MediaType.DIVINA,
        fileExtension = FileExtension("divina")
    )

    private val lcpaFormat = Format(
        specification = FormatSpecification(ZipSpecification, RpfSpecification, LcpSpecification),
        mediaType = MediaType.LCP_PROTECTED_AUDIOBOOK,
        fileExtension = FileExtension("lcpa")
    )

    private val lcpdfFormat = Format(
        specification = FormatSpecification(ZipSpecification, RpfSpecification, LcpSpecification),
        mediaType = MediaType.LCP_PROTECTED_PDF,
        fileExtension = FileExtension("lcpdf")
    )
}

/** Sniffs a W3C Web Publication Manifest. */
public object W3cWpubSniffer : FormatSniffer {

    override suspend fun sniffBlob(
        format: Format,
        source: Readable
    ): Try<Format, ReadError> {
        if (format.hasMoreThan(JsonSpecification) || !source.canReadWholeBlob()) {
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
                Format(
                    specification = FormatSpecification(
                        JsonSpecification,
                        W3cPubManifestSpecification
                    ),
                    mediaType = MediaType.W3C_WPUB_MANIFEST,
                    fileExtension = FileExtension("json")
                )
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
        hints: FormatHints
    ): Format? {
        if (
            hints.hasFileExtension("epub") ||
            hints.hasMediaType("application/epub+zip")
        ) {
            return epubFormatSpecification
        }

        return null
    }

    override suspend fun sniffContainer(
        format: Format,
        container: Container<Readable>
    ): Try<Format, ReadError> {
        if (format.hasMoreThan(ZipSpecification)) {
            return Try.success(format)
        }

        val mimetype = container[RelativeUrl("mimetype")!!]
            ?.readDecodeOrElse(
                decode = { it.decodeString() },
                recoverRead = { return Try.failure(it) },
                recoverDecode = { null }
            )?.trim()

        if (mimetype == "application/epub+zip") {
            return Try.success(epubFormatSpecification)
        }

        return Try.success(format)
    }

    private val epubFormatSpecification = Format(
        specification = FormatSpecification(ZipSpecification, EpubSpecification),
        mediaType = MediaType.EPUB,
        fileExtension = FileExtension("epub")
    )
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
        hints: FormatHints
    ): Format? {
        if (
            hints.hasFileExtension("lpf") ||
            hints.hasMediaType("application/lpf+zip")
        ) {
            return lpfFormat
        }

        return null
    }

    override suspend fun sniffContainer(
        format: Format,
        container: Container<Readable>
    ): Try<Format, ReadError> {
        if (format.hasMoreThan(ZipSpecification)) {
            return Try.success(format)
        }

        if (RelativeUrl("index.html")!! in container) {
            return Try.success(lpfFormat)
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
                    return Try.success(lpfFormat)
                }
            }

        return Try.success(format)
    }

    private val lpfFormat = Format(
        specification = FormatSpecification(ZipSpecification, LpfSpecification),
        mediaType = MediaType.LPF,
        fileExtension = FileExtension("lpf")
    )
}

/**
 * Sniffs a RAR archive.
 *
 * At the moment, only hints are supported.
 */
public object RarSniffer : FormatSniffer {

    override fun sniffHints(
        hints: FormatHints
    ): Format? {
        if (
            hints.hasFileExtension("rar") ||
            hints.hasMediaType("application/vnd.rar") ||
            hints.hasMediaType("application/x-rar") ||
            hints.hasMediaType("application/x-rar-compressed")
        ) {
            return Format(
                specification = FormatSpecification(RarSpecification),
                mediaType = MediaType.RAR,
                fileExtension = FileExtension("rar")
            )
        }

        return null
    }
}

/**
 * Sniffs a ZIP archive.
 */
public object ZipSniffer : FormatSniffer {

    override fun sniffHints(
        hints: FormatHints
    ): Format? {
        if (hints.hasMediaType("application/zip") ||
            hints.hasFileExtension("zip")
        ) {
            return Format(
                specification = FormatSpecification(ZipSpecification),
                mediaType = MediaType.ZIP,
                fileExtension = FileExtension("zip")
            )
        }

        return null
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
        hints: FormatHints
    ): Format? {
        if (
            hints.hasFileExtension("cbz") ||
            hints.hasMediaType(
                "application/vnd.comicbook+zip",
                "application/x-cbz"
            )
        ) {
            return Format(
                specification = FormatSpecification(ZipSpecification, InformalComicSpecification),
                mediaType = MediaType.CBZ,
                fileExtension = FileExtension("cbz")
            )
        }

        if (
            hints.hasFileExtension("cbr") ||
            hints.hasMediaType("application/vnd.comicbook-rar") ||
            hints.hasMediaType("application/x-cbr")
        ) {
            return Format(
                specification = FormatSpecification(RarSpecification, InformalComicSpecification),
                mediaType = MediaType.CBR,
                fileExtension = FileExtension("cbr")
            )
        }

        if (hints.hasFileExtension("zab")) {
            return Format(
                specification = FormatSpecification(
                    ZipSpecification,
                    InformalAudiobookSpecification
                ),
                mediaType = MediaType.ZAB,
                fileExtension = FileExtension("zab")
            )
        }

        return null
    }

    override suspend fun sniffContainer(
        format: Format,
        container: Container<Readable>
    ): Try<Format, ReadError> {
        if (format.hasMoreThan(ZipSpecification, RarSpecification)) {
            return Try.success(format)
        }

        fun isIgnored(url: Url): Boolean =
            url.filename?.startsWith(".") == true || url.filename == "Thumbs.db"

        fun archiveContainsOnlyExtensions(fileExtensions: List<String>): Boolean =
            container.all { url ->
                isIgnored(url) || url.extension?.value?.let {
                    fileExtensions.contains(
                        it.lowercase(Locale.ROOT)
                    )
                } == true
            }

        if (container.entries.isEmpty()) {
            return Try.success(format)
        }

        if (archiveContainsOnlyExtensions(cbzExtensions)) {
            val mediaType =
                if (format.conformsTo(RarSpecification)) {
                    MediaType.CBR
                } else {
                    MediaType.CBZ
                }

            val extension =
                if (format.conformsTo(RarSpecification)) {
                    FileExtension("cbr")
                } else {
                    FileExtension("cbz")
                }

            return Try.success(
                Format(
                    specification = format.specification + InformalComicSpecification,
                    mediaType = mediaType,
                    fileExtension = extension
                )
            )
        }

        if (archiveContainsOnlyExtensions(zabExtensions)) {
            val mediaType =
                if (format.conformsTo(ZipSpecification)) {
                    MediaType.ZAB
                } else {
                    format.mediaType
                }

            val extension =
                if (format.conformsTo(ZipSpecification)) {
                    FileExtension("zab")
                } else {
                    format.fileExtension
                }

            return Try.success(
                Format(
                    specification = format.specification + InformalAudiobookSpecification,
                    mediaType = mediaType,
                    fileExtension = extension
                )
            )
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
        hints: FormatHints
    ): Format? {
        if (
            hints.hasFileExtension("pdf") ||
            hints.hasMediaType("application/pdf")
        ) {
            return pdfFormat
        }

        return null
    }

    override suspend fun sniffBlob(
        format: Format,
        source: Readable
    ): Try<Format, ReadError> {
        if (format.hasAnySpecification()) {
            return Try.success(format)
        }

        source.read(0L until 5L)
            .getOrElse { return Try.failure(it) }
            .let { tryOrNull { it.toString(Charsets.UTF_8) } }
            .takeIf { it == "%PDF-" }
            ?.let { return Try.success(pdfFormat) }

        return Try.success(format)
    }

    private val pdfFormat = Format(
        specification = FormatSpecification(PdfSpecification),
        mediaType = MediaType.PDF,
        fileExtension = FileExtension("pdf")
    )
}

/** Sniffs a JSON document. */
public object JsonSniffer : FormatSniffer {
    override fun sniffHints(
        hints: FormatHints
    ): Format? {
        if (hints.hasFileExtension("json") ||
            hints.hasMediaType("application/json")
        ) {
            return Format(
                specification = FormatSpecification(JsonSpecification),
                mediaType = MediaType.JSON,
                fileExtension = FileExtension("json")
            )
        }

        if (hints.hasMediaType("application/problem+json")) {
            return Format(
                specification = FormatSpecification(JsonSpecification, ProblemDetailsSpecification),
                mediaType = MediaType.JSON_PROBLEM_DETAILS,
                fileExtension = FileExtension("json")
            )
        }

        return null
    }

    override suspend fun sniffBlob(
        format: Format,
        source: Readable
    ): Try<Format, ReadError> {
        if (format.hasMoreThan() || !source.canReadWholeBlob()) {
            return Try.success(format)
        }

        source.readDecodeOrElse(
            decode = { it.decodeJson() },
            recoverRead = { return Try.failure(it) },
            recoverDecode = { null }
        )?.let {
            return Try.success(
                Format(
                    specification = FormatSpecification(JsonSpecification),
                    mediaType = MediaType.JSON,
                    fileExtension = FileExtension("json")
                )
            )
        }

        return Try.success(format)
    }
}

/**
 * Sniffs LCP and Adept protection on EPUBs.
 */
public object EpubDrmSniffer : FormatSniffer {

    override suspend fun sniffContainer(
        format: Format,
        container: Container<Readable>
    ): Try<Format, ReadError> {
        if (
            !format.conformsTo(EpubSpecification) ||
            format.conformsTo(AdeptSpecification) ||
            format.conformsTo(LcpSpecification)
        ) {
            return Try.success(format)
        }

        if (RelativeUrl("META-INF/license.lcpl")!! in container) {
            return Try.success(format.addSpecifications(LcpSpecification))
        }

        val encryptionDocument = container[Url("META-INF/encryption.xml")!!]
            ?.readDecodeOrElse(
                decode = { it.decodeXml() },
                recover = { return Try.failure(it) }
            )

        encryptionDocument
            ?.get("EncryptedData", EpubEncryption.ENC)
            ?.flatMap { it.get("KeyInfo", EpubEncryption.SIG) }
            ?.flatMap { it.get("RetrievalMethod", EpubEncryption.SIG) }
            ?.any { it.getAttr("URI") == "license.lcpl#/encryption/content_key" }
            ?.takeIf { it }
            ?.let { return Try.success(format.addSpecifications(LcpSpecification)) }

        encryptionDocument
            ?.get("EncryptedData", EpubEncryption.ENC)
            ?.flatMap { it.get("KeyInfo", EpubEncryption.SIG) }
            ?.flatMap { it.get("resource", "http://ns.adobe.com/adept") }
            ?.takeIf { it.isNotEmpty() }
            ?.let { return Try.success(format.addSpecifications(AdeptSpecification)) }

        container[Url("META-INF/rights.xml")!!]
            ?.readDecodeOrElse(
                decode = { it.decodeXml() },
                recover = { null }
            )
            ?.takeIf { it.namespace == "http://ns.adobe.com/adept" }
            ?.let { return Try.success(format.addSpecifications(AdeptSpecification)) }

        return Try.success(format)
    }
}

/**
 * Sniffs CSS.
 */

public object CssSniffer : FormatSniffer {
    override fun sniffHints(hints: FormatHints): Format? {
        if (hints.hasFileExtension("css") ||
            hints.hasMediaType("text/css")
        ) {
            return Format(
                specification = FormatSpecification(CssSpecification),
                mediaType = MediaType.CSS,
                fileExtension = FileExtension("css")
            )
        }

        return null
    }
}

/**
 * Sniffs JavaScript.
 */

public object JavaScriptSniffer : FormatSniffer {
    override fun sniffHints(hints: FormatHints): Format? {
        if (hints.hasFileExtension("js") ||
            hints.hasMediaType("text/javascript") ||
            hints.hasMediaType("application/javascript")
        ) {
            return Format(
                specification = FormatSpecification(JavaScriptSpecification),
                mediaType = MediaType.JAVASCRIPT,
                fileExtension = FileExtension("js")
            )
        }

        return null
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

private fun Format.addSpecifications(
    vararg specifications: Specification
): Format =
    copy(specification = specification + specifications.toSet())

private fun Format.hasAnySpecification() =
    specification.specifications.isNotEmpty()

private fun Format.hasMoreThan(vararg specifications: Specification) =
    !specifications.toSet().containsAll(specification.specifications)

private object EpubEncryption {
    const val ENC = "http://www.w3.org/2001/04/xmlenc#"
    const val SIG = "http://www.w3.org/2000/09/xmldsig#"
}
