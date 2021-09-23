/*
 * Copyright 2021 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.testapp.outline

import android.os.Bundle
import org.readium.r2.shared.publication.Locator

object OutlineContract {

    private const val DESTINATION_KEY = "locator"

    val REQUEST_KEY: String = OutlineContract::class.java.name

    data class Result(val destination: Locator)

    fun createResult(locator: Locator): Bundle =
        Bundle().apply { putParcelable(DESTINATION_KEY, locator) }

    fun parseResult(result: Bundle): Result {
        val destination = requireNotNull(result.getParcelable<Locator>(DESTINATION_KEY))
        return Result(destination)
    }
}