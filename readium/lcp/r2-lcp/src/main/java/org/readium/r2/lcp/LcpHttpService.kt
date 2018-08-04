/*
 * Module: r2-lcp-kotlin
 * Developers: Aferdita Muriqi, Clément Baumann
 *
 * Copyright (c) 2018. Readium Foundation. All rights reserved.
 * Use of this source code is governed by a BSD-style license which is detailed in the
 * LICENSE file present in the project repository where this source code is maintained.
 */

package org.readium.r2.lcp

import android.content.Context
import android.util.Log
import com.github.kittinunf.fuel.Fuel
import nl.komponents.kovenant.Promise
import nl.komponents.kovenant.task
import nl.komponents.kovenant.then
import org.json.JSONObject
import org.readium.r2.lcp.model.documents.LicenseDocument
import org.readium.r2.lcp.model.documents.StatusDocument
import org.readium.r2.shared.contentTypeEncoding
import org.readium.r2.shared.promise
import java.io.File
import java.nio.charset.Charset
import java.util.*
import android.util.Base64

class LcpHttpService {

    fun statusDocument(url: String): Promise<StatusDocument, Exception> {
        return Fuel.get(url,null).promise() then {
            val (_, _, result) = it
            StatusDocument(result)
        }
    }
    fun fetchUpdatedLicense(url: String): Promise<LicenseDocument, Exception> {
        return Fuel.get(url,null).promise() then {
            val (_, _, result) = it
            LicenseDocument(result)
        }
    }

    fun publicationUrl(context:Context, url: String, parameters: List<Pair<String, Any?>>? = null): Promise<String, Exception> {
        val rootDir:String = context.getExternalFilesDir(null).path + "/"
        val fileName = UUID.randomUUID().toString()
        return Fuel.download(url).destination { _, _ ->
            Log.i("LCP  destination ", rootDir + fileName)
            File(rootDir, fileName)
        }.promise() then {
            val (_, response, _) = it
            Log.i("LCP destination ", rootDir + fileName)
            Log.i("LCP then ", response.url.toString())
            rootDir + fileName
        }
    }

    fun certificateRevocationList(url: String): Promise<String, Exception> {
        return Fuel.get(url,null).promise() then {
            val (_, _, result) = it
            "-----BEGIN X509 CRL-----${ Base64.encodeToString(result, Base64.DEFAULT)}-----END X509 CRL-----";
//            "-----BEGIN X509 CRL-----${Base64.getEncoder().encodeToString(result)}-----END X509 CRL-----";
        }
    }
    
    fun register(registerUrl: String, params: List<Pair<String, Any?>>): Promise<String?, Exception> {
        return Fuel.post(registerUrl.toString(), params).promise() then {
            val (_, response, result) = it
            var status:String? = null
                if (response.statusCode.equals(200)) {
                val jsonObject = JSONObject(String(result, Charset.forName(response.contentTypeEncoding)))
                status = jsonObject["status"] as String
            }
            status
        }
    }

    fun renewLicense(url: String): Promise<String?, Exception> {
        return task { null }
    }

    fun returnLicense(url: String): Promise<String?, Exception> {
        return task { null }
    }

}
