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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import org.readium.r2.shared.publication.Publication
import java.io.File
import java.net.URLConnection
import java.util.*
import java.util.zip.ZipEntry

/**
 * Determines if the provided content matches a known format.
 *
 * @param context Holds the file metadata and cached content, which are shared among the sniffers.
 */
typealias FormatSniffer = suspend (context: FormatSnifferContext) -> Format?

/**
 * Default format sniffers provided by Readium.
 */
object FormatSniffers {

    /**
     * The default sniffers provided by Readium 2 to resolve a [Format].
     * The sniffers order is important, because some formats are subsets of other formats.
     */
    val all: List<FormatSniffer> = listOf(
        ::html, ::opds, ::lcpLicense, ::bitmap,
        ::webpub, ::w3cWPUB, ::epub, ::lpf, ::zip, ::pdf
    )

    /** Sniffs an HTML document. */
    suspend fun html(context: FormatSnifferContext): Format? {
        if (context.hasFileExtension("htm", "html", "xht", "xhtml") || context.hasMediaType("text/html", "application/xhtml+xml")) {
            return Format.HTML
        }
        // [contentAsXml] will fail if the HTML is not a proper XML document, hence the doctype check.
        if (context.contentAsXml()?.name?.toLowerCase(Locale.ROOT) == "html" || context.contentAsString()?.trimStart()?.startsWith("<!DOCTYPE html>") == true) {
            return Format.HTML
        }
        return null
    }

    /** Sniffs an OPDS document. */
    suspend fun opds(context: FormatSnifferContext): Format? {
        // OPDS 1
        if (context.hasMediaType("application/atom+xml;type=entry;profile=opds-catalog")) {
            return Format.OPDS1_ENTRY
        }
        if (context.hasMediaType("application/atom+xml;profile=opds-catalog")) {
            return Format.OPDS1_FEED
        }
        context.contentAsXml()?.let { xml ->
            if (xml.namespace == "http://www.w3.org/2005/Atom") {
                if (xml.name == "feed") {
                    return Format.OPDS1_FEED
                } else if (xml.name == "entry") {
                    return Format.OPDS1_ENTRY
                }
            }
        }

        // OPDS 2
        if (context.hasMediaType("application/opds+json")) {
            return Format.OPDS2_FEED
        }
        if (context.hasMediaType("application/opds-publication+json")) {
            return Format.OPDS2_PUBLICATION
        }
        context.contentAsRwpm()?.let { rwpm ->
            if (rwpm.linkWithRel("self")?.mediaType?.matches("application/opds+json") == true) {
                return Format.OPDS2_FEED
            }
            if (rwpm.linkWithRelMatching { it.startsWith("http://opds-spec.org/acquisition") } != null) {
                return Format.OPDS2_PUBLICATION
            }
        }

        // OPDS Authentication Document.
        if (context.hasMediaType("application/opds-authentication+json") || context.hasMediaType("application/vnd.opds.authentication.v1.0+json")) {
            return Format.OPDS_AUTHENTICATION
        }
        if (context.containsJsonKeys("id", "title", "authentication")) {
            return Format.OPDS_AUTHENTICATION
        }

        return null
    }

    /** Sniffs an LCP License Document. */
    suspend fun lcpLicense(context: FormatSnifferContext): Format? {
        if (context.hasFileExtension("lcpl") || context.hasMediaType("application/vnd.readium.lcp.license.v1.0+json")) {
            return Format.LCP_LICENSE
        }
        if (context.containsJsonKeys("id", "issued", "provider", "encryption")) {
            return Format.LCP_LICENSE
        }
        return null
    }

    /** Sniffs a bitmap image. */
    suspend fun bitmap(context: FormatSnifferContext): Format? {
        if (context.hasFileExtension("bmp", "dib") || context.hasMediaType("image/bmp", "image/x-bmp")) {
            return Format.BMP
        }
        if (context.hasFileExtension("gif") || context.hasMediaType("image/gif")) {
            return Format.GIF
        }
        if (context.hasFileExtension("jpg", "jpeg", "jpe", "jif", "jfif", "jfi") || context.hasMediaType("image/jpeg")) {
            return Format.JPEG
        }
        if (context.hasFileExtension("png") || context.hasMediaType("image/png")) {
            return Format.PNG
        }
        if (context.hasFileExtension("tiff", "tif") || context.hasMediaType("image/tiff", "image/tiff-fx")) {
            return Format.TIFF
        }
        if (context.hasFileExtension("webp") || context.hasMediaType("image/webp")) {
            return Format.WEBP
        }
        return null
    }

    /** Sniffs a Readium Web Publication, protected or not by LCP. */
    suspend fun webpub(context: FormatSnifferContext): Format? {
        if (context.hasFileExtension("audiobook") || context.hasMediaType("application/audiobook+zip")) {
            return Format.READIUM_AUDIOBOOK
        }
        if (context.hasMediaType("application/audiobook+json")) {
            return Format.READIUM_AUDIOBOOK_MANIFEST
        }

        if (context.hasFileExtension("divina") || context.hasMediaType("application/divina+zip")) {
            return Format.DIVINA
        }
        if (context.hasMediaType("application/divina+json")) {
            return Format.DIVINA_MANIFEST
        }

        if (context.hasFileExtension("webpub") || context.hasMediaType("application/webpub+zip")) {
            return Format.READIUM_WEBPUB
        }
        if (context.hasMediaType("application/webpub+json")) {
            return Format.READIUM_WEBPUB_MANIFEST
        }

        if (context.hasFileExtension("lcpa") || context.hasMediaType("application/audiobook+lcp")) {
            return Format.LCP_PROTECTED_AUDIOBOOK
        }
        if (context.hasFileExtension("lcpdf") || context.hasMediaType("application/pdf+lcp")) {
            return Format.LCP_PROTECTED_PDF
        }

        // Reads a RWPM, either from a manifest.json file, or from a manifest.json ZIP entry, if
        // the file is a ZIP archive.
        var isManifest = true
        val publication: Publication? =
            try {
                // manifest.json
                context.contentAsRwpm() ?:
                // ZIP package
                context.readZipEntryAt("manifest.json")
                    ?.let { Publication.fromJSON(JSONObject(String(it))) }
                    ?.also { isManifest = false }
            } catch (e: Exception) {
                null
            }

        if (publication != null) {
            val isLcpProtected = context.containsZipEntryAt("license.lcpl")

            if (publication.metadata.type == "http://schema.org/Audiobook" || publication.allReadingOrderIsAudio) {
                return if (isManifest) Format.READIUM_AUDIOBOOK_MANIFEST
                else (if (isLcpProtected) Format.LCP_PROTECTED_AUDIOBOOK else Format.READIUM_AUDIOBOOK)
            }
            if (publication.allReadingOrderIsBitmap) {
                return if (isManifest) Format.DIVINA_MANIFEST else Format.DIVINA
            }
            if (isLcpProtected && publication.allReadingOrderMatchesAnyOf(MediaType.PDF)) {
                return Format.LCP_PROTECTED_PDF
            }
            if (publication.linkWithRel("self")?.mediaType?.matches("application/webpub+json") == true) {
                return if (isManifest) Format.READIUM_WEBPUB_MANIFEST else Format.READIUM_WEBPUB
            }
        }

        return null
    }

    /** Sniffs a W3C Web Publication Manifest. */
    suspend fun w3cWPUB(context: FormatSnifferContext): Format? {
        // Somehow, [JSONObject] can't access JSON-LD keys such as `@context`.
        val content = context.contentAsString() ?: ""
        if (content.contains("@context") && content.contains("https://www.w3.org/ns/wp-context")) {
            return Format.W3C_WPUB_MANIFEST
        }

        return null
    }

    /**
     * Sniffs an EPUB publication.
     *
     * Reference: https://www.w3.org/publishing/epub3/epub-ocf.html#sec-zip-container-mime
     */
    suspend fun epub(context: FormatSnifferContext): Format? {
        if (context.hasFileExtension("epub") || context.hasMediaType("application/epub+zip")) {
            return Format.EPUB
        }

        val mimetype = context.readZipEntryAt("mimetype")
            ?.let { String(it, charset = Charsets.US_ASCII).trim() }
        if (mimetype == "application/epub+zip") {
            return Format.EPUB
        }

        return null
    }

    /**
     * Sniffs a Lightweight Packaging Format (LPF).
     *
     * References:
     *  - https://www.w3.org/TR/lpf/
     *  - https://www.w3.org/TR/pub-manifest/
     */
    suspend fun lpf(context: FormatSnifferContext): Format? {
        if (context.hasFileExtension("lpf") || context.hasMediaType("application/lpf+zip")) {
            return Format.LPF
        }
        if (context.containsZipEntryAt("index.html")) {
            return Format.LPF
        }

        // Somehow, [JSONObject] can't access JSON-LD keys such as `@context`.
        context.readZipEntryAt("publication.json")
            ?.let { String(it) }
            ?.let { manifest ->
                if (manifest.contains("@context") && manifest.contains("https://www.w3.org/ns/pub-context")) {
                    return Format.LPF
                }
            }

        return null
    }

    /**
     * Authorized extensions for resources in a CBZ archive.
     * Reference: https://wiki.mobileread.com/wiki/CBR_and_CBZ
     */
    private val CBZ_EXTENSIONS = listOf(
        // bitmap
        "bmp", "dib", "gif", "jif", "jfi", "jfif", "jpg", "jpeg", "png", "tif", "tiff", "webp",
        // metadata
        "acbf", "xml"
    )

    /**
     * Authorized extensions for resources in a ZAB archive (Zipped Audio Book).
     */
    private val ZAB_EXTENSIONS = listOf(
        // audio
        "aac", "aiff", "alac", "flac", "m4a", "m4b", "mp3", "ogg", "oga", "mogg", "opus", "wav", "webm",
        // playlist
        "asx", "bio", "m3u", "m3u8", "pla", "pls", "smil", "vlc", "wpl", "xspf", "zpl"
    )

    /**
     * Sniffs a simple ZIP-based format, like Comic Book Archive or Zipped Audio Book.
     *
     * Reference: https://wiki.mobileread.com/wiki/CBR_and_CBZ
     */
    suspend fun zip(context: FormatSnifferContext): Format? {
        if (context.hasFileExtension("cbz") || context.hasMediaType("application/vnd.comicbook+zip", "application/x-cbz", "application/x-cbr")) {
            return Format.CBZ
        }
        if (context.hasFileExtension("zab")) {
            return Format.ZAB
        }

        if (context.contentAsZip() != null) {
            fun isIgnored(entry: ZipEntry, file: File): Boolean =
                entry.isDirectory || file.name.startsWith(".") || file.name == "Thumbs.db"

            suspend fun zipContainsOnlyExtensions(fileExtensions: List<String>): Boolean =
                context.zipEntriesAllSatisfy { entry ->
                    val file = File(entry.name)
                    isIgnored(entry, file) || fileExtensions.contains(file.extension.toLowerCase(Locale.ROOT))
                }

            if (zipContainsOnlyExtensions(CBZ_EXTENSIONS)) {
                return Format.CBZ
            }
            if (zipContainsOnlyExtensions(ZAB_EXTENSIONS)) {
                return Format.ZAB
            }
        }

        return null
    }

    /**
     * Sniffs a PDF document.
     *
     * Reference: https://www.loc.gov/preservation/digital/formats/fdd/fdd000123.shtml
     */
    suspend fun pdf(context: FormatSnifferContext): Format? {
        if (context.hasFileExtension("pdf") || context.hasMediaType("application/pdf")) {
            return Format.PDF
        }
        if (context.readFileSignature(length = 5) == "%PDF-") {
            return Format.PDF
        }

        return null
    }

    /**
     * Sniffs the system-wide registered media types using [MimeTypeMap] and
     * [URLConnection.guessContentTypeFromStream].
     */
    suspend fun system(context: FormatSnifferContext): Format? {
        val mimetypes = MimeTypeMap.getSingleton()

        fun createFormat(mediaType: MediaType, extension: String) =
            Format(name = extension.toUpperCase(Locale.ROOT), mediaType = mediaType, fileExtension = extension)

        fun sniffExtension(extension: String): Format? {
            val mediaType = mimetypes.getMimeTypeFromExtension(extension)?.let { MediaType.parse(it) }
                ?: return null
            val preferredExtension = mimetypes.getExtensionFromMimeType(mediaType.toString())
                ?: return null
            return createFormat(mediaType, preferredExtension)
        }

        fun sniffMediaType(mediaType: MediaType): Format? {
            val extension = mimetypes.getExtensionFromMimeType(mediaType.toString())
                ?: return null
            val preferredMediaType = mimetypes.getMimeTypeFromExtension(extension)?.let { MediaType.parse(it) }
                ?: return null
            return createFormat(preferredMediaType, extension)
        }

        for (mediaType in context.mediaTypes) {
            return sniffMediaType(mediaType) ?: continue
        }

        for (extension in context.fileExtensions) {
            return sniffExtension(extension) ?: continue
        }

        return withContext(Dispatchers.IO) {
            context.stream()
                ?.let { URLConnection.guessContentTypeFromStream(it) }
                ?.let { MediaType.parse(it) }
                ?.let {
                    sniffMediaType(it)
                }
        }
    }

}
