/*
 * Copyright 2023 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

@file:OptIn(InternalReadiumApi::class)

package org.readium.r2.shared.util

import com.eygraber.uri.Uri
import kotlin.jvm.JvmInline
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
            uri.encodedQuery
                ?.split('&')
                ?.filter { it.isNotEmpty() }
                ?.map { parameter ->
                    val name = parameter.substringBefore('=')
                    val value = parameter
                        .substringAfter('=', missingDelimiterValue = "")

                    QueryParameter(
                        name = Uri.decode(name),
                        value = Uri.decode(value).takeUnless { it.isBlank() }
                    )
                }
                ?: emptyList()
        )

    /**
     * Returns a copy of this URL after dropping its query.
     */
    public open fun removeQuery(): Url =
        if (uri.query == null) {
            this
        } else {
            checkNotNull(invoke(uri.buildUpon().clearQuery().build()))
        }

    /**
     * Returns a copy of this URL after adding the given decoded fragment..
     */
    @InternalReadiumApi
    public fun addFragment(fragment: String): Url =
        checkNotNull(
            invoke(
                this.uri.buildUpon()
                    .fragment(fragment)
                    .build()
            )
        )

    /**
     * Returns the decoded fragment present in this URL, if any.
     */
    public val fragment: String? get() =
        uri.fragment?.takeUnless { it.isBlank() }

    /**
     * Returns a copy of this URL after dropping its fragment.
     */
    public open fun removeFragment(): Url =
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
            is RelativeUrl -> checkNotNull(invoke(uri.resolve(url.uri)))
        }

    /**
     * Relativizes the given [url] against this URL.
     *
     * For example:
     *     this = "http://example.com/foo/"
     *     url = "http://example.com/foo/bar/baz"
     *     result = "bar/baz"
     */
    public open fun relativize(url: Url): Url =
        uri.relativize(url.uri)
            ?.let { checkNotNull(invoke(it)) }
            ?: url

    /**
     * Normalizes the URL using a subset of the RFC-3986 rules.
     *
     * https://datatracker.ietf.org/doc/html/rfc3986#section-6
     */
    public open fun normalize(): Url =
        uri.buildUpon()
            .apply {
                path?.let {
                    var normalizedPath = it.normalizePathSegments()
                    if (it.endsWith("/")) {
                        // FIXME: A path reduced to "/" gets a second separator appended (e.g.
                        //  `http://example.com/` becomes `http://example.com//`). This quirk is
                        //  inherited from the historical `java.io.File.normalize()` implementation
                        //  and preserved by the characterization tests for now.
                        normalizedPath += "/"
                    }
                    path(normalizedPath)
                }

                if (this@Url is AbsoluteUrl) {
                    scheme(scheme.value)
                }
            }
            .build()
            .let { checkNotNull(invoke(it)) }

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
        if (other == null || this::class != other::class) return false

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
public class AbsoluteUrl private constructor(private val url: String) : Url() {

    @IgnoredOnParcel
    override val uri: Uri = Uri.parse(url)

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
                AbsoluteUrl(uri.toString())
            }
    }

    public override fun resolve(url: Url): AbsoluteUrl =
        super.resolve(url) as AbsoluteUrl

    public override fun normalize(): AbsoluteUrl =
        super.normalize() as AbsoluteUrl

    public override fun removeFragment(): AbsoluteUrl =
        super.removeFragment() as AbsoluteUrl

    public override fun removeQuery(): AbsoluteUrl =
        super.removeQuery() as AbsoluteUrl

    /**
     * Identifies the type of URL.
     */
    public val scheme: Url.Scheme
        get() = Url.Scheme(uri.scheme!!)

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
     * Hostname of the URL.
     */
    public val host: String? get() = uri.host
}

/**
 * Represents a relative Uniform Resource Locator.
 */
@Parcelize
public class RelativeUrl private constructor(private val url: String) : Url() {

    @IgnoredOnParcel
    override val uri: Uri = Uri.parse(url)

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
                RelativeUrl(uri.toString())
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

/**
 * Resolves [reference] against this base URI, using the RFC 3986 §5.2 algorithm, adjusted to
 * preserve the relativity of paths like `java.net.URI` does (e.g. resolving `../quz/baz` against
 * `foo/bar` yields `quz/baz` instead of `/quz/baz`).
 *
 * [reference] must be a relative URI.
 */
private fun Uri.resolve(reference: Uri): Uri {
    val builder = Uri.Builder()
    scheme?.let { builder.scheme(it) }

    when {
        reference.encodedAuthority != null -> {
            builder.encodedAuthority(reference.encodedAuthority)
            builder.encodedPath(reference.encodedPath?.removeDotSegments())
            builder.encodedQuery(reference.encodedQuery)
        }
        reference.encodedPath.isNullOrEmpty() -> {
            builder.encodedAuthority(encodedAuthority)
            builder.encodedPath(encodedPath)
            builder.encodedQuery(reference.encodedQuery ?: encodedQuery)
        }
        else -> {
            builder.encodedAuthority(encodedAuthority)
            val referencePath = checkNotNull(reference.encodedPath)
            val mergedPath =
                if (referencePath.startsWith("/")) {
                    referencePath
                } else {
                    mergePathWith(referencePath)
                }
            builder.encodedPath(mergedPath.removeDotSegments())
            builder.encodedQuery(reference.encodedQuery)
        }
    }

    builder.encodedFragment(reference.encodedFragment)
    return builder.build()
}

/**
 * Merges the path of a relative reference with the receiver's path, per RFC 3986 §5.2.3.
 */
private fun Uri.mergePathWith(referencePath: String): String {
    val basePath = encodedPath.orEmpty()
    if (encodedAuthority != null && basePath.isEmpty()) {
        return "/$referencePath"
    }
    val lastSlashIndex = basePath.lastIndexOf('/')
    return if (lastSlashIndex == -1) {
        referencePath
    } else {
        basePath.substring(0, lastSlashIndex + 1) + referencePath
    }
}

/**
 * Relativizes [child] against this base URI, mimicking `java.net.URI.relativize()`.
 *
 * Returns null when [child] cannot be relativized against the receiver.
 */
private fun Uri.relativize(child: Uri): Uri? {
    if (isOpaque || child.isOpaque) return null
    if (scheme?.lowercase() != child.scheme?.lowercase()) return null
    if (encodedAuthority != child.encodedAuthority) return null

    var basePath = encodedPath.orEmpty().removeDotSegments()
    val childPath = child.encodedPath.orEmpty().removeDotSegments()
    if (basePath != childPath) {
        if (!basePath.endsWith("/")) {
            basePath += "/"
        }
        if (!childPath.startsWith(basePath)) {
            return null
        }
    }

    return Uri.Builder()
        .encodedPath(childPath.substring(basePath.length.coerceAtMost(childPath.length)))
        .encodedQuery(child.encodedQuery)
        .encodedFragment(child.encodedFragment)
        .build()
}

/**
 * Collapses the `.` and `..` segments of a path, mirroring the behavior of
 * `java.io.File.normalize()`:
 * - duplicate separators are collapsed,
 * - leading `..` segments which cannot be resolved are retained,
 * - no trailing separator is produced.
 */
internal fun String.normalizePathSegments(): String {
    val segments = mutableListOf<String>()
    for (segment in split('/')) {
        when (segment) {
            "", "." -> {}
            ".." ->
                if (segments.isNotEmpty() && segments.last() != "..") {
                    segments.removeAt(segments.lastIndex)
                } else {
                    segments.add("..")
                }
            else -> segments.add(segment)
        }
    }
    return (if (startsWith("/")) "/" else "") + segments.joinToString("/")
}

/**
 * Removes the `.` and `..` segments of a path, mimicking `java.net.URI.normalize()`: like
 * [normalizePathSegments], but a trailing `.` or `..` segment which consumed a real segment
 * produces a trailing separator.
 *
 * For example:
 * - `a/b/..` becomes `a/` (the trailing `..` consumed `b`, so the separator is kept),
 * - `a/b/../` stays `a/`,
 * - `a/./b` becomes `a/b` (the `.` is not trailing),
 * - `..` stays `..` (nothing was consumed: the result still ends with a literal `..`,
 *   detected by the `result.substringAfterLast('/') != ".."` check below).
 */
private fun String.removeDotSegments(): String {
    var result = normalizePathSegments()

    // Last segment of the original path, ignoring any trailing separator.
    val lastSegment = trimEnd('/').substringAfterLast('/')
    val endsWithSlash = endsWith("/") ||
        ((lastSegment == "." || lastSegment == "..") && result.substringAfterLast('/') != "..")

    if (endsWithSlash && result.isNotEmpty() && !result.endsWith("/")) {
        result += "/"
    }
    return result
}

private fun String.isValidUrl(): Boolean {
    // We approximate the validation performed by `java.net.URI`: only characters from the
    // RFC 3986 sets (plus `%` and `#`) are accepted, and percent escapes must be well-formed.
    // In particular, whitespace and non-ASCII characters are rejected.
    if (isBlank() || !isPrintableAscii()) return false

    for ((index, char) in withIndex()) {
        val isValidChar =
            char in 'a'..'z' || char in 'A'..'Z' || char in '0'..'9' ||
                char in "-._~:/?#[]@!$&'()*+,;=%"
        if (!isValidChar) return false

        if (char == '%' &&
            (
                index + 2 >= length ||
                    !this[index + 1].isHexDigit() ||
                    !this[index + 2].isHexDigit()
                )
        ) {
            return false
        }
    }
    return true
}

private fun Char.isHexDigit(): Boolean =
    this in '0'..'9' || this in 'a'..'f' || this in 'A'..'F'

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
