/*
 * Copyright 2022 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.navigator.media3.tts2

import android.app.Application
import android.content.Context
import android.media.AudioManager
import android.os.Looper
import android.view.Surface
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.view.TextureView
import androidx.media3.common.*
import androidx.media3.common.C.*
import androidx.media3.common.text.CueGroup
import androidx.media3.common.util.Clock
import androidx.media3.common.util.ListenerSet
import androidx.media3.common.util.Util
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import org.readium.r2.shared.ExperimentalReadiumApi
import timber.log.Timber

@ExperimentalReadiumApi
@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
internal class TtsPlayer(
    private val application: Application,
    private val ttsEngineFacade: TtsEngineFacade<*, *>,
    private val playlistMetadata: MediaMetadata,
    private val mediaItems: List<MediaItem>
) : BasePlayer() {

    private val coroutineScope: CoroutineScope =
        MainScope()

    private var lastPlayback: TtsEngineFacadePlayback =
        ttsEngineFacade.playback.value

    init {
        ttsEngineFacade.playback
            .onEach { playback ->
                notifyListeners(lastPlayback, playback)
                lastPlayback = playback
            }.launchIn(coroutineScope)
    }

    private var listeners: ListenerSet<Player.Listener> =
        ListenerSet(
            applicationLooper,
            Clock.DEFAULT,
        ) { listener: Player.Listener, flags: FlagSet? ->
            listener.onEvents(this, Player.Events(flags!!))
        }

    private val permanentAvailableCommands =
        Player.Commands.Builder()
            .addAll(
                COMMAND_PLAY_PAUSE,
                COMMAND_STOP,
                COMMAND_SEEK_TO_NEXT_MEDIA_ITEM,
                COMMAND_SEEK_TO_PREVIOUS_MEDIA_ITEM,
                COMMAND_SEEK_TO_NEXT,
                COMMAND_SEEK_TO_PREVIOUS
                // COMMAND_GET_AUDIO_ATTRIBUTES,
                // COMMAND_GET_CURRENT_MEDIA_ITEM,
                // COMMAND_GET_MEDIA_ITEMS_METADATA,
                // COMMAND_GET_TEXT
            ).build()

    private val audioManager: AudioManager =
        application.getSystemService(Context.AUDIO_SERVICE) as AudioManager

    override fun getApplicationLooper(): Looper {
        return Looper.getMainLooper()
    }

    override fun addListener(listener: Player.Listener) {
        Timber.d("addListener")
        listeners.add(listener)
    }

    override fun removeListener(listener: Player.Listener) {
        Timber.d("removeListener")
        listeners.remove(listener)
    }

    override fun setMediaItems(mediaItems: MutableList<MediaItem>, resetPosition: Boolean) {
        throw NotImplementedError()
    }

    override fun setMediaItems(
        mediaItems: MutableList<MediaItem>,
        startIndex: Int,
        startPositionMs: Long
    ) {
        throw NotImplementedError()
    }

    override fun addMediaItems(index: Int, mediaItems: MutableList<MediaItem>) {
        throw NotImplementedError()
    }

    override fun moveMediaItems(fromIndex: Int, toIndex: Int, newIndex: Int) {
        throw NotImplementedError()
    }

    override fun removeMediaItems(fromIndex: Int, toIndex: Int) {
        throw NotImplementedError()
    }

    override fun getAvailableCommands(): Player.Commands {
        return Player.Commands.Builder()
            .addAll(permanentAvailableCommands)
            .build()
    }

    override fun prepare() {
        throw NotImplementedError()
    }

    override fun getPlaybackState(): Int {
        return ttsEngineFacade.playback.value.state.value
    }

    override fun getPlaybackSuppressionReason(): Int {
        return PLAYBACK_SUPPRESSION_REASON_NONE // TODO
    }

    override fun getPlayerError(): PlaybackException? {
        return null // TODO
    }

    override fun setPlayWhenReady(playWhenReady: Boolean) {
        if (playWhenReady) {
            ttsEngineFacade.play()
        } else {
            ttsEngineFacade.pause()
        }
    }

    override fun getPlayWhenReady(): Boolean {
        return ttsEngineFacade.playback.value.playWhenReady
    }

    override fun setRepeatMode(repeatMode: Int) {
        throw NotImplementedError()
    }

    override fun getRepeatMode(): Int {
        return REPEAT_MODE_OFF
    }

    override fun setShuffleModeEnabled(shuffleModeEnabled: Boolean) {
        throw NotImplementedError()
    }

    override fun getShuffleModeEnabled(): Boolean {
        return false
    }

    override fun isLoading(): Boolean {
        return false
    }

    override fun seekTo(mediaItemIndex: Int, positionMs: Long) {
        throw NotImplementedError()
    }

    override fun getSeekBackIncrement(): Long {
        return 0
    }

    override fun getSeekForwardIncrement(): Long {
        return 0
    }

    override fun getMaxSeekToPreviousPosition(): Long {
        return 0
    }

    override fun setPlaybackParameters(playbackParameters: PlaybackParameters) {
        throw NotImplementedError() // TODO
    }

    override fun getPlaybackParameters(): PlaybackParameters {
        return PlaybackParameters.DEFAULT
    }

    override fun stop() {
        ttsEngineFacade.stop()
    }

    @Deprecated("Deprecated in Java")
    override fun stop(reset: Boolean) {}

    override fun release() {
        ttsEngineFacade.close()
    }

    override fun getCurrentTracks(): Tracks {
        throw NotImplementedError()
    }

    override fun getTrackSelectionParameters(): TrackSelectionParameters {
        return TrackSelectionParameters.Builder(application)
            .build()
    }

    override fun setTrackSelectionParameters(parameters: TrackSelectionParameters) {
        throw NotImplementedError()
    }

    override fun getMediaMetadata(): MediaMetadata {
        return currentTimeline.getWindow(currentMediaItemIndex, window).mediaItem.mediaMetadata
    }

    override fun getPlaylistMetadata(): MediaMetadata {
        return playlistMetadata
    }

    override fun setPlaylistMetadata(mediaMetadata: MediaMetadata) {
        throw NotImplementedError()
    }

    override fun getCurrentTimeline(): Timeline {
        // MediaNotificationManager requires a non-empty timeline to start foreground playing.
        return TtsTimeline(mediaItems)
        /*return SinglePeriodTimeline(
            TIME_UNSET, false, false, false, null, mediaItem)*/
    }

    override fun getCurrentPeriodIndex(): Int {
        return lastPlayback.index
    }

    override fun getCurrentMediaItemIndex(): Int {
        return lastPlayback.index
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

    override fun getTotalBufferedDuration(): Long {
        return 0
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
            .build()
    }

    override fun setVolume(volume: Float) {
        throw NotImplementedError()
    }

    override fun getVolume(): Float {
        return 1.0f
    }

    override fun clearVideoSurface() {
        throw NotImplementedError()
    }

    override fun clearVideoSurface(surface: Surface?) {
        throw NotImplementedError()
    }

    override fun setVideoSurface(surface: Surface?) {
        throw NotImplementedError()
    }

    override fun setVideoSurfaceHolder(surfaceHolder: SurfaceHolder?) {
        throw NotImplementedError()
    }

    override fun clearVideoSurfaceHolder(surfaceHolder: SurfaceHolder?) {
        throw NotImplementedError()
    }

    override fun setVideoSurfaceView(surfaceView: SurfaceView?) {
        throw NotImplementedError()
    }

    override fun clearVideoSurfaceView(surfaceView: SurfaceView?) {
        throw NotImplementedError()
    }

    override fun setVideoTextureView(textureView: TextureView?) {
        throw NotImplementedError()
    }

    override fun clearVideoTextureView(textureView: TextureView?) {
        throw NotImplementedError()
    }

    override fun getVideoSize(): VideoSize {
        return VideoSize.UNKNOWN
    }

    override fun getCurrentCues(): CueGroup {
        return CueGroup(emptyList())
    }

    override fun getDeviceInfo(): DeviceInfo {
        val minVolume = if (Util.SDK_INT >= 28) audioManager.getStreamMinVolume(STREAM_TYPE_MUSIC) else 0
        val maxVolume = audioManager.getStreamMaxVolume(STREAM_TYPE_MUSIC)
        return DeviceInfo(DeviceInfo.PLAYBACK_TYPE_LOCAL, minVolume, maxVolume)
    }

    override fun getDeviceVolume(): Int {
        return audioManager.getStreamVolume(STREAM_TYPE_MUSIC)
    }

    override fun isDeviceMuted(): Boolean {
        return if (Util.SDK_INT >= 23) {
            audioManager.isStreamMute(STREAM_TYPE_MUSIC)
        } else {
            deviceVolume == 0
        }
    }

    override fun setDeviceVolume(volume: Int) {
        throw NotImplementedError()
    }

    override fun increaseDeviceVolume() {
        throw NotImplementedError()
    }

    override fun decreaseDeviceVolume() {
        throw NotImplementedError()
    }

    override fun setDeviceMuted(muted: Boolean) {
        throw NotImplementedError()
    }

    private fun notifyListeners(
        previousPlaybackInfo: TtsEngineFacadePlayback,
        playbackInfo: TtsEngineFacadePlayback,
        // playWhenReadyChangeReason: @Player.PlayWhenReadyChangeReason Int,
    ) {
        /*if (previousPlaybackInfo.playbackError != playbackInfo.playbackError) {
            listeners.queueEvent(
                EVENT_PLAYER_ERROR
            ) { listener: Player.Listener ->
                listener.onPlayerErrorChanged(
                    playbackInfo.playbackError
                )
            }
            if (playbackInfo.playbackError != null) {
                listeners.queueEvent(
                    EVENT_PLAYER_ERROR
                ) { listener: Player.Listener ->
                    listener.onPlayerError(
                        playbackInfo.playbackError!!
                    )
                }
            }
        }*/

        if (previousPlaybackInfo.isPlaying != playbackInfo.isPlaying) {
            listeners.queueEvent(
                EVENT_PLAYBACK_STATE_CHANGED
            ) { listener: Player.Listener ->
                listener.onPlaybackStateChanged(
                    playbackInfo.state.value
                )
            }
        }

        if (previousPlaybackInfo.playWhenReady != playbackInfo.playWhenReady) {
            listeners.queueEvent(
                EVENT_PLAY_WHEN_READY_CHANGED
            ) { listener: Player.Listener ->
                listener.onPlayWhenReadyChanged(
                    playbackInfo.playWhenReady,
                    if (playbackInfo.state == TtsEngineFacadePlayback.State.ENDED)
                        PLAY_WHEN_READY_CHANGE_REASON_END_OF_MEDIA_ITEM
                    else
                        PLAY_WHEN_READY_CHANGE_REASON_USER_REQUEST
                    // PLAYBACK_SUPPRESSION_REASON_NONE
                    // playWhenReadyChangeReason
                )
            }
        }

        if (isPlaying(previousPlaybackInfo) != isPlaying(playbackInfo)) {
            listeners.queueEvent(
                EVENT_IS_PLAYING_CHANGED
            ) { listener: Player.Listener ->
                listener.onIsPlayingChanged(isPlaying(playbackInfo))
            }
        }
        /*if (previousPlaybackInfo.playbackParameters != playbackInfo.playbackParameters) {
            listeners.queueEvent(
                EVENT_PLAYBACK_PARAMETERS_CHANGED
            ) { listener: Player.Listener ->
                listener.onPlaybackParametersChanged(
                    playbackInfo.playbackParameters
                )
            }
        }*/

        listeners.flushEvents()
    }

    private fun isPlaying(playbackInfo: TtsEngineFacadePlayback): Boolean {
        return (playbackInfo.state == TtsEngineFacadePlayback.State.READY && playbackInfo.playWhenReady)
    }
}
