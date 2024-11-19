/*
 * Copyright 2022 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.navigator.media.common

import kotlin.time.Duration
import kotlinx.coroutines.flow.StateFlow
import org.readium.r2.shared.ExperimentalReadiumApi

/**
 * A [MediaNavigator] whose locations provide time offsets.
 */
@ExperimentalReadiumApi
public interface TimeBasedMediaNavigator<
    L : TimeBasedMediaNavigator.Location,
    P : TimeBasedMediaNavigator.Playback,
    R : TimeBasedMediaNavigator.ReadingOrder,
    > : MediaNavigator<L, P, R> {

    /**
     * Location of the navigator.
     */
    public interface Location : MediaNavigator.Location {

        /**
         * The duration offset in the resource.
         */
        public val offset: Duration
    }

    /**
     * State of the playback.
     */
    public interface Playback : MediaNavigator.Playback {

        /**
         * Position of the playback in the current item.
         */
        public val offset: Duration

        /**
         * Position in the current item until which the content is buffered.
         */
        public val buffered: Duration?
    }

    /**
     * Data about the content to play.
     */
    public interface ReadingOrder : MediaNavigator.ReadingOrder {

        /**
         * Total duration of the content to play.
         */
        public val duration: Duration?

        /**
         * List of items to play.
         */
        override val items: List<Item>

        /**
         * A piece of the content to play.
         */
        public interface Item : MediaNavigator.ReadingOrder.Item {

            /**
             * Duration of the item.
             */
            public val duration: Duration?
        }
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
     * Skips to [offset] in the item at [index].
     */
    public fun skipTo(index: Int, offset: Duration)

    /**
     * Skips [duration] either forward or backward if [duration] is negative.
     */
    public fun skip(duration: Duration)

    /**
     * Skips forward a small increment.
     */
    public fun skipForward()

    /**
     * Skips backward a small increment.
     */
    public fun skipBackward()
}
