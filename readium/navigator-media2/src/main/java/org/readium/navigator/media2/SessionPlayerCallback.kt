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
import kotlin.time.ExperimentalTime
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import timber.log.Timber

@OptIn(ExperimentalTime::class)
internal class SessionPlayerCallback(
    private val positionRefreshDelay: Duration,
    private val seekCompletedSender: SendChannel<Long>
) : SessionPlayer.PlayerCallback() {

    private val coroutineScope: CoroutineScope =
        MainScope()

    var playbackCompleted: Boolean =
        false

    val playerState: StateFlow<SessionPlayerState>
        get() = _playerState

    val bufferingState: StateFlow<SessionPlayerBufferingState>
        get() = _bufferingState

    val currentItem: StateFlow<ItemState>
        get() = _currentItem

    val playbackSpeed: StateFlow<Float>
        get() = _playbackSpeed

    private val _playerState = MutableStateFlow(
        SessionPlayerState.Idle
    )

    private val _bufferingState = MutableStateFlow(
        SessionPlayerBufferingState.BUFFERING_STATE_UNKNOWN
    )

    private val _currentItem = MutableStateFlow(
        ItemState(0, Duration.ZERO, Duration.ZERO, null)
    )

    private val _playbackSpeed = MutableStateFlow(1f)

    override fun onPlaylistChanged(
        player: SessionPlayer,
        list: MutableList<MediaItem>?,
        metadata: MediaMetadata?
    ) {
        Timber.d("onPlaylistChanged")

        _currentItem.tryEmit(player.currentItem)
        _playerState.tryEmit(player.stateEnum)
        _playbackSpeed.tryEmit(player.playbackSpeed)

        coroutineScope.launch {
            while (isActive) {
                _currentItem.tryEmit(player.currentItem)
                delay(positionRefreshDelay)
            }
        }
    }

    override fun onSeekCompleted(player: SessionPlayer, position: Long) {
        Timber.d("onSeekCompleted $position")
        _currentItem.tryEmit(player.currentItem)
        playbackCompleted = false
        seekCompletedSender.trySend(position)
    }

    override fun onPlayerStateChanged(player: SessionPlayer, state: Int) {
        Timber.d("onPlayerStateChanged $state")
        val newState = SessionPlayerState.fromCode(state)
        _playerState.tryEmit(newState)
        if (newState == SessionPlayerState.Playing) {
            playbackCompleted = false
        }
    }

    override fun onBufferingStateChanged(player: SessionPlayer, item: MediaItem?, buffState: Int) {
        Timber.d("onBufferingStateChanged $buffState")
        val newState = SessionPlayerBufferingState.fromCode(buffState)
        _bufferingState.tryEmit(newState)
    }

    override fun onPlaybackCompleted(player: SessionPlayer) {
        Timber.d("onPlaybackCompleted")
        playbackCompleted = true
    }

    override fun onCurrentMediaItemChanged(player: SessionPlayer, item: MediaItem?) {
        Timber.d("onCurrentMediaItemChanged $item")
        _currentItem.tryEmit(player.currentItem)
    }

    override fun onPlaybackSpeedChanged(player: SessionPlayer, playbackSpeed: Float) {
        Timber.d("onPlaybackSpeedChanged")
        _playbackSpeed.tryEmit(playbackSpeed)
    }

    fun close() {
        coroutineScope.cancel()
    }
}
