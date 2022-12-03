/*
 * Copyright 2020 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.shared.util

import android.net.Uri
import android.net.UrlQuerySanitizer
import android.webkit.URLUtil
import java.net.IDN
import java.net.URI
import java.net.URL
import org.readium.r2.shared.extensions.addPrefix
import timber.log.Timber

/**
 * Represents an HREF, optionally relative to another one.
 *
 * This is used to normalize the string representation.
 */
class Href(
    private val href: String,
    baseHref: String = "/"
) {

    data class QueryParameter(val name: String, val value: String?)

    private val baseHref = if (baseHref.isEmpty()) "/" else baseHref

    /** Returns the normalized string representation for this HREF. */
    val string: String get() {
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

        val path = href.substring(0, splitIndex)
        val suffix = href.substring(splitIndex)

        return try {
            val uri = URI.create(baseHref.percentEncodedPath()).resolve(path.percentEncodedPath())
            val url = (if (URLUtil.isNetworkUrl(uri.toString())) uri.toString() else uri.path.addPrefix("/")) + suffix
            return url.removePercentEncoding()
        } catch (e: Exception) {
            Timber.e(e)
            "$baseHref/$href"
        }
    }

    /**
     * Returns the normalized string representation for this HREF, encoded for URL uses.
     *
     * Taken from https://stackoverflow.com/a/49796882/1474476
     */
    val percentEncodedString: String get() {
        var string = string
        if (string.startsWith("/")) {
            string = string.addPrefix("file://")
        }

        return try {
            val url = URL(string)
            val uri = URI(url.protocol, url.userInfo, IDN.toASCII(url.host), url.port, url.path, url.query, url.ref)
            uri.toASCIIString().removePrefix("file://")
        } catch (e: Exception) {
            Timber.e(e)
            this.string
        }
    }

    /** Returns the query parameters present in this HREF, in the order they appear. */
    val queryParameters: List<QueryParameter> get() {
        val url = percentEncodedString.substringBefore("#")
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
}

fun List<Href.QueryParameter>.firstNamedOrNull(name: String): String? =
    firstOrNull { it.name == name }?.value

fun List<Href.QueryParameter>.allNamed(name: String): List<String> =
    filter { it.name == name }.mapNotNull { it.value }
