package org.readium.r2.navigator.media2

import androidx.media2.common.MediaItem
import androidx.media2.common.MediaMetadata
import androidx.media2.common.SessionPlayer
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.*
import org.readium.r2.navigator.ExperimentalAudiobook
import timber.log.Timber
import kotlin.time.Duration
import kotlin.time.ExperimentalTime

@ExperimentalAudiobook
@OptIn(ExperimentalTime::class)
internal class SessionPlayerCallback(
    private val positionRefreshDelay: Duration,
) : SessionPlayer.PlayerCallback() {

    var playbackCompleted: Boolean =
        false

    val playerState: Flow<SessionPlayerState>
        get() = _playerState.filterNotNull().distinctUntilChanged()

    val currentPosition: Flow<Duration>
        get() = _currentPosition.filterNotNull().distinctUntilChanged()

    val bufferedPosition: Flow<Duration>
        get() = _bufferedPosition.filterNotNull().distinctUntilChanged()

    val currentItem: Flow<MediaMetadata>
        get() = _currentItem.filterNotNull()

    val playbackSpeed: Flow<Float>
        get() = _playbackSpeed

    private val _playerState = MutableSharedFlow<SessionPlayerState>(
        replay = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )

    private val _currentPosition = MutableSharedFlow<Duration?>(
        replay = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )

    private val _bufferedPosition = MutableSharedFlow<Duration?>(
        replay = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )

    private val _currentItem = MutableSharedFlow<MediaMetadata?>(
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
        coroutineScope.launch {
            while (isActive) {
                updatePosition(player)
                delay(positionRefreshDelay)
            }
        }
    }

    override fun onSeekCompleted(player: SessionPlayer, position: Long) {
        Timber.d("onSeekCompleted $position")
        updatePosition(player)
        playbackCompleted = false
    }

    private fun updatePosition(player: SessionPlayer) {
        _currentPosition.tryEmit(player.currentPositionDuration)
        _bufferedPosition.tryEmit(player.bufferedPositionDuration)
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
        _currentItem.tryEmit(item?.metadata)
    }

    override fun onPlaybackSpeedChanged(player: SessionPlayer, playbackSpeed: Float) {
        Timber.d("onPlaybackSpeedChanged")
        _playbackSpeed.tryEmit(playbackSpeed)
    }

    fun close() {
        coroutineScope.cancel()
    }
}
