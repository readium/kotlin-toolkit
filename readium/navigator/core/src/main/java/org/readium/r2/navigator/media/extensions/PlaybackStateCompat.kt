/*
 * Copyright 2020 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.navigator.media.extensions

import android.os.SystemClock
import android.support.v4.media.session.PlaybackStateCompat
import org.readium.r2.navigator.ExperimentalAudiobook
import org.readium.r2.navigator.media.MediaPlayback

internal inline val PlaybackStateCompat.isPrepared get() =
    (state == PlaybackStateCompat.STATE_BUFFERING) ||
    (state == PlaybackStateCompat.STATE_PLAYING) ||
    (state == PlaybackStateCompat.STATE_PAUSED)

internal inline val PlaybackStateCompat.isPlaying get() =
    (state == PlaybackStateCompat.STATE_BUFFERING) ||
    (state == PlaybackStateCompat.STATE_PLAYING)

internal inline val PlaybackStateCompat.canPlay get() =
    (actions and PlaybackStateCompat.ACTION_PLAY != 0L) ||
    ((actions and PlaybackStateCompat.ACTION_PLAY_PAUSE != 0L) && (state == PlaybackStateCompat.STATE_PAUSED))

/**
 * Calculates the current playback position based on last update time along with playback
 * state and speed.
 */
internal inline val PlaybackStateCompat.elapsedPosition: Long get() =
    if (state == PlaybackStateCompat.STATE_PLAYING) {
        val timeDelta = SystemClock.elapsedRealtime() - lastPositionUpdateTime
        (position + (timeDelta * playbackSpeed)).toLong()
    } else {
        position
    }

@ExperimentalAudiobook
internal fun PlaybackStateCompat.toPlaybackState(): MediaPlayback.State =
    when (state) {
        PlaybackStateCompat.STATE_BUFFERING, PlaybackStateCompat.STATE_CONNECTING,
        PlaybackStateCompat.STATE_SKIPPING_TO_NEXT, PlaybackStateCompat.STATE_SKIPPING_TO_PREVIOUS,
        PlaybackStateCompat.STATE_SKIPPING_TO_QUEUE_ITEM ->
            MediaPlayback.State.Loading

        PlaybackStateCompat.STATE_PLAYING, PlaybackStateCompat.STATE_FAST_FORWARDING,
        PlaybackStateCompat.STATE_REWINDING ->
            MediaPlayback.State.Playing

        PlaybackStateCompat.STATE_PAUSED -> MediaPlayback.State.Paused

        else -> MediaPlayback.State.Idle
    }
