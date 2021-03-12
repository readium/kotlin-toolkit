/*
 * Copyright 2021 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.testapp.tts

import android.os.Bundle
import org.readium.r2.shared.publication.Locator

object ScreenReaderContract {

    private const val LOCATOR_KEY = "locator"

    val REQUEST_KEY: String = ScreenReaderContract::class.java.name

    data class Arguments(val locator: Locator)

    fun createArguments(locator: Locator): Bundle =
        Bundle().apply { putParcelable(LOCATOR_KEY, locator) }

    fun parseArguments(result: Bundle): Arguments {
        val locator = requireNotNull(result.getParcelable<Locator>(LOCATOR_KEY))
        return Arguments(locator)
    }

    data class Result(val locator: Locator)

    fun createResult(locator: Locator): Bundle =
        Bundle().apply { putParcelable(LOCATOR_KEY, locator) }

    fun parseResult(result: Bundle): Result {
        val destination = requireNotNull(result.getParcelable<Locator>(LOCATOR_KEY))
        return Result(destination)
    }
}