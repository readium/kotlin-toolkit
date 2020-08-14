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
import android.content.SharedPreferences
import android.os.Build
import kotlinx.coroutines.runBlocking
import org.joda.time.DateTime
import org.joda.time.Days
import org.readium.r2.lcp.BuildConfig.DEBUG
import org.readium.r2.lcp.LcpException
import org.readium.r2.shared.util.Try
import timber.log.Timber
import java.util.*

internal class CRLService(val network: NetworkService, val context: Context) {

    private val preferences: SharedPreferences = context.getSharedPreferences("org.readium.r2.lcp", Context.MODE_PRIVATE)

    companion object {
        const val expiration = 7
        const val crlKey = "org.readium.r2-lcp-swift.CRL"
        const val dateKey = "org.readium.r2-lcp-swift.CRLDate"
    }

    suspend fun retrieve(): String {
        val (localCRL, isExpired) = readLocal()
        if (localCRL != null && !isExpired) {
            return localCRL
        }

        return try {
            fetch()
                .also { saveLocal(it) }

        } catch (e: Exception) {
            if (DEBUG) Timber.e(e)
            localCRL ?: throw e
        }
    }

    private suspend fun fetch(): String {
        val url = "http://crl.edrlab.telesec.de/rl/EDRLab_CA.crl"
        val (status, data) = network.fetch(url, NetworkService.Method.GET)
        if (DEBUG) Timber.d("Status $status")
        if (status != 200 || data == null) {
            throw LcpException.CrlFetching
        }

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                "-----BEGIN X509 CRL-----${Base64.getEncoder().encodeToString(data)}-----END X509 CRL-----"
            } else {
                "-----BEGIN X509 CRL-----${android.util.Base64.encodeToString(data, android.util.Base64.DEFAULT)}-----END X509 CRL-----"
            }
    }

    // Returns (CRL, expired)
    private fun readLocal(): Pair<String?, Boolean> {
        val crl = preferences.getString(crlKey, null)
        val date = preferences.getString(dateKey, null)?.let {
            DateTime(preferences.getString(dateKey, null))
        }
        val expired = date?.let { daysSince(date) >= expiration } ?: true
        return Pair(crl, expired)
    }

    private fun saveLocal(crl: String): String {
        preferences.edit().putString(crlKey, crl).apply()
        preferences.edit().putString(dateKey, DateTime().toString()).apply()
        return crl
    }

    private fun daysSince(date: DateTime): Int {
        return Days.daysBetween(date, DateTime.now()).days
    }
}

