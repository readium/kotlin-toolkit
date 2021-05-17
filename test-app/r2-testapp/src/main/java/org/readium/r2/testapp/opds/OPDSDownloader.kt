/*
 * Module: r2-testapp-kotlin
 * Developers: Aferdita Muriqi, Clément Baumann
 *
 * Copyright (c) 2018. European Digital Reading Lab. All rights reserved.
 * Licensed to the Readium Foundation under one or more contributor license agreements.
 * Use of this source code is governed by a BSD-style license which is detailed in the
 * LICENSE file present in the project repository where this source code is maintained.
 */

package org.readium.r2.testapp.opds

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import org.readium.r2.shared.util.Try
import org.readium.r2.shared.util.flatMap
import org.readium.r2.shared.util.http.*
import org.readium.r2.shared.util.mediatype.MediaType
import org.readium.r2.testapp.BuildConfig.DEBUG
import timber.log.Timber
import java.io.File
import java.io.FileOutputStream
import java.util.*

class OPDSDownloader(context: Context) {

    private val useExternalFileDir = useExternalDir(context)

    private val rootDir: String = if (useExternalFileDir) {
        context.getExternalFilesDir(null)?.path + "/"
    } else {
        context.filesDir.path + "/"
    }

    private fun useExternalDir(context: Context): Boolean {
        val properties = Properties()
        val inputStream = context.assets.open("configs/config.properties")
        properties.load(inputStream)
        return properties.getProperty("useExternalFileDir", "false")!!.toBoolean()
    }

    suspend fun publicationUrl(
        url: String,
        parameters: List<Pair<String, Any?>>? = null
    ): Try<Pair<String, String>, Exception> {
        val fileName = UUID.randomUUID().toString()
        if (DEBUG) Timber.i("download url %s", url)
        return DefaultHttpClient().download(HttpRequest(url), File(rootDir, fileName))
            .flatMap {
                try {
                    if (DEBUG) Timber.i("response url %s", it.url)
                    if (DEBUG) Timber.i("download destination %s %s %s", "%s%s", rootDir, fileName)
                    if (url == it.url) {
                        Try.success(Pair(rootDir + fileName, fileName))
                    } else {
                        redirectedDownload(it.url, fileName)
                    }
                } catch (e: Exception) {
                    Try.failure(e)
                }
            }
    }

    private suspend fun redirectedDownload(
        responseUrl: String,
        fileName: String
    ): Try<Pair<String, String>, Exception> {
        return DefaultHttpClient().download(HttpRequest(responseUrl), File(rootDir, fileName))
            .flatMap {
                if (DEBUG) Timber.i("response url %s", it.url)
                if (DEBUG) Timber.i("download destination %s %s %s", "%s%s", rootDir, fileName)
                try {
                    Try.success(Pair(rootDir + fileName, fileName))
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

}
