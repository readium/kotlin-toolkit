/*
 * Module: r2-lcp-kotlin
 * Developers: Aferdita Muriqi
 *
 * Copyright (c) 2019. Readium Foundation. All rights reserved.
 * Use of this source code is governed by a BSD-style license which is detailed in the
 * LICENSE file present in the project repository where this source code is maintained.
 */

package org.readium.r2.lcp.service

import awaitByteArrayResponse
import com.github.kittinunf.fuel.Fuel
import kotlinx.coroutines.runBlocking
import org.readium.r2.lcp.BuildConfig.DEBUG
import timber.log.Timber

class NetworkService {
    enum class Method(val rawValue: String) {
        get("GET"), post("POST"), put("PUT");

        companion object {
            operator fun invoke(rawValue: String) = values().firstOrNull { it.rawValue == rawValue }
        }
    }

    fun fetch(url: String, method: Method? = Method.get, params: List<Pair<String, Any?>>? = null, completion: (status: Int, data: ByteArray?) -> Unit) = runBlocking {

        val (request, response, result) =

            when (method) {
                Method.get -> Fuel.get(url).awaitByteArrayResponse()
                Method.post -> Fuel.post(url, params).awaitByteArrayResponse()
                Method.put -> Fuel.put(url, params).awaitByteArrayResponse()
                null -> Fuel.get(url).awaitByteArrayResponse()
            }

            result.fold(
                { data ->
                    completion(response.statusCode, data)
                },
                { error ->
                    if (DEBUG) Timber.e("An error of type ${error.exception} happened: ${error.message}")
                    completion(error.response.statusCode, null)
                }
        )

    }

}
