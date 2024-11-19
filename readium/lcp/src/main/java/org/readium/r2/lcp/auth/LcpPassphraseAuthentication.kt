/*
 * Copyright 2020 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.lcp.auth

import org.readium.r2.lcp.LcpAuthenticating

/**
 * An [LcpAuthenticating] implementation which can directly use a provided clear or hashed
 * passphrase.
 *
 * If the provided [passphrase] is incorrect, the given [fallback] authentication is used.
 */
public class LcpPassphraseAuthentication(
    private val passphrase: String,
    private val fallback: LcpAuthenticating? = null,
) : LcpAuthenticating {

    override suspend fun retrievePassphrase(
        license: LcpAuthenticating.AuthenticatedLicense,
        reason: LcpAuthenticating.AuthenticationReason,
        allowUserInteraction: Boolean,
    ): String? {
        if (reason != LcpAuthenticating.AuthenticationReason.PassphraseNotFound) {
            return fallback?.retrievePassphrase(
                license,
                reason,
                allowUserInteraction = allowUserInteraction
            )
        }

        return passphrase
    }
}
