/*
 * Copyright 2023 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.shared.util.http

import android.net.Uri
import android.os.Bundle
import java.io.Serializable
import java.net.URLEncoder
import kotlin.time.Duration
import org.readium.r2.shared.extensions.toMutable
import org.readium.r2.shared.util.AbsoluteUrl
import org.readium.r2.shared.util.toUri

/**
 * Holds the information about an HTTP request performed by an [HttpClient].
 *
 * @param url Address of the remote resource to request.
 * @param method HTTP method to use for the request.
 * @param headers Additional HTTP headers to use.
 * @param body Content put in the body of the HTTP request.
 * @param extras Bundle of additional information, which might be used by a specific implementation
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
    public val extras: Bundle = Bundle(),
    public val connectTimeout: Duration? = null,
    public val readTimeout: Duration? = null,
    public val allowUserInteraction: Boolean = false,
) : Serializable {

    /** Supported HTTP methods. */
    public enum class Method : Serializable {
        DELETE,
        GET,
        HEAD,
        PATCH,
        POST,
        PUT,
    }

    /** Supported body values. */
    public sealed class Body : Serializable {
        public class Bytes(public val bytes: ByteArray) : Body()
        public class File(public val file: java.io.File) : Body()
    }

    public fun buildUpon(): Builder = Builder(
        url = url,
        method = method,
        headers = headers.toMutable(),
        body = body,
        extras = extras,
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
        public var extras: Bundle = Bundle(),
        public var connectTimeout: Duration? = null,
        public var readTimeout: Duration? = null,
        public var allowUserInteraction: Boolean = false,
    ) {

        private var uriBuilder: Uri.Builder = url.toUri().buildUpon()

        public fun appendQueryParameter(key: String, value: String?): Builder {
            if (value != null) {
                uriBuilder.appendQueryParameter(key, value)
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
                        "$key=${URLEncoder.encode(value ?: "", "UTF-8")}"
                    }
                    .joinToString("&")
                    .toByteArray()
            )

            return this
        }

        public fun build(): HttpRequest = HttpRequest(
            url = url,
            method = method,
            headers = headers.toMap(),
            body = body,
            extras = extras,
            connectTimeout = connectTimeout,
            readTimeout = readTimeout,
            allowUserInteraction = allowUserInteraction
        )
    }
}
