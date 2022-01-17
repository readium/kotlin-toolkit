/*
 * Copyright 2022 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.navigator.media2

import androidx.media2.common.SessionPlayer

internal enum class SessionPlayerState {
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
