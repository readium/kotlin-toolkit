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
import com.github.kittinunf.fuel.Fuel
import com.github.kittinunf.fuel.coroutines.awaitStringResponseResult
import org.readium.r2.testapp.BuildConfig.DEBUG
import timber.log.Timber
import java.io.File
import java.net.URL
import java.util.*

class OPDSDownloader(context: Context) {

    private val useExternalFileDir = useExternalDir(context)

    private val rootDir: String =  if (useExternalFileDir) {
        context.getExternalFilesDir(null)?.path + "/"
    } else {
        context.filesDir.path + "/"
    }

    private fun useExternalDir(context: Context): Boolean {
        val properties =  Properties()
        val inputStream = context.assets.open("configs/config.properties")
        properties.load(inputStream)
        return properties.getProperty("useExternalFileDir", "false")!!.toBoolean()
    }

    suspend fun publicationUrl(url: String, parameters: List<Pair<String, Any?>>? = null): OpdsDownloadResult {
        val fileName = UUID.randomUUID().toString()
        if (DEBUG) Timber.i("download url %s", url)
        val (request, response, result) = Fuel.download(url).fileDestination { _, request ->
            if (DEBUG) Timber.i("request url %s", request.url)
            if (DEBUG) Timber.i("download destination %s %s %s", "%s%s", rootDir, fileName)
            File(rootDir, fileName)
        }.awaitStringResponseResult()
        if (DEBUG) Timber.i("response url %s", response.url.toString())
        return result.fold(
            { data ->
                if (url == response.url.toString()) {
                    OpdsDownloadResult.OnSuccess(Pair(rootDir + fileName, fileName))
                } else {
                    val redirectedDownload = redirectedDownload(response.url, fileName)
                    when (redirectedDownload) {
                        is OpdsDownloadResult.OnSuccess -> OpdsDownloadResult.OnSuccess(redirectedDownload.data)
                        is OpdsDownloadResult.OnFailure -> OpdsDownloadResult.OnFailure(redirectedDownload.exception)
                    }
                }
            },
            { error -> OpdsDownloadResult.OnFailure(error.message) }
        )
    }

    private suspend fun redirectedDownload(responseUrl: URL, fileName: String): OpdsDownloadResult {
        val (request, response, result) = Fuel.download(responseUrl.toString()).fileDestination { _, request ->
            if (DEBUG) Timber.i("request url %s", request.url)
            if (DEBUG) Timber.i("download destination %s %s %s", "%s%s", rootDir, fileName)
            File(rootDir, fileName)
        }.awaitStringResponseResult()
        return result.fold(
            { data ->
                OpdsDownloadResult.OnSuccess(Pair(rootDir + fileName, fileName))
            },
            { error -> OpdsDownloadResult.OnFailure(error.message) }
        )
    }

}

sealed class OpdsDownloadResult {
    data class OnSuccess(val data : Pair<String, String>) : OpdsDownloadResult()
    data class OnFailure(val exception : String?) : OpdsDownloadResult()
}
