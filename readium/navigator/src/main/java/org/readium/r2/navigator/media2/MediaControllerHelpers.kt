package org.readium.r2.navigator.media2

import androidx.media2.common.SessionPlayer
import androidx.media2.session.MediaController
import kotlin.time.Duration
import kotlin.time.ExperimentalTime

internal val MediaController.stateEnum: MediaControllerState
    get() = MediaControllerState.from(playerState)

internal val MediaController.playbackSpeedNullable
    get() =  playbackSpeed.takeUnless { it == 0f  }?.toDouble()

@ExperimentalTime
internal val MediaController.currentPositionDuration: Duration?
    get() = msToDuration(currentPosition)

@ExperimentalTime
internal val MediaController.bufferedPositionDuration: Duration?
    get() = msToDuration(bufferedPosition)

@ExperimentalTime
private fun msToDuration(ms: Long): Duration? =
    if (ms == SessionPlayer.UNKNOWN_TIME)
        null
    else
        Duration.milliseconds(ms)
