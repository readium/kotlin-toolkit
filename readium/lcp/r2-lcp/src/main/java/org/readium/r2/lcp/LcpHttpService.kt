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
import android.os.Build
import awaitByteArrayResponse
import awaitByteArrayResult
import com.github.kittinunf.fuel.Fuel
import kotlinx.coroutines.runBlocking
import nl.komponents.kovenant.Promise
import nl.komponents.kovenant.then
import org.json.JSONObject
import org.readium.r2.lcp.model.documents.LicenseDocument
import org.readium.r2.lcp.model.documents.StatusDocument
import org.readium.r2.shared.contentTypeEncoding
import org.readium.r2.shared.promise
import timber.log.Timber
import java.io.File
import java.nio.charset.Charset
import java.util.*

class LcpHttpService {

    fun statusDocument(url: String): Any {
        return runBlocking {
            Fuel.get(url).awaitByteArrayResult().fold({ data ->
                StatusDocument(data)
            }, { error ->
                Timber.e("An error of type ${error.exception} happened: ${error.message}")
                error.exception
            })
        }
    }

    fun fetchUpdatedLicense(url: String): Any {
        return runBlocking {
            Fuel.get(url).awaitByteArrayResult().fold({ data ->
                LicenseDocument(data)
            }, { error ->
                Timber.e("An error of type ${error.exception} happened: ${error.message}")
                error.exception
            })
        }
    }

    fun publicationUrl(context:Context, url: String, parameters: List<Pair<String, Any?>>? = null): Promise<String, Exception> {
        val rootDir:String = context.getExternalFilesDir(null).path + "/"
        val fileName = UUID.randomUUID().toString()
        return Fuel.download(url).destination { _, _ ->
            Timber.i("LCP  destination %s%s", rootDir, fileName)
            File(rootDir, fileName)
        }.promise() then {
            val (_, response, _) = it
            Timber.i( "LCP destination %s %s", rootDir , fileName)
            Timber.i("LCP then  %s", response.url.toString())
            rootDir + fileName
        }
    }

    fun certificateRevocationList(url: String, session:LcpSession): String? {
        Timber.i("certificateRevocationList %s", url)
        return runBlocking {
            val (request, response, result) = Fuel.get(url).awaitByteArrayResponse()
            return@runBlocking result.fold(
                    { data ->
                        Timber.i("certificateRevocationList %s", response.statusCode)
                        var status:String? = null
                        if (response.statusCode == 200) {
                            status = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                "-----BEGIN X509 CRL-----${Base64.getEncoder().encodeToString(data)}-----END X509 CRL-----"
                            } else {
                                "-----BEGIN X509 CRL-----${android.util.Base64.encodeToString(data, android.util.Base64.DEFAULT)}-----END X509 CRL-----"
                            };
                        }
                        status
                    },
                    { error ->
                        Timber.e("An error of type ${error.exception} happened: ${error.message}")
                        error.exception
                    }
            )
        }.toString()
    }
    
    fun register(url: String, params: List<Pair<String, Any?>>): String? {
        Timber.i("register %s", url)
        return runBlocking {
            val (request, response, result) = Fuel.post(url,params).awaitByteArrayResponse()
            return@runBlocking result.fold(
                    { data ->
                        Timber.i("register %s", response.statusCode)
                        var status:String? = null
                        if (response.statusCode == 200) {
                            val jsonObject = JSONObject(String(data, Charset.forName(response.contentTypeEncoding)))
                            status = jsonObject["status"] as String
                        }
                        return@fold status
                    },
                    { error -> Timber.e("An error of type ${error.exception} happened: ${error.message}") }
            )
        }.toString()
    }

    fun renewLicense(url: String, params: List<Pair<String, Any?>>): String? {
        Timber.i("renewLicense %s", url)
        return runBlocking {
            val (request, response, result) = Fuel.put(url,params).awaitByteArrayResponse()
            return@runBlocking result.fold(
                    { data ->
                        Timber.i("renewLicense %s", response.statusCode)
                        var status:String? = null
                        if (response.statusCode == 200) {
                            val jsonObject = JSONObject(String(data, Charset.forName(response.contentTypeEncoding)))
                            status = jsonObject["status"] as String
                        }
                        return@fold status
                    },
                    { error -> Timber.e("An error of type ${error.exception} happened: ${error.message}") }
            )
        }.toString()
    }

    fun returnLicense(url: String, params: List<Pair<String, Any?>>): String? {
        Timber.i("returnLicense %s", url)
        return runBlocking {
            val (request, response, result) = Fuel.put(url,params).awaitByteArrayResponse()
            return@runBlocking result.fold(
                    { data ->
                        Timber.i("returnLicense %s", response.statusCode)
                        var status:String? = null
                        if (response.statusCode == 200) {
                            val jsonObject = JSONObject(String(data, Charset.forName(response.contentTypeEncoding)))
                            status = jsonObject["status"] as String
                        }
                        return@fold status
                    },
                    { error -> Timber.e("An error of type ${error.exception} happened: ${error.message}") }
            )
        }.toString()
    }

}
