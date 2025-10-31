/*
 * Copyright 2022 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.navigator.media.tts.session

import android.app.Application
import android.os.Handler
import android.os.Looper
import android.view.Surface
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.view.TextureView
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C.*
import androidx.media3.common.DeviceInfo
import androidx.media3.common.FlagSet
import androidx.media3.common.IllegalSeekPositionException
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackException
import androidx.media3.common.PlaybackException.*
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.common.Player.*
import androidx.media3.common.Timeline
import androidx.media3.common.TrackSelectionParameters
import androidx.media3.common.Tracks
import androidx.media3.common.VideoSize
import androidx.media3.common.text.CueGroup
import androidx.media3.common.util.Clock
import androidx.media3.common.util.ListenerSet
import androidx.media3.common.util.Size
import androidx.media3.common.util.Util
import java.lang.Error
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import org.readium.navigator.media.tts.TtsEngine
import org.readium.navigator.media.tts.TtsPlayer
import org.readium.r2.shared.ExperimentalReadiumApi
import org.readium.r2.shared.util.ErrorException
import org.readium.r2.shared.util.data.ReadError
import org.readium.r2.shared.util.http.HttpError

/**
 * Adapts the [TtsPlayer] to media3 [Player] interface.
 */
@ExperimentalReadiumApi
@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
internal class TtsSessionAdapter<E : TtsEngine.Error>(
    private val application: Application,
    private val ttsPlayer: TtsPlayer<*, *, E, *>,
    private val playlistMetadata: MediaMetadata,
    private val mediaItems: List<MediaItem>,
    private val onStop: () -> Unit,
    private val playbackParametersState: StateFlow<PlaybackParameters>,
    private val updatePlaybackParameters: (PlaybackParameters) -> Unit,
    private val mapEngineError: (E) -> PlaybackException,
) : Player {

    private val coroutineScope: CoroutineScope =
        MainScope()

    private val eventHandler: Handler =
        Handler(applicationLooper)

    private val window: Timeline.Window =
        Timeline.Window()

    private var lastPlayback: TtsPlayer.Playback =
        ttsPlayer.playback.value

    private var lastPlaybackParameters: PlaybackParameters =
        playbackParametersState.value

    private val streamVolumeManager = StreamVolumeManager(
        application,
        eventHandler,
        StreamVolumeManagerListener()
    )

    init {
        val streamType = audioAttributes.streamType
        streamVolumeManager.setStreamType(streamType)
    }

    private val audioFocusManager = AudioFocusManager(
        application,
        eventHandler,
        AudioFocusManagerListener()
    )

    init {
        audioFocusManager.setAudioAttributes(audioAttributes)
    }

    private val audioBecomingNoisyManager = AudioBecomingNoisyManager(
        application,
        eventHandler,
        AudioBecomingNoisyManagerListener()
    )

    init {
        audioBecomingNoisyManager.setEnabled(true)
    }

    private var deviceInfo: DeviceInfo =
        createDeviceInfo(streamVolumeManager)

    init {
        ttsPlayer.playback
            .onEach { playback ->
                notifyListenersPlaybackChanged(lastPlayback, playback)
                lastPlayback = playback
                audioFocusManager.updateAudioFocus(
                    playback.playWhenReady,
                    playback.state.playerCode
                )
            }.launchIn(coroutineScope)

        playbackParametersState
            .onEach { playbackParameters ->
                notifyListenersPlaybackParametersChanged(lastPlaybackParameters, playbackParameters)
                lastPlaybackParameters = playbackParameters
            }.launchIn(coroutineScope)
    }

    private var listeners: ListenerSet<Listener> =
        ListenerSet(
            applicationLooper,
            Clock.DEFAULT
        ) { listener: Listener, flags: FlagSet ->
            listener.onEvents(this, Events(flags))
        }

    private val permanentAvailableCommands =
        Commands.Builder()
            .addAll(
                COMMAND_PLAY_PAUSE,
                COMMAND_STOP,

                // COMMAND_SEEK_BACK,
                // COMMAND_SEEK_FORWARD,

                // COMMAND_SEEK_TO_NEXT_MEDIA_ITEM,
                // COMMAND_SEEK_TO_PREVIOUS_MEDIA_ITEM,

                COMMAND_GET_AUDIO_ATTRIBUTES,
                COMMAND_GET_DEVICE_VOLUME,
                COMMAND_SET_DEVICE_VOLUME_WITH_FLAGS,

                COMMAND_SET_SPEED_AND_PITCH,
                COMMAND_GET_CURRENT_MEDIA_ITEM,
                COMMAND_GET_METADATA,
                COMMAND_GET_TEXT,
                COMMAND_GET_TIMELINE
            ).build()

    override fun getApplicationLooper(): Looper {
        return Looper.getMainLooper()
    }

    override fun addListener(listener: Listener) {
        listeners.add(listener)
    }

    override fun removeListener(listener: Listener) {
        listeners.remove(listener)
    }

    override fun setMediaItems(mediaItems: MutableList<MediaItem>) {
    }

    override fun setMediaItems(mediaItems: MutableList<MediaItem>, resetPosition: Boolean) {
    }

    override fun setMediaItems(
        mediaItems: MutableList<MediaItem>,
        startIndex: Int,
        startPositionMs: Long,
    ) {
    }

    override fun setMediaItem(mediaItem: MediaItem) {
    }

    override fun setMediaItem(mediaItem: MediaItem, startPositionMs: Long) {
    }

    override fun setMediaItem(mediaItem: MediaItem, resetPosition: Boolean) {
    }

    override fun addMediaItem(mediaItem: MediaItem) {
    }

    override fun addMediaItem(index: Int, mediaItem: MediaItem) {
    }

    override fun addMediaItems(mediaItems: MutableList<MediaItem>) {
    }

    override fun addMediaItems(index: Int, mediaItems: MutableList<MediaItem>) {
    }

    override fun moveMediaItem(currentIndex: Int, newIndex: Int) {
    }

    override fun moveMediaItems(fromIndex: Int, toIndex: Int, newIndex: Int) {
    }

    override fun replaceMediaItem(index: Int, mediaItem: MediaItem) {
    }

    override fun replaceMediaItems(
        fromIndex: Int,
        toIndex: Int,
        mediaItems: MutableList<MediaItem>,
    ) {
    }

    override fun removeMediaItem(index: Int) {
    }

    override fun removeMediaItems(fromIndex: Int, toIndex: Int) {
    }

    override fun clearMediaItems() {
    }

    override fun isCommandAvailable(command: Int): Boolean {
        return command in availableCommands
    }

    override fun canAdvertiseSession(): Boolean {
        return true
    }

    override fun getAvailableCommands(): Commands {
        return Commands.Builder()
            .addAll(permanentAvailableCommands)
            .build()
    }

    override fun prepare() {
        ttsPlayer.tryRecover()
    }

    override fun getPlaybackState(): Int {
        return ttsPlayer.playback.value.state.playerCode
    }

    override fun getPlaybackSuppressionReason(): Int {
        return PLAYBACK_SUPPRESSION_REASON_NONE
    }

    override fun isPlaying(): Boolean {
        return (
            playbackState == STATE_READY && playWhenReady &&
                playbackSuppressionReason == PLAYBACK_SUPPRESSION_REASON_NONE
            )
    }

    override fun getPlayerError(): PlaybackException? {
        return (lastPlayback.state as? TtsPlayer.State.Failure)?.toPlaybackException()
    }

    override fun play() {
        playWhenReady = true
    }

    override fun pause() {
        playWhenReady = false
    }

    override fun setPlayWhenReady(playWhenReady: Boolean) {
        if (playWhenReady) {
            ttsPlayer.play()
        } else {
            ttsPlayer.pause()
        }
    }

    override fun getPlayWhenReady(): Boolean {
        return ttsPlayer.playback.value.playWhenReady
    }

    override fun setRepeatMode(repeatMode: Int) {
    }

    override fun getRepeatMode(): Int {
        return REPEAT_MODE_OFF
    }

    override fun setShuffleModeEnabled(shuffleModeEnabled: Boolean) {
    }

    override fun getShuffleModeEnabled(): Boolean {
        return false
    }

    override fun isLoading(): Boolean {
        return false
    }

    override fun seekToDefaultPosition() {
        seekToDefaultPosition(currentMediaItemIndex)
    }

    override fun seekToDefaultPosition(mediaItemIndex: Int) {
        seekTo(mediaItemIndex, 0L)
    }

    override fun seekTo(positionMs: Long) {
    }

    override fun seekTo(mediaItemIndex: Int, positionMs: Long) {
        val timeline: Timeline = currentTimeline
        if (mediaItemIndex < 0 || !timeline.isEmpty && mediaItemIndex >= timeline.windowCount
        ) {
            throw IllegalSeekPositionException(timeline, mediaItemIndex, positionMs)
        }

        ttsPlayer.go(mediaItemIndex)
    }

    override fun getSeekBackIncrement(): Long {
        return 0
    }

    override fun seekBack() {
        ttsPlayer.previousUtterance()
    }

    override fun getSeekForwardIncrement(): Long {
        return 0
    }

    override fun seekForward() {
        ttsPlayer.nextUtterance()
    }

    override fun hasPreviousMediaItem(): Boolean {
        return previousMediaItemIndex != INDEX_UNSET
    }

    override fun seekToPreviousMediaItem() {
        val previousMediaItemIndex = previousMediaItemIndex
        if (previousMediaItemIndex != INDEX_UNSET) {
            seekToDefaultPosition(previousMediaItemIndex)
        }
    }

    override fun getMaxSeekToPreviousPosition(): Long {
        return 0
    }

    override fun seekToPrevious() {
        val timeline = currentTimeline
        if (timeline.isEmpty || isPlayingAd) {
            return
        }
        val hasPreviousMediaItem = hasPreviousMediaItem()
        if (isCurrentMediaItemLive && !isCurrentMediaItemSeekable) {
            if (hasPreviousMediaItem) {
                seekToPreviousMediaItem()
            }
        } else if (hasPreviousMediaItem && currentPosition <= maxSeekToPreviousPosition) {
            seekToPreviousMediaItem()
        } else {
            seekTo( /* positionMs= */0)
        }
    }

    override fun hasNextMediaItem(): Boolean {
        return nextMediaItemIndex != INDEX_UNSET
    }

    override fun seekToNextMediaItem() {
        val nextMediaItemIndex = nextMediaItemIndex
        if (nextMediaItemIndex != INDEX_UNSET) {
            seekToDefaultPosition(nextMediaItemIndex)
        }
    }

    override fun seekToNext() {
        val timeline = currentTimeline
        if (timeline.isEmpty || isPlayingAd) {
            return
        }
        if (hasNextMediaItem()) {
            seekToNextMediaItem()
        } else if (isCurrentMediaItemLive && isCurrentMediaItemDynamic) {
            seekToDefaultPosition()
        }
    }

    override fun setPlaybackParameters(playbackParameters: PlaybackParameters) {
        updatePlaybackParameters(playbackParameters)
    }

    override fun setPlaybackSpeed(speed: Float) {
        updatePlaybackParameters(playbackParametersState.value.withSpeed(speed))
    }

    override fun getPlaybackParameters(): PlaybackParameters {
        return playbackParametersState.value
    }

    override fun stop() {
        onStop()
    }

    override fun release() {
        streamVolumeManager.release()
        audioFocusManager.release()
        audioBecomingNoisyManager.setEnabled(false)
        eventHandler.removeCallbacksAndMessages(null)
        // This object does not own the TtsPlayer instance, do not close it.
    }

    override fun getCurrentTracks(): Tracks {
        return Tracks.EMPTY
    }

    override fun getTrackSelectionParameters(): TrackSelectionParameters {
        return TrackSelectionParameters.Builder()
            .build()
    }

    override fun setTrackSelectionParameters(parameters: TrackSelectionParameters) {
    }

    override fun getMediaMetadata(): MediaMetadata {
        return currentTimeline.getWindow(currentMediaItemIndex, window)
            .mediaItem.mediaMetadata
    }

    override fun getPlaylistMetadata(): MediaMetadata {
        return playlistMetadata
    }

    override fun setPlaylistMetadata(mediaMetadata: MediaMetadata) {
    }

    override fun getCurrentManifest(): Any? {
        val timeline = currentTimeline
        return if (timeline.isEmpty) {
            null
        } else {
            timeline.getWindow(currentMediaItemIndex, window).manifest
        }
    }

    override fun getCurrentTimeline(): Timeline {
        // MediaNotificationManager requires a non-empty timeline to start foreground playing.
        // Report a single-item timeline in order to show a notification, but without skip buttons.
        return if (mediaItems.isNotEmpty()) {
            TtsTimeline(listOf(mediaItems[currentMediaItemIndex]))
        } else {
            TtsTimeline(emptyList())
        }
    }

    override fun getCurrentPeriodIndex(): Int {
        return 0
    }

    @Deprecated("Deprecated in Java", ReplaceWith("currentMediaItemIndex"))
    override fun getCurrentWindowIndex(): Int {
        return currentMediaItemIndex
    }

    override fun getCurrentMediaItemIndex(): Int {
        // Reporting a single-item timeline, so index is always 0.
        return 0
    }

    @Deprecated("Deprecated in Java", ReplaceWith("nextMediaItemIndex"))
    override fun getNextWindowIndex(): Int {
        return nextMediaItemIndex
    }

    override fun getNextMediaItemIndex(): Int {
        val timeline = currentTimeline
        return if (timeline.isEmpty) {
            INDEX_UNSET
        } else {
            timeline.getNextWindowIndex(
                currentMediaItemIndex,
                getRepeatModeForNavigation(),
                shuffleModeEnabled
            )
        }
    }

    @Deprecated("Deprecated in Java", ReplaceWith("previousMediaItemIndex"))
    override fun getPreviousWindowIndex(): Int {
        return previousMediaItemIndex
    }

    override fun getPreviousMediaItemIndex(): Int {
        val timeline = currentTimeline
        return if (timeline.isEmpty) {
            INDEX_UNSET
        } else {
            timeline.getPreviousWindowIndex(
                currentMediaItemIndex,
                getRepeatModeForNavigation(),
                shuffleModeEnabled
            )
        }
    }

    override fun getCurrentMediaItem(): MediaItem? {
        val timeline = currentTimeline
        return if (timeline.isEmpty) {
            null
        } else {
            timeline.getWindow(currentMediaItemIndex, window).mediaItem
        }
    }

    override fun getMediaItemCount(): Int {
        return currentTimeline.windowCount
    }

    override fun getMediaItemAt(index: Int): MediaItem {
        return currentTimeline.getWindow(index, window).mediaItem
    }

    override fun getDuration(): Long {
        return TIME_UNSET
    }

    override fun getCurrentPosition(): Long {
        return 0
    }

    override fun getBufferedPosition(): Long {
        return 0
    }

    override fun getBufferedPercentage(): Int {
        val position = bufferedPosition
        val duration = duration
        return if (position == TIME_UNSET || duration == TIME_UNSET) {
            0
        } else if (duration == 0L) {
            100
        } else {
            Util.constrainValue(
                (position * 100 / duration).toInt(),
                0,
                100
            )
        }
    }

    override fun getTotalBufferedDuration(): Long {
        return 0
    }

    @Deprecated("Deprecated in Java", ReplaceWith("isCurrentMediaItemDynamic"))
    override fun isCurrentWindowDynamic(): Boolean {
        return isCurrentMediaItemDynamic
    }

    override fun isCurrentMediaItemDynamic(): Boolean {
        val timeline = currentTimeline
        return !timeline.isEmpty && timeline.getWindow(currentMediaItemIndex, window).isDynamic
    }

    @Deprecated("Deprecated in Java", ReplaceWith("isCurrentMediaItemLive"))
    override fun isCurrentWindowLive(): Boolean {
        return isCurrentMediaItemLive
    }

    override fun isCurrentMediaItemLive(): Boolean {
        val timeline = currentTimeline
        return !timeline.isEmpty && timeline.getWindow(currentMediaItemIndex, window).isLive
    }

    override fun getCurrentLiveOffset(): Long {
        val timeline = currentTimeline
        if (timeline.isEmpty) {
            return TIME_UNSET
        }
        val windowStartTimeMs = timeline.getWindow(currentMediaItemIndex, window).windowStartTimeMs
        return if (windowStartTimeMs == TIME_UNSET) {
            TIME_UNSET
        } else {
            window.currentUnixTimeMs - window.windowStartTimeMs - contentPosition
        }
    }

    @Deprecated("Deprecated in Java", ReplaceWith("isCurrentMediaItemSeekable"))
    override fun isCurrentWindowSeekable(): Boolean {
        return isCurrentMediaItemSeekable
    }

    override fun isCurrentMediaItemSeekable(): Boolean {
        val timeline = currentTimeline
        return !timeline.isEmpty && timeline.getWindow(currentMediaItemIndex, window).isSeekable
    }

    override fun isPlayingAd(): Boolean {
        return false
    }

    override fun getCurrentAdGroupIndex(): Int {
        return INDEX_UNSET
    }

    override fun getCurrentAdIndexInAdGroup(): Int {
        return INDEX_UNSET
    }

    override fun getContentDuration(): Long {
        val timeline = currentTimeline
        return if (timeline.isEmpty) {
            TIME_UNSET
        } else {
            timeline.getWindow(currentMediaItemIndex, window).durationMs
        }
    }

    override fun getContentPosition(): Long {
        return 0
    }

    override fun getContentBufferedPosition(): Long {
        return 0
    }

    override fun getAudioAttributes(): AudioAttributes {
        return AudioAttributes.Builder()
            .setUsage(USAGE_MEDIA)
            .setContentType(AUDIO_CONTENT_TYPE_SPEECH)
            .setAllowedCapturePolicy(ALLOW_CAPTURE_BY_SYSTEM)
            .build()
    }

    override fun setVolume(volume: Float) {
    }

    override fun getVolume(): Float {
        return 1.0f
    }

    override fun clearVideoSurface() {
    }

    override fun clearVideoSurface(surface: Surface?) {
    }

    override fun setVideoSurface(surface: Surface?) {
    }

    override fun setVideoSurfaceHolder(surfaceHolder: SurfaceHolder?) {
    }

    override fun clearVideoSurfaceHolder(surfaceHolder: SurfaceHolder?) {
    }

    override fun setVideoSurfaceView(surfaceView: SurfaceView?) {
    }

    override fun clearVideoSurfaceView(surfaceView: SurfaceView?) {
    }

    override fun setVideoTextureView(textureView: TextureView?) {
    }

    override fun clearVideoTextureView(textureView: TextureView?) {
    }

    override fun getVideoSize(): VideoSize {
        return VideoSize.UNKNOWN
    }

    override fun getSurfaceSize(): Size {
        return Size.UNKNOWN
    }

    override fun getCurrentCues(): CueGroup {
        return CueGroup.EMPTY_TIME_ZERO
    }

    override fun getDeviceInfo(): DeviceInfo {
        return deviceInfo
    }

    override fun getDeviceVolume(): Int {
        return streamVolumeManager.getVolume()
    }

    override fun isDeviceMuted(): Boolean {
        return streamVolumeManager.isMuted()
    }

    @Deprecated("Deprecated in Java")
    override fun setDeviceVolume(volume: Int) {
        streamVolumeManager.setVolume(volume)
    }

    override fun setDeviceVolume(volume: Int, flags: Int) {
        streamVolumeManager.setVolume(volume)
    }

    @Deprecated("Deprecated in Java")
    override fun increaseDeviceVolume() {
        streamVolumeManager.increaseVolume()
    }

    override fun increaseDeviceVolume(flags: Int) {
        streamVolumeManager.increaseVolume()
    }

    @Deprecated("Deprecated in Java")
    override fun decreaseDeviceVolume() {
        streamVolumeManager.decreaseVolume()
    }

    override fun decreaseDeviceVolume(flags: Int) {
        streamVolumeManager.decreaseVolume()
    }

    @Deprecated("Deprecated in Java")
    override fun setDeviceMuted(muted: Boolean) {
        streamVolumeManager.setMuted(muted)
    }

    override fun setDeviceMuted(muted: Boolean, flags: Int) {
        streamVolumeManager.setMuted(muted)
    }

    override fun setAudioAttributes(audioAttributes: AudioAttributes, handleAudioFocus: Boolean) {
        audioFocusManager.setAudioAttributes(audioAttributes)
    }

    private fun notifyListenersPlaybackChanged(
        previousPlaybackInfo: TtsPlayer.Playback,
        playbackInfo: TtsPlayer.Playback,
        // playWhenReadyChangeReason: @Player.PlayWhenReadyChangeReason Int,
    ) {
        if (previousPlaybackInfo.state as? TtsPlayer.State.Failure != playbackInfo.state as? Error) {
            listeners.queueEvent(
                EVENT_PLAYER_ERROR
            ) { listener: Listener ->
                listener.onPlayerErrorChanged(
                    (playbackInfo.state as? TtsPlayer.State.Failure)?.toPlaybackException()
                )
            }
            if (playbackInfo.state is TtsPlayer.State.Failure) {
                listeners.queueEvent(
                    EVENT_PLAYER_ERROR
                ) { listener: Listener ->
                    listener.onPlayerError(
                        playbackInfo.state.toPlaybackException()
                    )
                }
            }
        }

        if (previousPlaybackInfo.state != playbackInfo.state) {
            listeners.queueEvent(
                EVENT_PLAYBACK_STATE_CHANGED
            ) { listener: Listener ->
                listener.onPlaybackStateChanged(
                    playbackInfo.state.playerCode
                )
            }
        }

        if (previousPlaybackInfo.playWhenReady != playbackInfo.playWhenReady) {
            listeners.queueEvent(
                EVENT_PLAY_WHEN_READY_CHANGED
            ) { listener: Listener ->
                listener.onPlayWhenReadyChanged(
                    playbackInfo.playWhenReady,
                    PLAY_WHEN_READY_CHANGE_REASON_USER_REQUEST
                    // PLAYBACK_SUPPRESSION_REASON_NONE
                    // playWhenReadyChangeReason
                )
            }
        }

        if (isPlaying(previousPlaybackInfo) != isPlaying(playbackInfo)) {
            listeners.queueEvent(
                EVENT_IS_PLAYING_CHANGED
            ) { listener: Listener ->
                listener.onIsPlayingChanged(isPlaying(playbackInfo))
            }
        }

        listeners.flushEvents()
    }

    private fun notifyListenersPlaybackParametersChanged(
        previousPlaybackParameters: PlaybackParameters,
        playbackParameters: PlaybackParameters,
    ) {
        if (previousPlaybackParameters != playbackParameters) {
            listeners.sendEvent(
                EVENT_PLAYBACK_PARAMETERS_CHANGED
            ) { listener: Listener ->
                listener.onPlaybackParametersChanged(
                    playbackParameters
                )
            }
        }
    }

    private fun createDeviceInfo(streamVolumeManager: StreamVolumeManager): DeviceInfo {
        val newDeviceInfo = DeviceInfo.Builder(
            DeviceInfo.PLAYBACK_TYPE_LOCAL
        )
            .setMinVolume(streamVolumeManager.minVolume)
            .setMaxVolume(streamVolumeManager.maxVolume)
            .build()
        deviceInfo = newDeviceInfo
        return newDeviceInfo
    }

    private fun isPlaying(playbackInfo: TtsPlayer.Playback): Boolean {
        return (playbackInfo.state == TtsPlayer.State.Ready && playbackInfo.playWhenReady)
    }

    private fun getRepeatModeForNavigation(): @RepeatMode Int {
        val repeatMode = repeatMode
        return if (repeatMode == REPEAT_MODE_ONE) REPEAT_MODE_OFF else repeatMode
    }

    private inner class StreamVolumeManagerListener : StreamVolumeManager.Listener {

        override fun onStreamTypeChanged(streamType: @StreamType Int) {
            val newDeviceInfo = createDeviceInfo(streamVolumeManager)
            if (newDeviceInfo != deviceInfo) {
                listeners.sendEvent(
                    EVENT_DEVICE_INFO_CHANGED
                ) { listener: Listener ->
                    listener.onDeviceInfoChanged(
                        newDeviceInfo
                    )
                }
            }
        }

        override fun onStreamVolumeChanged(streamVolume: Int, streamMuted: Boolean) {
            listeners.sendEvent(
                EVENT_DEVICE_VOLUME_CHANGED
            ) { listener: Listener ->
                listener.onDeviceVolumeChanged(
                    streamVolume,
                    streamMuted
                )
            }
        }
    }

    private inner class AudioFocusManagerListener : AudioFocusManager.PlayerControl {

        override fun setVolumeMultiplier(volumeMultiplier: Float) {
            // Do nothing as we're not supposed to duck volume with
            // contentType == C.AUDIO_CONTENT_TYPE_SPEECH
        }

        override fun executePlayerCommand(playerCommand: Int) {
            playWhenReady = playWhenReady && playerCommand != AudioFocusManager.PLAYER_COMMAND_DO_NOT_PLAY
        }
    }

    private inner class AudioBecomingNoisyManagerListener :
        AudioBecomingNoisyManager.EventListener {

        override fun onAudioBecomingNoisy() {
            playWhenReady = false
        }
    }

    private val TtsPlayer.State.playerCode get() = when (this) {
        TtsPlayer.State.Ready -> STATE_READY
        TtsPlayer.State.Ended -> STATE_ENDED
        is TtsPlayer.State.Failure -> STATE_IDLE
    }

    @Suppress("Unchecked_cast")
    private fun TtsPlayer.State.Failure.toPlaybackException(): PlaybackException = when (this) {
        is TtsPlayer.State.Failure.Engine<*> -> {
            mapEngineError(error as E)
        }
        is TtsPlayer.State.Failure.Content -> {
            val errorCode = when (error) {
                is ReadError.Access ->
                    when (error.cause) {
                        is HttpError.ErrorResponse ->
                            ERROR_CODE_IO_BAD_HTTP_STATUS
                        is HttpError.Timeout ->
                            ERROR_CODE_IO_NETWORK_CONNECTION_TIMEOUT
                        is HttpError.Unreachable ->
                            ERROR_CODE_IO_NETWORK_CONNECTION_FAILED
                        else -> ERROR_CODE_UNSPECIFIED
                    }

                else ->
                    ERROR_CODE_UNSPECIFIED
            }
            PlaybackException(
                error.message,
                error.cause?.let { ErrorException(it) },
                errorCode
            )
        }
    }
}
