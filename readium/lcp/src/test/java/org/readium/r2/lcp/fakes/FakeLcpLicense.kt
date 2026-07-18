/*
 * Copyright 2026 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.lcp.fakes

import kotlin.time.Instant
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.json.JSONObject
import org.readium.r2.lcp.LcpError
import org.readium.r2.lcp.LcpLicense
import org.readium.r2.lcp.license.model.LicenseDocument
import org.readium.r2.lcp.license.model.StatusDocument
import org.readium.r2.shared.util.Try

class FakeLcpLicense(
    override val canCopy: Boolean = true,
    override val canPrint: Boolean = true,
) : LcpLicense {
    var closed = false

    override val license: LicenseDocument
        get() = LicenseDocument(json = JSONObject())
    override val status: StatusDocument? = null
    override val charactersToCopyLeft: StateFlow<Int?> = MutableStateFlow(null)
    override val pagesToPrintLeft: StateFlow<Int?> = MutableStateFlow(null)
    override val canRenewLoan: Boolean = false
    override val maxRenewDate: Instant? = null
    override val canReturnPublication: Boolean = false

    override suspend fun renewLoan(
        listener: LcpLicense.RenewListener,
        prefersWebPage: Boolean,
    ): Try<Instant?, LcpError> =
        Try.failure(failure = LcpError.Unknown(throwable = Exception()))

    override suspend fun returnPublication(): Try<Unit, LcpError> =
        Try.failure(failure = LcpError.Unknown(throwable = Exception()))

    override suspend fun decrypt(data: ByteArray): Try<ByteArray, LcpError> =
        Try.success(success = data)

    override fun close() {
        closed = true
    }

    override fun canCopy(text: String): Boolean = true
    override fun canPrint(pageCount: Int): Boolean = true
    override suspend fun copy(text: String): Boolean = true
    override suspend fun print(pageCount: Int): Boolean = true
}
