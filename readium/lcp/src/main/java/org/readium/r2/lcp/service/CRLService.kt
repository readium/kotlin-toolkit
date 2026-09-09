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
import androidx.core.content.edit
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

        private const val CRL_URL = "http://crl.edrlab.telesec.de/rl/EDRLab_CA.crl"
    }

    suspend fun retrieve(): String {
        val (localCRL, isExpired) = readLocal()
        if (localCRL != null && !isExpired) {
            return localCRL.pem
        }

        return try {
            fetch()
                .also { saveLocal(it) }
                .pem
        } catch (e: Exception) {
            if (DEBUG) Timber.e(e)
            localCRL?.pem ?: throw e
        }
    }

    private suspend fun fetch(): Crl {
        val absoluteUrl = AbsoluteUrl(url = CRL_URL)!!
        val data = httpClient.fetch(HttpRequest(absoluteUrl))
            .map { it.body }
            .getOrElse { throw LcpException(LcpError.CrlFetching) }

        // The response might not be a CRL at all, for example when a captive portal returns its
        // HTML login page with a 200 status. Caching it would break the license validation until
        // the CRL expires, so we reject anything which is not a genuine X.509 CRL.
        // See https://github.com/readium/kotlin-toolkit/issues/832
        return Crl.fromDer(data)
            ?: run {
                if (DEBUG) Timber.e("The fetched CRL is not a valid X.509 CRL")
                throw LcpException(LcpError.CrlFetching)
            }
    }

    // Returns (CRL, expired)
    private fun readLocal(): Pair<Crl?, Boolean> {
        val crl = preferences.getString(CRL_KEY, null)
            ?.let { Crl.parsePem(it) }
        val date = preferences.getString(DATE_KEY, null)?.let { Instant.parse(input = it) }
        val expired = date?.let { daysSince(date) >= EXPIRATION } ?: true
        return Pair(crl, expired)
    }

    private fun saveLocal(crl: Crl) {
        preferences.edit {
            putString(CRL_KEY, crl.pem)
            putString(DATE_KEY, Clock.System.now().toString())
        }
    }

    private fun daysSince(date: Instant): Int {
        return date.daysUntil(other = Clock.System.now(), timeZone = TimeZone.currentSystemDefault())
    }
}
