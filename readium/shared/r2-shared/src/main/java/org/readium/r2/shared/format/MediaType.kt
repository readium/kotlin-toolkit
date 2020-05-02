/*
 * Module: r2-shared-kotlin
 * Developers: Mickaël Menu
 *
 * Copyright (c) 2020. Readium Foundation. All rights reserved.
 * Use of this source code is governed by a BSD-style license which is detailed in the
 * LICENSE file present in the project repository where this source code is maintained.
 */

package org.readium.r2.shared.format

import org.readium.r2.shared.publication.Link
import java.lang.IllegalArgumentException
import java.nio.charset.Charset
import java.util.*

/**
 * Represents a string media type.
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
 */
class MediaType private constructor(string: String) {

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
        this.type = types[0].toLowerCase(Locale.ROOT)
        this.subtype = types[1].toLowerCase(Locale.ROOT)

        // > Parameter names are case-insensitive and no meaning is attached to the order in which
        // > they appear.
        val parameters = components.drop(1)
            .map { it.split("=") }
            .filter { it.size == 2 }
            .associate { Pair(it[0].toLowerCase(Locale.ROOT), it[1]) }
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
            parameters["charset"] = it.toUpperCase(Locale.ROOT)
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

    /** Returns whether this media type is structured as a ZIP archive. */
    val isZip: Boolean get() {
        return matches(ZIP)
            || matches(LCP_PROTECTED_AUDIOBOOK)
            || matches(LCP_PROTECTED_PDF)
            || structuredSyntaxSuffix == "+zip"
    }

    /** Returns whether this media type is structured as a JSON file. */
    val isJson: Boolean get() {
        return matches(JSON)
            || structuredSyntaxSuffix == "+json"
    }

    /** Returns whether this media type is of an OPDS feed. */
    val isOpds: Boolean get() {
        return matches(OPDS1)
            || matches(OPDS1_ENTRY)
            || matches(OPDS2)
            || matches(OPDS2_PUBLICATION)
            || matches(OPDS_AUTHENTICATION)
    }

    /** Returns whether this media type is of an HTML document. */
    val isHtml: Boolean get() {
        return matches(HTML)
            || matches(XHTML)
    }

    /** Returns whether this media type is of a bitmap image, so excluding vectorial formats. */
    val isBitmap: Boolean get() {
        return matches(BMP)
            || matches(GIF)
            || matches(JPEG)
            || matches(PNG)
            || matches(TIFF)
            || matches(WEBP)
    }

    /** Returns whether this media type is of an audio clip. */
    val isAudio: Boolean get() =
        type == "audio"

    /** Returns whether this media type is of a Readium Web Publication Manifest. */
    val isRwpm: Boolean get() {
        return matches(AUDIOBOOK_MANIFEST)
            || matches(DIVINA_MANIFEST)
            || matches(WEBPUB_MANIFEST)
    }

    companion object {

        /**
         * Creates a [MediaType] from its string representation.
         */
        fun parse(string: String): MediaType? =
            try {
                MediaType(string)
            } catch (e: Exception) {
                null
            }

        // Known Media Types

        val AAC = MediaType("audio/aac")
        val ACSM = MediaType("application/vnd.adobe.adept+xml")
        val AIFF = MediaType("audio/aiff")
        val AUDIOBOOK = MediaType("application/audiobook+zip")
        val AUDIOBOOK_MANIFEST = MediaType("application/audiobook+json")
        val AVI = MediaType("video/x-msvideo")
        val BINARY = MediaType("application/octet-stream")
        val BMP = MediaType("image/bmp")
        val CBZ = MediaType("application/vnd.comicbook+zip")
        val CSS = MediaType("text/css")
        val DIVINA = MediaType("application/divina+zip")
        val DIVINA_MANIFEST = MediaType("application/divina+json")
        val EPUB = MediaType("application/epub+zip")
        val GIF = MediaType("image/gif")
        val GZ = MediaType("application/gzip")
        val JAVASCRIPT = MediaType("text/javascript")
        val JPEG = MediaType("image/jpeg")
        val HTML = MediaType("text/html")
        val OPDS1 = MediaType("application/atom+xml;profile=opds-catalog")
        val OPDS1_ENTRY = MediaType("application/atom+xml;type=entry;profile=opds-catalog")
        val OPDS2 = MediaType("application/opds+json")
        val OPDS2_PUBLICATION = MediaType("application/opds-publication+json")
        val OPDS_AUTHENTICATION = MediaType("application/opds-authentication+json")
        val JSON = MediaType("application/json")
        val LCP_PROTECTED_AUDIOBOOK = MediaType("application/audiobook+lcp")
        val LCP_PROTECTED_PDF = MediaType("application/pdf+lcp")
        val LCP_LICENSE_DOCUMENT = MediaType("application/vnd.readium.lcp.license.v1.0+json")
        val LCP_STATUS_DOCUMENT = MediaType("application/vnd.readium.license.status.v1.0+json")
        val LPF = MediaType("application/lpf+zip")
        val MP3 = MediaType("audio/mpeg")
        val MPEG = MediaType("video/mpeg")
        val NCX = MediaType("application/x-dtbncx+xml")
        val OGG = MediaType("audio/ogg")
        val OGV = MediaType("video/ogg")
        val OPUS = MediaType("audio/opus")
        val OTF = MediaType("font/otf")
        val PDF = MediaType("application/pdf")
        val PNG = MediaType("image/png")
        val SMIL = MediaType("application/smil+xml")
        val SVG = MediaType("image/svg+xml")
        val TEXT = MediaType("text/plain")
        val TIFF = MediaType("image/tiff")
        val TTF = MediaType("font/ttf")
        val W3C_WPUB_MANIFEST = MediaType("application/x.readium.w3c.wpub+json")  // non-existent
        val WAV = MediaType("audio/wav")
        val WEBM_AUDIO = MediaType("audio/webm")
        val WEBM_VIDEO = MediaType("video/webm")
        val WEBP = MediaType("image/webp")
        val WEBPUB = MediaType("application/webpub+zip")
        val WEBPUB_MANIFEST = MediaType("application/webpub+json")
        val WOFF = MediaType("font/woff")
        val WOFF2 = MediaType("font/woff2")
        val XHTML = MediaType("application/xhtml+xml")
        val XML = MediaType("application/xml")
        val ZAB = MediaType("application/x.readium.zab+zip")  // non-existent
        val ZIP = MediaType("application/zip")

    }

}
