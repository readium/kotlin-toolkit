/*
 * Copyright 2022 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.navigator.media3.api

import kotlin.time.Duration
import kotlinx.coroutines.flow.StateFlow
import org.readium.r2.shared.ExperimentalReadiumApi

/**
 * A [MediaNavigator] which can play audio files.
 */
@ExperimentalReadiumApi
interface AudioNavigator<L : AudioNavigator.Location, P : MediaNavigator.Playback,
    R : AudioNavigator.ReadingOrder> : MediaNavigator<L, P, R> {

    /**
     *  Marker interface for the [Location] flow.
     */
    interface Location : MediaNavigator.Location

    /**
     * State of the playback.
     */
    interface Playback : MediaNavigator.Playback {

        /**
         * Position of the playback in the current item.
         */
        val offset: Duration

        /**
         * Position in the current item until which the content is buffered.
         */
        val buffered: Duration?
    }

    /**
     * Data about the content to play.
     */
    interface ReadingOrder : MediaNavigator.ReadingOrder {

        /**
         * Total duration of the content to play.
         */
        val duration: Duration?

        /**
         * List of items to play.
         */
        override val items: List<Item>

        /**
         * A piece of the content to play..
         */
        interface Item : MediaNavigator.ReadingOrder.Item {

            /**
             * Duration of the item.
             */
            val duration: Duration?
        }
    }

    /**
     * Current location of the navigator.
     */
    override val location: StateFlow<L>

    /**
     * Reading order being read by this navigator.
     */
    override val readingOrder: R
}
