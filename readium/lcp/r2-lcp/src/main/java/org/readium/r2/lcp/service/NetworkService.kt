/*
 * Module: r2-lcp-kotlin
 * Developers: Aferdita Muriqi
 *
 * Copyright (c) 2019. Readium Foundation. All rights reserved.
 * Use of this source code is governed by a BSD-style license which is detailed in the
 * LICENSE file present in the project repository where this source code is maintained.
 */

package org.readium.r2.lcp.service

import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.readium.r2.lcp.LcpException
import org.readium.r2.shared.util.Try
import org.readium.r2.shared.util.mediatype.MediaType
import org.readium.r2.shared.util.mediatype.sniffMediaType
import timber.log.Timber
import java.io.BufferedInputStream
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import kotlin.math.round
import kotlin.time.Duration
import kotlin.time.ExperimentalTime


internal typealias URLParameters = Map<String, String?>

internal class NetworkException(val status: Int?, cause: Throwable? = null) : Exception("Network failure with status $status", cause)

internal class NetworkService {
    enum class Method(val rawValue: String) {
        GET("GET"), POST("POST"), PUT("PUT");

        companion object {
            operator fun invoke(rawValue: String) = values().firstOrNull { it.rawValue == rawValue }
        }
    }

    @OptIn(ExperimentalTime::class)
    suspend fun fetch(url: String, method: Method = Method.GET, parameters: URLParameters = emptyMap(), timeout: Duration? = null): Try<ByteArray, NetworkException> =
        withContext(Dispatchers.IO) {
            try {
                @Suppress("NAME_SHADOWING")
                val url = URL(Uri.parse(url).buildUpon().appendQueryParameters(parameters).build().toString())

                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = method.rawValue
                if (timeout != null) {
                    connection.connectTimeout = timeout.inWholeMilliseconds.toInt()
                }

                val status = connection.responseCode
                if (status >= 400) {
                    Try.failure(NetworkException(status))
                } else {
                    Try.success(connection.inputStream.readBytes())
                }
            } catch (e: Exception) {
                Timber.e(e)
                Try.failure(NetworkException(status = null, cause = e))
            }
        }

    private fun Uri.Builder.appendQueryParameters(parameters: URLParameters): Uri.Builder =
        apply {
            for ((key, value) in parameters) {
                if (value != null) {
                    appendQueryParameter(key, value)
                }
            }
        }

    suspend fun download(url: URL, destination: File, mediaType: String? = null, onProgress: (Double) -> Unit): MediaType? = withContext(Dispatchers.IO) {
        try {
            val connection = url.openConnection() as HttpURLConnection
            if (connection.responseCode >= 400) {
                throw LcpException.Network(NetworkException(connection.responseCode))
            }

            var readLength = 0L
            val expectedLength =
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
                    connection.contentLengthLong.toDouble()
                } else {
                    connection.contentLength.toDouble()
                }
            var lastProgress = 0.0

            BufferedInputStream(connection.inputStream).use { input ->
                FileOutputStream(destination).use { output ->
                    val buf = ByteArray(2048)
                    var n: Int
                    while (-1 != input.read(buf).also { n = it }) {
                        output.write(buf, 0, n)
                        readLength += n

                        if (expectedLength > 0) {
                            // Rounds the progress to avoid notifying too much which my decrease
                            // performances.
                            val progress = (readLength / expectedLength)
                                .coerceIn(0.0, 1.0).roundToDecimals(2)
                            if (lastProgress < progress) {
                                withContext(Dispatchers.Main) {
                                    onProgress(progress)
                                }
                            }
                            lastProgress = progress
                        }
                    }
                }
            }

            connection.sniffMediaType(mediaTypes = listOfNotNull(mediaType))

        } catch (e: Exception) {
            Timber.e(e)
            throw LcpException.Network(e)
        }
    }

}

private fun Double.roundToDecimals(decimals: Int): Double {
    var multiplier = 1.0
    repeat(decimals) { multiplier *= 10 }
    return round(this * multiplier) / multiplier
}