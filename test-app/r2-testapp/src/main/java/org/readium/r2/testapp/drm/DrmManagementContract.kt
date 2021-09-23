/*
 * Copyright 2021 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.testapp.drm

import android.os.Bundle

object DrmManagementContract {

    private const val HAS_RETURNED_KEY = "hasReturned"

    val REQUEST_KEY: String = DrmManagementContract::class.java.name

    data class Result(val hasReturned: Boolean)

    fun createResult(hasReturned: Boolean): Bundle {
        return Bundle().apply {
            putBoolean(HAS_RETURNED_KEY, hasReturned)
        }
    }

    fun parseResult(result: Bundle): Result {
        val hasReturned = requireNotNull(result.getBoolean(HAS_RETURNED_KEY))
        return Result(hasReturned)
    }
}