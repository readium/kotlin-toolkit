package org.readium.r2.navigator.media2

import androidx.media2.common.SessionPlayer

internal enum class MediaControllerState {
    Idle,
    Paused,
    Playing,
    Error;

    companion object {
        fun from(sessionPlayerState: Int) = when (sessionPlayerState) {
            SessionPlayer.PLAYER_STATE_IDLE -> Idle
            SessionPlayer.PLAYER_STATE_PAUSED -> Paused
            SessionPlayer.PLAYER_STATE_PLAYING -> Playing
            else -> Error // SessionPlayer.PLAYER_STATE_ERROR
        }
    }
}
