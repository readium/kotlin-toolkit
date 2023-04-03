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
 * A [MediaNavigator] aware of the utterances being read aloud.
 */
@ExperimentalReadiumApi
interface TextAwareMediaNavigator<L : TextAwareMediaNavigator.Location, P : TextAwareMediaNavigator.Playback,
    R : TextAwareMediaNavigator.ReadingOrder> : MediaNavigator<L, P, R> {

    /**
     * Location of the navigator.
     */
    interface Location : MediaNavigator.Location {

        /**
         * The utterance being played.
         */
        val utterance: String

        /**
         * The text right before the utterance being played, if any in the current item.
         */
        val textBefore: String?

        /**
         * The text right after the utterance being played, if any in the current item.
         */
        val textAfter: String?

        /**
         * The range of [utterance] being played, if known.
         */
        val range: IntRange?

        /**
         * A locator pointing to the current utterance.
         */
        val utteranceLocator: Locator

        /**
         * A locator pointing to the current token, if known.
         */
        val tokenLocator: Locator?
    }

    /**
     * State of the playback.
     */
    interface Playback : MediaNavigator.Playback {

        /**
         * The utterance being played.
         */
        val utterance: String

        /**
         * The range of [utterance] being played.
         */
        val range: IntRange?
    }

    /**
     * Data about the content to play.
     */
    interface ReadingOrder : MediaNavigator.ReadingOrder {

        /**
         * List of items to play.
         */
        override val items: List<Item>

        /**
         * A piece of the content to play..
         */
        interface Item : MediaNavigator.ReadingOrder.Item
    }

    /**
     * Current location of the navigator.
     */
    override val location: StateFlow<L>

    /**
     * Reading order being read by this navigator.
     */
    override val readingOrder: R

    /**
     * Jumps to the previous.
     *
     * Does nothing if the current utterance is the first one.
     */
    fun previousUtterance()

    /**
     * Jumps to the next utterance.
     *
     * Does nothing if the current utterance is the last one.
     */
    fun nextUtterance()

    /**
     * Whether the current utterance has a previous one or is the first one.
     */
    fun hasPreviousUtterance(): Boolean

    /**
     * Whether the current utterance has a next utterance or is the last one.
     */
    fun hasNextUtterance(): Boolean
}
