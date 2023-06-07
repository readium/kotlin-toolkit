/*
 * Copyright 2022 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.navigator.media3.api

import androidx.media3.common.Player
import kotlinx.coroutines.flow.StateFlow
import org.readium.r2.navigator.Navigator
import org.readium.r2.shared.ExperimentalReadiumApi
import org.readium.r2.shared.util.Closeable
import org.readium.r2.shared.util.Href

/**
 * A [Navigator] which can play multimedia content.
 */
@ExperimentalReadiumApi
interface MediaNavigator<
    L : MediaNavigator.Location,
    P : MediaNavigator.Playback,
    R : MediaNavigator.ReadingOrder
    > : Navigator, Closeable {

    /**
     *  Location of the navigator.
     */
    interface Location {

        val href: Href
    }

    /**
     * State of the player.
     */
    sealed interface State {

        /**
         * The navigator is ready to play.
         */
        interface Ready : State

        /**
         * The end of the media has been reached.
         */
        interface Ended : State

        /**
         * The navigator cannot play because the buffer is starved.
         */
        interface Buffering : State

        /**
         * The navigator cannot play because an error occurred.
         */
        interface Error : State
    }

    /**
     * State of the playback.
     */
    interface Playback {

        /**
         * The current state.
         */
        val state: State

        /**
         * Indicates if the navigator should play as soon as the state is Ready.
         */
        val playWhenReady: Boolean

        /**
         * Index of the reading order item currently being played.
         */
        val index: Int
    }

    /**
     * Data about the content to play.
     */
    interface ReadingOrder {

        /**
         * List of items to play.
         */
        val items: List<Item>

        /**
         * A piece of the content to play.
         */
        interface Item
    }

    /**
     * Current state of the playback.
     */
    val playback: StateFlow<P>

    /**
     * Current location of the navigator.
     */
    val location: StateFlow<L>

    /**
     * Reading order being read by this navigator.
     */
    val readingOrder: R

    /**
     * Resumes the playback at the current location.
     */
    fun play()

    /**
     * Pauses the playback.
     */
    fun pause()
}
