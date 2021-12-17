package org.readium.r2.navigator.media2

import androidx.media2.common.MediaItem
import androidx.media2.common.MediaMetadata
import androidx.media2.session.MediaController
import androidx.media2.session.SessionCommandGroup
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.*
import org.readium.r2.navigator.ExperimentalAudiobook
import timber.log.Timber
import kotlin.time.Duration
import kotlin.time.ExperimentalTime

@ExperimentalAudiobook
@OptIn(ExperimentalTime::class)
internal class MediaControllerCallback(
    private val positionRefreshDelay: Duration,
) : MediaController.ControllerCallback() {

    val connectedState: Flow<Boolean>
        get() = _connectedState

    val mediaControllerState: Flow<MediaControllerState>
        get() = _playerState.filterNotNull().distinctUntilChanged()

    val currentItem: Flow<MediaMetadata>
        get() = _currentItem.filterNotNull().distinctUntilChanged()

    val currentPosition: Flow<Duration>
        get() = _currentPosition.filterNotNull().distinctUntilChanged()

    val bufferedPosition: Flow<Duration>
        get() = _bufferedPosition.filterNotNull().distinctUntilChanged()

    private val _connectedState = MutableStateFlow(false)

    private val _playerState = MutableSharedFlow<MediaControllerState?>(
        replay = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )

    private val _currentItem = MutableSharedFlow<MediaMetadata?>(
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

    private val coroutineScope: CoroutineScope =
        CoroutineScope(Dispatchers.Default + SupervisorJob())


    private lateinit var controller: MediaController

    override fun onConnected(controller: MediaController, allowedCommands: SessionCommandGroup) {
        Timber.d("onConnected")
        this.controller = controller
        this._connectedState.value = true

        // These two callbacks might not get called if the player had been set up before
        // the controller was connected to the session.
        onPlayerStateChanged(controller, controller.playerState)
        onCurrentMediaItemChanged(controller, controller.currentMediaItem)

        coroutineScope.launch {
            while (isActive) {
                _currentPosition.tryEmit(controller.currentPositionDuration)
                _bufferedPosition.tryEmit(controller.bufferedPositionDuration)
                delay(positionRefreshDelay)
            }
        }
    }

    override fun onDisconnected(controller: MediaController) {
        Timber.d("onDisconnected")
        this._connectedState.value = false
        coroutineScope.cancel()
    }

    override fun onPlayerStateChanged(controller: MediaController, state: Int) {
        Timber.d("onPlayerStateChanged $state")
        _playerState.tryEmit(MediaControllerState.from(state))
    }

    override fun onCurrentMediaItemChanged(controller: MediaController, item: MediaItem?) {
        Timber.d("onCurrentMediaItemChanged $item")
        _currentItem.tryEmit(item?.metadata)
    }
}
