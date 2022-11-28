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
import android.provider.MediaStore
import android.webkit.MimeTypeMap
import java.io.File
import java.nio.charset.Charset
import java.util.*
import org.readium.r2.shared.BuildConfig.DEBUG
import org.readium.r2.shared.extensions.queryProjection
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
class MediaType(
    string: String,
    val name: String? = null,
    val fileExtension: String? = null
) {

    /** The type component, e.g. `application` in `application/epub+zip`. */
    val type: String

    /** The subtype component, e.g. `epub+zip` in `application/epub+zip`. */
    val subtype: String

    /** The parameters in the media type, such as `charset=utf-8`. */
    val parameters: Map<String, String>

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
    val structuredSyntaxSuffix: String? get() {
        val parts = subtype.split("+")
        return if (parts.size > 1) "+${parts.last()}" else null
    }

    /**
     * Encoding as declared in the `charset` parameter, if there's any.
     */
    val charset: Charset? get() =
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
    suspend fun canonicalMediaType(): MediaType =
        of(mediaType = toString()) ?: this

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
    fun contains(other: MediaType?): Boolean {
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
    fun contains(other: String?): Boolean {
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
    fun matches(other: MediaType?): Boolean =
        contains(other) || (other?.contains(this) == true)

    /**
     * Returns whether this media type and `other` are the same, ignoring parameters that are not
     * in both media types.
     */
    fun matches(other: String?): Boolean =
        matches(other?.let { parse(it) })

    /**
     * Returns whether this media type matches any of the `others` media types.
     */
    fun matchesAny(vararg others: MediaType?): Boolean =
        others.any { matches(it) }

    /**
     * Returns whether this media type matches any of the `others` media types.
     */
    fun matchesAny(vararg others: String?): Boolean =
        others.any { matches(it) }

    /** Returns whether this media type is structured as a ZIP archive. */
    val isZip: Boolean get() =
        matchesAny(ZIP, LCP_PROTECTED_AUDIOBOOK, LCP_PROTECTED_PDF) ||
            structuredSyntaxSuffix == "+zip"

    /** Returns whether this media type is structured as a JSON file. */
    val isJson: Boolean get() =
        matches(JSON) || structuredSyntaxSuffix == "+json"

    /** Returns whether this media type is of an OPDS feed. */
    val isOpds: Boolean get() =
        matchesAny(OPDS1, OPDS1_ENTRY, OPDS2, OPDS2_PUBLICATION, OPDS_AUTHENTICATION)

    /** Returns whether this media type is of an HTML document. */
    val isHtml: Boolean get() =
        matchesAny(HTML, XHTML)

    /** Returns whether this media type is of a bitmap image, so excluding vectorial formats. */
    val isBitmap: Boolean get() =
        matchesAny(BMP, GIF, JPEG, PNG, TIFF, WEBP)

    /** Returns whether this media type is of an audio clip. */
    val isAudio: Boolean get() =
        type == "audio"

    /** Returns whether this media type is of a video clip. */
    val isVideo: Boolean get() =
        type == "video"

    /** Returns whether this media type is of a Readium Web Publication Manifest. */
    val isRwpm: Boolean get() =
        matchesAny(READIUM_AUDIOBOOK_MANIFEST, DIVINA_MANIFEST, READIUM_WEBPUB_MANIFEST)

    /** Returns whether this media type is of a publication file. */
    val isPublication: Boolean get() = matchesAny(
        READIUM_AUDIOBOOK, READIUM_AUDIOBOOK_MANIFEST, CBZ, DIVINA, DIVINA_MANIFEST, EPUB, LCP_PROTECTED_AUDIOBOOK,
        LCP_PROTECTED_PDF, LPF, PDF, W3C_WPUB_MANIFEST, READIUM_WEBPUB, READIUM_WEBPUB_MANIFEST, ZAB
    )

    @Deprecated("Format and MediaType got merged together", replaceWith = ReplaceWith(""), level = DeprecationLevel.ERROR)
    val mediaType: MediaType
        get() = this

    companion object {

        /**
         * Creates a [MediaType] from its RFC 6838 string representation.
         *
         * @param name A human readable name identifying the media type, which may be presented to the user.
         * @param fileExtension The default file extension to use for this media type.
         */
        fun parse(string: String, name: String? = null, fileExtension: String? = null): MediaType? =
            tryOrNull { MediaType(string = string, name = name, fileExtension = fileExtension) }

        // Known Media Types
        //
        // Reading apps are welcome to extend the static constants with additional media types.

        val AAC = MediaType("audio/aac", fileExtension = "aac")
        val ACSM = MediaType("application/vnd.adobe.adept+xml", name = "Adobe Content Server Message", fileExtension = "acsm")
        val AIFF = MediaType("audio/aiff", fileExtension = "aiff")
        val AVI = MediaType("video/x-msvideo", fileExtension = "avi")
        val AVIF = MediaType("image/avif", fileExtension = "avif")
        val BINARY = MediaType("application/octet-stream")
        val BMP = MediaType("image/bmp", fileExtension = "bmp")
        val CBZ = MediaType("application/vnd.comicbook+zip", name = "Comic Book Archive", fileExtension = "cbz")
        val CSS = MediaType("text/css", fileExtension = "css")
        val DIVINA = MediaType("application/divina+zip", name = "Digital Visual Narratives", fileExtension = "divina")
        val DIVINA_MANIFEST = MediaType("application/divina+json", name = "Digital Visual Narratives", fileExtension = "json")
        val EPUB = MediaType("application/epub+zip", name = "EPUB", fileExtension = "epub")
        val GIF = MediaType("image/gif", fileExtension = "gif")
        val GZ = MediaType("application/gzip", fileExtension = "gz")
        val HTML = MediaType("text/html", fileExtension = "html")
        val JAVASCRIPT = MediaType("text/javascript", fileExtension = "js")
        val JPEG = MediaType("image/jpeg", fileExtension = "jpeg")
        val JSON = MediaType("application/json")
        val JSON_PROBLEM_DETAILS = MediaType("application/problem+json", name = "HTTP Problem Details", fileExtension = "json")
        val JXL = MediaType("image/jxl", fileExtension = "jxl")
        val LCP_LICENSE_DOCUMENT = MediaType("application/vnd.readium.lcp.license.v1.0+json", name = "LCP License", fileExtension = "lcpl")
        val LCP_PROTECTED_AUDIOBOOK = MediaType("application/audiobook+lcp", name = "LCP Protected Audiobook", fileExtension = "lcpa")
        val LCP_PROTECTED_PDF = MediaType("application/pdf+lcp", name = "LCP Protected PDF", fileExtension = "lcpdf")
        val LCP_STATUS_DOCUMENT = MediaType("application/vnd.readium.license.status.v1.0+json")
        val LPF = MediaType("application/lpf+zip", fileExtension = "lpf")
        val MP3 = MediaType("audio/mpeg", fileExtension = "mp3")
        val MPEG = MediaType("video/mpeg", fileExtension = "mpeg")
        val NCX = MediaType("application/x-dtbncx+xml", fileExtension = "ncx")
        val OGG = MediaType("audio/ogg", fileExtension = "oga")
        val OGV = MediaType("video/ogg", fileExtension = "ogv")
        val OPDS1 = MediaType("application/atom+xml;profile=opds-catalog")
        val OPDS1_ENTRY = MediaType("application/atom+xml;type=entry;profile=opds-catalog")
        val OPDS2 = MediaType("application/opds+json")
        val OPDS2_PUBLICATION = MediaType("application/opds-publication+json")
        val OPDS_AUTHENTICATION = MediaType("application/opds-authentication+json")
        val OPUS = MediaType("audio/opus", fileExtension = "opus")
        val OTF = MediaType("font/otf", fileExtension = "otf")
        val PDF = MediaType("application/pdf", name = "PDF", fileExtension = "pdf")
        val PNG = MediaType("image/png", fileExtension = "png")
        val READIUM_AUDIOBOOK = MediaType("application/audiobook+zip", name = "Readium Audiobook", fileExtension = "audiobook")
        val READIUM_AUDIOBOOK_MANIFEST = MediaType("application/audiobook+json", name = "Readium Audiobook", fileExtension = "json")
        val READIUM_WEBPUB = MediaType("application/webpub+zip", name = "Readium Web Publication", fileExtension = "webpub")
        val READIUM_WEBPUB_MANIFEST = MediaType("application/webpub+json", name = "Readium Web Publication", fileExtension = "json")
        val SMIL = MediaType("application/smil+xml", fileExtension = "smil")
        val SVG = MediaType("image/svg+xml", fileExtension = "svg")
        val TEXT = MediaType("text/plain", fileExtension = "txt")
        val TIFF = MediaType("image/tiff", fileExtension = "tiff")
        val TTF = MediaType("font/ttf", fileExtension = "ttf")
        val W3C_WPUB_MANIFEST = MediaType("application/x.readium.w3c.wpub+json", name = "Web Publication", fileExtension = "json") // non-existent
        val WAV = MediaType("audio/wav", fileExtension = "wav")
        val WEBM_AUDIO = MediaType("audio/webm", fileExtension = "webm")
        val WEBM_VIDEO = MediaType("video/webm", fileExtension = "webm")
        val WEBP = MediaType("image/webp", fileExtension = "webp")
        val WOFF = MediaType("font/woff", fileExtension = "woff")
        val WOFF2 = MediaType("font/woff2", fileExtension = "woff2")
        val XHTML = MediaType("application/xhtml+xml", fileExtension = "xhtml")
        val XML = MediaType("application/xml", fileExtension = "xml")
        val ZAB = MediaType("application/x.readium.zab+zip", name = "Zipped Audio Book", fileExtension = "zab") // non-existent
        val ZIP = MediaType("application/zip", fileExtension = "zip")

        // Sniffing

        /**
         * The default sniffers provided by Readium 2 to resolve a [MediaType].
         * You can register additional sniffers globally by modifying this list.
         * The sniffers order is important, because some formats are subsets of other formats.
         */
        val sniffers = Sniffers.all.toMutableList()

        /**
         * Resolves a format from a single file extension and media type hint, without checking the actual
         * content.
         */
        suspend fun of(
            mediaType: String? = null,
            fileExtension: String? = null,
            sniffers: List<Sniffer> = MediaType.sniffers
        ): MediaType? {
            if (DEBUG && mediaType?.startsWith("/") == true) {
                throw IllegalArgumentException("The provided media type is incorrect: $mediaType. To pass a file path, you must wrap it in a File().")
            }
            return of(content = null, mediaTypes = listOfNotNull(mediaType), fileExtensions = listOfNotNull(fileExtension), sniffers = sniffers)
        }

        /**
         * Resolves a format from file extension and media type hints, without checking the actual
         * content.
         */
        suspend fun of(
            mediaTypes: List<String>,
            fileExtensions: List<String>,
            sniffers: List<Sniffer> = MediaType.sniffers
        ): MediaType? {
            return of(content = null, mediaTypes = mediaTypes, fileExtensions = fileExtensions, sniffers = sniffers)
        }

        /**
         * Resolves a format from a local file path.
         */
        suspend fun ofFile(
            file: File,
            mediaType: String? = null,
            fileExtension: String? = null,
            sniffers: List<Sniffer> = MediaType.sniffers
        ): MediaType? {
            return ofFile(file, mediaTypes = listOfNotNull(mediaType), fileExtensions = listOfNotNull(fileExtension), sniffers = sniffers)
        }

        /**
         * Resolves a format from a local file path.
         */
        suspend fun ofFile(
            file: File,
            mediaTypes: List<String>,
            fileExtensions: List<String>,
            sniffers: List<Sniffer> = MediaType.sniffers
        ): MediaType? {
            return of(content = SnifferFileContent(file), mediaTypes = mediaTypes, fileExtensions = listOf(file.extension) + fileExtensions, sniffers = sniffers)
        }

        /**
         * Resolves a format from a local file path.
         */
        suspend fun ofFile(
            path: String,
            mediaType: String? = null,
            fileExtension: String? = null,
            sniffers: List<Sniffer> = MediaType.sniffers
        ): MediaType? {
            return ofFile(File(path), mediaType = mediaType, fileExtension = fileExtension, sniffers = sniffers)
        }

        /**
         * Resolves a format from a local file path.
         */
        suspend fun ofFile(
            path: String,
            mediaTypes: List<String>,
            fileExtensions: List<String>,
            sniffers: List<Sniffer> = MediaType.sniffers
        ): MediaType? {
            return ofFile(File(path), mediaTypes = mediaTypes, fileExtensions = fileExtensions, sniffers = sniffers)
        }

        /**
         * Resolves a format from bytes, e.g. from an HTTP response.
         */
        suspend fun ofBytes(
            bytes: () -> ByteArray,
            mediaType: String? = null,
            fileExtension: String? = null,
            sniffers: List<Sniffer> = MediaType.sniffers
        ): MediaType? {
            return ofBytes(bytes, mediaTypes = listOfNotNull(mediaType), fileExtensions = listOfNotNull(fileExtension), sniffers = sniffers)
        }

        /**
         * Resolves a format from bytes, e.g. from an HTTP response.
         */
        suspend fun ofBytes(
            bytes: () -> ByteArray,
            mediaTypes: List<String>,
            fileExtensions: List<String>,
            sniffers: List<Sniffer> = MediaType.sniffers
        ): MediaType? {
            return of(content = SnifferBytesContent(bytes), mediaTypes = mediaTypes, fileExtensions = fileExtensions, sniffers = sniffers)
        }

        /**
         * Resolves a format from a content URI and a [ContentResolver].
         * Accepts the following URI schemes: content, android.resource, file.
         */
        suspend fun ofUri(
            uri: Uri,
            contentResolver: ContentResolver,
            mediaType: String? = null,
            fileExtension: String? = null,
            sniffers: List<Sniffer> = MediaType.sniffers
        ): MediaType? {
            return ofUri(uri, contentResolver, mediaTypes = listOfNotNull(mediaType), fileExtensions = listOfNotNull(fileExtension), sniffers = sniffers)
        }

        /**
         * Resolves a format from a content URI and a [ContentResolver].
         * Accepts the following URI schemes: content, android.resource, file.
         */
        suspend fun ofUri(
            uri: Uri,
            contentResolver: ContentResolver,
            mediaTypes: List<String>,
            fileExtensions: List<String>,
            sniffers: List<Sniffer> = MediaType.sniffers
        ): MediaType? {
            val allMediaTypes = mediaTypes.toMutableList()
            val allFileExtensions = fileExtensions.toMutableList()

            MimeTypeMap.getFileExtensionFromUrl(uri.toString()).ifEmpty { null }?.let {
                allFileExtensions.add(0, it)
            }

            if (uri.scheme == ContentResolver.SCHEME_CONTENT) {
                contentResolver.getType(uri)
                    ?.takeUnless { MediaType.BINARY.matches(it) }
                    ?.let { allMediaTypes.add(0, it) }

                contentResolver.queryProjection(uri, MediaStore.MediaColumns.DISPLAY_NAME)?.let { filename ->
                    allFileExtensions.add(0, File(filename).extension)
                }
            }

            val content = SnifferUriContent(uri = uri, contentResolver = contentResolver)
            return of(content = content, mediaTypes = allMediaTypes, fileExtensions = allFileExtensions, sniffers = sniffers)
        }

        /**
         * Resolves a media type from a sniffer context.
         *
         * Sniffing a media type is done in two rounds, because we want to give an opportunity to all
         * sniffers to return a [MediaType] quickly before inspecting the content itself:
         *  - Light Sniffing checks only the provided file extension or media type hints.
         *  - Heavy Sniffing reads the bytes to perform more advanced sniffing.
         */
        private suspend fun of(
            content: SnifferContent?,
            mediaTypes: List<String>,
            fileExtensions: List<String>,
            sniffers: List<Sniffer>
        ): MediaType? {
            // Light sniffing with only media type hints
            if (mediaTypes.isNotEmpty()) {
                val context = SnifferContext(mediaTypes = mediaTypes)
                for (sniffer in sniffers) {
                    val mediaType = sniffer(context)
                    if (mediaType != null) {
                        return mediaType
                    }
                }
            }

            // Light sniffing with both media type hints and file extensions
            if (fileExtensions.isNotEmpty()) {
                val context = SnifferContext(mediaTypes = mediaTypes, fileExtensions = fileExtensions)
                for (sniffer in sniffers) {
                    val mediaType = sniffer(context)
                    if (mediaType != null) {
                        return mediaType
                    }
                }
            }

            // Heavy sniffing
            if (content != null) {
                val context = SnifferContext(content = content, mediaTypes = mediaTypes, fileExtensions = fileExtensions)
                for (sniffer in sniffers) {
                    val mediaType = sniffer(context)
                    if (mediaType != null) {
                        return mediaType
                    }
                }
            }

            // Falls back on the system-wide registered media types using [MimeTypeMap].
            // Note: This is done after the heavy sniffing of the provided [sniffers], because
            // otherwise it will detect JSON, XML or ZIP formats before we have a chance of sniffing
            // their content (for example, for RWPM).
            val context = SnifferContext(content = content, mediaTypes = mediaTypes, fileExtensions = fileExtensions)
            Sniffers.system(context)?.let { return it }

            // If nothing else worked, we try to parse the first valid media type hint.
            for (mediaType in mediaTypes) {
                parse(mediaType)?.let { return it }
            }

            return null
        }

        /* Deprecated */

        @Deprecated("Use [READIUM_AUDIOBOOK] instead", ReplaceWith("MediaType.READIUM_AUDIOBOOK"), level = DeprecationLevel.ERROR)
        val AUDIOBOOK: MediaType get() = READIUM_AUDIOBOOK
        @Deprecated("Use [READIUM_AUDIOBOOK_MANIFEST] instead", ReplaceWith("MediaType.READIUM_AUDIOBOOK_MANIFEST"), level = DeprecationLevel.ERROR)
        val AUDIOBOOK_MANIFEST: MediaType get() = READIUM_AUDIOBOOK_MANIFEST
        @Deprecated("Use [READIUM_WEBPUB] instead", ReplaceWith("MediaType.READIUM_WEBPUB"), level = DeprecationLevel.ERROR)
        val WEBPUB: MediaType get() = READIUM_WEBPUB
        @Deprecated("Use [READIUM_WEBPUB_MANIFEST] instead", ReplaceWith("MediaType.READIUM_WEBPUB_MANIFEST"), level = DeprecationLevel.ERROR)
        val WEBPUB_MANIFEST: MediaType get() = READIUM_WEBPUB_MANIFEST
        @Deprecated("Use [OPDS1] instead", ReplaceWith("MediaType.OPDS1"), level = DeprecationLevel.ERROR)
        val OPDS1_FEED: MediaType get() = OPDS1
        @Deprecated("Use [OPDS2] instead", ReplaceWith("MediaType.OPDS2"), level = DeprecationLevel.ERROR)
        val OPDS2_FEED: MediaType get() = OPDS2
        @Deprecated("Use [LCP_LICENSE_DOCUMENT] instead", ReplaceWith("MediaType.LCP_LICENSE_DOCUMENT"), level = DeprecationLevel.ERROR)
        val LCP_LICENSE: MediaType get() = LCP_LICENSE_DOCUMENT

        @Suppress("UNUSED_PARAMETER")
        @Deprecated("Renamed to [ofFile()]", ReplaceWith("MediaType.ofFile(file, mediaType, fileExtension, sniffers)"), level = DeprecationLevel.ERROR)
        fun of(
            file: File,
            mediaType: String? = null,
            fileExtension: String? = null,
            sniffers: List<Sniffer> = MediaType.sniffers
        ): MediaType? = null

        @Suppress("UNUSED_PARAMETER")
        @Deprecated("Renamed to [ofFile()]", ReplaceWith("MediaType.ofFile(file, mediaTypes, fileExtensions, sniffers)"), level = DeprecationLevel.ERROR)
        fun of(
            file: File,
            mediaTypes: List<String>,
            fileExtensions: List<String>,
            sniffers: List<Sniffer> = MediaType.sniffers
        ): MediaType? = null

        @Suppress("UNUSED_PARAMETER")
        @Deprecated("Renamed to [ofBytes()]", ReplaceWith("MediaType.ofBytes(bytes, mediaType, fileExtension, sniffers)"), level = DeprecationLevel.ERROR)
        fun of(
            bytes: () -> ByteArray,
            mediaType: String? = null,
            fileExtension: String? = null,
            sniffers: List<Sniffer> = MediaType.sniffers
        ): MediaType? = null

        @Suppress("UNUSED_PARAMETER")
        @Deprecated("Renamed to [ofBytes()]", ReplaceWith("MediaType.ofBytes(bytes, mediaTypes, fileExtensions, sniffers)"), level = DeprecationLevel.ERROR)
        fun of(
            bytes: () -> ByteArray,
            mediaTypes: List<String>,
            fileExtensions: List<String>,
            sniffers: List<Sniffer> = MediaType.sniffers
        ): MediaType? = null

        @Suppress("UNUSED_PARAMETER")
        @Deprecated("Renamed to [ofUri()]", ReplaceWith("MediaType.ofUri(uri, contentResolver, mediaType, fileExtension, sniffers)"), level = DeprecationLevel.ERROR)
        fun of(
            uri: Uri,
            contentResolver: ContentResolver,
            mediaType: String? = null,
            fileExtension: String? = null,
            sniffers: List<Sniffer> = MediaType.sniffers
        ): MediaType? = null

        @Suppress("UNUSED_PARAMETER")
        @Deprecated("Renamed to [ofUri()]", ReplaceWith("MediaType.ofUri(uri, contentResolver, mediaTypes, fileExtensions, sniffers)"), level = DeprecationLevel.ERROR)
        fun of(
            uri: Uri,
            contentResolver: ContentResolver,
            mediaTypes: List<String>,
            fileExtensions: List<String>,
            sniffers: List<Sniffer> = MediaType.sniffers
        ): MediaType? = null
    }
}
