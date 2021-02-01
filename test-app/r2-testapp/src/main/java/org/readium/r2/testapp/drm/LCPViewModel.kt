//TODO
/*
 * Module: r2-testapp-kotlin
 * Developers: Aferdita Muriqi
 *
 * Copyright (c) 2019. European Digital Reading Lab. All rights reserved.
 * Licensed to the Readium Foundation under one or more contributor license agreements.
 * Use of this source code is governed by a BSD-style license which is detailed in the
 * LICENSE file present in the project repository where this source code is maintained.
 */

package org.readium.r2.testapp.drm

import androidx.fragment.app.FragmentActivity
import org.readium.r2.lcp.LcpLicense
import org.readium.r2.lcp.LcpService
import org.readium.r2.lcp.MaterialRenewListener
import org.readium.r2.shared.util.Try
import java.io.File
import java.io.Serializable
import java.util.*

class LCPViewModel(val lcpLicense: LcpLicense, val activity: FragmentActivity) : DRMViewModel(activity), Serializable {

    companion object {

        suspend fun from(file: File, activity: FragmentActivity): LCPViewModel? {
            val service = LcpService(activity) ?: return null
            val license = service.retrieveLicense(file, allowUserInteraction = false)?.getOrNull() ?: return null
            return LCPViewModel(license, activity)
        }

    }

    override val type: String = "LCP"

    override val state: String? = lcpLicense.status?.status?.rawValue

    override val provider: String? = lcpLicense.license.provider

    override val issued: Date? = lcpLicense.license.issued

    override val updated: Date? = lcpLicense.license.updated

    override val start: Date? = lcpLicense.license.rights.start

    override val end: Date? = lcpLicense.license.rights.end

    override val copiesLeft: String =
        lcpLicense.charactersToCopyLeft
            ?.let { "$it characters" }
            ?: super.copiesLeft

    override val printsLeft: String =
        lcpLicense.pagesToPrintLeft
            ?.let { "$it pages" }
            ?: super.printsLeft

    override val canRenewLoan: Boolean = lcpLicense.canRenewLoan

    override suspend fun renewLoan(): Try<Date?, Exception> =
        lcpLicense.renewLoan(renewListener)

    private val renewListener = MaterialRenewListener(
        license = lcpLicense,
        caller = activity,
        fragmentManager = activity.supportFragmentManager
    )

    override val canReturnPublication: Boolean
        get() = lcpLicense.canReturnPublication

    override suspend fun returnPublication(): Try<Unit, Exception> =
        lcpLicense.returnPublication()

}
