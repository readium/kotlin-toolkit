/*
 * Copyright 2026 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.lcp.service

import java.io.ByteArrayInputStream
import java.security.cert.CertificateFactory
import kotlin.io.encoding.Base64
import org.readium.r2.lcp.BuildConfig.DEBUG
import timber.log.Timber

/**
 * A genuine X.509 certificate revocation list, in its PEM encoding.
 *
 * An instance can only be created from a payload which was successfully parsed as an X.509 CRL.
 */
@JvmInline
internal value class Crl private constructor(val pem: String) {

    companion object {
        private const val PEM_HEADER = "-----BEGIN X509 CRL-----"
        private const val PEM_FOOTER = "-----END X509 CRL-----"

        /**
         * Creates a [Crl] from its DER encoding, or returns null if [data] is not an X.509 CRL.
         */
        fun fromDer(data: ByteArray): Crl? {
            if (!isX509Crl(data)) {
                return null
            }
            return Crl("$PEM_HEADER${Base64.encode(data)}$PEM_FOOTER")
        }

        /**
         * Parses a PEM-encoded [Crl], or returns null if [pem] is not an X.509 CRL.
         */
        fun parsePem(pem: String): Crl? {
            if (!pem.startsWith(PEM_HEADER) || !pem.endsWith(PEM_FOOTER)) {
                return null
            }
            val base64 = pem.substring(PEM_HEADER.length, pem.length - PEM_FOOTER.length)
            val data = try {
                // Decoding with [Base64.Mime] instead of [Base64.Default], as it ignores the line
                // breaks found in the CRLs cached by previous versions of the toolkit on API level
                // 25 and below.
                Base64.Mime.decode(base64)
            } catch (e: IllegalArgumentException) {
                if (DEBUG) Timber.e(e)
                return null
            }
            if (!isX509Crl(data)) {
                return null
            }
            return Crl(pem)
        }

        /**
         * Checks that [data] contains a DER-encoded X.509 CRL.
         */
        private fun isX509Crl(data: ByteArray): Boolean =
            try {
                CertificateFactory.getInstance("X.509")
                    .generateCRL(ByteArrayInputStream(data)) != null
            } catch (e: Exception) {
                if (DEBUG) Timber.e(e)
                false
            }
    }
}
