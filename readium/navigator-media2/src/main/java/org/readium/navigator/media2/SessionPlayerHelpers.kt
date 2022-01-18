/*
 * Copyright 2022 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.navigator.media2

import androidx.media2.common.MediaItem
import androidx.media2.common.MediaMetadata
import androidx.media2.common.SessionPlayer
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.ExperimentalTime

internal val SessionPlayer.stateEnum: SessionPlayerState
    get() = SessionPlayerState.from(playerState)

internal val SessionPlayer.playbackSpeedNullable
    get() =  playbackSpeed.takeUnless { it == 0f  }?.toDouble()

@ExperimentalTime
internal val SessionPlayer.currentPositionDuration: Duration?
    get() = msToDuration(currentPosition)

@ExperimentalTime
internal val SessionPlayer.bufferedPositionDuration: Duration?
    get() = msToDuration(bufferedPosition)

@ExperimentalTime
private fun msToDuration(ms: Long): Duration? =
    if (ms == SessionPlayer.UNKNOWN_TIME)
        null
    else
        ms.milliseconds

/**
 * Metadata
 */

internal val MediaMetadata.index: Int
    get() = getLong(MediaMetadata.METADATA_KEY_TRACK_NUMBER).toInt()

@ExperimentalTime
internal val MediaMetadata.duration: Duration?
    get() = getLong(MediaMetadata.METADATA_KEY_DURATION)
        .takeUnless { it == 0L }
        ?.milliseconds

@ExperimentalTime
internal val List<MediaMetadata>.durations: List<Duration>?
    get() {
        val durations = mapNotNull { it.duration }
        return durations.takeIf { it.size == this.size }
    }

@ExperimentalTime
internal val List<MediaItem>.metadata: List<MediaMetadata>
    get() = map { it.metadata!! }

