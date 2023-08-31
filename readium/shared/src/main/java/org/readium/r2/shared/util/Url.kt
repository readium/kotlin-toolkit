/*
 * Copyright 2023 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.shared.util

import android.net.Uri
import java.io.File
import java.net.URI
import java.net.URL
import org.readium.r2.shared.extensions.tryOrNull

/**
 * A Uniform Resource Locator.
 *
 * https://url.spec.whatwg.org/
 */
public sealed class Url private constructor(internal val uri: Uri) {

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

        internal operator fun invoke(uri: Uri): Url? =
            tryOrNull {
                require(uri.isHierarchical)
                requireNotNull(uri.path)

                if (uri.isAbsolute) {
                    Absolute(uri)
                } else {
                    Relative(uri)
                }
            }
    }

    init {
        // Refuse URI like mailto:nobody@google.com
        require(uri.isHierarchical)
        requireNotNull(uri.path)
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
     * Extension of the filename portion of the URL path.
     */
    public val extension: String?
        get() = filename?.substringAfterLast('.', "")
            ?.takeIf { it.isNotEmpty() }

    /**
     * Resolves the given relative [url] to this URL.
     */
    public open fun resolve(url: Url): Url =
        when (url) {
            is Absolute -> url
            is Relative -> checkNotNull(URI(toString()).resolve(url.toString()).toUrl())
        }

    /**
     * Represents an absolute Uniform Resource Locator.
     */
    public class Absolute internal constructor(uri: Uri) : Url(uri) {

        init {
            require(uri.isAbsolute)
        }

        public override fun resolve(url: Url): Absolute =
            super.resolve(url) as Absolute

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
    public class Relative internal constructor(uri: Uri) : Url(uri) {
        init {
            require(uri.isRelative)
        }
    }

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

public fun File.toUrl(): Url.Absolute =
    Url(Uri.fromFile(this)) as Url.Absolute

public fun Uri.toUrl(): Url? =
    Url(this)

public fun Url.toUri(): Uri =
    uri

private fun Url.toURL(): URL =
    URL(toString())

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
