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
import nl.komponents.kovenant.Promise
import nl.komponents.kovenant.then
import org.readium.r2.shared.promise
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
        val properties =  Properties();
        val inputStream = context.assets.open("configs/config.properties");
        properties.load(inputStream);
        return properties.getProperty("useExternalFileDir", "false")!!.toBoolean()
    }

    fun publicationUrl(url: String, parameters: List<Pair<String, Any?>>? = null): Promise<Pair<String, String>, Exception> {
        val fileName = UUID.randomUUID().toString()
        Timber.i("download url %s", url)

        return Fuel.download(url).destination { _, request_url ->
            Timber.i("request url %s", request_url.toString())
            Timber.i("download destination %s %s %s", "%s%s", rootDir, fileName)
            File(rootDir, fileName)
        }.promise() then {
            val (_, response, _) = it
            Timber.i("response url %s", response.url.toString())
            if (url == response.url.toString()) {
                Pair(rootDir + fileName, fileName)
            } else {
                redirectedDownload(response.url, fileName).get()
            }
        }
    }

    private fun redirectedDownload(responseUrl: URL, fileName: String): Promise<Pair<String, String>, Exception> {
        return Fuel.download(responseUrl.toString()).destination { _, request_url ->
            Timber.i("request url %s", request_url.toString())
            Timber.i("download destination %s %s %s", "%s%s", rootDir, fileName)
            File(rootDir, fileName)
        }.promise() then {
            val (_, response, _) = it
            Timber.i("response url %s", response.url.toString())
            Pair(rootDir + fileName, fileName)
        }
    }

}
