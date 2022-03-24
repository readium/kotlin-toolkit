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
    get() = SessionPlayerState.fromCode(playerState)

internal val SessionPlayer.playbackSpeedNullable
    get() =  playbackSpeed.takeUnless { it == 0f  }?.toDouble()

internal val SessionPlayer.currentIndexNullable
    get() = currentMediaItemIndex.takeUnless { it == SessionPlayer.INVALID_ITEM_INDEX }

@ExperimentalTime
internal val SessionPlayer.currentPositionDuration: Duration?
    get() = msToDuration(currentPosition)

@ExperimentalTime
internal val SessionPlayer.bufferedPositionDuration: Duration?
    get() = msToDuration(bufferedPosition)

@ExperimentalTime
internal val SessionPlayer.currentDuration: Duration?
    get() = msToDuration(duration)

@ExperimentalTime
private fun msToDuration(ms: Long): Duration? =
    if (ms == SessionPlayer.UNKNOWN_TIME)
        null
    else
        ms.milliseconds

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

