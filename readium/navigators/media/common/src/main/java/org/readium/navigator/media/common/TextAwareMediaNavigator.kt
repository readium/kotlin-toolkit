/*
 * Copyright 2022 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.navigator.media.common

import kotlinx.coroutines.flow.StateFlow
import org.readium.r2.shared.ExperimentalReadiumApi
import org.readium.r2.shared.publication.Locator

/**
 * A [MediaNavigator] aware of the utterances being read aloud.
 */
@ExperimentalReadiumApi
public interface TextAwareMediaNavigator<
    L : TextAwareMediaNavigator.Location,
    P : TextAwareMediaNavigator.Playback,
    R : TextAwareMediaNavigator.ReadingOrder,
    > : MediaNavigator<L, P, R> {

    /**
     * Location of the navigator.
     */
    public interface Location : MediaNavigator.Location {

        /**
         * The utterance being played.
         */
        public val utterance: String

        /**
         * The text right before the utterance being played, if any in the current item.
         */
        public val textBefore: String?

        /**
         * The text right after the utterance being played, if any in the current item.
         */
        public val textAfter: String?

        /**
         * The range of [utterance] being played, if known.
         */
        public val range: IntRange?

        /**
         * A locator pointing to the current utterance.
         */
        public val utteranceLocator: Locator

        /**
         * A locator pointing to the current token, if known.
         */
        public val tokenLocator: Locator?
    }

    /**
     * State of the playback.
     */
    public interface Playback : MediaNavigator.Playback {

        /**
         * The utterance being played.
         */
        public val utterance: String

        /**
         * The range of [utterance] being played.
         */
        public val range: IntRange?
    }

    /**
     * Data about the content to play.
     */
    public interface ReadingOrder : MediaNavigator.ReadingOrder {

        /**
         * List of items to play.
         */
        override val items: List<Item>

        /**
         * A piece of the content to play..
         */
        public interface Item : MediaNavigator.ReadingOrder.Item
    }

    /**
     * Current state of the playback.
     */
    override val playback: StateFlow<P>

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
    public fun skipToPreviousUtterance()

    /**
     * Jumps to the next utterance.
     *
     * Does nothing if the current utterance is the last one.
     */
    public fun skipToNextUtterance()

    /**
     * Whether the current utterance has a previous one or is the first one.
     */
    public fun hasPreviousUtterance(): Boolean

    /**
     * Whether the current utterance has a next utterance or is the last one.
     */
    public fun hasNextUtterance(): Boolean
}
