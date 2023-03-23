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
interface SynchronizedMediaNavigator<P : SynchronizedMediaNavigator.Position> : MediaNavigator {

    interface Position : MediaNavigator.Position {

        val text: String

        val textBefore: String?

        val textAfter: String?

        val range: IntRange?

        val utteranceLocator: Locator

        val tokenLocator: Locator?
    }

    interface Resource : MediaNavigator.Resource {
        val utterance: String

        val range: IntRange?
    }

    interface ReadingOrder : MediaNavigator.ReadingOrder {

        override val items: List<Item>

        interface Item : MediaNavigator.ReadingOrder.Item
    }

    override val position: StateFlow<P>

    override val resource: StateFlow<Resource>

    override val readingOrder: ReadingOrder

    fun previousUtterance()

    fun nextUtterance()

    fun hasPreviousUtterance(): Boolean

    fun hasNextUtterance(): Boolean
}
