/*
 * Copyright 2022 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.navigator.media.common

import kotlinx.coroutines.flow.StateFlow
import org.readium.r2.navigator.Navigator
import org.readium.r2.shared.ExperimentalReadiumApi
import org.readium.r2.shared.util.Closeable
import org.readium.r2.shared.util.Url

/**
 * A [Navigator] which can play multimedia content.
 */
@ExperimentalReadiumApi
public interface MediaNavigator<
    L : MediaNavigator.Location,
    P : MediaNavigator.Playback,
    R : MediaNavigator.ReadingOrder,
    > : Navigator, Closeable {

    /**
     *  Location of the navigator.
     */
    public interface Location {

        public val href: Url
    }

    /**
     * State of the player.
     */
    public sealed interface State {

        /**
         * The navigator is ready to play.
         */
        public interface Ready : State

        /**
         * The end of the media has been reached.
         */
        public interface Ended : State

        /**
         * The navigator cannot play because the buffer is starved.
         */
        public interface Buffering : State

        /**
         * The navigator cannot play because an error occurred.
         */
        public interface Failure : State
    }

    /**
     * State of the playback.
     */
    /*
     * Copyright 2022 Readium Foundation. All rights reserved.
     * Use of this source code is governed by the BSD-style license
     * available in the top-level LICENSE file of the project.
     */
    public interface Playback {

        /**
         * The current state.
         */
        public val state: State

        /**
         * Indicates if the navigator should play as soon as the state is Ready.
         */
        public val playWhenReady: Boolean

        /**
         * Index of the reading order item currently being played.
         */
        public val index: Int
    }

    /**
     * Data about the content to play.
     */
    public interface ReadingOrder {

        /**
         * List of items to play.
         */
        public val items: List<Item>

        /**
         * A piece of the content to play.
         */
        public interface Item
    }

    /**
     * Current state of the playback.
     */
    public val playback: StateFlow<P>

    /**
     * Current location of the navigator.
     */
    public val location: StateFlow<L>

    /**
     * Reading order being read by this navigator.
     */
    public val readingOrder: R

    /**
     * Resumes the playback at the current location.
     */
    public fun play()

    /**
     * Pauses the playback.
     */
    public fun pause()
}
