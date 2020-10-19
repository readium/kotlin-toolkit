/*
 * Module: r2-testapp-kotlin
 * Developers: Aferdita Muriqi, Clément Baumann
 *
 * Copyright (c) 2018. Readium Foundation. All rights reserved.
 * Use of this source code is governed by a BSD-style license which is detailed in the
 * LICENSE file present in the project repository where this source code is maintained.
 */

package org.readium.r2.testapp

import android.os.Bundle
import org.readium.r2.lcp.LcpService
import org.readium.r2.lcp.auth.LcpDialogAuthentication
import org.readium.r2.shared.util.Try
import org.readium.r2.testapp.drm.DRMFulfilledPublication
import org.readium.r2.testapp.library.LibraryActivity
import java.io.File

class CatalogActivity : LibraryActivity() {

    private lateinit var lcpService: LcpService

    override fun onCreate(savedInstanceState: Bundle?) {
            lcpService = LcpService.create(this) ?: throw Exception("liblcp is missing on the classpath")
        contentProtections = listOf(lcpService.contentProtection())
        super.onCreate(savedInstanceState)
    }

    override suspend fun fulfill(file: File): Try<DRMFulfilledPublication, Exception> =
        lcpService.acquirePublication(file).map { DRMFulfilledPublication(it.localFile, it.suggestedFilename) }

}
