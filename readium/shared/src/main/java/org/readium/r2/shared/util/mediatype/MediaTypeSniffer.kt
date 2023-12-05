/*
 * Copyright 2023 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.shared.util.mediatype

import android.webkit.MimeTypeMap
import java.io.IOException
import java.net.URLConnection
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import org.readium.r2.shared.extensions.asInstance
import org.readium.r2.shared.extensions.tryOrNull
import org.readium.r2.shared.publication.Link
import org.readium.r2.shared.publication.Manifest
import org.readium.r2.shared.publication.Publication
import org.readium.r2.shared.util.Error
import org.readium.r2.shared.util.RelativeUrl
import org.readium.r2.shared.util.Try
import org.readium.r2.shared.util.Url
import org.readium.r2.shared.util.data.Container
import org.readium.r2.shared.util.data.DecodeError
import org.readium.r2.shared.util.data.ReadError
import org.readium.r2.shared.util.data.Readable
import org.readium.r2.shared.util.data.asInputStream
import org.readium.r2.shared.util.data.borrow
import org.readium.r2.shared.util.data.containsJsonKeys
import org.readium.r2.shared.util.data.readAsJson
import org.readium.r2.shared.util.data.readAsRwpm
import org.readium.r2.shared.util.data.readAsString
import org.readium.r2.shared.util.data.readAsXml
import org.readium.r2.shared.util.getOrDefault
import org.readium.r2.shared.util.getOrElse

public sealed class MediaTypeSnifferError(
    override val message: String,
    override val cause: Error?
) : Error {
    public data object NotRecognized :
        MediaTypeSnifferError("Media type of resource could not be inferred.", null)

    public data class Reading(override val cause: ReadError) :
        MediaTypeSnifferError("An error occurred while trying to read content.", cause)
}

/**
 * Sniffs a [MediaType] from media type and file extension hints.
 */
public interface HintMediaTypeSniffer {

    public fun sniffHints(
        hints: MediaTypeHints
    ): Try<MediaType, MediaTypeSnifferError.NotRecognized>
}

/**
 * Sniffs a [MediaType] from a [Readable] blob.
 */
public interface BlobMediaTypeSniffer {

    public suspend fun sniffBlob(
        source: Readable
    ): Try<MediaType, MediaTypeSnifferError>
}

/**
 * Sniffs a [MediaType] from a [Container].
 */
public interface ContainerMediaTypeSniffer {

    public suspend fun sniffContainer(
        container: Container<Readable>
    ): Try<MediaType, MediaTypeSnifferError>
}

/**
 * Sniffs a [MediaType] from media type and file extension hints or asset content.
 */
public interface MediaTypeSniffer :
    HintMediaTypeSniffer,
    BlobMediaTypeSniffer,
    ContainerMediaTypeSniffer {

    public override fun sniffHints(
        hints: MediaTypeHints
    ): Try<MediaType, MediaTypeSnifferError.NotRecognized> =
        Try.failure(MediaTypeSnifferError.NotRecognized)

    public override suspend fun sniffBlob(
        source: Readable
    ): Try<MediaType, MediaTypeSnifferError> =
        Try.failure(MediaTypeSnifferError.NotRecognized)

    public override suspend fun sniffContainer(
        container: Container<Readable>
    ): Try<MediaType, MediaTypeSnifferError> =
        Try.failure(MediaTypeSnifferError.NotRecognized)
}

public class CompositeMediaTypeSniffer(
    private val sniffers: List<MediaTypeSniffer>
) : MediaTypeSniffer {

    public constructor(vararg sniffers: MediaTypeSniffer) : this(sniffers.toList())

    override fun sniffHints(hints: MediaTypeHints): Try<MediaType, MediaTypeSnifferError.NotRecognized> {
        for (sniffer in sniffers) {
            sniffer.sniffHints(hints)
                .getOrNull()
                ?.let { return Try.success(it) }
        }

        return Try.failure(MediaTypeSnifferError.NotRecognized)
    }

    override suspend fun sniffBlob(source: Readable): Try<MediaType, MediaTypeSnifferError> {
        for (sniffer in sniffers) {
            sniffer.sniffBlob(source)
                .getOrElse { error ->
                    when (error) {
                        MediaTypeSnifferError.NotRecognized ->
                            null
                        else ->
                            return Try.failure(error)
                    }
                }
                ?.let { return Try.success(it) }
        }

        return Try.failure(MediaTypeSnifferError.NotRecognized)
    }

    override suspend fun sniffContainer(container: Container<Readable>): Try<MediaType, MediaTypeSnifferError> {
        for (sniffer in sniffers) {
            sniffer.sniffContainer(container)
                .getOrElse { error ->
                    when (error) {
                        MediaTypeSnifferError.NotRecognized ->
                            null
                        else ->
                            return Try.failure(error)
                    }
                }
                ?.let { return Try.success(it) }
        }

        return Try.failure(MediaTypeSnifferError.NotRecognized)
    }
}

/**
 * Sniffs an XHTML document.
 *
 * Must precede the HTML sniffer.
 */
public object XhtmlMediaTypeSniffer : MediaTypeSniffer {
    override fun sniffHints(hints: MediaTypeHints): Try<MediaType, MediaTypeSnifferError.NotRecognized> {
        if (
            hints.hasFileExtension("xht", "xhtml") ||
            hints.hasMediaType("application/xhtml+xml")
        ) {
            return Try.success(MediaType.XHTML)
        }

        return Try.failure(MediaTypeSnifferError.NotRecognized)
    }

    override suspend fun sniffBlob(source: Readable): Try<MediaType, MediaTypeSnifferError> {
        if (!source.canReadWholeBlob()) {
            return Try.failure(MediaTypeSnifferError.NotRecognized)
        }

        source.readAsXml()
            .getOrElse {
                when (it) {
                    is DecodeError.Reading ->
                        return Try.failure(
                            MediaTypeSnifferError.Reading(it.cause)
                        )
                    is DecodeError.Decoding ->
                        null
                }
            }
            ?.takeIf {
                it.name.lowercase(Locale.ROOT) == "html" &&
                    it.namespace.lowercase(Locale.ROOT).contains("xhtml")
            }?.let {
                return Try.success(MediaType.XHTML)
            }

        return Try.failure(MediaTypeSnifferError.NotRecognized)
    }
}

/** Sniffs an HTML document. */
public object HtmlMediaTypeSniffer : MediaTypeSniffer {
    override fun sniffHints(hints: MediaTypeHints): Try<MediaType, MediaTypeSnifferError.NotRecognized> {
        if (
            hints.hasFileExtension("htm", "html") ||
            hints.hasMediaType("text/html")
        ) {
            return Try.success(MediaType.HTML)
        }

        return Try.failure(MediaTypeSnifferError.NotRecognized)
    }

    override suspend fun sniffBlob(source: Readable): Try<MediaType, MediaTypeSnifferError> {
        if (!source.canReadWholeBlob()) {
            return Try.failure(MediaTypeSnifferError.NotRecognized)
        }

        // [contentAsXml] will fail if the HTML is not a proper XML document, hence the doctype check.
        source.readAsXml()
            .getOrElse {
                when (it) {
                    is DecodeError.Reading ->
                        return Try.failure(MediaTypeSnifferError.Reading(it.cause))
                    is DecodeError.Decoding ->
                        null
                }
            }
            ?.takeIf { it.name.lowercase(Locale.ROOT) == "html" }
            ?.let { return Try.success(MediaType.HTML) }

        source.readAsString()
            .getOrElse {
                when (it) {
                    is DecodeError.Reading ->
                        return Try.failure(MediaTypeSnifferError.Reading(it.cause))

                    is DecodeError.Decoding ->
                        null
                }
            }
            ?.takeIf { it.trimStart().take(15).lowercase() == "<!doctype html>" }
            ?.let { return Try.success(MediaType.HTML) }

        return Try.failure(MediaTypeSnifferError.NotRecognized)
    }
}

/** Sniffs an OPDS document. */
public object OpdsMediaTypeSniffer : MediaTypeSniffer {

    override fun sniffHints(hints: MediaTypeHints): Try<MediaType, MediaTypeSnifferError.NotRecognized> {
        // OPDS 1
        if (hints.hasMediaType("application/atom+xml;type=entry;profile=opds-catalog")) {
            return Try.success(MediaType.OPDS1_ENTRY)
        }
        if (hints.hasMediaType("application/atom+xml;profile=opds-catalog;kind=navigation")) {
            return Try.success(MediaType.OPDS1_NAVIGATION_FEED)
        }
        if (hints.hasMediaType("application/atom+xml;profile=opds-catalog;kind=acquisition")) {
            return Try.success(MediaType.OPDS1_ACQUISITION_FEED)
        }
        if (hints.hasMediaType("application/atom+xml;profile=opds-catalog")) {
            return Try.success(MediaType.OPDS1)
        }

        // OPDS 2
        if (hints.hasMediaType("application/opds+json")) {
            return Try.success(MediaType.OPDS2)
        }
        if (hints.hasMediaType("application/opds-publication+json")) {
            return Try.success(MediaType.OPDS2_PUBLICATION)
        }

        // OPDS Authentication Document.
        if (
            hints.hasMediaType("application/opds-authentication+json") ||
            hints.hasMediaType("application/vnd.opds.authentication.v1.0+json")
        ) {
            return Try.success(MediaType.OPDS_AUTHENTICATION)
        }

        return Try.failure(MediaTypeSnifferError.NotRecognized)
    }

    override suspend fun sniffBlob(source: Readable): Try<MediaType, MediaTypeSnifferError> {
        if (!source.canReadWholeBlob()) {
            return Try.failure(MediaTypeSnifferError.NotRecognized)
        }

        // OPDS 1
        source.readAsXml()
            .getOrElse {
                when (it) {
                    is DecodeError.Reading ->
                        return Try.failure(MediaTypeSnifferError.Reading(it.cause))
                    is DecodeError.Decoding ->
                        null
                }
            }
            ?.takeIf { it.namespace == "http://www.w3.org/2005/Atom" }
            ?.let { xml ->
                if (xml.name == "feed") {
                    return Try.success(MediaType.OPDS1)
                } else if (xml.name == "entry") {
                    return Try.success(MediaType.OPDS1_ENTRY)
                }
            }

        // OPDS 2
        source.readAsRwpm()
            .getOrElse {
                when (it) {
                    is DecodeError.Reading ->
                        return Try.failure(MediaTypeSnifferError.Reading(it.cause))
                    is DecodeError.Decoding ->
                        null
                }
            }
            ?.let { rwpm ->
                if (rwpm.linkWithRel("self")?.mediaType?.matches("application/opds+json") == true
                ) {
                    return Try.success(MediaType.OPDS2)
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
                    return Try.success(MediaType.OPDS2_PUBLICATION)
                }
            }

        // OPDS Authentication Document.
        source.containsJsonKeys("id", "title", "authentication")
            .getOrElse {
                when (it) {
                    is DecodeError.Reading ->
                        return Try.failure(MediaTypeSnifferError.Reading(it.cause))

                    is DecodeError.Decoding ->
                        null
                }
            }
            ?.takeIf { it }
            ?.let { return Try.success(MediaType.OPDS_AUTHENTICATION) }

        return Try.failure(MediaTypeSnifferError.NotRecognized)
    }
}

/** Sniffs an LCP License Document. */
public object LcpLicenseMediaTypeSniffer : MediaTypeSniffer {
    override fun sniffHints(hints: MediaTypeHints): Try<MediaType, MediaTypeSnifferError.NotRecognized> {
        if (
            hints.hasFileExtension("lcpl") ||
            hints.hasMediaType("application/vnd.readium.lcp.license.v1.0+json")
        ) {
            return Try.success(MediaType.LCP_LICENSE_DOCUMENT)
        }

        return Try.failure(MediaTypeSnifferError.NotRecognized)
    }

    override suspend fun sniffBlob(source: Readable): Try<MediaType, MediaTypeSnifferError> {
        if (!source.canReadWholeBlob()) {
            return Try.failure(MediaTypeSnifferError.NotRecognized)
        }

        source.containsJsonKeys("id", "issued", "provider", "encryption")
            .getOrElse {
                when (it) {
                    is DecodeError.Reading ->
                        return Try.failure(MediaTypeSnifferError.Reading(it.cause))

                    is DecodeError.Decoding ->
                        null
                }
            }
            ?.takeIf { it }
            ?.let { return Try.success(MediaType.LCP_LICENSE_DOCUMENT) }

        return Try.failure(MediaTypeSnifferError.NotRecognized)
    }
}

/** Sniffs a bitmap image. */
public object BitmapMediaTypeSniffer : MediaTypeSniffer {
    override fun sniffHints(hints: MediaTypeHints): Try<MediaType, MediaTypeSnifferError.NotRecognized> {
        if (
            hints.hasFileExtension("avif") ||
            hints.hasMediaType("image/avif")
        ) {
            return Try.success(MediaType.AVIF)
        }
        if (
            hints.hasFileExtension("bmp", "dib") ||
            hints.hasMediaType("image/bmp", "image/x-bmp")
        ) {
            return Try.success(MediaType.BMP)
        }
        if (
            hints.hasFileExtension("gif") ||
            hints.hasMediaType("image/gif")
        ) {
            return Try.success(MediaType.GIF)
        }
        if (
            hints.hasFileExtension("jpg", "jpeg", "jpe", "jif", "jfif", "jfi") ||
            hints.hasMediaType("image/jpeg")
        ) {
            return Try.success(MediaType.JPEG)
        }
        if (
            hints.hasFileExtension("jxl") ||
            hints.hasMediaType("image/jxl")
        ) {
            return Try.success(MediaType.JXL)
        }
        if (
            hints.hasFileExtension("png") ||
            hints.hasMediaType("image/png")
        ) {
            return Try.success(MediaType.PNG)
        }
        if (
            hints.hasFileExtension("tiff", "tif") ||
            hints.hasMediaType("image/tiff", "image/tiff-fx")
        ) {
            return Try.success(MediaType.TIFF)
        }
        if (
            hints.hasFileExtension("webp") ||
            hints.hasMediaType("image/webp")
        ) {
            return Try.success(MediaType.WEBP)
        }
        return Try.failure(MediaTypeSnifferError.NotRecognized)
    }
}

/** Sniffs a Readium Web Manifest. */
public object WebPubManifestMediaTypeSniffer : MediaTypeSniffer {
    override fun sniffHints(hints: MediaTypeHints): Try<MediaType, MediaTypeSnifferError.NotRecognized> {
        if (hints.hasMediaType("application/audiobook+json")) {
            return Try.success(MediaType.READIUM_AUDIOBOOK_MANIFEST)
        }

        if (hints.hasMediaType("application/divina+json")) {
            return Try.success(MediaType.DIVINA_MANIFEST)
        }

        if (hints.hasMediaType("application/webpub+json")) {
            return Try.success(MediaType.READIUM_WEBPUB_MANIFEST)
        }

        return Try.failure(MediaTypeSnifferError.NotRecognized)
    }

    public override suspend fun sniffBlob(source: Readable): Try<MediaType, MediaTypeSnifferError> {
        if (!source.canReadWholeBlob()) {
            return Try.failure(MediaTypeSnifferError.NotRecognized)
        }

        val manifest: Manifest =
            source.readAsRwpm()
                .getOrElse {
                    when (it) {
                        is DecodeError.Reading ->
                            return Try.failure(MediaTypeSnifferError.Reading(it.cause))

                        is DecodeError.Decoding ->
                            null
                    }
                }
                ?: return Try.failure(MediaTypeSnifferError.NotRecognized)

        if (manifest.conformsTo(Publication.Profile.AUDIOBOOK)) {
            return Try.success(MediaType.READIUM_AUDIOBOOK_MANIFEST)
        }

        if (manifest.conformsTo(Publication.Profile.DIVINA)) {
            return Try.success(MediaType.DIVINA_MANIFEST)
        }
        if (manifest.linkWithRel("self")?.mediaType?.matches("application/webpub+json") == true) {
            return Try.success(MediaType.READIUM_WEBPUB_MANIFEST)
        }

        return Try.failure(MediaTypeSnifferError.NotRecognized)
    }
}

/** Sniffs a Readium Web Publication, protected or not by LCP. */
public object WebPubMediaTypeSniffer : MediaTypeSniffer {
    override fun sniffHints(hints: MediaTypeHints): Try<MediaType, MediaTypeSnifferError.NotRecognized> {
        if (
            hints.hasFileExtension("audiobook") ||
            hints.hasMediaType("application/audiobook+zip")
        ) {
            return Try.success(MediaType.READIUM_AUDIOBOOK)
        }

        if (
            hints.hasFileExtension("divina") ||
            hints.hasMediaType("application/divina+zip")
        ) {
            return Try.success(MediaType.DIVINA)
        }

        if (
            hints.hasFileExtension("webpub") ||
            hints.hasMediaType("application/webpub+zip")
        ) {
            return Try.success(MediaType.READIUM_WEBPUB)
        }

        if (
            hints.hasFileExtension("lcpa") ||
            hints.hasMediaType("application/audiobook+lcp")
        ) {
            return Try.success(MediaType.LCP_PROTECTED_AUDIOBOOK)
        }
        if (
            hints.hasFileExtension("lcpdf") ||
            hints.hasMediaType("application/pdf+lcp")
        ) {
            return Try.success(MediaType.LCP_PROTECTED_PDF)
        }

        return Try.failure(MediaTypeSnifferError.NotRecognized)
    }

    override suspend fun sniffContainer(container: Container<Readable>): Try<MediaType, MediaTypeSnifferError> {
        // Reads a RWPM from a manifest.json archive entry.
        val manifest: Manifest =
            container[RelativeUrl("manifest.json")!!]
                ?.read()
                ?.getOrElse { error ->
                    return Try.failure(MediaTypeSnifferError.Reading(error))
                }
                ?.let { tryOrNull { Manifest.fromJSON(JSONObject(String(it))) } }
                ?: return Try.failure(MediaTypeSnifferError.NotRecognized)

        val isLcpProtected = RelativeUrl("license.lcpl")!! in container

        if (manifest.conformsTo(Publication.Profile.AUDIOBOOK)) {
            return if (isLcpProtected) {
                Try.success(MediaType.LCP_PROTECTED_AUDIOBOOK)
            } else {
                Try.success(MediaType.READIUM_AUDIOBOOK)
            }
        }
        if (manifest.conformsTo(Publication.Profile.DIVINA)) {
            return Try.success(MediaType.DIVINA)
        }
        if (isLcpProtected && manifest.conformsTo(Publication.Profile.PDF)) {
            return Try.success(MediaType.LCP_PROTECTED_PDF)
        }
        if (manifest.linkWithRel("self")?.mediaType?.matches("application/webpub+json") == true) {
            return Try.success(MediaType.READIUM_WEBPUB)
        }

        return Try.failure(MediaTypeSnifferError.NotRecognized)
    }
}

/** Sniffs a W3C Web Publication Manifest. */
public object W3cWpubMediaTypeSniffer : MediaTypeSniffer {
    override suspend fun sniffBlob(source: Readable): Try<MediaType, MediaTypeSnifferError> {
        if (!source.canReadWholeBlob()) {
            return Try.failure(MediaTypeSnifferError.NotRecognized)
        }

        // Somehow, [JSONObject] can't access JSON-LD keys such as `@content`.
        val string = source.readAsString()
            .getOrElse {
                when (it) {
                    is DecodeError.Reading ->
                        return Try.failure(MediaTypeSnifferError.Reading(it.cause))

                    is DecodeError.Decoding ->
                        null
                }
            } ?: ""
        if (
            string.contains("@context") &&
            string.contains("https://www.w3.org/ns/wp-context")
        ) {
            return Try.success(MediaType.W3C_WPUB_MANIFEST)
        }

        return Try.failure(MediaTypeSnifferError.NotRecognized)
    }
}

/**
 * Sniffs an EPUB publication.
 *
 * Reference: https://www.w3.org/publishing/epub3/epub-ocf.html#sec-zip-container-mime
 */
public object EpubMediaTypeSniffer : MediaTypeSniffer {
    override fun sniffHints(hints: MediaTypeHints): Try<MediaType, MediaTypeSnifferError.NotRecognized> {
        if (
            hints.hasFileExtension("epub") ||
            hints.hasMediaType("application/epub+zip")
        ) {
            return Try.success(MediaType.EPUB)
        }

        return Try.failure(MediaTypeSnifferError.NotRecognized)
    }

    override suspend fun sniffContainer(container: Container<Readable>): Try<MediaType, MediaTypeSnifferError> {
        val mimetype = container[RelativeUrl("mimetype")!!]
            ?.readAsString(charset = Charsets.US_ASCII)
            ?.getOrElse { error ->
                when (error) {
                    is DecodeError.Decoding ->
                        null
                    is DecodeError.Reading ->
                        return Try.failure(MediaTypeSnifferError.Reading(error.cause))
                }
            }?.trim()
        if (mimetype == "application/epub+zip") {
            return Try.success(MediaType.EPUB)
        }

        return Try.failure(MediaTypeSnifferError.NotRecognized)
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
    override fun sniffHints(hints: MediaTypeHints): Try<MediaType, MediaTypeSnifferError.NotRecognized> {
        if (
            hints.hasFileExtension("lpf") ||
            hints.hasMediaType("application/lpf+zip")
        ) {
            return Try.success(MediaType.LPF)
        }

        return Try.failure(MediaTypeSnifferError.NotRecognized)
    }

    override suspend fun sniffContainer(container: Container<Readable>): Try<MediaType, MediaTypeSnifferError> {
        if (RelativeUrl("index.html")!! in container) {
            return Try.success(MediaType.LPF)
        }

        // Somehow, [JSONObject] can't access JSON-LD keys such as `@content`.
        container[RelativeUrl("publication.json")!!]
            ?.read()
            ?.getOrElse { error ->
                return Try.failure(MediaTypeSnifferError.Reading(error))
            }
            ?.let { tryOrNull { String(it) } }
            ?.let { manifest ->
                if (
                    manifest.contains("@context") &&
                    manifest.contains("https://www.w3.org/ns/pub-context")
                ) {
                    return Try.success(MediaType.LPF)
                }
            }

        return Try.failure(MediaTypeSnifferError.NotRecognized)
    }
}

/**
 * Sniffs a RAR archive.
 *
 * At the moment, only hints are supported.
 */
public object RarMediaTypeSniffer : MediaTypeSniffer {

    override fun sniffHints(hints: MediaTypeHints): Try<MediaType, MediaTypeSnifferError.NotRecognized> {
        if (
            hints.hasFileExtension("rar") ||
            hints.hasMediaType("application/vnd.rar") ||
            hints.hasMediaType("application/x-rar") ||
            hints.hasMediaType("application/x-rar-compressed")
        ) {
            return Try.success(MediaType.RAR)
        }

        return Try.failure(MediaTypeSnifferError.NotRecognized)
    }
}

/**
 * Sniffs a simple Archive-based publication format, like Comic Book Archive or Zipped Audio Book.
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

    override fun sniffHints(hints: MediaTypeHints): Try<MediaType, MediaTypeSnifferError.NotRecognized> {
        if (
            hints.hasFileExtension("cbz") ||
            hints.hasMediaType(
                "application/vnd.comicbook+zip",
                "application/x-cbz"
            )
        ) {
            return Try.success(MediaType.CBZ)
        }

        if (
            hints.hasFileExtension("cbr") ||
            hints.hasMediaType("application/vnd.comicbook-rar") ||
            hints.hasMediaType("application/x-cbr")
        ) {
            return Try.success(MediaType.CBR)
        }

        if (hints.hasFileExtension("zab")) {
            return Try.success(MediaType.ZAB)
        }

        return Try.failure(MediaTypeSnifferError.NotRecognized)
    }

    override suspend fun sniffContainer(container: Container<Readable>): Try<MediaType, MediaTypeSnifferError> {
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
            container.archiveMediaType?.matches(MediaType.ZIP) == true
        ) {
            return Try.success(MediaType.CBZ)
        }

        if (
            archiveContainsOnlyExtensions(cbzExtensions) &&
            container.archiveMediaType?.matches(MediaType.RAR) == true
        ) {
            return Try.success(MediaType.CBR)
        }

        if (
            archiveContainsOnlyExtensions(zabExtensions) &&
            container.archiveMediaType?.matches(MediaType.ZIP) == true
        ) {
            return Try.success(MediaType.ZAB)
        }

        return Try.failure(MediaTypeSnifferError.NotRecognized)
    }
}

/**
 * Sniffs a PDF document.
 *
 * Reference: https://www.loc.gov/preservation/digital/formats/fdd/fdd000123.shtml
 */
public object PdfMediaTypeSniffer : MediaTypeSniffer {
    override fun sniffHints(hints: MediaTypeHints): Try<MediaType, MediaTypeSnifferError.NotRecognized> {
        if (
            hints.hasFileExtension("pdf") ||
            hints.hasMediaType("application/pdf")
        ) {
            return Try.success(MediaType.PDF)
        }

        return Try.failure(MediaTypeSnifferError.NotRecognized)
    }

    override suspend fun sniffBlob(source: Readable): Try<MediaType, MediaTypeSnifferError> {
        source.read(0L until 5L)
            .getOrElse { error ->
                return Try.failure(MediaTypeSnifferError.Reading(error))
            }
            .let { tryOrNull { it.toString(Charsets.UTF_8) } }
            .takeIf { it == "%PDF-" }
            ?.let { return Try.success(MediaType.PDF) }

        return Try.failure(MediaTypeSnifferError.NotRecognized)
    }
}

/** Sniffs a JSON document. */
public object JsonMediaTypeSniffer : MediaTypeSniffer {
    override fun sniffHints(hints: MediaTypeHints): Try<MediaType, MediaTypeSnifferError.NotRecognized> {
        if (hints.hasMediaType("application/problem+json")) {
            return Try.success(MediaType.JSON_PROBLEM_DETAILS)
        }

        return Try.failure(MediaTypeSnifferError.NotRecognized)
    }

    override suspend fun sniffBlob(source: Readable): Try<MediaType, MediaTypeSnifferError> {
        if (!source.canReadWholeBlob()) {
            return Try.failure(MediaTypeSnifferError.NotRecognized)
        }

        source.readAsJson()
            .getOrElse {
                when (it) {
                    is DecodeError.Reading ->
                        return Try.failure(MediaTypeSnifferError.Reading(it.cause))

                    is DecodeError.Decoding ->
                        null
                }
            }
            ?.let { return Try.success(MediaType.JSON) }

        return Try.failure(MediaTypeSnifferError.NotRecognized)
    }
}

/**
 * Sniffs the system-wide registered media types using [MimeTypeMap] and
 * [URLConnection.guessContentTypeFromStream].
 */
public object SystemMediaTypeSniffer : MediaTypeSniffer {

    private val mimetypes = tryOrNull { MimeTypeMap.getSingleton() }

    override fun sniffHints(hints: MediaTypeHints): Try<MediaType, MediaTypeSnifferError.NotRecognized> {
        for (mediaType in hints.mediaTypes) {
            sniffType(mediaType.toString())
                ?.let { return Try.success(it) }
        }

        for (extension in hints.fileExtensions) {
            sniffExtension(extension)
                ?.let { return Try.success(it) }
        }

        return Try.failure(MediaTypeSnifferError.NotRecognized)
    }

    override suspend fun sniffBlob(source: Readable): Try<MediaType, MediaTypeSnifferError> {
        source.borrow()
            .asInputStream(wrapError = ::SystemSnifferException)
            .use { stream ->
                try {
                    withContext(Dispatchers.IO) {
                        URLConnection.guessContentTypeFromStream(stream)
                            ?.let { sniffType(it) }
                    }
                } catch (e: Exception) {
                    e.asInstance(SystemSnifferException::class.java)
                        ?.let {
                            return Try.failure(
                                MediaTypeSnifferError.Reading(it.error)
                            )
                        }
                }
            }
            ?.let { return Try.success(it) }

        return Try.failure(MediaTypeSnifferError.NotRecognized)
    }

    private class SystemSnifferException(
        val error: ReadError
    ) : IOException()

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

private suspend fun Readable.canReadWholeBlob() =
    length().getOrDefault(0) < 5 * 1000 * 1000
