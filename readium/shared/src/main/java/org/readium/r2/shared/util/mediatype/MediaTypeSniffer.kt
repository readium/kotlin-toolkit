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

public fun interface MediaTypeSniffer {
    public suspend fun sniff(context: MediaTypeSnifferContext): MediaType?
}

public open class CompositeMediaTypeSniffer(
    private val sniffers: List<MediaTypeSniffer>
) : MediaTypeSniffer {

    override suspend fun sniff(context: MediaTypeSnifferContext): MediaType? =
        sniffers.firstNotNullOfOrNull { it.sniff(context) }
}

/**
 * The default sniffer provided by Readium 2 to resolve a [MediaType].
 */
public class DefaultMediaTypeSniffer : CompositeMediaTypeSniffer(MediaTypeSniffers.all)

/**
 * Default media type sniffers provided by Readium.
 */
public object MediaTypeSniffers {

    /**
     * Sniffs an XHTML document.
     *
     * Must precede the HTML sniffer.
     */
    public val xhtml: MediaTypeSniffer = MediaTypeSniffer { context ->
        if (
            context.hasFileExtension("xht", "xhtml") ||
            context.hasMediaType("application/xhtml+xml")
        ) {
            return@MediaTypeSniffer MediaType.XHTML
        }

        if (context !is ContentMediaTypeSnifferContext) {
            return@MediaTypeSniffer null
        }

        context.contentAsXml()?.let {
            if (
                it.name.lowercase(Locale.ROOT) == "html" &&
                it.namespace.lowercase(Locale.ROOT).contains("xhtml")
            ) {
                return@MediaTypeSniffer MediaType.XHTML
            }
        }
        return@MediaTypeSniffer null
    }

    /** Sniffs an HTML document. */
    public val html: MediaTypeSniffer = MediaTypeSniffer { context ->
        if (
            context.hasFileExtension("htm", "html") ||
            context.hasMediaType("text/html")
        ) {
            return@MediaTypeSniffer MediaType.HTML
        }

        if (context !is ContentMediaTypeSnifferContext) {
            return@MediaTypeSniffer null
        }

        // [contentAsXml] will fail if the HTML is not a proper XML document, hence the doctype check.
        if (
            context.contentAsXml()?.name?.lowercase(Locale.ROOT) == "html" ||
            context.contentAsString()?.trimStart()?.take(15)?.lowercase() == "<!doctype html>"
        ) {
            return@MediaTypeSniffer MediaType.HTML
        }
        return@MediaTypeSniffer null
    }

    /** Sniffs an OPDS document. */
    public val opds: MediaTypeSniffer = MediaTypeSniffer { context ->
        // OPDS 1
        if (context.hasMediaType("application/atom+xml;type=entry;profile=opds-catalog")) {
            return@MediaTypeSniffer MediaType.OPDS1_ENTRY
        }
        if (context.hasMediaType("application/atom+xml;profile=opds-catalog")) {
            return@MediaTypeSniffer MediaType.OPDS1
        }

        // OPDS 2
        if (context.hasMediaType("application/opds+json")) {
            return@MediaTypeSniffer MediaType.OPDS2
        }
        if (context.hasMediaType("application/opds-publication+json")) {
            return@MediaTypeSniffer MediaType.OPDS2_PUBLICATION
        }

        // OPDS Authentication Document.
        if (context.hasMediaType("application/opds-authentication+json") || context.hasMediaType(
                "application/vnd.opds.authentication.v1.0+json"
            )
        ) {
            return@MediaTypeSniffer MediaType.OPDS_AUTHENTICATION
        }

        if (context !is ContentMediaTypeSnifferContext) {
            return@MediaTypeSniffer null
        }

        // OPDS 1
        context.contentAsXml()?.let { xml ->
            if (xml.namespace == "http://www.w3.org/2005/Atom") {
                if (xml.name == "feed") {
                    return@MediaTypeSniffer MediaType.OPDS1
                } else if (xml.name == "entry") {
                    return@MediaTypeSniffer MediaType.OPDS1_ENTRY
                }
            }
        }

        // OPDS 2
        context.contentAsRwpm()?.let { rwpm ->
            if (rwpm.linkWithRel("self")?.mediaType?.matches("application/opds+json") == true) {
                return@MediaTypeSniffer MediaType.OPDS2
            }

            /**
             * Finds the first [Link] having a relation matching the given [predicate].
             */
            fun List<Link>.firstWithRelMatching(predicate: (String) -> Boolean): Link? =
                firstOrNull { it.rels.any(predicate) }

            if (rwpm.links.firstWithRelMatching { it.startsWith("http://opds-spec.org/acquisition") } != null) {
                return@MediaTypeSniffer MediaType.OPDS2_PUBLICATION
            }
        }

        // OPDS Authentication Document.
        if (context.containsJsonKeys("id", "title", "authentication")) {
            return@MediaTypeSniffer MediaType.OPDS_AUTHENTICATION
        }

        return@MediaTypeSniffer null
    }

    /** Sniffs an LCP License Document. */
    public val lcpLicense: MediaTypeSniffer = MediaTypeSniffer { context ->
        if (context.hasFileExtension("lcpl") || context.hasMediaType(
                "application/vnd.readium.lcp.license.v1.0+json"
            )
        ) {
            return@MediaTypeSniffer MediaType.LCP_LICENSE_DOCUMENT
        }

        if (context !is ContentMediaTypeSnifferContext) {
            return@MediaTypeSniffer null
        }

        if (context.containsJsonKeys("id", "issued", "provider", "encryption")) {
            return@MediaTypeSniffer MediaType.LCP_LICENSE_DOCUMENT
        }
        return@MediaTypeSniffer null
    }

    /** Sniffs a bitmap image. */
    public val bitmap: MediaTypeSniffer = MediaTypeSniffer { context ->
        if (context.hasFileExtension("avif") || context.hasMediaType("image/avif")) {
            return@MediaTypeSniffer MediaType.AVIF
        }
        if (context.hasFileExtension("bmp", "dib") || context.hasMediaType(
                "image/bmp",
                "image/x-bmp"
            )
        ) {
            return@MediaTypeSniffer MediaType.BMP
        }
        if (context.hasFileExtension("gif") || context.hasMediaType("image/gif")) {
            return@MediaTypeSniffer MediaType.GIF
        }
        if (context.hasFileExtension("jpg", "jpeg", "jpe", "jif", "jfif", "jfi") || context.hasMediaType(
                "image/jpeg"
            )
        ) {
            return@MediaTypeSniffer MediaType.JPEG
        }
        if (context.hasFileExtension("jxl") || context.hasMediaType("image/jxl")) {
            return@MediaTypeSniffer MediaType.JXL
        }
        if (context.hasFileExtension("png") || context.hasMediaType("image/png")) {
            return@MediaTypeSniffer MediaType.PNG
        }
        if (context.hasFileExtension("tiff", "tif") || context.hasMediaType(
                "image/tiff",
                "image/tiff-fx"
            )
        ) {
            return@MediaTypeSniffer MediaType.TIFF
        }
        if (context.hasFileExtension("webp") || context.hasMediaType("image/webp")) {
            return@MediaTypeSniffer MediaType.WEBP
        }
        return@MediaTypeSniffer null
    }

    /** Sniffs a Readium Web Manifest. */
    public val webpubManifest: MediaTypeSniffer = MediaTypeSniffer { context ->
        if (context.hasMediaType("application/audiobook+json")) {
            return@MediaTypeSniffer MediaType.READIUM_AUDIOBOOK_MANIFEST
        }

        if (context.hasMediaType("application/divina+json")) {
            return@MediaTypeSniffer MediaType.DIVINA_MANIFEST
        }

        if (context.hasMediaType("application/webpub+json")) {
            return@MediaTypeSniffer MediaType.READIUM_WEBPUB_MANIFEST
        }

        if (context !is ContentMediaTypeSnifferContext) {
            return@MediaTypeSniffer null
        }

        val manifest: Manifest =
            context.contentAsRwpm() ?: return@MediaTypeSniffer null

        if (manifest.conformsTo(Publication.Profile.AUDIOBOOK)) {
            return@MediaTypeSniffer MediaType.READIUM_AUDIOBOOK_MANIFEST
        }

        if (manifest.conformsTo(Publication.Profile.DIVINA)) {
            return@MediaTypeSniffer MediaType.DIVINA_MANIFEST
        }
        if (manifest.linkWithRel("self")?.mediaType?.matches("application/webpub+json") == true) {
            return@MediaTypeSniffer MediaType.READIUM_WEBPUB_MANIFEST
        }

        return@MediaTypeSniffer null
    }

    /** Sniffs a Readium Web Publication, protected or not by LCP. */
    public val webpub: MediaTypeSniffer = MediaTypeSniffer { context ->
        if (context.hasFileExtension("audiobook") || context.hasMediaType(
                "application/audiobook+zip"
            )
        ) {
            return@MediaTypeSniffer MediaType.READIUM_AUDIOBOOK
        }

        if (context.hasFileExtension("divina") || context.hasMediaType("application/divina+zip")) {
            return@MediaTypeSniffer MediaType.DIVINA
        }

        if (context.hasFileExtension("webpub") || context.hasMediaType("application/webpub+zip")) {
            return@MediaTypeSniffer MediaType.READIUM_WEBPUB
        }

        if (context.hasFileExtension("lcpa") || context.hasMediaType("application/audiobook+lcp")) {
            return@MediaTypeSniffer MediaType.LCP_PROTECTED_AUDIOBOOK
        }
        if (context.hasFileExtension("lcpdf") || context.hasMediaType("application/pdf+lcp")) {
            return@MediaTypeSniffer MediaType.LCP_PROTECTED_PDF
        }

        if (context !is ContainerMediaTypeSnifferContext) {
            return@MediaTypeSniffer null
        }

        // Reads a RWPM from a manifest.json archive entry.
        val manifest: Manifest? =
            try {
                context.read("manifest.json")
                    ?.let { Manifest.fromJSON(JSONObject(String(it))) }
            } catch (e: Exception) {
                null
            }

        if (manifest != null) {
            val isLcpProtected = context.contains("license.lcpl")

            if (manifest.conformsTo(Publication.Profile.AUDIOBOOK)) {
                return@MediaTypeSniffer if (isLcpProtected) MediaType.LCP_PROTECTED_AUDIOBOOK else MediaType.READIUM_AUDIOBOOK
            }
            if (manifest.conformsTo(Publication.Profile.DIVINA)) {
                return@MediaTypeSniffer MediaType.DIVINA
            }
            if (isLcpProtected && manifest.conformsTo(Publication.Profile.PDF)) {
                return@MediaTypeSniffer MediaType.LCP_PROTECTED_PDF
            }
            if (manifest.linkWithRel("self")?.mediaType?.matches("application/webpub+json") == true) {
                return@MediaTypeSniffer MediaType.READIUM_WEBPUB
            }
        }

        return@MediaTypeSniffer null
    }

    /** Sniffs a W3C Web Publication Manifest. */
    public val w3cWPUB: MediaTypeSniffer = MediaTypeSniffer { context ->
        if (context !is ContentMediaTypeSnifferContext) {
            return@MediaTypeSniffer null
        }

        // Somehow, [JSONObject] can't access JSON-LD keys such as `@context`.
        val content = context.contentAsString() ?: ""
        if (content.contains("@context") && content.contains("https://www.w3.org/ns/wp-context")) {
            return@MediaTypeSniffer MediaType.W3C_WPUB_MANIFEST
        }

        return@MediaTypeSniffer null
    }

    /**
     * Sniffs an EPUB publication.
     *
     * Reference: https://www.w3.org/publishing/epub3/epub-ocf.html#sec-zip-container-mime
     */
    public val epub: MediaTypeSniffer = MediaTypeSniffer { context ->
        if (context.hasFileExtension("epub") || context.hasMediaType("application/epub+zip")) {
            return@MediaTypeSniffer MediaType.EPUB
        }

        if (context !is ContainerMediaTypeSnifferContext) {
            return@MediaTypeSniffer null
        }

        val mimetype = context.read("mimetype")
            ?.let { String(it, charset = Charsets.US_ASCII).trim() }
        if (mimetype == "application/epub+zip") {
            return@MediaTypeSniffer MediaType.EPUB
        }

        return@MediaTypeSniffer null
    }

    /**
     * Sniffs a Lightweight Packaging Format (LPF).
     *
     * References:
     *  - https://www.w3.org/TR/lpf/
     *  - https://www.w3.org/TR/pub-manifest/
     */
    public val lpf: MediaTypeSniffer = MediaTypeSniffer { context ->
        if (context.hasFileExtension("lpf") || context.hasMediaType("application/lpf+zip")) {
            return@MediaTypeSniffer MediaType.LPF
        }

        if (context !is ContainerMediaTypeSnifferContext) {
            return@MediaTypeSniffer null
        }

        if (context.contains("index.html")) {
            return@MediaTypeSniffer MediaType.LPF
        }

        // Somehow, [JSONObject] can't access JSON-LD keys such as `@context`.
        context.read("publication.json")
            ?.let { String(it) }
            ?.let { manifest ->
                if (manifest.contains("@context") && manifest.contains(
                        "https://www.w3.org/ns/pub-context"
                    )
                ) {
                    return@MediaTypeSniffer MediaType.LPF
                }
            }

        return@MediaTypeSniffer null
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
     * Sniffs a simple Archive-based format, like Comic Book Archive or Zipped Audio Book.
     *
     * Reference: https://wiki.mobileread.com/wiki/CBR_and_CBZ
     */
    public val archive: MediaTypeSniffer = MediaTypeSniffer { context ->
        if (context.hasFileExtension("cbz") || context.hasMediaType(
                "application/vnd.comicbook+zip",
                "application/x-cbz",
                "application/x-cbr"
            )
        ) {
            return@MediaTypeSniffer MediaType.CBZ
        }
        if (context.hasFileExtension("zab")) {
            return@MediaTypeSniffer MediaType.ZAB
        }

        if (context !is ContainerMediaTypeSnifferContext) {
            return@MediaTypeSniffer null
        }

        fun isIgnored(file: File): Boolean =
            file.name.startsWith(".") || file.name == "Thumbs.db"

        suspend fun archiveContainsOnlyExtensions(fileExtensions: List<String>): Boolean =
            context.entries()?.all { path ->
                val file = File(path)
                isIgnored(file) || fileExtensions.contains(file.extension.lowercase(Locale.ROOT))
            } ?: false

        if (archiveContainsOnlyExtensions(CBZ_EXTENSIONS)) {
            return@MediaTypeSniffer MediaType.CBZ
        }
        if (archiveContainsOnlyExtensions(ZAB_EXTENSIONS)) {
            return@MediaTypeSniffer MediaType.ZAB
        }

        return@MediaTypeSniffer null
    }

    /**
     * Sniffs a PDF document.
     *
     * Reference: https://www.loc.gov/preservation/digital/formats/fdd/fdd000123.shtml
     */
    public val pdf: MediaTypeSniffer = MediaTypeSniffer { context ->
        if (context.hasFileExtension("pdf") || context.hasMediaType("application/pdf")) {
            return@MediaTypeSniffer MediaType.PDF
        }

        if (context !is ContentMediaTypeSnifferContext) {
            return@MediaTypeSniffer null
        }

        if (context.read(0L until 5L)?.toString(Charsets.UTF_8) == "%PDF-") {
            return@MediaTypeSniffer MediaType.PDF
        }

        return@MediaTypeSniffer null
    }

    /** Sniffs a JSON document. */
    public val json: MediaTypeSniffer = MediaTypeSniffer { context ->
        if (context.hasMediaType("application/problem+json")) {
            return@MediaTypeSniffer MediaType.JSON_PROBLEM_DETAILS
        }

        if (context !is ContentMediaTypeSnifferContext) {
            return@MediaTypeSniffer null
        }

        if (context.contentAsJson() != null) {
            return@MediaTypeSniffer MediaType.JSON
        }
        return@MediaTypeSniffer null
    }

    /** Sniffs an XML document. */
    public val xml: MediaTypeSniffer = MediaTypeSniffer { context ->
        if (context is ContentMediaTypeSnifferContext && context.contentAsXml() != null) {
            return@MediaTypeSniffer MediaType.XML
        }
        return@MediaTypeSniffer null
    }

    /** Sniffs a ZIP archive. */
    public val zip: MediaTypeSniffer = MediaTypeSniffer { context ->
        if (context.hasMediaType("application/zip") && context is ContainerMediaTypeSnifferContext) {
            return@MediaTypeSniffer MediaType.ZIP
        }
        return@MediaTypeSniffer null
    }

    /**
     * Sniffs the system-wide registered media types using [MimeTypeMap] and
     * [URLConnection.guessContentTypeFromStream].
     */
    public fun system(excluded: List<MediaType>): MediaTypeSniffer = MediaTypeSniffer { context ->
        val mimetypes = tryOrNull { MimeTypeMap.getSingleton() }
            ?: return@MediaTypeSniffer null

        fun sniffExtension(extension: String): MediaType? =
            mimetypes.getMimeTypeFromExtension(extension)
                ?.let { MediaType(it) }
                ?.takeUnless { it in excluded }

        fun sniffType(type: String): MediaType? {
            val extension = mimetypes.getExtensionFromMimeType(type)
                ?: return null
            val preferredType = mimetypes.getMimeTypeFromExtension(extension)
                ?: return null
            return MediaType(preferredType)
                .takeUnless { it in excluded }
        }

        for (mediaType in context.hints.mediaTypes) {
            return@MediaTypeSniffer sniffType(mediaType.toString()) ?: continue
        }

        for (extension in context.hints.fileExtensions) {
            return@MediaTypeSniffer sniffExtension(extension) ?: continue
        }

        if (context !is ContentMediaTypeSnifferContext) {
            return@MediaTypeSniffer null
        }

        return@MediaTypeSniffer withContext(Dispatchers.IO) {
            context.contentAsStream()
                .let { URLConnection.guessContentTypeFromStream(it) }
                ?.let { sniffType(it) }
        }
    }

    /**
     * The default sniffers provided by Readium 2 for all known formats.
     * The sniffers order is important, because some formats are subsets of other formats.
     */
    public val all: List<MediaTypeSniffer> = listOf(
        xhtml,
        html,
        opds,
        lcpLicense,
        bitmap,
        webpubManifest,
        webpub,
        w3cWPUB,
        epub,
        lpf,
        archive,
        pdf,
        json,
        xml,
        zip,
        // Note: We exclude JSON, XML or ZIP formats otherwise they will be detected during the
        // light sniffing step and bypass the RWPM or EPUB heavy sniffing.
        system(excluded = listOf(MediaType.JSON, MediaType.XML, MediaType.ZIP))
    )
}
