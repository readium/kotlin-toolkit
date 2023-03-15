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
interface SynchronizedMediaNavigator<P : MediaNavigator.Position,
    U : SynchronizedMediaNavigator.Utterance.Position> :
    MediaNavigator<P> {

    interface Utterance<U : Utterance.Position> {
        val text: String

        val position: U

        val range: IntRange?

        val utteranceLocator: Locator

        val tokenLocator: Locator?

        interface Position
    }

    val utterance: StateFlow<Utterance<U>>

    fun previousUtterance()

    fun nextUtterance()

    fun hasPreviousUtterance(): Boolean

    fun hasNextUtterance(): Boolean
}
