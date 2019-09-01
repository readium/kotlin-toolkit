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

import android.content.Context
import android.content.Intent
import android.net.Uri
import org.joda.time.DateTime
import org.readium.r2.lcp.public.LCPLicense
import org.readium.r2.shared.drm.DRM
import java.io.Serializable
import java.net.URL

class LCPViewModel(drm: DRM, context: Context) : DRMViewModel(drm, context), Serializable {

    private val lcpLicense: LCPLicense?
        get() {
            val license = license ?: return null
            return license as? LCPLicense
        }
    override val type: String
        get() = "LCP"
    override val state: String?
        get() = lcpLicense?.status?.status?.rawValue
    override val provider: String?
        get() = lcpLicense?.license?.provider
    override val issued: DateTime?
        get() = lcpLicense?.license?.issued
    override val updated: DateTime?
        get() = lcpLicense?.license?.updated
    override val start: DateTime?
        get() = lcpLicense?.license?.rights?.start
    override val end: DateTime?
        get() = lcpLicense?.license?.rights?.end
    override val copiesLeft: String
        get() {
            lcpLicense?.charactersToCopyLeft?.let {
                return "$it characters"
            }
            return super.copiesLeft
        }
    override val printsLeft: String
        get() {
            lcpLicense?.pagesToPrintLeft?.let {
                return "$it pages"
            }
            return super.printsLeft
        }
    override val canRenewLoan: Boolean
        get() = lcpLicense?.canRenewLoan ?: false

    // TODO do i need this?
//    private var renewCallbacks: Map<Int, () -> Unit> = mapOf()

    override fun renewLoan(end: DateTime?, completion: (Exception?) -> Unit) {
        val lcpLicense = lcpLicense
        if (lcpLicense == null) {
            completion(null)
            return
        }
        lcpLicense.renewLoan(end, { url: URL, dismissed: () -> Unit ->
            val intent = Intent(Intent.ACTION_VIEW)
            intent.data = Uri.parse(url.toString())
            context.startActivity(intent)
        }, completion)
    }

    override val canReturnPublication: Boolean
        get() = lcpLicense?.canReturnPublication ?: false

    override fun returnPublication(completion: (Exception?) -> Unit) {
        val lcpLicense = lcpLicense
        if (lcpLicense == null) {
            completion(null)
            return
        }
        lcpLicense.returnPublication(completion = completion)
    }
}
