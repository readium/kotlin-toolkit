/*
 * Copyright 2023 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

@file:OptIn(InternalReadiumApi::class)

package org.readium.r2.shared.util

import android.net.Uri
import android.net.UrlQuerySanitizer
import android.os.Parcelable
import java.io.File
import java.net.URI
import java.net.URL
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
    public val path: String?
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
        public val parameters: List<QueryParameter>,
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
        public val value: String?,
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

    /**
     * Normalizes the URL using a subset of the RFC-3986 rules.
     *
     * https://datatracker.ietf.org/doc/html/rfc3986#section-6
     */
    public open fun normalize(): Url =
        uri.buildUpon()
            .apply {
                path?.let {
                    var normalizedPath = File(it).normalize().path
                    if (it.endsWith("/")) {
                        normalizedPath += "/"
                    }
                    path(normalizedPath)
                }

                if (this@Url is AbsoluteUrl) {
                    scheme(scheme.value)
                }
            }
            .build()
            .toUrl()!!

    override fun toString(): String =
        uri.toString()

    /**
     * Returns whether two URLs are strictly equal, by comparing their string representation.
     *
     * WARNING: Strict URL comparisons can be a source of bug, if the URLs are not normalized.
     * In most cases, you should compare using [Url.isEquivalent].
     */
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Url

        if (uri.toString() != other.uri.toString()) return false

        return true
    }

    /**
     * Returns whether the receiver is equivalent to the given `url` after normalization.
     */
    @OptIn(DelicateReadiumApi::class)
    public fun isEquivalent(url: Url?): Boolean {
        url ?: return false
        return normalize() == url.normalize()
    }

    override fun hashCode(): Int =
        uri.toString().hashCode()

    /**
     * A URL scheme, e.g. http or file.
     */
    @JvmInline
    public value class Scheme private constructor(public val value: String) {

        public companion object {
            public operator fun invoke(scheme: String): Scheme =
                Scheme(scheme.lowercase())
        }

        override fun toString(): String = value

        public val isFile: Boolean
            get() = value == "file"

        public val isHttp: Boolean
            get() = value == "http" || value == "https"

        public val isContent: Boolean
            get() = value == "content"
    }
}

/**
 * Represents an absolute Uniform Resource Locator.
 */
@Parcelize
public class AbsoluteUrl private constructor(override val uri: Uri) : Url() {

    public companion object {

        /**
         * Creates an [AbsoluteUrl] from its encoded string representation.
         */
        public operator fun invoke(url: String): AbsoluteUrl? {
            if (!url.isValidUrl()) return null
            return invoke(Uri.parse(url))
        }

        internal operator fun invoke(uri: Uri): AbsoluteUrl? =
            tryOrNull {
                require(uri.isAbsolute)
                require(uri.isHierarchical)
                AbsoluteUrl(uri)
            }
    }

    public override fun resolve(url: Url): AbsoluteUrl =
        super.resolve(url) as AbsoluteUrl

    public override fun normalize(): AbsoluteUrl =
        super.normalize() as AbsoluteUrl

    /**
     * Identifies the type of URL.
     */
    public val scheme: Scheme
        get() = Scheme(uri.scheme!!)

    /**
     * Indicates whether this URL points to a HTTP resource.
     */
    public val isHttp: Boolean get() =
        scheme.isHttp

    /**
     * Indicates whether this URL points to a file.
     */
    public val isFile: Boolean get() =
        scheme.isFile

    /**
     * Indicates whether this URL points to an Android content resource.
     */
    public val isContent: Boolean get() =
        scheme.isContent

    /**
     * Converts the URL to a [File], if it's a file URL.
     */
    public fun toFile(): File? =
        if (isFile) File(path!!) else null
}

/**
 * Represents a relative Uniform Resource Locator.
 */
@Parcelize
public class RelativeUrl private constructor(override val uri: Uri) : Url() {

    public companion object {

        /**
         * Creates a [RelativeUrl] from its encoded string representation.
         */
        public operator fun invoke(url: String): RelativeUrl? {
            if (!url.isValidUrl()) return null
            return invoke(Uri.parse(url))
        }

        internal operator fun invoke(uri: Uri): RelativeUrl? =
            tryOrNull {
                require(uri.isRelative)
                RelativeUrl(uri)
            }
    }

    public override fun normalize(): RelativeUrl =
        super.normalize() as RelativeUrl
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
    AbsoluteUrl(href) ?: fromDecodedPath(href.removePrefix("/"))

/**
 * According to the EPUB specification, the HREFs in the EPUB package must be valid URLs (so
 * percent-encoded). Unfortunately, many EPUBs don't follow this rule, and use invalid HREFs such
 * as `my chapter.html` or `/dir/my chapter.html`.
 *
 * As a workaround, we assume the HREFs are valid percent-encoded URLs, and fallback to decoded paths
 * if we can't parse the URL.
 */
@InternalReadiumApi
public fun Url.Companion.fromEpubHref(href: String): Url? =
    Url(href) ?: fromDecodedPath(href)

public fun File.toUrl(): AbsoluteUrl =
    checkNotNull(AbsoluteUrl(Uri.fromFile(this)))

public fun Uri.toUrl(): Url? =
    Url(this)

public fun Uri.toAbsoluteUrl(): AbsoluteUrl? =
    AbsoluteUrl(this)

public fun Uri.toRelativeUrl(): RelativeUrl? =
    RelativeUrl(this)

public fun Url.toUri(): Uri =
    uri

internal fun Url.toURI(): URI =
    URI(toString())

public fun URL.toUrl(): Url? =
    Url(toUri())

public fun URL.toAbsoluteUrl(): AbsoluteUrl? =
    AbsoluteUrl(toUri())

public fun URL.toRelativeUrl(): RelativeUrl? =
    RelativeUrl(toUri())

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
    public val value: String,
) {
    override fun toString(): String = value
}

/**
 * Appends this file extension to [filename].
 */
public fun FileExtension?.appendToFilename(filename: String): String =
    this?.let { "$filename.$value" } ?: filename

/**
 * Returns whether the receiver is equivalent to the given `url` after normalization.
 */
@OptIn(DelicateReadiumApi::class)
public fun Url?.isEquivalent(url: Url?): Boolean {
    if (this == null && url == null) return true
    return this?.normalize() == url?.normalize()
}

/**
 * Returns the value of the first key matching `key` after normalization.
 */
@OptIn(DelicateReadiumApi::class)
public fun <T> Map<Url, T>.getEquivalent(key: Url): T? =
    get(key) ?: run {
        val url = key.normalize()
        keys.firstOrNull { it.normalize() == url }
            ?.let { get(it) }
    }
