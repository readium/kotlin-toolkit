/*
 * Copyright 2022 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.navigator.media3.api

import kotlinx.coroutines.flow.StateFlow
import org.readium.r2.shared.ExperimentalReadiumApi
import org.readium.r2.shared.publication.Locator

@ExperimentalReadiumApi
interface SynchronizedMediaNavigator<E : MediaNavigator.Error> : MediaNavigator<E> {

    data class Utterance(
        val locator: Locator,
        val range: IntRange?
    ) {

        val rangeLocator: Locator? = range
            ?.let { locator.copy(text = locator.text.substring(it)) }
    }

    val utterance: StateFlow<Utterance>
}
