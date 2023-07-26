/*
 * Module: r2-shared-kotlin
 * Developers: Mickaël Menu
 *
 * Copyright (c) 2020. Readium Foundation. All rights reserved.
 * Use of this source code is governed by a BSD-style license which is detailed in the
 * LICENSE file present in the project repository where this source code is maintained.
 */

package org.readium.r2.shared.util.mediatype

import android.content.ContentResolver
import android.net.Uri
import java.io.File
import java.nio.charset.Charset
import java.util.*
import org.readium.r2.shared.extensions.tryOrNull

/**
 * Represents a document format, identified by a unique RFC 6838 media type.
 *
 * [MediaType] handles:
 *  - components parsing – eg. type, subtype and parameters,
 *  - media types comparison.
 *
 * Comparing media types is more complicated than it looks, since they can contain parameters,
 * such as `charset=utf-8`. We can't ignore them because some formats use parameters in their
 * media type, for example `application/atom+xml;profile=opds-catalog` for an OPDS 1 catalog.
 *
 * Specification: https://tools.ietf.org/html/rfc6838
 *
 * @param string String representation for this media type.
 * @param name A human readable name identifying the media type, which may be presented to the user.
 * @param fileExtension The default file extension to use for this media type.
 */
public class MediaType(
    string: String,
    public val name: String? = null,
    public val fileExtension: String? = null
) {

    /** The type component, e.g. `application` in `application/epub+zip`. */
    public val type: String

    /** The subtype component, e.g. `epub+zip` in `application/epub+zip`. */
    public val subtype: String

    /** The parameters in the media type, such as `charset=utf-8`. */
    public val parameters: Map<String, String>

    init {
        if (string.isEmpty()) {
            throw IllegalArgumentException("Invalid media type: $string")
        }

        // Grammar: https://tools.ietf.org/html/rfc2045#section-5.1
        val components = string.split(";")
            .map { it.trim() }
        val types = components[0].split("/")
        if (types.size != 2) {
            throw IllegalArgumentException("Invalid media type: $string")
        }

        // > Both top-level type and subtype names are case-insensitive.
        this.type = types[0].lowercase(Locale.ROOT)
        this.subtype = types[1].lowercase(Locale.ROOT)

        // > Parameter names are case-insensitive and no meaning is attached to the order in which
        // > they appear.
        val parameters = components.drop(1)
            .map { it.split("=") }
            .filter { it.size == 2 }
            .associate { Pair(it[0].lowercase(Locale.ROOT), it[1]) }
            .toMutableMap()

        // For now, we only support case-insensitive `charset`.
        //
        // > Parameter values might or might not be case-sensitive, depending on the semantics of
        // > the parameter name.
        // > https://tools.ietf.org/html/rfc2616#section-3.7
        //
        // > The character set names may be up to 40 characters taken from the printable characters
        // > of US-ASCII.  However, no distinction is made between use of upper and lower case
        // > letters.
        // > https://www.iana.org/assignments/character-sets/character-sets.xhtml
        parameters["charset"]?.let {
            parameters["charset"] =
                (try { Charset.forName(it).name() } catch (e: Exception) { it })
                    .uppercase(Locale.ROOT)
        }

        this.parameters = parameters
    }

    /**
     * Structured syntax suffix, e.g. `+zip` in `application/epub+zip`.
     *
     * Gives a hint on the underlying structure of this media type.
     * See. https://tools.ietf.org/html/rfc6838#section-4.2.8
     */
    public val structuredSyntaxSuffix: String? get() {
        val parts = subtype.split("+")
        return if (parts.size > 1) "+${parts.last()}" else null
    }

    /**
     * Encoding as declared in the `charset` parameter, if there's any.
     */
    public val charset: Charset? get() =
        parameters["charset"]?.let { Charset.forName(it) }

    /**
     * Returns the canonical version of this media type, if it is known.
     *
     * This is useful to find the name and file extension of a known media type, or to get the
     * canonical media type from an alias. For example, `application/x-cbz` is an alias of the
     * canonical `application/vnd.comicbook+zip`.
     *
     * Non-significant parameters are also discarded.
     */
    @Deprecated("Use MediaTypeRetriever instead", replaceWith = ReplaceWith("mediaTypeRetriever.canonicalMediaType()"), level = DeprecationLevel.ERROR)
    public fun canonicalMediaType(): MediaType {
        TODO()
    }

    /** The string representation of this media type. */
    override fun toString(): String {
        var params = parameters.map { "${it.key}=${it.value}" }
            .sorted()
            .joinToString(separator = ";")
        if (params.isNotEmpty()) {
            params = ";$params"
        }
        return "$type/$subtype$params"
    }

    /**
     * Returns whether two media types are equal, checking the type, subtype and parameters.
     * Parameters order is ignored.
     *
     * WARNING: Strict media type comparisons can be a source of bug, if parameters are present.
     * `text/html` != `text/html;charset=utf-8` with strict equality comparison, which is most
     * likely not the desired result. Instead, you can use [matches] to check if any of the media
     * types is a parameterized version of the other one.
     */
    override fun equals(other: Any?): Boolean {
        return toString() == (other as? MediaType)?.toString()
    }

    override fun hashCode(): Int {
        var result = type.hashCode()
        result = 31 * result + subtype.hashCode()
        result = 31 * result + parameters.hashCode()
        return result
    }

    /**
     * Returns whether the given [other] media type is included in this media type.
     *
     * For example, `text/html` contains `text/html;charset=utf-8`.
     *
     * - [other] must match the parameters in the [parameters] property, but extra parameters
     *    are ignored.
     * - Order of parameters is ignored.
     * - Wildcards are supported, meaning that `image/*` contains `image/png` and `*/*` contains
     *   everything.
     */
    public fun contains(other: MediaType?): Boolean {
        if (other == null || (type != "*" && type != other.type) || (subtype != "*" && subtype != other.subtype)) {
            return false
        }
        val paramsSet = parameters.map { "${it.key}=${it.value}" }.toSet()
        val otherParamsSet = other.parameters.map { "${it.key}=${it.value}" }.toSet()
        return otherParamsSet.containsAll(paramsSet)
    }

    /**
     * Returns whether the given [other] media type is included in this media type.
     */
    public fun contains(other: String?): Boolean {
        val mediaType = other?.let { parse(it) }
            ?: return false

        return contains(mediaType)
    }

    /**
     * Returns whether this media type and `other` are the same, ignoring parameters that are not
     * in both media types.
     *
     * For example, `text/html` matches `text/html;charset=utf-8`, but `text/html;charset=ascii`
     * doesn't. This is basically like `contains`, but working in both direction.
     */
    public fun matches(other: MediaType?): Boolean =
        contains(other) || (other?.contains(this) == true)

    /**
     * Returns whether this media type and `other` are the same, ignoring parameters that are not
     * in both media types.
     */
    public fun matches(other: String?): Boolean =
        matches(other?.let { parse(it) })

    /**
     * Returns whether this media type matches any of the `others` media types.
     */
    public fun matchesAny(vararg others: MediaType?): Boolean =
        others.any { matches(it) }

    /**
     * Returns whether this media type matches any of the `others` media types.
     */
    public fun matchesAny(vararg others: String?): Boolean =
        others.any { matches(it) }

    /** Returns whether this media type is structured as a ZIP archive. */
    public val isZip: Boolean get() =
        matchesAny(ZIP, LCP_PROTECTED_AUDIOBOOK, LCP_PROTECTED_PDF) ||
            structuredSyntaxSuffix == "+zip"

    /** Returns whether this media type is structured as a JSON file. */
    public val isJson: Boolean get() =
        matches(JSON) || structuredSyntaxSuffix == "+json"

    /** Returns whether this media type is of an OPDS feed. */
    public val isOpds: Boolean get() =
        matchesAny(OPDS1, OPDS1_ENTRY, OPDS2, OPDS2_PUBLICATION, OPDS_AUTHENTICATION)

    /** Returns whether this media type is of an HTML document. */
    public val isHtml: Boolean get() =
        matchesAny(HTML, XHTML)

    /** Returns whether this media type is of a bitmap image, so excluding vectorial formats. */
    public val isBitmap: Boolean get() =
        matchesAny(BMP, GIF, JPEG, PNG, TIFF, WEBP)

    /** Returns whether this media type is of an audio clip. */
    public val isAudio: Boolean get() =
        type == "audio"

    /** Returns whether this media type is of a video clip. */
    public val isVideo: Boolean get() =
        type == "video"

    /** Returns whether this media type is of a Readium Web Publication Manifest. */
    public val isRwpm: Boolean get() =
        matchesAny(READIUM_AUDIOBOOK_MANIFEST, DIVINA_MANIFEST, READIUM_WEBPUB_MANIFEST)

    /** Returns whether this media type is of a publication file. */
    public val isPublication: Boolean get() = matchesAny(
        READIUM_AUDIOBOOK, READIUM_AUDIOBOOK_MANIFEST, CBZ, DIVINA, DIVINA_MANIFEST, EPUB, LCP_PROTECTED_AUDIOBOOK,
        LCP_PROTECTED_PDF, LPF, PDF, W3C_WPUB_MANIFEST, READIUM_WEBPUB, READIUM_WEBPUB_MANIFEST, ZAB
    )

    @Deprecated("Format and MediaType got merged together", replaceWith = ReplaceWith(""), level = DeprecationLevel.ERROR)
    public val mediaType: MediaType
        get() = this

    public companion object {

        /**
         * Creates a [MediaType] from its RFC 6838 string representation.
         *
         * @param name A human readable name identifying the media type, which may be presented to the user.
         * @param fileExtension The default file extension to use for this media type.
         */
        public fun parse(string: String, name: String? = null, fileExtension: String? = null): MediaType? =
            tryOrNull { MediaType(string = string, name = name, fileExtension = fileExtension) }

        // Known Media Types
        //
        // Reading apps are welcome to extend the static constants with additional media types.

        public val AAC: MediaType = MediaType("audio/aac", fileExtension = "aac")
        public val ACSM: MediaType = MediaType("application/vnd.adobe.adept+xml", name = "Adobe Content Server Message", fileExtension = "acsm")
        public val AIFF: MediaType = MediaType("audio/aiff", fileExtension = "aiff")
        public val AVI: MediaType = MediaType("video/x-msvideo", fileExtension = "avi")
        public val AVIF: MediaType = MediaType("image/avif", fileExtension = "avif")
        public val BINARY: MediaType = MediaType("application/octet-stream")
        public val BMP: MediaType = MediaType("image/bmp", fileExtension = "bmp")
        public val CBZ: MediaType = MediaType("application/vnd.comicbook+zip", name = "Comic Book Archive", fileExtension = "cbz")
        public val CSS: MediaType = MediaType("text/css", fileExtension = "css")
        public val DIVINA: MediaType = MediaType("application/divina+zip", name = "Digital Visual Narratives", fileExtension = "divina")
        public val DIVINA_MANIFEST: MediaType = MediaType("application/divina+json", name = "Digital Visual Narratives", fileExtension = "json")
        public val EPUB: MediaType = MediaType("application/epub+zip", name = "EPUB", fileExtension = "epub")
        public val GIF: MediaType = MediaType("image/gif", fileExtension = "gif")
        public val GZ: MediaType = MediaType("application/gzip", fileExtension = "gz")
        public val HTML: MediaType = MediaType("text/html", fileExtension = "html")
        public val JAVASCRIPT: MediaType = MediaType("text/javascript", fileExtension = "js")
        public val JPEG: MediaType = MediaType("image/jpeg", fileExtension = "jpeg")
        public val JSON: MediaType = MediaType("application/json")
        public val JSON_PROBLEM_DETAILS: MediaType = MediaType("application/problem+json", name = "HTTP Problem Details", fileExtension = "json")
        public val JXL: MediaType = MediaType("image/jxl", fileExtension = "jxl")
        public val LCP_LICENSE_DOCUMENT: MediaType = MediaType("application/vnd.readium.lcp.license.v1.0+json", name = "LCP License", fileExtension = "lcpl")
        public val LCP_PROTECTED_AUDIOBOOK: MediaType = MediaType("application/audiobook+lcp", name = "LCP Protected Audiobook", fileExtension = "lcpa")
        public val LCP_PROTECTED_PDF: MediaType = MediaType("application/pdf+lcp", name = "LCP Protected PDF", fileExtension = "lcpdf")
        public val LCP_STATUS_DOCUMENT: MediaType = MediaType("application/vnd.readium.license.status.v1.0+json")
        public val LPF: MediaType = MediaType("application/lpf+zip", fileExtension = "lpf")
        public val MP3: MediaType = MediaType("audio/mpeg", fileExtension = "mp3")
        public val MPEG: MediaType = MediaType("video/mpeg", fileExtension = "mpeg")
        public val NCX: MediaType = MediaType("application/x-dtbncx+xml", fileExtension = "ncx")
        public val OGG: MediaType = MediaType("audio/ogg", fileExtension = "oga")
        public val OGV: MediaType = MediaType("video/ogg", fileExtension = "ogv")
        public val OPDS1: MediaType = MediaType("application/atom+xml;profile=opds-catalog")
        public val OPDS1_ENTRY: MediaType = MediaType("application/atom+xml;type=entry;profile=opds-catalog")
        public val OPDS2: MediaType = MediaType("application/opds+json")
        public val OPDS2_PUBLICATION: MediaType = MediaType("application/opds-publication+json")
        public val OPDS_AUTHENTICATION: MediaType = MediaType("application/opds-authentication+json")
        public val OPUS: MediaType = MediaType("audio/opus", fileExtension = "opus")
        public val OTF: MediaType = MediaType("font/otf", fileExtension = "otf")
        public val PDF: MediaType = MediaType("application/pdf", name = "PDF", fileExtension = "pdf")
        public val PNG: MediaType = MediaType("image/png", fileExtension = "png")
        public val READIUM_AUDIOBOOK: MediaType = MediaType("application/audiobook+zip", name = "Readium Audiobook", fileExtension = "audiobook")
        public val READIUM_AUDIOBOOK_MANIFEST: MediaType = MediaType("application/audiobook+json", name = "Readium Audiobook", fileExtension = "json")
        public val READIUM_WEBPUB: MediaType = MediaType("application/webpub+zip", name = "Readium Web Publication", fileExtension = "webpub")
        public val READIUM_WEBPUB_MANIFEST: MediaType = MediaType("application/webpub+json", name = "Readium Web Publication", fileExtension = "json")
        public val SMIL: MediaType = MediaType("application/smil+xml", fileExtension = "smil")
        public val SVG: MediaType = MediaType("image/svg+xml", fileExtension = "svg")
        public val TEXT: MediaType = MediaType("text/plain", fileExtension = "txt")
        public val TIFF: MediaType = MediaType("image/tiff", fileExtension = "tiff")
        public val TTF: MediaType = MediaType("font/ttf", fileExtension = "ttf")
        public val W3C_WPUB_MANIFEST: MediaType = MediaType("application/x.readium.w3c.wpub+json", name = "Web Publication", fileExtension = "json") // non-existent
        public val WAV: MediaType = MediaType("audio/wav", fileExtension = "wav")
        public val WEBM_AUDIO: MediaType = MediaType("audio/webm", fileExtension = "webm")
        public val WEBM_VIDEO: MediaType = MediaType("video/webm", fileExtension = "webm")
        public val WEBP: MediaType = MediaType("image/webp", fileExtension = "webp")
        public val WOFF: MediaType = MediaType("font/woff", fileExtension = "woff")
        public val WOFF2: MediaType = MediaType("font/woff2", fileExtension = "woff2")
        public val XHTML: MediaType = MediaType("application/xhtml+xml", fileExtension = "xhtml")
        public val XML: MediaType = MediaType("application/xml", fileExtension = "xml")
        public val ZAB: MediaType = MediaType("application/x.readium.zab+zip", name = "Zipped Audio Book", fileExtension = "zab") // non-existent
        public val ZIP: MediaType = MediaType("application/zip", fileExtension = "zip")

        // Sniffing

        /**
         * The default sniffers provided by Readium 2 to resolve a [MediaType].
         * You can register additional sniffers globally by modifying this list.
         * The sniffers order is important, because some formats are subsets of other formats.
         */
        public val sniffers: MutableList<Sniffer> = Sniffers.all.toMutableList()

        /**
         * Resolves a format from a single file extension and media type hint, without checking the actual
         * content.
         */
        @Deprecated(message = "Use MediaTypeRetriever instead", replaceWith = ReplaceWith("MediaTypeRetriever().retrieve(mediaType = mediaType, fileExtension = fileExtension)"), level = DeprecationLevel.ERROR)
        @Suppress("UNUSED_PARAMETER")
        public fun of(
            mediaType: String? = null,
            fileExtension: String? = null,
            sniffers: List<Sniffer> = MediaType.sniffers
        ): MediaType? {
            TODO()
        }

        /**
         * Resolves a format from file extension and media type hints, without checking the actual
         * content.
         */
        @Deprecated(message = "Use MediaTypeRetriever instead", replaceWith = ReplaceWith("MediaTypeRetriever().retrieve(mediaTypes = mediaTypes, fileExtensions = fileExtensions)"), level = DeprecationLevel.ERROR)
        @Suppress("UNUSED_PARAMETER")
        public fun of(
            mediaTypes: List<String>,
            fileExtensions: List<String>,
            sniffers: List<Sniffer> = MediaType.sniffers
        ): MediaType? {
            TODO()
        }

        /**
         * Resolves a format from a local file path.
         */
        @Suppress("UNUSED_PARAMETER")
        @Deprecated(message = "Use MediaTypeRetriever instead", replaceWith = ReplaceWith("MediaTypeRetriever().retrieve(file)"), level = DeprecationLevel.ERROR)
        public fun ofFile(
            file: File,
            mediaType: String? = null,
            fileExtension: String? = null,
            sniffers: List<Sniffer> = MediaType.sniffers
        ): MediaType? {
            TODO()
        }

        /**
         * Resolves a format from a local file path.
         */
        @Deprecated(message = "Use MediaTypeRetriever instead", replaceWith = ReplaceWith("MediaTypeRetriever().retrieve(file, mediaTypes = mediaTypes, fileExtensions = fileExtensions)"), level = DeprecationLevel.ERROR)
        @Suppress("UNUSED_PARAMETER")
        public fun ofFile(
            file: File,
            mediaTypes: List<String>,
            fileExtensions: List<String>,
            sniffers: List<Sniffer> = MediaType.sniffers
        ): MediaType? {
            TODO()
        }

        /**
         * Resolves a format from a local file path.
         */
        @Suppress("UNUSED_PARAMETER")
        @Deprecated(message = "Use MediaTypeRetriever instead", replaceWith = ReplaceWith("MediaTypeRetriever().retrieve(File(path))"), level = DeprecationLevel.ERROR)
        public fun ofFile(
            path: String,
            mediaType: String? = null,
            fileExtension: String? = null,
            sniffers: List<Sniffer> = MediaType.sniffers
        ): MediaType? {
            TODO()
        }

        /**
         * Resolves a format from a local file path.
         */
        @Suppress("UNUSED_PARAMETER")
        @Deprecated(message = "Use MediaTypeRetriever instead", replaceWith = ReplaceWith("MediaTypeRetriever().retrieve(File(path), mediaTypes = mediaTypes, fileExtensions = fileExtensions)"), level = DeprecationLevel.ERROR)
        public fun ofFile(
            path: String,
            mediaTypes: List<String>,
            fileExtensions: List<String>,
            sniffers: List<Sniffer> = MediaType.sniffers
        ): MediaType? {
            TODO()
        }

        /**
         * Resolves a format from bytes, e.g. from an HTTP response.
         */
        @Suppress("UNUSED_PARAMETER")
        @Deprecated(message = "Use MediaTypeRetriever instead", replaceWith = ReplaceWith("MediaTypeRetriever().retrieve(bytes)"), level = DeprecationLevel.ERROR)
        public fun ofBytes(
            bytes: () -> ByteArray,
            mediaType: String? = null,
            fileExtension: String? = null,
            sniffers: List<Sniffer> = MediaType.sniffers
        ): MediaType? {
            TODO()
        }

        /**
         * Resolves a format from bytes, e.g. from an HTTP response.
         */
        @Suppress("UNUSED_PARAMETER")
        @Deprecated(message = "Use MediaTypeRetriever instead", replaceWith = ReplaceWith("MediaTypeRetriever().retrieve(bytes, mediaTypes = mediaTypes, fileExtensions = fileExtensions)"), level = DeprecationLevel.ERROR)
        public fun ofBytes(
            bytes: () -> ByteArray,
            mediaTypes: List<String>,
            fileExtensions: List<String>,
            sniffers: List<Sniffer> = MediaType.sniffers
        ): MediaType? {
            TODO()
        }

        /**
         * Resolves a format from a content URI and a [ContentResolver].
         * Accepts the following URI schemes: content, android.resource, file.
         */
        @Suppress("UNUSED_PARAMETER")
        @Deprecated(message = "Use MediaTypeRetriever instead", replaceWith = ReplaceWith("MediaTypeRetriever(contentResolver = contentResolver).retrieve(uri)"), level = DeprecationLevel.ERROR)
        public fun ofUri(
            uri: Uri,
            contentResolver: ContentResolver,
            mediaType: String? = null,
            fileExtension: String? = null,
            sniffers: List<Sniffer> = MediaType.sniffers
        ): MediaType? {
            TODO()
        }

        /**
         * Resolves a format from a content URI and a [ContentResolver].
         * Accepts the following URI schemes: content, android.resource, file.
         */
        @Suppress("UNUSED_PARAMETER")
        @Deprecated(message = "Use MediaTypeRetriever instead", replaceWith = ReplaceWith("MediaTypeRetriever(contentResolver = contentResolver).retrieve(uri, mediaTypes = mediaTypes, fileExtensions = fileExtensions)"), level = DeprecationLevel.ERROR)
        public fun ofUri(
            uri: Uri,
            contentResolver: ContentResolver,
            mediaTypes: List<String>,
            fileExtensions: List<String>,
            sniffers: List<Sniffer> = MediaType.sniffers
        ): MediaType? {
            TODO()
        }

        /* Deprecated */

        @Deprecated("Use [READIUM_AUDIOBOOK] instead", ReplaceWith("MediaType.READIUM_AUDIOBOOK"), level = DeprecationLevel.ERROR)
        public val AUDIOBOOK: MediaType get() = READIUM_AUDIOBOOK
        @Deprecated("Use [READIUM_AUDIOBOOK_MANIFEST] instead", ReplaceWith("MediaType.READIUM_AUDIOBOOK_MANIFEST"), level = DeprecationLevel.ERROR)
        public val AUDIOBOOK_MANIFEST: MediaType get() = READIUM_AUDIOBOOK_MANIFEST
        @Deprecated("Use [READIUM_WEBPUB] instead", ReplaceWith("MediaType.READIUM_WEBPUB"), level = DeprecationLevel.ERROR)
        public val WEBPUB: MediaType get() = READIUM_WEBPUB
        @Deprecated("Use [READIUM_WEBPUB_MANIFEST] instead", ReplaceWith("MediaType.READIUM_WEBPUB_MANIFEST"), level = DeprecationLevel.ERROR)
        public val WEBPUB_MANIFEST: MediaType get() = READIUM_WEBPUB_MANIFEST
        @Deprecated("Use [OPDS1] instead", ReplaceWith("MediaType.OPDS1"), level = DeprecationLevel.ERROR)
        public val OPDS1_FEED: MediaType get() = OPDS1
        @Deprecated("Use [OPDS2] instead", ReplaceWith("MediaType.OPDS2"), level = DeprecationLevel.ERROR)
        public val OPDS2_FEED: MediaType get() = OPDS2
        @Deprecated("Use [LCP_LICENSE_DOCUMENT] instead", ReplaceWith("MediaType.LCP_LICENSE_DOCUMENT"), level = DeprecationLevel.ERROR)
        public val LCP_LICENSE: MediaType get() = LCP_LICENSE_DOCUMENT

        @Suppress("UNUSED_PARAMETER")
        @Deprecated("Use `MediaTypeRetriever` instead", level = DeprecationLevel.ERROR)
        public fun of(
            file: File,
            mediaType: String? = null,
            fileExtension: String? = null,
            sniffers: List<Sniffer> = MediaType.sniffers
        ): MediaType? = null

        @Suppress("UNUSED_PARAMETER")
        @Deprecated("Use `MediaTypeRetriever` instead", level = DeprecationLevel.ERROR)
        public fun of(
            file: File,
            mediaTypes: List<String>,
            fileExtensions: List<String>,
            sniffers: List<Sniffer> = MediaType.sniffers
        ): MediaType? = null

        @Suppress("UNUSED_PARAMETER")
        @Deprecated("Use `MediaTypeRetriever` instead", level = DeprecationLevel.ERROR)
        public fun of(
            bytes: () -> ByteArray,
            mediaType: String? = null,
            fileExtension: String? = null,
            sniffers: List<Sniffer> = MediaType.sniffers
        ): MediaType? = null

        @Suppress("UNUSED_PARAMETER")
        @Deprecated("Use `MediaTypeRetriever` instead", level = DeprecationLevel.ERROR)
        public fun of(
            bytes: () -> ByteArray,
            mediaTypes: List<String>,
            fileExtensions: List<String>,
            sniffers: List<Sniffer> = MediaType.sniffers
        ): MediaType? = null

        @Suppress("UNUSED_PARAMETER")
        @Deprecated("Use `MediaTypeRetriever` instead", level = DeprecationLevel.ERROR)
        public fun of(
            uri: Uri,
            contentResolver: ContentResolver,
            mediaType: String? = null,
            fileExtension: String? = null,
            sniffers: List<Sniffer> = MediaType.sniffers
        ): MediaType? = null

        @Suppress("UNUSED_PARAMETER")
        @Deprecated("Use `MediaTypeRetriever` instead", level = DeprecationLevel.ERROR)
        public fun of(
            uri: Uri,
            contentResolver: ContentResolver,
            mediaTypes: List<String>,
            fileExtensions: List<String>,
            sniffers: List<Sniffer> = MediaType.sniffers
        ): MediaType? = null
    }
}
