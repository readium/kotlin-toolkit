/*
 * Copyright 2022 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.navigator.media2

import androidx.media2.common.SessionPlayer
import org.readium.r2.shared.util.Try
import kotlin.IllegalStateException

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

internal enum class SessionPlayerError{
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
            return when(resultCode) {
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
