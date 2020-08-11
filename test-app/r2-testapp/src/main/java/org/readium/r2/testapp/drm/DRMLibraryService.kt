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

import org.readium.r2.lcp.LCPAuthenticatedLicense
import org.readium.r2.shared.drm.DRM
import org.readium.r2.shared.util.Try


data class DRMFulfilledPublication(
    val localURL: String,
    val suggestedFilename: String
)

interface DRMLibraryService {
    val brand: DRM.Brand
    fun canFulfill(file: String) : Boolean
    suspend fun fulfill(byteArray: ByteArray): Try<DRMFulfilledPublication, Exception>
}
