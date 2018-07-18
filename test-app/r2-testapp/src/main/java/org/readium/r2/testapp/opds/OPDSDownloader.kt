/*
 * Copyright 2018 Readium Foundation. All rights reserved.
 * Licensed to the Readium Foundation under one or more contributor license agreements.
 * Use of this source code is governed by a BSD-style license which is detailed in the
 * LICENSE file present in the project repository where this source code is maintained.
 */

package org.readium.r2.testapp.opds

import android.content.Context
import android.util.Log
import com.github.kittinunf.fuel.Fuel
import nl.komponents.kovenant.Promise
import nl.komponents.kovenant.then
import org.readium.r2.shared.promise
import java.io.File
import java.util.*

class OPDSDownloader(context: Context) {

    val rootDir:String = context.getExternalFilesDir(null).path + "/"

    fun publicationUrl(url: String, parameters: List<Pair<String, Any?>>? = null): Promise<Pair<String, String>, Exception> {
        val fileName = UUID.randomUUID().toString()
        return Fuel.download(url).destination { _, _ ->
            Log.i("download destination ", rootDir + fileName)
            File(rootDir, fileName)
        }.promise() then {
            val (_, response, _) = it
            Log.i("download destination ", rootDir + fileName)
            Log.i("download url ", response.url.toString())
            Pair(rootDir + fileName, fileName)
        }
    }

}
