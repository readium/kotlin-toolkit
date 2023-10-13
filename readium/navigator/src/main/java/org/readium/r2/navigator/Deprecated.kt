/*
 * Module: r2-navigator-kotlin
 * Developers: Aferdita Muriqi
 *
 * Copyright (c) 2019. Readium Foundation. All rights reserved.
 * Use of this source code is governed by a BSD-style license which is detailed in the
 * LICENSE file present in the project repository where this source code is maintained.
 */

package org.readium.r2.navigator

import kotlin.time.Duration
import kotlinx.coroutines.flow.Flow
import org.readium.r2.navigator.media.MediaPlayback

@Deprecated("Use navigator fragments.", level = DeprecationLevel.ERROR)
public interface IR2Activity

@Deprecated("Use TtsNavigator.", level = DeprecationLevel.ERROR)
public interface IR2TTS

/**
 * A navigator rendering an audio or video publication.
 */
@Deprecated("Use the new readium-navigator-media modules.")
@OptIn(ExperimentalAudiobook::class)
public interface MediaNavigator : Navigator {

    /**
     * Current playback information.
     */
    public val playback: Flow<MediaPlayback>

    /**
     * Indicates whether the navigator is currently playing.
     */
    public val isPlaying: Boolean

    /**
     * Sets the speed of the media playback.
     *
     * Normal speed is 1.0 and 0.0 is incorrect.
     */
    public fun setPlaybackRate(rate: Double)

    /**
     * Resumes or start the playback at the current location.
     */
    public fun play()

    /**
     * Pauses the playback.
     */
    public fun pause()

    /**
     * Toggles the playback.
     * Can be useful as a handler for play/pause button.
     */
    public fun playPause()

    /**
     * Stops the playback.
     *
     * Compared to [pause], the navigator may clear its state in whatever way is appropriate. For
     * example, recovering a player's resources.
     */
    public fun stop()

    /**
     * Seeks to the given time in the current resource.
     */
    public fun seekTo(position: Duration)

    /**
     * Seeks relatively from the current position in the current resource.
     */
    public fun seekRelative(offset: Duration)

    public interface Listener : Navigator.Listener
}

/**
 * Moves to the left content portion (eg. page) relative to the reading progression direction.
 */
@Deprecated(
    "Use a DirectionalNavigationAdapter or goFoward and goBackward.",
    level = DeprecationLevel.ERROR
)
public fun VisualNavigator.goLeft(animated: Boolean = false, completion: () -> Unit = {}): Boolean {
    throw NotImplementedError()
}

/**
 * Moves to the right content portion (eg. page) relative to the reading progression direction.
 */
@Deprecated(
    "Use a DirectionalNavigationAdapter or goFoward and goBackward.",
    level = DeprecationLevel.ERROR
)
public fun VisualNavigator.goRight(animated: Boolean = false, completion: () -> Unit = {}): Boolean {
    throw NotImplementedError()
}
