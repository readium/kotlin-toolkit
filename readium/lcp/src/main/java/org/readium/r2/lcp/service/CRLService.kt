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
import androidx.core.content.edit
import java.util.Base64
import kotlin.time.Clock
import kotlin.time.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.daysUntil
import org.readium.r2.lcp.BuildConfig.DEBUG
import org.readium.r2.lcp.LcpError
import org.readium.r2.lcp.LcpException
import org.readium.r2.shared.util.AbsoluteUrl
import org.readium.r2.shared.util.getOrElse
import org.readium.r2.shared.util.http.HttpClient
import org.readium.r2.shared.util.http.HttpRequest
import org.readium.r2.shared.util.http.fetch
import timber.log.Timber

internal class CRLService(val httpClient: HttpClient, val context: Context) {

    private val preferences: SharedPreferences = context.getSharedPreferences(
        "org.readium.r2.lcp",
        Context.MODE_PRIVATE
    )

    companion object {
        const val EXPIRATION = 7
        const val CRL_KEY = "org.readium.r2-lcp-swift.CRL"
        const val DATE_KEY = "org.readium.r2-lcp-swift.CRLDate"
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
        val absoluteUrl = AbsoluteUrl(url = "http://crl.edrlab.telesec.de/rl/EDRLab_CA.crl")!!
        val data = httpClient.fetch(HttpRequest(absoluteUrl))
            .map { it.body }
            .getOrElse { throw LcpException(LcpError.CrlFetching) }

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            "-----BEGIN X509 CRL-----${Base64.getEncoder().encodeToString(data)}-----END X509 CRL-----"
        } else {
            "-----BEGIN X509 CRL-----${android.util.Base64.encodeToString(
                data,
                android.util.Base64.DEFAULT
            )}-----END X509 CRL-----"
        }
    }

    // Returns (CRL, expired)
    private fun readLocal(): Pair<String?, Boolean> {
        val crl = preferences.getString(CRL_KEY, null)
        val date = preferences.getString(DATE_KEY, null)?.let { Instant.parse(input = it) }
        val expired = date?.let { daysSince(date) >= EXPIRATION } ?: true
        return Pair(crl, expired)
    }

    private fun saveLocal(crl: String): String {
        preferences.edit { putString(CRL_KEY, crl) }
        preferences.edit { putString(DATE_KEY, Clock.System.now().toString()) }
        return crl
    }

    private fun daysSince(date: Instant): Int {
        return date.daysUntil(other = Clock.System.now(), timeZone = TimeZone.currentSystemDefault())
    }
}
