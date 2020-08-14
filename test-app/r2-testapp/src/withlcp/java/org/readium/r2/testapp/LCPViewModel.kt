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

package org.readium.r2.testapp

import android.content.Intent
import android.net.Uri
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.browser.customtabs.CustomTabsIntent
import androidx.core.content.ContextCompat
import kotlinx.coroutines.runBlocking
import org.joda.time.DateTime
import org.readium.r2.lcp.LcpLicense
import org.readium.r2.lcp.LcpService
import org.readium.r2.shared.util.Try
import java.io.File
import java.io.Serializable
import java.net.URL
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class LCPViewModel(val file: File, val activity: ComponentActivity) : DRMViewModel(activity), Serializable {

    private val lcpLicense: LcpLicense? = runBlocking {
        LcpService.create(activity)
            .retrieveLicense(file, null, allowUserInteraction = false)
            ?.getOrNull()
    }

    override val type: String = "LCP"

    override val state: String? = lcpLicense?.status?.status?.rawValue

    override val provider: String? = lcpLicense?.license?.provider

    override val issued: DateTime? = lcpLicense?.license?.issued

    override val updated: DateTime? = lcpLicense?.license?.updated

    override val start: DateTime? = lcpLicense?.license?.rights?.start

    override val end: DateTime? = lcpLicense?.license?.rights?.end

    override val copiesLeft: String =
        lcpLicense?.charactersToCopyLeft
            ?.let { "$it characters" }
            ?: super.copiesLeft

    override val printsLeft: String =
        lcpLicense?.pagesToPrintLeft
            ?.let { "$it pages" }
            ?: super.printsLeft

    override val canRenewLoan: Boolean = lcpLicense?.canRenewLoan ?: false

    override suspend fun renewLoan(end: DateTime?): Try<Unit, Exception> {
        val lcpLicense = lcpLicense ?: return super.renewLoan(end)

        suspend fun urlPresenter(url: URL): Unit = suspendCoroutine { cont ->
            val intent = CustomTabsIntent.Builder().build().intent.apply {
                data = Uri.parse(url.toString())
            }

            val launcher = activity.registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
                cont.resume(Unit)
            }
            launcher.launch(intent)
        }

        return lcpLicense.renewLoan(end, ::urlPresenter)
    }

    override val canReturnPublication: Boolean
        get() = lcpLicense?.canReturnPublication ?: false

    override suspend fun returnPublication(): Try<Unit, Exception> =
        lcpLicense?.returnPublication() ?: super.returnPublication()
}
