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
import org.readium.r2.shared.util.mediatype.MediaType
import org.readium.r2.shared.util.mediatype.sniffMediaType
import timber.log.Timber
import java.io.BufferedInputStream
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import kotlin.time.Duration
import kotlin.time.ExperimentalTime


internal typealias URLParameters = Map<String, String?>

internal class NetworkService {
    enum class Method(val rawValue: String) {
        GET("GET"), POST("POST"), PUT("PUT");

        companion object {
            operator fun invoke(rawValue: String) = values().firstOrNull { it.rawValue == rawValue }
        }
    }

    @OptIn(ExperimentalTime::class)
    suspend fun fetch(url: String, method: Method = Method.GET, parameters: URLParameters = emptyMap(), timeout: Duration? = null): Pair<Int, ByteArray?> =
        withContext(Dispatchers.IO) {
            try {
                @Suppress("NAME_SHADOWING")
                val url = URL(Uri.parse(url).buildUpon().appendQueryParameters(parameters).build().toString())

                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = method.rawValue
                if (timeout != null) {
                    connection.connectTimeout = timeout.toLongMilliseconds().toInt()
                }

                val status = connection.responseCode
                if (status != HttpURLConnection.HTTP_OK) {
                    Pair(status, null)
                } else {
                    Pair(status, connection.inputStream.readBytes())
                }
            } catch (e: Exception) {
                Timber.e(e)
                Pair(HttpURLConnection.HTTP_INTERNAL_ERROR, null)
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

    suspend fun download(url: URL, destination: File, mediaType: String? = null): MediaType? = withContext(Dispatchers.IO) {
        try {
            val connection = url.openConnection() as HttpURLConnection
            if (connection.responseCode != HttpURLConnection.HTTP_OK) {
                throw LcpException.Network(Exception("Download failed with status ${connection.responseCode}"))
            }

            BufferedInputStream(connection.inputStream).use { input ->
                FileOutputStream(destination).use { output ->
                    val buf = ByteArray(2048)
                    var n: Int
                    while (-1 != input.read(buf).also { n = it }) {
                        output.write(buf, 0, n)
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
