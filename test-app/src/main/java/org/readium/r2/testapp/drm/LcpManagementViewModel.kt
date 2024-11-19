/*
 * Copyright 2021 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.testapp.drm

import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import java.util.Date
import org.readium.r2.lcp.LcpError
import org.readium.r2.lcp.LcpLicense
import org.readium.r2.shared.util.Try
import org.readium.r2.testapp.domain.toUserError
import org.readium.r2.testapp.utils.UserError

class LcpManagementViewModel(
    private val lcpLicense: LcpLicense,
    private val renewListener: LcpLicense.RenewListener,
) : DrmManagementViewModel() {

    class Factory(
        private val lcpLicense: LcpLicense,
        private val renewListener: LcpLicense.RenewListener,
    ) : ViewModelProvider.NewInstanceFactory() {

        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            modelClass.getDeclaredConstructor(
                LcpLicense::class.java,
                LcpLicense.RenewListener::class.java
            )
                .newInstance(lcpLicense, renewListener)
    }

    class LcpDrmError(
        override val error: LcpError,
    ) : DrmError {

        override fun toUserError(): UserError =
            error.toUserError()
    }

    override val type: String = "LCP"

    override val state: String?
        get() = lcpLicense.status?.status?.value

    override val provider: String
        get() = lcpLicense.license.provider

    override val issued: Date
        get() = lcpLicense.license.issued.toJavaDate()

    override val updated: Date
        get() = lcpLicense.license.updated.toJavaDate()

    override val start: Date?
        get() = lcpLicense.license.rights.start?.toJavaDate()

    override val end: Date?
        get() = lcpLicense.license.rights.end?.toJavaDate()

    override val copiesLeft: String =
        lcpLicense.charactersToCopyLeft.value
            ?.let { "$it characters" }
            ?: super.copiesLeft

    override val printsLeft: String =
        lcpLicense.pagesToPrintLeft.value
            ?.let { "$it pages" }
            ?: super.printsLeft

    override val canRenewLoan: Boolean
        get() = lcpLicense.canRenewLoan

    override suspend fun renewLoan(fragment: Fragment): Try<Date?, LcpDrmError> {
        return lcpLicense.renewLoan(renewListener)
            .map { it?.toJavaDate() }
            .mapFailure { LcpDrmError(it) }
    }

    override val canReturnPublication: Boolean
        get() = lcpLicense.canReturnPublication

    override suspend fun returnPublication(): Try<Unit, LcpDrmError> =
        lcpLicense.returnPublication()
            .mapFailure { LcpDrmError(it) }
}
