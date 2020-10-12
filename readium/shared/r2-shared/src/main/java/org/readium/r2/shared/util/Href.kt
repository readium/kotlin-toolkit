/*
 * Copyright 2020 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.shared.util

import android.net.Uri
import android.net.UrlQuerySanitizer
import org.readium.r2.shared.extensions.addPrefix
import timber.log.Timber
import java.net.IDN
import java.net.URI
import java.net.URL
import java.net.URLDecoder

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
        if (href.isBlank()) {
            return baseHref
        }

        val resolved =
            try {
                val absoluteUri = URI.create(baseHref).resolve(href)
                val absoluteString = absoluteUri.toString() // This is percent-decoded
                val addSlash = absoluteUri.scheme == null && !absoluteString.startsWith("/")
                (if (addSlash) "/" else "") + absoluteString

            } catch (e: IllegalArgumentException) {
                try {
                    // Android's Uri is more forgiving than URI.
                    val hrefUri = Uri.parse(href)
                    when {
                        hrefUri.isAbsolute -> href
                        baseHref.startsWith("/") -> baseHref + href
                        else -> "/$baseHref$href"
                    }
                } catch (e: Exception) {
                    if (href.startsWith("http://") || href.startsWith("https://"))
                        href
                    else
                        baseHref.removeSuffix("/") + href.addPrefix("/")
                }
            }

        return URLDecoder.decode(resolved, "UTF-8")
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
    val queryParameters: List<QueryParameter> get() =
        UrlQuerySanitizer(percentEncodedString).parameterList
            .map { QueryParameter(name = it.mParameter, value = it.mValue) }

}

fun List<Href.QueryParameter>.firstNamedOrNull(name: String): String? =
    firstOrNull { it.name == name }?.value

fun List<Href.QueryParameter>.allNamed(name: String): List<String> =
    filter { it.name == name }.mapNotNull { it.value }
