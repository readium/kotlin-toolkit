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
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.readium.r2.lcp.LCPError
import org.readium.r2.shared.format.Format
import org.readium.r2.shared.format.sniffFormat
import timber.log.Timber
import java.io.BufferedInputStream
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL


internal typealias URLParameters = Map<String, String?>

internal class NetworkService {
    enum class Method(val rawValue: String) {
        GET("GET"), POST("POST"), PUT("PUT");

        companion object {
            operator fun invoke(rawValue: String) = values().firstOrNull { it.rawValue == rawValue }
        }
    }

    fun fetch(url: String, method: Method = Method.GET, parameters: URLParameters = emptyMap(), completion: (status: Int, data: ByteArray?) -> Unit) = runBlocking {
        try {
            @Suppress("NAME_SHADOWING")
            val url = URL(Uri.parse(url).buildUpon().appendQueryParameters(parameters).build().toString())

            withContext(Dispatchers.IO) {
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = method.rawValue
                val status = connection.responseCode
                if (status != HttpURLConnection.HTTP_OK) {
                    completion(status, null)
                } else {
                    completion(status, connection.inputStream.readBytes())
                }
            }
        } catch (e: Exception) {
            Timber.e(e)
            completion(HttpURLConnection.HTTP_INTERNAL_ERROR, null)
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

    suspend fun download(url: URL, destination: File): Format? = withContext(Dispatchers.IO) {
        try {
            val connection = url.openConnection() as HttpURLConnection
            if (connection.responseCode != HttpURLConnection.HTTP_OK) {
                throw LCPError.network(Exception("Download failed with status ${connection.responseCode}"))
            }

            BufferedInputStream(connection.inputStream).use { input ->
                FileOutputStream(destination).use { output ->
                    val buf = ByteArray(2048)
                    var n = 0
                    while (-1 != input.read(buf).also { n = it }) {
                        output.write(buf, 0, n)
                    }
                }
            }

            connection.sniffFormat()

        } catch (e: Exception) {
            Timber.e(e)
            throw LCPError.network(e)
        }
    }

}
