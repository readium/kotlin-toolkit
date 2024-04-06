/*
 * Copyright 2023 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.shared.util

import android.net.Uri
import android.net.UrlQuerySanitizer
import android.os.Parcelable
import androidx.core.net.toUri
import java.io.File
import java.net.URI
import java.net.URL
import kotlinx.parcelize.IgnoredOnParcel
import kotlinx.parcelize.Parcelize
import org.readium.r2.shared.DelicateReadiumApi
import org.readium.r2.shared.InternalReadiumApi
import org.readium.r2.shared.extensions.isPrintableAscii
import org.readium.r2.shared.extensions.percentEncodedPath
import org.readium.r2.shared.extensions.tryOrNull

/**
 * A Uniform Resource Locator.
 *
 * https://url.spec.whatwg.org/
 */
public sealed class Url : Parcelable {

    internal abstract val uri: Uri

    public companion object {

        /**
         * Creates a [RelativeUrl] from a percent-decoded path.
         */
        public fun fromDecodedPath(path: String): RelativeUrl? =
            RelativeUrl(path.percentEncodedPath())

        /**
         * Creates a [Url] from its encoded string representation.
         */
        public operator fun invoke(url: String): Url? {
            if (!url.isValidUrl()) return null
            return invoke(Uri.parse(url))
        }

        internal operator fun invoke(uri: Uri): Url? =
            if (uri.isAbsolute) {
                AbsoluteUrl(uri)
            } else {
                RelativeUrl(uri)
            }
    }

    /**
     * Decoded path segments identifying a location.
     */
    public open val path: String?
        get() = uri.path?.takeUnless { it.isBlank() }

    /**
     * Decoded filename portion of the URL path.
     */
    public val filename: String?
        get() = if (path?.endsWith("/") == true) {
            null
        } else {
            uri.lastPathSegment
        }

    /**
     * Extension of the filename portion of the URL path.
     */
    public val extension: FileExtension?
        get() = filename?.substringAfterLast('.', "")
            ?.takeIf { it.isNotEmpty() }
            ?.let { FileExtension(it) }

    /**
     * Represents a list of query parameters in a URL.
     */
    public data class Query(
        public val parameters: List<QueryParameter>
    ) {

        /**
         * Returns the first value for the parameter with the given [name].
         */
        public fun firstNamedOrNull(name: String): String? =
            parameters.firstOrNull { it.name == name }?.value

        /**
         * Returns all the values for the parameter with the given [name].
         */
        public fun allNamed(name: String): List<String> =
            parameters.filter { it.name == name }.mapNotNull { it.value }
    }

    /**
     * Represents a single query parameter and its value in a URL.
     */
    public data class QueryParameter(
        public val name: String,
        public val value: String?
    )

    /**
     * Returns the decoded query parameters present in this URL, in the order they appear.
     */
    @InternalReadiumApi
    public val query: Query get() =
        Query(
            UrlQuerySanitizer(removeFragment().toString()).parameterList
                .map { p ->
                    QueryParameter(
                        name = p.mParameter,
                        value = p.mValue.takeUnless { it.isBlank() }
                    )
                }
        )

    /**
     * Returns a copy of this URL after dropping its query.
     */
    public fun removeQuery(): Url =
        if (uri.query == null) {
            this
        } else {
            checkNotNull(invoke(uri.buildUpon().clearQuery().build()))
        }

    /**
     * Returns the decoded fragment present in this URL, if any.
     */
    public val fragment: String? get() =
        uri.fragment?.takeUnless { it.isBlank() }

    /**
     * Returns a copy of this URL after dropping its fragment.
     */
    public fun removeFragment(): Url =
        if (fragment == null) {
            this
        } else {
            // FIXME: Check URL with only a fragment #id
            checkNotNull(invoke(uri.buildUpon().fragment(null).build()))
        }

    /**
     * Resolves the given [url] to this URL.
     *
     * For example:
     *     this = "http://example.com/foo/"
     *     url = "bar/baz"
     *     result = "http://example.com/foo/bar/baz"
     */
    public open fun resolve(url: Url): Url =
        when (url) {
            is AbsoluteUrl -> url
            is RelativeUrl -> checkNotNull(toURI().resolve(url.toURI()).toUrl())
        }

    /**
     * Relativizes the given [url] against this URL.
     *
     * For example:
     *     this = "http://example.com/foo"
     *     url = "http://example.com/foo/bar/baz"
     *     result = "bar/baz"
     */
    public open fun relativize(url: Url): Url =
        checkNotNull(toURI().relativize(url.toURI()).toUrl())

    override fun toString(): String =
        uri.toString()

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Url

        return uri.toString() == other.uri.toString()
    }

    override fun hashCode(): Int =
        uri.toString().hashCode()

    /**
     * Returns an [RelativeUrl] if this URL is a relative URL, or `null` otherwise.
     */
    public fun toRelativeUrl(): RelativeUrl? =
        if (this is RelativeUrl) {
            this
        } else {
            RelativeUrl(uri)
        }

    /**
     * Returns an [AbsoluteUrl] if this URL is an absolute URL, or `null` otherwise.
     */
    public fun toAbsoluteUrl(): AbsoluteUrl? =
        if (this is AbsoluteUrl) {
            this
        } else {
            AbsoluteUrl(uri)
        }

    /**
     * Returns a [FileUrl] if this URL is a file URL, or `null` otherwise.
     */
    public fun toFileUrl(): FileUrl? =
        if (this is FileUrl) {
            this
        } else {
            FileUrl(uri)
        }

    /**
     * Returns an [HttpUrl] if this URL is an HTTP URL, or `null` otherwise.
     */
    public fun toHttpUrl(): HttpUrl? =
        if (this is HttpUrl) {
            this
        } else {
            HttpUrl(uri)
        }

    /**
     * Returns a [ContentUrl] if this URL is a content URL, or `null` otherwise.
     */
    public fun toContentUrl(): ContentUrl? =
        if (this is ContentUrl) {
            this
        } else {
            ContentUrl(uri)
        }
}

/**
 * Represents a relative Uniform Resource Locator.
 */
@Parcelize
public class RelativeUrl private constructor(override val uri: Uri) : Url() {

    public companion object {
        /**
         * Creates a [RelativeUrl] from a [Uri].
         */
        internal operator fun invoke(uri: Uri): RelativeUrl? =
            tryOrNull { RelativeUrl(uri) }

        /**
         * Creates a [RelativeUrl] from its encoded string representation.
         */
        public operator fun invoke(url: String): RelativeUrl? {
            if (!url.isValidUrl()) return null
            return invoke(Uri.parse(url))
        }
    }

    init {
        require(uri.isRelative)
    }
}

/**
 * Represents an absolute Uniform Resource Locator with a scheme.
 */
public abstract class AbsoluteUrl : Url() {

    /**
     * A URL scheme, e.g. http or file.
     */
    @JvmInline
    @Parcelize
    public value class Scheme private constructor(public val value: String) : Parcelable {

        public companion object {
            public operator fun invoke(scheme: String): Scheme =
                Scheme(scheme.lowercase())

            public val CONTENT: Scheme = Scheme("content")
            public val DATA: Scheme = Scheme("data")
            public val FILE: Scheme = Scheme("file")
            public val FTP: Scheme = Scheme("ftp")
            public val HTTP: Scheme = Scheme("http")
            public val HTTPS: Scheme = Scheme("https")
            public val OPDS: Scheme = Scheme("opds")
        }

        override fun toString(): String = value
    }

    public companion object {
        /**
         * Creates an [AbsoluteUrl] from a [Uri].
         */
        internal operator fun invoke(uri: Uri): AbsoluteUrl? =
            when (Scheme(uri.scheme ?: "")) {
                Scheme.HTTP, Scheme.HTTPS -> HttpUrl(uri)
                Scheme.FILE -> FileUrl(uri)
                Scheme.CONTENT -> ContentUrl(uri)
                else -> tryOrNull {
                    require(uri.isAbsolute)
                    require(uri.isHierarchical)
                    AbsoluteUrl(uri)
                }
            }

        /**
         * Creates an [AbsoluteUrl] from its encoded string representation.
         */
        public operator fun invoke(url: String): AbsoluteUrl? {
            if (!url.isValidUrl()) return null
            return invoke(Uri.parse(url))
        }
    }

    /**
     * Identifies the type of URL.
     */
    public abstract val scheme: Scheme

    /**
     * Origin of the URL.
     *
     * [See the specification](https://url.spec.whatwg.org/#origin).
     */
    public abstract val origin: String?

    public override fun resolve(url: Url): AbsoluteUrl =
        super.resolve(url) as AbsoluteUrl
}

/**
 * Represents an absolute URL with the special scheme `file`.
 *
 * See [the specification](https://url.spec.whatwg.org/#special-scheme).
 */
@Parcelize
public class FileUrl private constructor(
    override val uri: Uri
) : AbsoluteUrl() {

    public companion object {
        /**
         * Creates a file URL from a [Uri].
         */
        internal operator fun invoke(uri: Uri): FileUrl? =
            tryOrNull { FileUrl(uri) }

        /**
         * Creates a file URL from a [File].
         */
        public operator fun invoke(file: File): FileUrl =
            FileUrl(file.toUri())

        /**
         * Creates a file URL from a percent-decoded absolute path.
         */
        public operator fun invoke(path: String): FileUrl? {
            if (path == "/" || path.isBlank() || !path.startsWith("/")) {
                return null
            }
            return FileUrl(File(path))
        }
    }

    init {
        require(uri.scheme?.lowercase() == "file")
        require(uri.path != null)
    }

    override val scheme: Scheme get() = Scheme.FILE
    override val origin: String? get() = null
    override val path: String get() = uri.path!!

    /**
     * Converts the URL to a [File].
     */
    public fun toFile(): File = File(path)
}

/**
 * Represents an absolute URL with the special schemes `http` or `https`.
 *
 * See [the specification](https://url.spec.whatwg.org/#special-scheme).
 */
@Parcelize
public class HttpUrl private constructor(
    override val uri: Uri
) : AbsoluteUrl() {

    public companion object {
        /**
         * Creates an HTTP URL from a [Uri].
         */
        internal operator fun invoke(uri: Uri): HttpUrl? =
            tryOrNull { HttpUrl(uri) }
    }

    @IgnoredOnParcel
    override val scheme: Scheme =
        Scheme(requireNotNull(uri.scheme))

    @IgnoredOnParcel
    override val origin: String = buildString {
        append("$scheme://")
        uri.host?.let { host ->
            append(host)
            uri.port.takeIf { it != -1 }?.let { port ->
                append(":$port")
            }
        }
    }

    init {
        require(scheme == Scheme.HTTP || scheme == Scheme.HTTPS)
    }
}

/**
 * Represents an absolute URL with the special scheme `content`.
 *
 * See [the specification](https://url.spec.whatwg.org/#special-scheme).
 */
@Parcelize
public class ContentUrl private constructor(
    override val uri: Uri
) : AbsoluteUrl() {

    public companion object {
        /**
         * Creates a Content URL from a [Uri].
         */
        internal operator fun invoke(uri: Uri): ContentUrl? =
            tryOrNull { ContentUrl(uri) }
    }

    init {
        require(uri.scheme?.lowercase() == "content")
    }

    @IgnoredOnParcel
    override val scheme: Scheme get() = Scheme.CONTENT

    @IgnoredOnParcel
    override val origin: String get() = "content://$authority"

    /**
     * A string that identifies the entire content provider.
     */
    @IgnoredOnParcel
    public val authority: String = requireNotNull(uri.host)
}

/**
 * Creates an [Url] from a legacy HREF.
 *
 * For example, if it is a relative path such as `/dir/my chapter.html`, it will be
 * converted to the valid relative URL `dir/my%20chapter.html`.
 *
 * Only use this API when you are upgrading to Readium 3.x and migrating the HREFs stored in
 * your database. See the 3.0 migration guide for more information.
 */
@DelicateReadiumApi
public fun Url.Companion.fromLegacyHref(href: String): Url? =
    AbsoluteUrl(href) ?: Url.fromDecodedPath(href.removePrefix("/"))

/**
 * According to the EPUB specification, the HREFs in the EPUB package must be valid URLs (so
 * percent-encoded). Unfortunately, many EPUBs don't follow this rule, and use invalid HREFs such
 * as `my chapter.html` or `/dir/my chapter.html`.
 *
 * As a workaround, we assume the HREFs are valid percent-encoded URLs, and fallback to decoded
 * paths if we can't parse the URL.
 */
@InternalReadiumApi
public fun Url.Companion.fromEpubHref(href: String): Url? {
    return (Url(href) ?: Url.fromDecodedPath(href))
}

public fun File.toUrl(): FileUrl =
    FileUrl(this)

public fun Uri.toUrl(): Url? =
    Url(this)

public fun Url.toUri(): Uri =
    uri

internal fun Url.toURI(): URI =
    URI(toString())

public fun URL.toUrl(): Url? =
    Url(toUri())

private fun URL.toUri(): Uri =
    Uri.parse(toString()).addFileAuthority()

public fun URI.toUrl(): Url? =
    Url(Uri.parse(toString()).addFileAuthority())

/**
 * [URL] and [URI] can return a file URL without the empty authority, which is invalid.
 *
 * This method adds the empty authority if needed, for example:
 * `file:/path/to/file` becomes `file:///path/to/file`
 */
private fun Uri.addFileAuthority(): Uri =
    if (scheme?.lowercase() != "file" || authority != null) {
        this
    } else {
        buildUpon().authority("").build()
    }

private fun String.isValidUrl(): Boolean =
    // Uri.parse doesn't really validate the URL, it could contain invalid characters, so we use
    // URI. However, URI allows some non-ASCII characters.
    isNotBlank() && isPrintableAscii() && tryOrNull { URI(this) } != null

@JvmInline
public value class FileExtension(
    public val value: String
) {
    override fun toString(): String = value
}

/**
 * Appends this file extension to [filename].
 */
public fun FileExtension?.appendToFilename(filename: String): String =
    this?.let { "$filename.$value" } ?: filename
