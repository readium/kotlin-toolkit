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
import org.joda.time.DateTime
import org.readium.r2.shared.drm.DRM
import org.readium.r2.shared.drm.DRMLicense
import java.io.Serializable


open class DRMViewModel(val drm: DRM, val context: Context) : Serializable {


    companion object {
        fun make(drm: DRM, context: Context): DRMViewModel {
            if (DRM.Brand.lcp == drm.brand) {
                return LCPViewModel(drm = drm, context = context)
            }
            return DRMViewModel(drm = drm, context = context)
        }
    }

    val license: DRMLicense?
        get() = drm.license
    open val type: String
        get() = drm.brand.rawValue
    open val state: String?
        get() = null
    open val provider: String?
        get() = null
    open val issued: DateTime?
        get() = null
    open val updated: DateTime?
        get() = null
    open val start: DateTime?
        get() = null
    open val end: DateTime?
        get() = null
    open val copiesLeft: String
        get() = "unlimited"
    open val printsLeft: String
        get() = "unlimited"
    open val canRenewLoan: Boolean
        get() = false

    open fun renewLoan(end: DateTime?, completion: (Exception?) -> Unit) {
        completion(null)
    }

    open val canReturnPublication: Boolean
        get() = false

    open fun returnPublication(completion: (Exception?) -> Unit) {
        completion(null)
    }
}
