/*
 * Copyright 2020 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.shared.util

import android.net.Uri
import android.net.UrlQuerySanitizer
import java.io.File
import java.net.IDN
import java.net.URI
import java.net.URL
import org.readium.r2.shared.extensions.addPrefix
import org.readium.r2.shared.extensions.tryOrLog
import timber.log.Timber

/**
 * Represents an HREF in its absolute percent-decoded form.
 */
@JvmInline
public value class Href private constructor(
    public val string: String
) {

    public companion object {

        /**
         * Builds an HREF, which can be relative to a [baseHref].
         *
         * @param href The HREF string, which can be absolute or relative.
         * @param baseHref The base HREF to use when the [href] is relative.
         */
        public operator fun invoke(href: String, baseHref: String? = null): Href {
            @Suppress("Name_Shadowing")
            val baseHref: String = (baseHref?.takeUnless { it.isBlank() } ?: "/")
                .removePercentEncoding()

            // Isolates the path from the anchor/query portion, which should not be percent decoded.
            val splitIndex = href.indexOf("?").takeIf { it != -1 }
                ?: href.indexOf("#").takeIf { it != -1 }
                ?: (href.lastIndex + 1)

            val path = href.substring(0, splitIndex).removePercentEncoding()
            val suffix = href.substring(splitIndex)

            if (path.isBlank()) {
                return Href(baseHref + suffix)
            }

            // HREF is already absolute.
            if (Uri.parse(path).isAbsolute) {
                return Href(path + suffix)
            }

            return try {
                val baseUri = URI.create(baseHref.percentEncodedPath())
                if (baseUri.scheme?.lowercase() == "file") {
                    return if (path.startsWith("/")) {
                        Href("file://$path$suffix")
                    } else {
                        Href("file://" + File(baseUri.path, path).canonicalPath + suffix)
                    }
                }

                val uri = baseUri.resolve(path.percentEncodedPath())
                val url = (if (uri.scheme != null) uri.toString() else uri.path.addPrefix("/"))
                return Href(url.removePercentEncoding() + suffix)
            } catch (e: Exception) {
                Timber.e(e)
                Href("$baseHref/$path$suffix")
            }
        }

        private fun String.removePercentEncoding(): String =
            Uri.decode(this)
                // If the string contains invalid percent-encoded characters, assumes that it is already
                // percent-decoded. For example, if the string contains a standalone % character.
                .takeIf { !it.contains("\uFFFD") } ?: this

        /**
         * Percent-encodes an URL path section.
         *
         * Equivalent to Swift's `string.addingPercentEncoding(withAllowedCharacters: .urlPathAllowed)`
         */
        private fun String.percentEncodedPath(): String =
            Uri.encode(this, "$&+,/:=@")
    }

    public data class QueryParameter(val name: String, val value: String?)

    /**
     * Percent-encode the HREF to use in an URL.
     */
    public val percentEncodedString: String get() =
        percentEncode(string)

    /**
     * Returns the normalized string representation for [href], encoded for URL uses.
     *
     * Taken from https://stackoverflow.com/a/49796882/1474476
     */
    private fun percentEncode(href: String): String {
        @Suppress("NAME_SHADOWING")
        var href = href
        val hasScheme = !href.startsWith("/")
        if (!hasScheme) {
            href = href.addPrefix("file://")
        }

        tryOrLog {
            val url = URL(href)
            val uri = URI(
                url.protocol,
                url.userInfo,
                IDN.toASCII(url.host),
                url.port,
                url.path,
                url.query?.removePercentEncoding(),
                url.ref?.removePercentEncoding()
            )
            href = uri.toASCIIString()
        }

        if (!hasScheme) {
            href = href.removePrefix("file://")
        }
        return href
    }

    /** Returns the query parameters present in this HREF, in the order they appear. */
    public val queryParameters: List<QueryParameter> get() {
        val url = percentEncodedString.substringBefore("#")
        return UrlQuerySanitizer(url).parameterList
            .map { p ->
                QueryParameter(
                    name = p.mParameter,
                    value = p.mValue.takeUnless { it.isBlank() }
                )
            }
    }

    /**
     * Expands percent-encoded characters.
     */
    public fun toUrl(): Url? =
        Url(percentEncodedString)
}

public fun List<Href.QueryParameter>.firstNamedOrNull(name: String): String? =
    firstOrNull { it.name == name }?.value

public fun List<Href.QueryParameter>.allNamed(name: String): List<String> =
    filter { it.name == name }.mapNotNull { it.value }
