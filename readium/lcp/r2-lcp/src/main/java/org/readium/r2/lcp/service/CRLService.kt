/*
 * Module: r2-lcp-kotlin
 * Developers: Aferdita Muriqi
 *
 * Copyright (c) 2019. Readium Foundation. All rights reserved.
 * Use of this source code is governed by a BSD-style license which is detailed in the
 * LICENSE file present in the project repository where this source code is maintained.
 */

package org.readium.r2.lcp.service

import android.content.Context
import android.os.Build
import kotlinx.coroutines.runBlocking
import org.joda.time.DateTime
import org.joda.time.Days
import org.readium.r2.lcp.public.LCPError
import timber.log.Timber
import java.util.*

class CRLService(val network: NetworkService, val context: Context) {

    val preferences = context.getSharedPreferences("org.readium.r2.lcp", Context.MODE_PRIVATE)

    companion object {
        val expiration = 7
        val crlKey = "org.readium.r2-lcp-swift.CRL"
        val dateKey = "org.readium.r2-lcp-swift.CRLDate"
    }

    fun retrieve(completion: (String) -> Unit) {
        val localCRL = readLocal()
        localCRL?.let {
            if (daysSince(localCRL.second) < CRLService.expiration) {
                completion(localCRL.first)
            }
        }

        try {
            fetch() { received ->
                received?.let {
                    saveLocal(received)
                    completion(received)
                }
            }
        } catch (error: LCPError) {
            Timber.e(error)
            val (received, _) = localCRL ?: throw error
            completion(received)
        }

    }

    private fun fetch(completion: (String?) -> Unit) = runBlocking {

        val url = "http://crl.edrlab.telesec.de/rl/EDRLab_CA.crl"
        network.fetch(url, NetworkService.Method.get) { status, data ->

            Timber.d("Status $status")
            if (status != 200) {
                throw LCPError.crlFetching
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                completion("-----BEGIN X509 CRL-----${Base64.getEncoder().encodeToString(data)}-----END X509 CRL-----")
            } else {
                completion("-----BEGIN X509 CRL-----${android.util.Base64.encodeToString(data, android.util.Base64.DEFAULT)}-----END X509 CRL-----")
            }
        }
    }

    private fun readLocal(): Pair<String, DateTime>? {
        val crl = preferences.getString(CRLService.crlKey, null)
        val date = preferences.getString(CRLService.dateKey, null)?.let {
            DateTime(preferences.getString(CRLService.dateKey, null))
        }
        if (crl == null || date == null) {
            return null
        }
        return Pair(crl, date)
    }

    private fun saveLocal(crl: String): String {
        preferences.edit().putString(CRLService.crlKey, crl).apply()
        preferences.edit().putString(CRLService.dateKey, DateTime().toString()).apply()
        return crl
    }

    private fun daysSince(date: DateTime): Int {
        return Days.daysBetween(date, DateTime.now()).days
    }
}

