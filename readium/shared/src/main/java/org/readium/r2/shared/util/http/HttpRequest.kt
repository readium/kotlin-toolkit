package org.readium.r2.shared.util.http

import android.net.Uri
import android.os.Bundle
import java.io.Serializable
import java.net.URLEncoder
import kotlin.time.Duration

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
class HttpRequest(
    val url: String,
    val method: Method = Method.GET,
    val headers: Map<String, String> = mapOf(),
    val body: Body? = null,
    val extras: Bundle = Bundle(),
    val connectTimeout: Duration? = null,
    val readTimeout: Duration? = null,
    val allowUserInteraction: Boolean = false,
) : Serializable {

    /** Supported HTTP methods. */
    enum class Method : Serializable {
        DELETE, GET, HEAD, PATCH, POST, PUT;
    }

    /** Supported body values. */
    sealed class Body : Serializable {
        class Bytes(val bytes: ByteArray): Body()
        class File(val file: java.io.File): Body()
    }

    fun buildUpon() = Builder(
        url = url,
        method = method,
        headers = headers.toMutableMap(),
        body = body,
        extras = extras,
        connectTimeout = connectTimeout,
        readTimeout = readTimeout,
        allowUserInteraction = allowUserInteraction
    )

    companion object {
        operator fun invoke(url: String, build: Builder.() -> Unit): HttpRequest =
            Builder(url).apply(build).build()
    }

    class Builder(
        url: String,
        var method: Method = Method.GET,
        var headers: MutableMap<String, String> = mutableMapOf(),
        var body: Body? = null,
        var extras: Bundle = Bundle(),
        var connectTimeout: Duration? = null,
        var readTimeout: Duration? = null,
        var allowUserInteraction: Boolean = false,
    ) {

        var url: String
            get() = uriBuilder.build().toString()
            set(value) { uriBuilder = Uri.parse(value).buildUpon() }

        private var uriBuilder: Uri.Builder = Uri.parse(url).buildUpon()

        fun appendQueryParameter(key: String, value: String?): Builder {
            if (value != null) {
                uriBuilder.appendQueryParameter(key, value)
            }
            return this
        }

        fun appendQueryParameters(params: Map<String, String?>): Builder {
            for ((key, value) in params) {
                appendQueryParameter(key, value)
            }
            return this
        }

        fun setHeader(key: String, value: String): Builder {
            headers[key] = value
            return this
        }

        /**
         * Issue a byte range request. Use -1 to download until the end.
         */
        fun setRange(range: LongRange): Builder {
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
        fun setPostForm(form: Map<String, String?>): Builder {
            method = Method.POST
            setHeader("Content-Type", "application/x-www-form-urlencoded")

            body = Body.Bytes(form
                .map { (key, value) ->
                    "$key=${URLEncoder.encode(value ?: "", "UTF-8")}"
                }
                .joinToString("&")
                .toByteArray()
            )

            return this
        }


        fun build(): HttpRequest = HttpRequest(
            url = url,
            method = method,
            headers = headers.toMap(),
            body = body,
            extras = extras,
            connectTimeout = connectTimeout,
            readTimeout = readTimeout,
            allowUserInteraction = allowUserInteraction,
        )

    }

}
