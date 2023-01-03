/* Module: r2-testapp-kotlin
* Developers: Quentin Gliosca, Aferdita Muriqi, Clément Baumann
*
* Copyright (c) 2020. European Digital Reading Lab. All rights reserved.
* Licensed to the Readium Foundation under one or more contributor license agreements.
* Use of this source code is governed by a BSD-style license which is detailed in the
* LICENSE file present in the project repository where this source code is maintained.
*/

package org.readium.r2.testapp.utils.extensions

import java.io.File
import java.io.FileFilter
import java.io.FileOutputStream
import java.io.IOException
import java.net.URL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import org.readium.r2.shared.util.Try
import org.readium.r2.shared.util.flatMap
import org.readium.r2.shared.util.http.*
import org.readium.r2.shared.util.mediatype.MediaType
import org.readium.r2.testapp.BuildConfig
import timber.log.Timber

suspend fun File.moveTo(target: File) = withContext(Dispatchers.IO) {
    if (!this@moveTo.renameTo(target))
        throw IOException()
}

/**
 * As there are cases where [File.listFiles] returns null even though it is a directory, we return
 * an empty list instead.
 */
fun File.listFilesSafely(filter: FileFilter? = null): List<File> {
    val array: Array<File>? = if (filter == null) listFiles() else listFiles(filter)
    return array?.toList() ?: emptyList()
}

suspend fun URL.downloadTo(
    dest: File,
    maxRedirections: Int = 2
): Try<Unit, Exception> {
    if (maxRedirections == 0) {
        return Try.Failure(Exception("Too many HTTP redirections."))
    }

    val urlString = toString()

    if (BuildConfig.DEBUG) Timber.i("download url $urlString")
    return DefaultHttpClient().download(HttpRequest(toString()), dest)
        .flatMap {
            try {
                if (BuildConfig.DEBUG) Timber.i("response url ${it.url}")
                if (BuildConfig.DEBUG) Timber.i("download destination ${dest.path}")
                if (urlString == it.url) {
                    Try.success(Unit)
                } else {
                    URL(it.url).downloadTo(dest, maxRedirections - 1)
                }
            } catch (e: Exception) {
                Try.failure(e)
            }
        }
}

private suspend fun HttpClient.download(
    request: HttpRequest,
    destination: File,
): HttpTry<HttpResponse> =
    try {
        stream(request).flatMap { res ->
            withContext(Dispatchers.IO) {
                res.body.use { input ->
                    FileOutputStream(destination).use { output ->
                        val buf = ByteArray(1024 * 8)
                        var n: Int
                        var downloadedBytes = 0
                        while (-1 != input.read(buf).also { n = it }) {
                            ensureActive()
                            downloadedBytes += n
                            output.write(buf, 0, n)
                        }
                    }
                }
                var response = res.response
                if (response.mediaType.matches(MediaType.BINARY)) {
                    response = response.copy(
                        mediaType = MediaType.ofFile(destination) ?: response.mediaType
                    )
                }
                Try.success(response)
            }
        }
    } catch (e: Exception) {
        Try.failure(HttpException.wrap(e))
    }
