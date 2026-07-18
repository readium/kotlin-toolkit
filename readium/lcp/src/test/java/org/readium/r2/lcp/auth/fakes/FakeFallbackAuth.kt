/*
 * Copyright 2026 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.lcp.auth.fakes

import org.readium.r2.lcp.LcpAuthenticating

class FakeFallbackAuth(val returnedPassphrase: String?) : LcpAuthenticating {
    var called = false
    var lastReason: LcpAuthenticating.AuthenticationReason? = null

    override suspend fun retrievePassphrase(
        license: LcpAuthenticating.AuthenticatedLicense,
        reason: LcpAuthenticating.AuthenticationReason,
        allowUserInteraction: Boolean,
    ): String? {
        called = true
        lastReason = reason
        return returnedPassphrase
    }
}
