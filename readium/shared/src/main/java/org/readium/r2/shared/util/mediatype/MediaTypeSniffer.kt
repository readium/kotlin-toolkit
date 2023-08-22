/*
 * Copyright 2023 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.shared.util.mediatype

import android.webkit.MimeTypeMap
import java.io.File
import java.net.URLConnection
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import org.readium.r2.shared.extensions.tryOrNull
import org.readium.r2.shared.publication.Link
import org.readium.r2.shared.publication.Manifest
import org.readium.r2.shared.publication.Publication

public interface MediaTypeSniffer {
    public fun sniffHints(hints: MediaTypeHints): MediaType? = null
    public suspend fun sniffContent(content: MediaTypeSnifferContent): MediaType? = null
}

/**
 * Sniffs an XHTML document.
 *
 * Must precede the HTML sniffer.
 */
public object XhtmlMediaTypeSniffer : MediaTypeSniffer {
    override fun sniffHints(hints: MediaTypeHints): MediaType? {
        if (
            hints.hasFileExtension("xht", "xhtml") ||
            hints.hasMediaType("application/xhtml+xml")
        ) {
            return MediaType.XHTML
        }

        return null
    }

    override suspend fun sniffContent(content: MediaTypeSnifferContent): MediaType? {
        if (content !is ResourceMediaTypeSnifferContent) {
            return null
        }

        content.contentAsXml()?.let {
            if (
                it.name.lowercase(Locale.ROOT) == "html" &&
                it.namespace.lowercase(Locale.ROOT).contains("xhtml")
            ) {
                return MediaType.XHTML
            }
        }
        return null
    }
}

/** Sniffs an HTML document. */
public object HtmlMediaTypeSniffer : MediaTypeSniffer {
    override fun sniffHints(hints: MediaTypeHints): MediaType? {
        if (
            hints.hasFileExtension("htm", "html") ||
            hints.hasMediaType("text/html")
        ) {
            return MediaType.HTML
        }

        return null
    }

    override suspend fun sniffContent(content: MediaTypeSnifferContent): MediaType? {
        if (content !is ResourceMediaTypeSnifferContent) {
            return null
        }

        // [contentAsXml] will fail if the HTML is not a proper XML document, hence the doctype check.
        if (
            content.contentAsXml()?.name?.lowercase(Locale.ROOT) == "html" ||
            content.contentAsString()?.trimStart()?.take(15)?.lowercase() == "<!doctype html>"
        ) {
            return MediaType.HTML
        }
        return null
    }
}

/** Sniffs an OPDS document. */
public object OpdsMediaTypeSniffer : MediaTypeSniffer {

    override fun sniffHints(hints: MediaTypeHints): MediaType? {
        // OPDS 1
        if (hints.hasMediaType("application/atom+xml;type=entry;profile=opds-catalog")) {
            return MediaType.OPDS1_ENTRY
        }
        if (hints.hasMediaType("application/atom+xml;profile=opds-catalog;kind=navigation")) {
            return MediaType.OPDS1_NAVIGATION_FEED
        }
        if (hints.hasMediaType("application/atom+xml;profile=opds-catalog;kind=acquisition")) {
            return MediaType.OPDS1_ACQUISITION_FEED
        }
        if (hints.hasMediaType("application/atom+xml;profile=opds-catalog")) {
            return MediaType.OPDS1
        }

        // OPDS 2
        if (hints.hasMediaType("application/opds+json")) {
            return MediaType.OPDS2
        }
        if (hints.hasMediaType("application/opds-publication+json")) {
            return MediaType.OPDS2_PUBLICATION
        }

        // OPDS Authentication Document.
        if (
            hints.hasMediaType("application/opds-authentication+json") ||
            hints.hasMediaType("application/vnd.opds.authentication.v1.0+json")
        ) {
            return MediaType.OPDS_AUTHENTICATION
        }

        return null
    }

    override suspend fun sniffContent(content: MediaTypeSnifferContent): MediaType? {
        if (content !is ResourceMediaTypeSnifferContent) {
            return null
        }

        // OPDS 1
        content.contentAsXml()?.let { xml ->
            if (xml.namespace == "http://www.w3.org/2005/Atom") {
                if (xml.name == "feed") {
                    return MediaType.OPDS1
                } else if (xml.name == "entry") {
                    return MediaType.OPDS1_ENTRY
                }
            }
        }

        // OPDS 2
        content.contentAsRwpm()?.let { rwpm ->
            if (rwpm.linkWithRel("self")?.mediaType?.matches("application/opds+json") == true) {
                return MediaType.OPDS2
            }

            /**
             * Finds the first [Link] having a relation matching the given [predicate].
             */
            fun List<Link>.firstWithRelMatching(predicate: (String) -> Boolean): Link? =
                firstOrNull { it.rels.any(predicate) }

            if (rwpm.links.firstWithRelMatching { it.startsWith("http://opds-spec.org/acquisition") } != null) {
                return MediaType.OPDS2_PUBLICATION
            }
        }

        // OPDS Authentication Document.
        if (content.containsJsonKeys("id", "title", "authentication")) {
            return MediaType.OPDS_AUTHENTICATION
        }

        return null
    }
}

/** Sniffs an LCP License Document. */
public object LcpLicenseMediaTypeSniffer : MediaTypeSniffer {
    override fun sniffHints(hints: MediaTypeHints): MediaType? {
        if (
            hints.hasFileExtension("lcpl") ||
            hints.hasMediaType("application/vnd.readium.lcp.license.v1.0+json")
        ) {
            return MediaType.LCP_LICENSE_DOCUMENT
        }

        return null
    }

    override suspend fun sniffContent(content: MediaTypeSnifferContent): MediaType? {
        if (content !is ResourceMediaTypeSnifferContent) {
            return null
        }

        if (content.containsJsonKeys("id", "issued", "provider", "encryption")) {
            return MediaType.LCP_LICENSE_DOCUMENT
        }
        return null
    }
}

/** Sniffs a bitmap image. */
public object BitmapMediaTypeSniffer : MediaTypeSniffer {
    override fun sniffHints(hints: MediaTypeHints): MediaType? {
        if (
            hints.hasFileExtension("avif") ||
            hints.hasMediaType("image/avif")
        ) {
            return MediaType.AVIF
        }
        if (
            hints.hasFileExtension("bmp", "dib") ||
            hints.hasMediaType("image/bmp", "image/x-bmp")
        ) {
            return MediaType.BMP
        }
        if (
            hints.hasFileExtension("gif") ||
            hints.hasMediaType("image/gif")
        ) {
            return MediaType.GIF
        }
        if (
            hints.hasFileExtension("jpg", "jpeg", "jpe", "jif", "jfif", "jfi") ||
            hints.hasMediaType("image/jpeg")
        ) {
            return MediaType.JPEG
        }
        if (
            hints.hasFileExtension("jxl") ||
            hints.hasMediaType("image/jxl")
        ) {
            return MediaType.JXL
        }
        if (
            hints.hasFileExtension("png") ||
            hints.hasMediaType("image/png")
        ) {
            return MediaType.PNG
        }
        if (
            hints.hasFileExtension("tiff", "tif") ||
            hints.hasMediaType("image/tiff", "image/tiff-fx")
        ) {
            return MediaType.TIFF
        }
        if (
            hints.hasFileExtension("webp") ||
            hints.hasMediaType("image/webp")
        ) {
            return MediaType.WEBP
        }
        return null
    }
}

/** Sniffs a Readium Web Manifest. */
public object WebPubManifestMediaTypeSniffer : MediaTypeSniffer {
    override fun sniffHints(hints: MediaTypeHints): MediaType? {
        if (hints.hasMediaType("application/audiobook+json")) {
            return MediaType.READIUM_AUDIOBOOK_MANIFEST
        }

        if (hints.hasMediaType("application/divina+json")) {
            return MediaType.DIVINA_MANIFEST
        }

        if (hints.hasMediaType("application/webpub+json")) {
            return MediaType.READIUM_WEBPUB_MANIFEST
        }

        return null
    }

    override suspend fun sniffContent(content: MediaTypeSnifferContent): MediaType? {
        if (content !is ResourceMediaTypeSnifferContent) {
            return null
        }

        val manifest: Manifest =
            content.contentAsRwpm() ?: return null

        if (manifest.conformsTo(Publication.Profile.AUDIOBOOK)) {
            return MediaType.READIUM_AUDIOBOOK_MANIFEST
        }

        if (manifest.conformsTo(Publication.Profile.DIVINA)) {
            return MediaType.DIVINA_MANIFEST
        }
        if (manifest.linkWithRel("self")?.mediaType?.matches("application/webpub+json") == true) {
            return MediaType.READIUM_WEBPUB_MANIFEST
        }

        return null
    }
}

/** Sniffs a Readium Web Publication, protected or not by LCP. */
public object WebPubMediaTypeSniffer : MediaTypeSniffer {
    override fun sniffHints(hints: MediaTypeHints): MediaType? {
        if (
            hints.hasFileExtension("audiobook") ||
            hints.hasMediaType("application/audiobook+zip")
        ) {
            return MediaType.READIUM_AUDIOBOOK
        }

        if (
            hints.hasFileExtension("divina") ||
            hints.hasMediaType("application/divina+zip")
        ) {
            return MediaType.DIVINA
        }

        if (
            hints.hasFileExtension("webpub") ||
            hints.hasMediaType("application/webpub+zip")
        ) {
            return MediaType.READIUM_WEBPUB
        }

        if (
            hints.hasFileExtension("lcpa") ||
            hints.hasMediaType("application/audiobook+lcp")
        ) {
            return MediaType.LCP_PROTECTED_AUDIOBOOK
        }
        if (
            hints.hasFileExtension("lcpdf") ||
            hints.hasMediaType("application/pdf+lcp")
        ) {
            return MediaType.LCP_PROTECTED_PDF
        }

        return null
    }

    override suspend fun sniffContent(content: MediaTypeSnifferContent): MediaType? {
        if (content !is ContainerMediaTypeSnifferContent) {
            return null
        }

        // Reads a RWPM from a manifest.json archive entry.
        val manifest: Manifest? =
            try {
                content.read("manifest.json")
                    ?.let {
                        Manifest.fromJSON(JSONObject(String(it)))
                    }
            } catch (e: Exception) {
                null
            }

        if (manifest != null) {
            val isLcpProtected = content.contains("/license.lcpl")

            if (manifest.conformsTo(Publication.Profile.AUDIOBOOK)) {
                return if (isLcpProtected) {
                    MediaType.LCP_PROTECTED_AUDIOBOOK
                } else {
                    MediaType.READIUM_AUDIOBOOK
                }
            }
            if (manifest.conformsTo(Publication.Profile.DIVINA)) {
                return MediaType.DIVINA
            }
            if (isLcpProtected && manifest.conformsTo(Publication.Profile.PDF)) {
                return MediaType.LCP_PROTECTED_PDF
            }
            if (manifest.linkWithRel("self")?.mediaType?.matches("application/webpub+json") == true) {
                return MediaType.READIUM_WEBPUB
            }
        }

        return null
    }
}

/** Sniffs a W3C Web Publication Manifest. */
public object W3cWpubMediaTypeSniffer : MediaTypeSniffer {
    override suspend fun sniffContent(content: MediaTypeSnifferContent): MediaType? {
        if (content !is ResourceMediaTypeSnifferContent) {
            return null
        }

        // Somehow, [JSONObject] can't access JSON-LD keys such as `@content`.
        val string = content.contentAsString() ?: ""
        if (
            string.contains("@context") &&
            string.contains("https://www.w3.org/ns/wp-context")
        ) {
            return MediaType.W3C_WPUB_MANIFEST
        }

        return null
    }
}

/**
 * Sniffs an EPUB publication.
 *
 * Reference: https://www.w3.org/publishing/epub3/epub-ocf.html#sec-zip-container-mime
 */
public object EpubMediaTypeSniffer : MediaTypeSniffer {
    override fun sniffHints(hints: MediaTypeHints): MediaType? {
        if (
            hints.hasFileExtension("epub") ||
            hints.hasMediaType("application/epub+zip")
        ) {
            return MediaType.EPUB
        }

        return null
    }

    override suspend fun sniffContent(content: MediaTypeSnifferContent): MediaType? {
        if (content !is ContainerMediaTypeSnifferContent) {
            return null
        }

        val mimetype = content.read("mimetype")
            ?.let { String(it, charset = Charsets.US_ASCII).trim() }
        if (mimetype == "application/epub+zip") {
            return MediaType.EPUB
        }

        return null
    }
}

/**
 * Sniffs a Lightweight Packaging Format (LPF).
 *
 * References:
 *  - https://www.w3.org/TR/lpf/
 *  - https://www.w3.org/TR/pub-manifest/
 */
public object LpfMediaTypeSniffer : MediaTypeSniffer {
    override fun sniffHints(hints: MediaTypeHints): MediaType? {
        if (
            hints.hasFileExtension("lpf") ||
            hints.hasMediaType("application/lpf+zip")
        ) {
            return MediaType.LPF
        }

        return null
    }

    override suspend fun sniffContent(content: MediaTypeSnifferContent): MediaType? {
        if (content !is ContainerMediaTypeSnifferContent) {
            return null
        }

        if (content.contains("/index.html")) {
            return MediaType.LPF
        }

        // Somehow, [JSONObject] can't access JSON-LD keys such as `@content`.
        content.read("publication.json")
            ?.let { String(it) }
            ?.let { manifest ->
                if (
                    manifest.contains("@context") &&
                    manifest.contains("https://www.w3.org/ns/pub-context")
                ) {
                    return MediaType.LPF
                }
            }

        return null
    }
}

/**
 * Sniffs a simple Archive-based format, like Comic Book Archive or Zipped Audio Book.
 *
 * Reference: https://wiki.mobileread.com/wiki/CBR_and_CBZ
 */
public object ArchiveMediaTypeSniffer : MediaTypeSniffer {

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

    override fun sniffHints(hints: MediaTypeHints): MediaType? {
        if (
            hints.hasFileExtension("cbz") ||
            hints.hasMediaType(
                "application/vnd.comicbook+zip",
                "application/x-cbz",
                "application/x-cbr"
            )
        ) {
            return MediaType.CBZ
        }
        if (hints.hasFileExtension("zab")) {
            return MediaType.ZAB
        }

        return null
    }

    override suspend fun sniffContent(content: MediaTypeSnifferContent): MediaType? {
        if (content !is ContainerMediaTypeSnifferContent) {
            return null
        }

        fun isIgnored(file: File): Boolean =
            file.name.startsWith(".") || file.name == "Thumbs.db"

        suspend fun archiveContainsOnlyExtensions(fileExtensions: List<String>): Boolean =
            content.entries()?.all { path ->
                val file = File(path)
                isIgnored(file) || fileExtensions.contains(file.extension.lowercase(Locale.ROOT))
            } ?: false

        if (archiveContainsOnlyExtensions(cbzExtensions)) {
            return MediaType.CBZ
        }
        if (archiveContainsOnlyExtensions(zabExtensions)) {
            return MediaType.ZAB
        }

        return null
    }
}

/**
 * Sniffs a PDF document.
 *
 * Reference: https://www.loc.gov/preservation/digital/formats/fdd/fdd000123.shtml
 */
public object PdfMediaTypeSniffer : MediaTypeSniffer {
    override fun sniffHints(hints: MediaTypeHints): MediaType? {
        if (
            hints.hasFileExtension("pdf") ||
            hints.hasMediaType("application/pdf")
        ) {
            return MediaType.PDF
        }

        return null
    }

    override suspend fun sniffContent(content: MediaTypeSnifferContent): MediaType? {
        if (content !is ResourceMediaTypeSnifferContent) {
            return null
        }

        if (content.read(0L until 5L)?.toString(Charsets.UTF_8) == "%PDF-") {
            return MediaType.PDF
        }

        return null
    }
}

/** Sniffs a JSON document. */
public object JsonMediaTypeSniffer : MediaTypeSniffer {
    override fun sniffHints(hints: MediaTypeHints): MediaType? {
        if (hints.hasMediaType("application/problem+json")) {
            return MediaType.JSON_PROBLEM_DETAILS
        }

        return null
    }

    override suspend fun sniffContent(content: MediaTypeSnifferContent): MediaType? {
        if (content !is ResourceMediaTypeSnifferContent) {
            return null
        }

        if (content.contentAsJson() != null) {
            return MediaType.JSON
        }
        return null
    }
}

/**
 * Sniffs the system-wide registered media types using [MimeTypeMap] and
 * [URLConnection.guessContentTypeFromStream].
 */
public object SystemMediaTypeSniffer : MediaTypeSniffer {

    private val mimetypes = tryOrNull { MimeTypeMap.getSingleton() }

    override fun sniffHints(hints: MediaTypeHints): MediaType? {
        for (mediaType in hints.mediaTypes) {
            return sniffType(mediaType.toString()) ?: continue
        }

        for (extension in hints.fileExtensions) {
            return sniffExtension(extension) ?: continue
        }

        return null
    }

    override suspend fun sniffContent(content: MediaTypeSnifferContent): MediaType? {
        if (content !is ResourceMediaTypeSnifferContent) {
            return null
        }

        return withContext(Dispatchers.IO) {
            content.contentAsStream()
                .let { URLConnection.guessContentTypeFromStream(it) }
                ?.let { sniffType(it) }
        }
    }

    private fun sniffType(type: String): MediaType? {
        val extension = mimetypes?.getExtensionFromMimeType(type)
            ?: return null
        val preferredType = mimetypes.getMimeTypeFromExtension(extension)
            ?: return null
        return MediaType(preferredType)
    }

    private fun sniffExtension(extension: String): MediaType? =
        mimetypes?.getMimeTypeFromExtension(extension)
            ?.let { MediaType(it) }
}
