/*
 * Copyright 2022 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.navigator.media3.api

import kotlinx.coroutines.flow.StateFlow
import org.readium.r2.shared.ExperimentalReadiumApi
import org.readium.r2.shared.publication.Locator

/**
 * A [MediaNavigator] aware of the utterances that are being read aloud.
 */
@ExperimentalReadiumApi
interface SynchronizedMediaNavigator<P : MediaNavigator.Position> :
    MediaNavigator<P> {

    interface Utterance<P : MediaNavigator.Position> {
        val text: String

        val position: P

        val range: IntRange?

        val utteranceLocator: Locator

        val tokenLocator: Locator?
    }

    val utterance: StateFlow<Utterance<P>>
}
