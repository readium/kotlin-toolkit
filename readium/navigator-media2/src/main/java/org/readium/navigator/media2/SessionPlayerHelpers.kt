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
import org.readium.r2.shared.util.Try

internal enum class SessionPlayerState {
    Idle,
    Paused,
    Playing,
    Error;

    companion object {
        fun fromCode(sessionPlayerState: Int) = when (sessionPlayerState) {
            SessionPlayer.PLAYER_STATE_IDLE -> Idle
            SessionPlayer.PLAYER_STATE_PAUSED -> Paused
            SessionPlayer.PLAYER_STATE_PLAYING -> Playing
            else -> Error // SessionPlayer.PLAYER_STATE_ERROR
        }
    }
}

internal val SessionPlayer.stateEnum: SessionPlayerState
    get() = SessionPlayerState.fromCode(playerState)

internal enum class SessionPlayerBufferingState {
    BUFFERING_STATE_BUFFERING_AND_PLAYABLE,
    BUFFERING_STATE_BUFFERING_AND_STARVED,
    BUFFERING_STATE_COMPLETE,
    BUFFERING_STATE_UNKNOWN;

    companion object {

        fun fromCode(code: Int) = when (code) {
            SessionPlayer.BUFFERING_STATE_COMPLETE ->
                BUFFERING_STATE_COMPLETE
            SessionPlayer.BUFFERING_STATE_BUFFERING_AND_PLAYABLE ->
                BUFFERING_STATE_BUFFERING_AND_PLAYABLE
            SessionPlayer.BUFFERING_STATE_BUFFERING_AND_STARVED ->
                BUFFERING_STATE_BUFFERING_AND_STARVED
            SessionPlayer.BUFFERING_STATE_UNKNOWN ->
                BUFFERING_STATE_UNKNOWN
            else ->
                throw IllegalStateException("Invalid buffering state code.")
        }
    }
}

internal typealias SessionPlayerResult = Try<Unit, SessionPlayerException>

internal class SessionPlayerException(val error: SessionPlayerError) : Exception()

internal enum class SessionPlayerError {
    BAD_VALUE,
    INVALID_STATE,
    IO,
    NOT_SUPPORTED,
    PERMISSION_DENIED,
    SESSION_AUTHENTICATION_EXPIRED,
    SESSION_CONCURRENT_STREAM_LIMIT,
    SESSION_DISCONNECTED,
    SESSION_NOT_AVAILABLE_IN_REGION,
    SESSION_PARENTAL_CONTROL_RESTRICTED,
    SESSION_PREMIUM_ACCOUNT_REQUIRED,
    ERROR_SESSION_SETUP_REQUIRED,
    SESSION_SKIP_LIMIT_REACHED,
    UNKNOWN,
    INFO_SKIPPED;

    companion object {

        fun fromCode(resultCode: Int): SessionPlayerError {
            require(resultCode != 0)
            return when (resultCode) {
                -3 -> BAD_VALUE
                -2 -> INVALID_STATE
                -5 -> IO
                -6 -> NOT_SUPPORTED
                -4 -> PERMISSION_DENIED
                -102 -> SESSION_AUTHENTICATION_EXPIRED
                -104 -> SESSION_CONCURRENT_STREAM_LIMIT
                -100 -> SESSION_DISCONNECTED
                -106 -> SESSION_NOT_AVAILABLE_IN_REGION
                -105 -> SESSION_PARENTAL_CONTROL_RESTRICTED
                -103 -> SESSION_PREMIUM_ACCOUNT_REQUIRED
                -108 -> ERROR_SESSION_SETUP_REQUIRED
                -107 -> SESSION_SKIP_LIMIT_REACHED
                1 -> INFO_SKIPPED
                else -> UNKNOWN // -1
            }
        }
    }
}

internal data class ItemState(
    val index: Int,
    val position: Duration,
    val buffered: Duration,
    val duration: Duration?,
)

@OptIn(ExperimentalTime::class)
internal val SessionPlayer.currentItem: ItemState
    get() {
        fun currentItemUnsafe(): ItemState {
            val index = checkNotNull(currentIndexNullable)
            val position = currentPositionDuration ?: Duration.ZERO
            val buffered = bufferedPositionDuration ?: Duration.ZERO
            val duration = currentDuration ?: currentMediaItem?.metadata?.duration
            return ItemState(index, position, buffered, duration)
        }

        var item = currentItemUnsafe()
        while (item.index != currentMediaItemIndex) {
            // Index might have changed before the call to position, so data might be inconsistent
            item = currentItemUnsafe()
        }

        return item
    }

internal val SessionPlayer.playbackSpeedNullable
    get() = playbackSpeed.takeUnless { it == 0f }?.toDouble()

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
