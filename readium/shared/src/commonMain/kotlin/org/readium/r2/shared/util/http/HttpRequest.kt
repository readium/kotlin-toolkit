/*
 * Copyright 2023 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.shared.util.http

import kotlin.time.Duration
import org.readium.r2.shared.extensions.toMutable
import org.readium.r2.shared.util.AbsoluteUrl

/**
 * Holds the information about an HTTP request performed by an [HttpClient].
 *
 * @param url Address of the remote resource to request.
 * @param method HTTP method to use for the request.
 * @param headers Additional HTTP headers to use.
 * @param body Content put in the body of the HTTP request.
 * @param extras Map of additional information, which might be used by a specific implementation
 *        of HTTPClient.
 * @param connectTimeout Timeout used when establishing a connection to the resource. A null timeout
 *        is interpreted as the default value, while a timeout of zero as an infinite timeout.
 * @param readTimeout Timeout used when reading the input stream. A null timeout is interpreted
 *        as the default value, while a timeout of zero as an infinite timeout.
 * @param allowUserInteraction If true, the user might be presented with interactive dialogs, such
 *        as popping up an authentication dialog.
 */
public class HttpRequest(
    public val url: AbsoluteUrl,
    public val method: Method = Method.GET,
    public val headers: Map<String, List<String>> = mapOf(),
    public val body: Body? = null,
    public val extras: Map<String, String> = mapOf(),
    public val connectTimeout: Duration? = null,
    public val readTimeout: Duration? = null,
    public val allowUserInteraction: Boolean = false,
) {

    /** Supported HTTP methods. */
    public enum class Method {
        DELETE,
        GET,
        HEAD,
        PATCH,
        POST,
        PUT,
    }

    /** Supported body values. */
    public sealed class Body {
        public class Bytes(public val bytes: ByteArray) : Body()

        /**
         * Body streamed from the file at the given `file://` [url].
         */
        public class File(public val url: AbsoluteUrl) : Body()
    }

    public fun buildUpon(): Builder = Builder(
        url = url,
        method = method,
        headers = headers.toMutable(),
        body = body,
        extras = extras.toMutableMap(),
        connectTimeout = connectTimeout,
        readTimeout = readTimeout,
        allowUserInteraction = allowUserInteraction
    )

    public fun copy(build: Builder.() -> Unit): HttpRequest =
        buildUpon().apply(build).build()

    public companion object {
        public operator fun invoke(url: AbsoluteUrl, build: Builder.() -> Unit): HttpRequest =
            Builder(url).apply(build).build()
    }

    public class Builder(
        public val url: AbsoluteUrl,
        public var method: Method = Method.GET,
        public var headers: MutableMap<String, MutableList<String>> = mutableMapOf(),
        public var body: Body? = null,
        public var extras: MutableMap<String, String> = mutableMapOf(),
        public var connectTimeout: Duration? = null,
        public var readTimeout: Duration? = null,
        public var allowUserInteraction: Boolean = false,
    ) {

        private val uriBuilder = url.uri.buildUpon()
        private var uriModified: Boolean = false

        public fun appendQueryParameter(key: String, value: String?): Builder {
            if (value != null) {
                uriBuilder.appendQueryParameter(key, value)
                uriModified = true
            }
            return this
        }

        public fun appendQueryParameters(params: Map<String, String?>): Builder {
            for ((key, value) in params) {
                appendQueryParameter(key, value)
            }
            return this
        }

        /**
         * Sets header with key [key] to [values] overriding current values, if any.
         */
        public fun setHeader(key: String, values: List<String>): Builder {
            headers[key] = values.toMutableList()
            return this
        }

        /**
         * Sets header with [key] to [value] overriding current values, if any.
         */
        public fun setHeader(key: String, value: String): Builder {
            headers[key] = mutableListOf(value)
            return this
        }

        /**
         * Adds [value] to header values associated with [key].
         */
        public fun addHeader(key: String, value: String): Builder {
            headers.getOrPut(key) { mutableListOf() }.add(value)
            return this
        }

        /**
         * Issue a byte range request. Use -1 to download until the end.
         */
        public fun setRange(range: LongRange): Builder {
            val start = range.first.coerceAtLeast(0)
            var value = "$start-"
            if (range.last >= start) {
                value += range.last
            }
            setHeader("Range", "bytes=$value")
            return this
        }

        /**
         * Initializes a POST request with the given form data.
         */
        public fun setPostForm(form: Map<String, String?>): Builder {
            method = Method.POST
            setHeader("Content-Type", "application/x-www-form-urlencoded")

            body = Body.Bytes(
                form
                    .map { (key, value) ->
                        "$key=${(value ?: "").formUrlEncoded()}"
                    }
                    .joinToString("&")
                    .encodeToByteArray()
            )

            return this
        }

        public fun build(): HttpRequest = HttpRequest(
            url = if (uriModified) AbsoluteUrl(uriBuilder.build()) ?: url else url,
            method = method,
            headers = headers.toMap(),
            body = body,
            extras = extras.toMap(),
            connectTimeout = connectTimeout,
            readTimeout = readTimeout,
            allowUserInteraction = allowUserInteraction
        )
    }
}

/**
 * Percent-encodes the receiver as an `application/x-www-form-urlencoded` value, mimicking
 * `java.net.URLEncoder`.
 */
private fun String.formUrlEncoded(): String {
    val hexDigits = "0123456789ABCDEF"
    return buildString {
        for (byte in this@formUrlEncoded.encodeToByteArray()) {
            val char = (byte.toInt() and 0xFF).toChar()
            when {
                char in 'a'..'z' || char in 'A'..'Z' || char in '0'..'9' || char in "-._*" ->
                    append(char)
                char == ' ' ->
                    append('+')
                else -> {
                    append('%')
                    append(hexDigits[(byte.toInt() shr 4) and 0x0F])
                    append(hexDigits[byte.toInt() and 0x0F])
                }
            }
        }
    }
}
