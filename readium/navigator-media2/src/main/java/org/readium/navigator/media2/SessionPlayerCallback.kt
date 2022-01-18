/*
 * Copyright 2022 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.navigator.media2

import androidx.media2.common.MediaItem
import androidx.media2.common.MediaMetadata
import androidx.media2.common.SessionPlayer
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.*
import timber.log.Timber
import kotlin.time.Duration
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalTime::class)
internal class SessionPlayerCallback(
    private val positionRefreshDelay: Duration,
) : SessionPlayer.PlayerCallback() {

    data class Item(
        val index: Int,
        val position: Duration,
        val buffered: Duration,
        val duration: Duration?,
    )

    var playbackCompleted: Boolean =
        false

    val playerState: Flow<SessionPlayerState>
        get() = _playerState.distinctUntilChanged()

    val currentItem: Flow<Item>
        get() = _currentItem.distinctUntilChanged()

    val playbackSpeed: Flow<Float>
        get() = _playbackSpeed.distinctUntilChanged()

    private val _playerState = MutableSharedFlow<SessionPlayerState>(
        replay = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )

    private val _currentItem = MutableSharedFlow<Item>(
        replay = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )

    private val _playbackSpeed = MutableSharedFlow<Float>(
        replay = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )

    init {
        _playbackSpeed.tryEmit(1f)
    }

    private val coroutineScope: CoroutineScope =
        MainScope()

    override fun onPlaylistChanged(player: SessionPlayer, list: MutableList<MediaItem>?, metadata: MediaMetadata?) {
        Timber.d("onPlaylistChanged")

        val item = getCurrentItem(player)
            ?: Item(0, Duration.ZERO, Duration.ZERO, null)
        _currentItem.tryEmit(item)

        coroutineScope.launch {
            while (isActive) {
                getCurrentItem(player)?.let { _currentItem.tryEmit(it) }
                delay(positionRefreshDelay)
            }
        }
    }

    override fun onSeekCompleted(player: SessionPlayer, position: Long) {
        Timber.d("onSeekCompleted $position")
        getCurrentItem(player)?.let { _currentItem.tryEmit(it) }
        playbackCompleted = false
    }

    override fun onPlayerStateChanged(player: SessionPlayer, state: Int) {
        Timber.d("onPlayerStateChanged $state")
        val newState = SessionPlayerState.from(state)
        _playerState.tryEmit(newState)
        if (newState == SessionPlayerState.Playing) {
            playbackCompleted = false
        }
    }

    override fun onPlaybackCompleted(player: SessionPlayer) {
        Timber.d("onPlaybackCompleted")
        playbackCompleted = true
    }

    override fun onCurrentMediaItemChanged(player: SessionPlayer, item: MediaItem?) {
        Timber.d("onCurrentMediaItemChanged $item")
        getCurrentItem(player)?.let { _currentItem.tryEmit(it)  }
    }

    override fun onPlaybackSpeedChanged(player: SessionPlayer, playbackSpeed: Float) {
        Timber.d("onPlaybackSpeedChanged")
        _playbackSpeed.tryEmit(playbackSpeed)
    }

    fun close() {
        coroutineScope.cancel()
    }

    private fun getCurrentItem(player: SessionPlayer): Item? {
        val index = player.currentIndexNullable ?: return null
        val position = player.currentPositionDuration ?: return null
        val buffered = player.bufferedPositionDuration ?: return null
        val duration = player.currentDuration ?: player.currentMediaItem?.metadata?.duration

        return if (index != player.currentMediaItemIndex) {
            // Current item has changed and data is stale.
            Timber.d("Ignoring stale state.")
            null
        } else {
            Item(index, position, buffered, duration)
        }
    }
}
