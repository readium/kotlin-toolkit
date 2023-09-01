/*
 * Copyright 2023 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.shared.util

import android.net.Uri
import android.net.UrlQuerySanitizer
import android.os.Parcelable
import java.io.File
import java.net.URI
import java.net.URL
import kotlinx.parcelize.Parcelize
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
         * Creates a [Url] from its encoded string representation.
         */
        public operator fun invoke(url: String): Url? =
            tryOrNull {
                if (url.isBlank()) {
                    return null
                }
                invoke(Uri.parse(url))
            }

        /**
         * Creates a [Url] from a percent-decoded path.
         */
        public fun fromDecodedPath(path: String): RelativeUrl =
            Url(path.percentEncodedPath()) as RelativeUrl

        internal operator fun invoke(uri: Uri): Url? =
            tryOrNull {
                require(uri.isHierarchical)
                requireNotNull(uri.path)

                if (uri.isAbsolute) {
                    AbsoluteUrl(uri)
                } else {
                    RelativeUrl(uri)
                }
            }
    }

    /**
     * Decoded path segments identifying a location.
     */
    public val path: String
        get() = uri.path!!

    /**
     * Decoded filename portion of the URL path.
     */
    public val filename: String?
        get() = if (path.endsWith("/")) {
            null
        } else {
            uri.lastPathSegment
        }

    /**
     * Remove the filename portion of the URL path.
     */
    public fun removeFilename(): Url {
        if (path.endsWith("/")) {
            return this
        }
        val filename = uri.lastPathSegment ?: return this

        return checkNotNull(invoke(uri.buildUpon().path(path.removeSuffix(filename)).build()))
    }

    /**
     * Extension of the filename portion of the URL path.
     */
    public val extension: String?
        get() = filename?.substringAfterLast('.', "")
            ?.takeIf { it.isNotEmpty() }

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
     * Resolves the given relative [url] to this URL.
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

        if (uri.toString() != other.uri.toString()) return false

        return true
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
public class AbsoluteUrl internal constructor(override val uri: Uri) : Url() {

    init {
        require(uri.isAbsolute)
    }

    public override fun resolve(url: Url): AbsoluteUrl =
        super.resolve(url) as AbsoluteUrl

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
        if (isFile) File(path) else null
}

/**
 * Represents a relative Uniform Resource Locator.
 */
@Parcelize
public class RelativeUrl internal constructor(override val uri: Uri) : Url() {
    init {
        require(uri.isRelative)
    }
}

public fun File.toUrl(): AbsoluteUrl =
    Url(Uri.fromFile(this)) as AbsoluteUrl

public fun Uri.toUrl(): Url? =
    Url(this)

public fun Url.toUri(): Uri =
    uri

private fun Url.toURI(): URI =
    URI(toString())

public fun URL.toUrl(): Url? =
    Url(Uri.parse(toString()).addFileAuthority())

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
