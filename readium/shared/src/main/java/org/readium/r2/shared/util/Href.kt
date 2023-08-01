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
import timber.log.Timber

/**
 * Represents an HREF, which can be relative to a [baseHref].
 *
 * @param href The HREF string, which can be absolute or relative.
 * @param baseHref The base HREF to use when the [href] is relative.
 */
public class Href(
    public val href: String,
    baseHref: String? = null
) {

    public data class QueryParameter(val name: String, val value: String?)

    public val baseHref: String = baseHref?.takeUnless { it.isBlank() } ?: "/"

    @Deprecated("Use `absoluteHref` instead", ReplaceWith("absoluteHref()"), DeprecationLevel.ERROR)
    public val string: String get() = absoluteHref()
    @Deprecated("Use `absoluteHref(percentEncoded = true)` instead", ReplaceWith("absoluteHref(percentEncoded = true)"), DeprecationLevel.ERROR)
    public val percentEncodedString: String get() = absoluteHref(percentEncoded = true)

    /**
     * Returns the normalized string representation for this HREF.
     *
     * @param percentEncoded Percent-encode the HREF for URL uses.
     */
    public fun absoluteHref(percentEncoded: Boolean = false): String {
        var href = rawAbsoluteHref()
        if (percentEncoded) {
            href = percentEncode(href)
        }
        return href
    }

    private fun rawAbsoluteHref(): String {
        val baseHref = baseHref.removePercentEncoding()
        val href = href.removePercentEncoding()

        // HREF is just an anchor inside the base.
        if (href.isBlank() || href.startsWith("#")) {
            return baseHref + href
        }

        // HREF is already absolute.
        if (Uri.parse(href).isAbsolute) {
            return href
        }

        // Isolates the path from the anchor/query portion, which would be lost otherwise.
        val splitIndex = href.indexOf("?").takeIf { it != -1 }
            ?: href.indexOf("#").takeIf { it != -1 }
            ?: (href.lastIndex + 1)

        return try {
            val baseUri = URI.create(baseHref.percentEncodedPath())
            if (baseUri.scheme?.lowercase() == "file") {
                return if (href.startsWith("/")) {
                    "file://$href"
                } else {
                    "file://" + File(baseUri.path, href).canonicalPath
                }
            }

            val path = href.substring(0, splitIndex)
            val suffix = href.substring(splitIndex)
            val uri = baseUri.resolve(path.percentEncodedPath())
            val url = (if (uri.scheme != null) uri.toString() else uri.path.addPrefix("/")) + suffix
            return url.removePercentEncoding()
        } catch (e: Exception) {
            Timber.e(e)
            "$baseHref/$href"
        }
    }

    /**
     * Returns the normalized string representation for [href], encoded for URL uses.
     *
     * Taken from https://stackoverflow.com/a/49796882/1474476
     */
    private fun percentEncode(href: String): String {
        var string = href
        val hasScheme = !string.startsWith("/")
        if (!hasScheme) {
            string = string.addPrefix("file://")
        }

        return try {
            val url = URL(string)
            val uri = URI(url.protocol, url.userInfo, IDN.toASCII(url.host), url.port, url.path, url.query, url.ref)
            var result = uri.toASCIIString()
            if (!hasScheme) {
                result = result.removePrefix("file://")
            }
            return result
        } catch (e: Exception) {
            Timber.e(e)
            href
        }
    }

    /** Returns the query parameters present in this HREF, in the order they appear. */
    public val queryParameters: List<QueryParameter> get() {
        val url = absoluteHref(percentEncoded = true).substringBefore("#")
        return UrlQuerySanitizer(url).parameterList
            .map { p -> QueryParameter(name = p.mParameter, value = p.mValue.takeUnless { it.isBlank() }) }
    }

    /**
     * Percent-encodes an URL path section.
     *
     * Equivalent to Swift's `string.addingPercentEncoding(withAllowedCharacters: .urlPathAllowed)`
     */
    private fun String.percentEncodedPath(): String =
        Uri.encode(this, "$&+,/:=@")

    /**
     * Expands percent-encoded characters.
     */
    private fun String.removePercentEncoding(): String =
        Uri.decode(this)
            // If the string contains invalid percent-encoded characters, assumes that it is already
            // percent-decoded. For example, if the string contains a standalone % character.
            .takeIf { !it.contains("\uFFFD") } ?: this

    public fun toUrl(): Url? =
        Url(absoluteHref(percentEncoded = true))

    override fun toString(): String =
        absoluteHref()
}

public fun List<Href.QueryParameter>.firstNamedOrNull(name: String): String? =
    firstOrNull { it.name == name }?.value

public fun List<Href.QueryParameter>.allNamed(name: String): List<String> =
    filter { it.name == name }.mapNotNull { it.value }
