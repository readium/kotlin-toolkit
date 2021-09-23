/*
 * Copyright 2021 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.testapp.drm

import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import org.readium.r2.lcp.LcpLicense
import org.readium.r2.lcp.MaterialRenewListener
import org.readium.r2.shared.util.Try
import java.util.*

class LcpManagementViewModel(
    private val lcpLicense: LcpLicense,
    private val renewListener: LcpLicense.RenewListener,
) : DrmManagementViewModel() {

    class Factory(
        private val lcpLicense: LcpLicense,
        private val renewListener: LcpLicense.RenewListener,
    ) : ViewModelProvider.NewInstanceFactory() {

        override fun <T : ViewModel?> create(modelClass: Class<T>): T =
            modelClass.getDeclaredConstructor(LcpLicense::class.java, LcpLicense.RenewListener::class.java)
                .newInstance(lcpLicense, renewListener)
    }

    override val type: String = "LCP"

    override val state: String?
        get() = lcpLicense.status?.status?.rawValue

    override val provider: String?
        get() = lcpLicense.license.provider

    override val issued: Date?
        get() = lcpLicense.license.issued

    override val updated: Date?
        get() = lcpLicense.license.updated

    override val start: Date?
        get() = lcpLicense.license.rights.start

    override val end: Date?
        get() = lcpLicense.license.rights.end

    override val copiesLeft: String =
        lcpLicense.charactersToCopyLeft
            ?.let { "$it characters" }
            ?: super.copiesLeft

    override val printsLeft: String =
        lcpLicense.pagesToPrintLeft
            ?.let { "$it pages" }
            ?: super.printsLeft

    override val canRenewLoan: Boolean
        get() = lcpLicense.canRenewLoan

    override suspend fun renewLoan(fragment: Fragment): Try<Date?, Exception> {
        return lcpLicense.renewLoan(renewListener)
    }

    override val canReturnPublication: Boolean
        get() = lcpLicense.canReturnPublication

    override suspend fun returnPublication(): Try<Unit, Exception> =
        lcpLicense.returnPublication()

}
