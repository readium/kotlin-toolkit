/*
 * Copyright (C) 2018 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.readium.navigator.media.tts.session

import android.content.Context
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.AudioManager.OnAudioFocusChangeListener
import android.os.Build
import android.os.Handler
import androidx.annotation.IntDef
import androidx.annotation.RequiresApi
import androidx.annotation.VisibleForTesting
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.Player
import androidx.media3.common.util.Log
import androidx.media3.common.util.Util
import java.util.Objects

@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
/** Manages requesting and responding to changes in audio focus.
 *
 * @param context The current context.
 * @param eventHandler A [Handler] to for the thread on which the player is used.
 * @param playerControl A [PlayerControl] to handle commands from this instance.
 */
internal class AudioFocusManager(
    context: Context,
    eventHandler: Handler,
    playerControl: PlayerControl,
) {
    /** Interface to allow AudioFocusManager to give commands to a player.  */
    interface PlayerControl {
        /**
         * Called when the volume multiplier on the player should be changed.
         *
         * @param volumeMultiplier The new volume multiplier.
         */
        fun setVolumeMultiplier(volumeMultiplier: Float)

        /**
         * Called when a command must be executed on the player.
         *
         * @param playerCommand The command that must be executed.
         */
        fun executePlayerCommand(playerCommand: @PlayerCommand Int)
    }

    /**
     * Player commands. One of [.PLAYER_COMMAND_DO_NOT_PLAY], [ ][.PLAYER_COMMAND_WAIT_FOR_CALLBACK]
     * or [.PLAYER_COMMAND_PLAY_WHEN_READY].
     */
    @Target(AnnotationTarget.TYPE)
    @MustBeDocumented
    @Retention(AnnotationRetention.SOURCE)
    @IntDef(
        PLAYER_COMMAND_DO_NOT_PLAY,
        PLAYER_COMMAND_WAIT_FOR_CALLBACK,
        PLAYER_COMMAND_PLAY_WHEN_READY
    )
    annotation class PlayerCommand

    /** Audio focus state.  */
    @Target(AnnotationTarget.TYPE)
    @MustBeDocumented
    @Retention(AnnotationRetention.SOURCE)
    @IntDef(
        AUDIO_FOCUS_STATE_NO_FOCUS,
        AUDIO_FOCUS_STATE_HAVE_FOCUS,
        AUDIO_FOCUS_STATE_LOSS_TRANSIENT,
        AUDIO_FOCUS_STATE_LOSS_TRANSIENT_DUCK
    )
    private annotation class AudioFocusState

    /**
     * Audio focus types. One of [.AUDIOFOCUS_NONE], [.AUDIOFOCUS_GAIN], [ ][.AUDIOFOCUS_GAIN_TRANSIENT], [.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK] or [ ][.AUDIOFOCUS_GAIN_TRANSIENT_EXCLUSIVE].
     */
    @Target(AnnotationTarget.TYPE)
    @MustBeDocumented
    @Retention(AnnotationRetention.SOURCE)
    @IntDef(
        AUDIOFOCUS_NONE,
        AUDIOFOCUS_GAIN,
        AUDIOFOCUS_GAIN_TRANSIENT,
        AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK,
        AUDIOFOCUS_GAIN_TRANSIENT_EXCLUSIVE
    )
    private annotation class AudioFocusGain

    private val audioManager: AudioManager
    private val focusListener: AudioFocusListener
    private var playerControl: PlayerControl?
    private var audioAttributes: AudioAttributes? = null
    private var audioFocusState: @AudioFocusState Int
    private var focusGainToRequest = 0

    /** Gets the current player volume multiplier.  */
    var volumeMultiplier = VOLUME_MULTIPLIER_DEFAULT
        private set
    private lateinit var audioFocusRequest: AudioFocusRequest
    private var rebuildAudioFocusRequest = false

    init {
        audioManager = checkNotNull(
            context.applicationContext.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        )
        this.playerControl = playerControl
        focusListener = AudioFocusListener(eventHandler)
        audioFocusState = AUDIO_FOCUS_STATE_NO_FOCUS
    }

    /**
     * Sets audio attributes that should be used to manage audio focus.
     *
     *
     * Call [.updateAudioFocus] to update the audio focus based on these
     * attributes.
     *
     * @param audioAttributes The audio attributes or `null` if audio focus should not be
     * managed automatically.
     */
    fun setAudioAttributes(audioAttributes: AudioAttributes?) {
        if (!Objects.equals(this.audioAttributes, audioAttributes)) {
            this.audioAttributes = audioAttributes
            focusGainToRequest = convertAudioAttributesToFocusGain(audioAttributes)
            require(
                focusGainToRequest == AUDIOFOCUS_GAIN || focusGainToRequest == AUDIOFOCUS_NONE
            ) { "Automatic handling of audio focus is only available for USAGE_MEDIA and USAGE_GAME." }
        }
    }

    /**
     * Called by the player to abandon or request audio focus based on the desired player state.
     *
     * @param playWhenReady The desired value of playWhenReady.
     * @param playbackState The desired playback state.
     * @return A [PlayerCommand] to execute on the player.
     */
    fun updateAudioFocus(
        playWhenReady: Boolean,
        playbackState: @Player.State Int,
    ): @PlayerCommand Int {
        if (shouldAbandonAudioFocusIfHeld(playbackState)) {
            abandonAudioFocusIfHeld()
            return if (playWhenReady) PLAYER_COMMAND_PLAY_WHEN_READY else PLAYER_COMMAND_DO_NOT_PLAY
        }
        return if (playWhenReady) requestAudioFocus() else PLAYER_COMMAND_DO_NOT_PLAY
    }

    /**
     * Called when the manager is no longer required. Audio focus will be released without making any
     * calls to the [PlayerControl].
     */
    fun release() {
        playerControl = null
        abandonAudioFocusIfHeld()
    }

    // Internal methods.
    @VisibleForTesting
    fun /* package */getFocusListener(): OnAudioFocusChangeListener {
        return focusListener
    }

    private fun shouldAbandonAudioFocusIfHeld(playbackState: @Player.State Int): Boolean {
        return playbackState == Player.STATE_IDLE || focusGainToRequest != AUDIOFOCUS_GAIN
    }

    private fun requestAudioFocus(): @PlayerCommand Int {
        if (audioFocusState == AUDIO_FOCUS_STATE_HAVE_FOCUS) {
            return PLAYER_COMMAND_PLAY_WHEN_READY
        }
        val requestResult =
            if (Build.VERSION.SDK_INT >= 26) requestAudioFocusV26() else requestAudioFocusDefault()
        return if (requestResult == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
            setAudioFocusState(AUDIO_FOCUS_STATE_HAVE_FOCUS)
            PLAYER_COMMAND_PLAY_WHEN_READY
        } else {
            setAudioFocusState(AUDIO_FOCUS_STATE_NO_FOCUS)
            PLAYER_COMMAND_DO_NOT_PLAY
        }
    }

    private fun abandonAudioFocusIfHeld() {
        if (audioFocusState == AUDIO_FOCUS_STATE_NO_FOCUS) {
            return
        }
        if (Build.VERSION.SDK_INT >= 26) {
            abandonAudioFocusV26()
        } else {
            abandonAudioFocusDefault()
        }
        setAudioFocusState(AUDIO_FOCUS_STATE_NO_FOCUS)
    }

    @Suppress("Deprecation")
    private fun requestAudioFocusDefault(): Int {
        return audioManager.requestAudioFocus(
            focusListener,
            Util.getStreamTypeForAudioUsage(checkNotNull(audioAttributes).usage),
            focusGainToRequest
        )
    }

    @RequiresApi(26)
    private fun requestAudioFocusV26(): Int {
        if (!::audioFocusRequest.isInitialized || rebuildAudioFocusRequest) {
            val builder =
                if (!::audioFocusRequest.isInitialized) {
                    AudioFocusRequest.Builder(focusGainToRequest)
                } else {
                    AudioFocusRequest.Builder(
                        audioFocusRequest
                    )
                }
            val willPauseWhenDucked = willPauseWhenDucked()
            audioFocusRequest = builder
                .setAudioAttributes(
                    checkNotNull(audioAttributes).audioAttributesV21.audioAttributes
                )
                .setWillPauseWhenDucked(willPauseWhenDucked)
                .setOnAudioFocusChangeListener(focusListener)
                .build()
            rebuildAudioFocusRequest = false
        }
        return audioManager.requestAudioFocus(audioFocusRequest)
    }

    @Suppress("Deprecation")
    private fun abandonAudioFocusDefault() {
        audioManager.abandonAudioFocus(focusListener)
    }

    @RequiresApi(26)
    private fun abandonAudioFocusV26() {
        if (::audioFocusRequest.isInitialized) {
            audioManager.abandonAudioFocusRequest(audioFocusRequest)
        }
    }

    private fun willPauseWhenDucked(): Boolean {
        return audioAttributes != null && audioAttributes!!.contentType == C.AUDIO_CONTENT_TYPE_SPEECH
    }

    private fun setAudioFocusState(audioFocusState: @AudioFocusState Int) {
        if (this.audioFocusState == audioFocusState) {
            return
        }
        this.audioFocusState = audioFocusState
        val volumeMultiplier =
            if (audioFocusState == AUDIO_FOCUS_STATE_LOSS_TRANSIENT_DUCK) {
                VOLUME_MULTIPLIER_DUCK
            } else {
                VOLUME_MULTIPLIER_DEFAULT
            }
        if (this.volumeMultiplier == volumeMultiplier) {
            return
        }
        this.volumeMultiplier = volumeMultiplier
        if (playerControl != null) {
            playerControl!!.setVolumeMultiplier(volumeMultiplier)
        }
    }

    private fun handlePlatformAudioFocusChange(focusChange: Int) {
        when (focusChange) {
            AudioManager.AUDIOFOCUS_GAIN -> {
                setAudioFocusState(AUDIO_FOCUS_STATE_HAVE_FOCUS)
                executePlayerCommand(PLAYER_COMMAND_PLAY_WHEN_READY)
                return
            }
            AudioManager.AUDIOFOCUS_LOSS -> {
                executePlayerCommand(PLAYER_COMMAND_DO_NOT_PLAY)
                abandonAudioFocusIfHeld()
                return
            }
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT, AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> {
                if (focusChange == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT || willPauseWhenDucked()) {
                    executePlayerCommand(PLAYER_COMMAND_WAIT_FOR_CALLBACK)
                    setAudioFocusState(AUDIO_FOCUS_STATE_LOSS_TRANSIENT)
                } else {
                    setAudioFocusState(AUDIO_FOCUS_STATE_LOSS_TRANSIENT_DUCK)
                }
                return
            }
            else -> Log.w(
                TAG,
                "Unknown focus change type: $focusChange"
            )
        }
    }

    private fun executePlayerCommand(playerCommand: @PlayerCommand Int) {
        if (playerControl != null) {
            playerControl!!.executePlayerCommand(playerCommand)
        }
    }

    // Internal audio focus listener.
    private inner class AudioFocusListener(private val eventHandler: Handler) :
        OnAudioFocusChangeListener {
        override fun onAudioFocusChange(focusChange: Int) {
            eventHandler.post { handlePlatformAudioFocusChange(focusChange) }
        }
    }

    companion object {
        /** Do not play.  */
        const val PLAYER_COMMAND_DO_NOT_PLAY = -1

        /** Do not play now. Wait for callback to play.  */
        const val PLAYER_COMMAND_WAIT_FOR_CALLBACK = 0

        /** Play freely.  */
        const val PLAYER_COMMAND_PLAY_WHEN_READY = 1

        /** No audio focus is currently being held.  */
        private const val AUDIO_FOCUS_STATE_NO_FOCUS = 0

        /** The requested audio focus is currently held.  */
        private const val AUDIO_FOCUS_STATE_HAVE_FOCUS = 1

        /** Audio focus has been temporarily lost.  */
        private const val AUDIO_FOCUS_STATE_LOSS_TRANSIENT = 2

        /** Audio focus has been temporarily lost, but playback may continue with reduced volume.  */
        private const val AUDIO_FOCUS_STATE_LOSS_TRANSIENT_DUCK = 3

        /**
         * @see AudioManager.AUDIOFOCUS_NONE
         */
        private const val AUDIOFOCUS_NONE = AudioManager.AUDIOFOCUS_NONE

        /**
         * @see AudioManager.AUDIOFOCUS_GAIN
         */
        private const val AUDIOFOCUS_GAIN = AudioManager.AUDIOFOCUS_GAIN

        /**
         * @see AudioManager.AUDIOFOCUS_GAIN_TRANSIENT
         */
        private const val AUDIOFOCUS_GAIN_TRANSIENT = AudioManager.AUDIOFOCUS_GAIN_TRANSIENT

        /**
         * @see AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK
         */
        private const val AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK =
            AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK

        /**
         * @see AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_EXCLUSIVE
         */
        private const val AUDIOFOCUS_GAIN_TRANSIENT_EXCLUSIVE =
            AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_EXCLUSIVE
        private const val TAG = "AudioFocusManager"
        private const val VOLUME_MULTIPLIER_DUCK = 0.2f
        private const val VOLUME_MULTIPLIER_DEFAULT = 1.0f

        /**
         * Converts [AudioAttributes] to one of the audio focus request.
         *
         *
         * This follows the class Javadoc of [AudioFocusRequest].
         *
         * @param audioAttributes The audio attributes associated with this focus request.
         * @return The type of audio focus gain that should be requested.
         */
        private fun convertAudioAttributesToFocusGain(
            audioAttributes: AudioAttributes?,
        ): @AudioFocusGain Int {
            return if (audioAttributes == null) {
                // Don't handle audio focus. It may be either video only contents or developers
                // want to have more finer grained control. (e.g. adding audio focus listener)
                AUDIOFOCUS_NONE
            } else {
                when (audioAttributes.usage) {
                    C.USAGE_VOICE_COMMUNICATION_SIGNALLING -> AUDIOFOCUS_NONE
                    C.USAGE_GAME, C.USAGE_MEDIA -> AUDIOFOCUS_GAIN
                    C.USAGE_UNKNOWN -> {
                        Log.w(
                            TAG,
                            "Specify a proper usage in the audio attributes for audio focus" +
                                " handling. Using AUDIOFOCUS_GAIN by default."
                        )
                        AUDIOFOCUS_GAIN
                    }
                    C.USAGE_ALARM, C.USAGE_VOICE_COMMUNICATION -> AUDIOFOCUS_GAIN_TRANSIENT
                    C.USAGE_ASSISTANCE_NAVIGATION_GUIDANCE, C.USAGE_ASSISTANCE_SONIFICATION,
                    C.USAGE_NOTIFICATION, C.USAGE_NOTIFICATION_COMMUNICATION_DELAYED,
                    C.USAGE_NOTIFICATION_COMMUNICATION_INSTANT, C.USAGE_NOTIFICATION_COMMUNICATION_REQUEST,
                    C.USAGE_NOTIFICATION_EVENT, C.USAGE_NOTIFICATION_RINGTONE,
                    ->
                        AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK
                    C.USAGE_ASSISTANT ->
                        AUDIOFOCUS_GAIN_TRANSIENT_EXCLUSIVE

                    C.USAGE_ASSISTANCE_ACCESSIBILITY -> {
                        if (audioAttributes.contentType == C.AUDIO_CONTENT_TYPE_SPEECH) {
                            // Voice shouldn't be interrupted by other playback.
                            AUDIOFOCUS_GAIN_TRANSIENT
                        } else {
                            AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK
                        }
                    }
                    else -> {
                        Log.w(TAG, "Unidentified audio usage: " + audioAttributes.usage)
                        AUDIOFOCUS_NONE
                    }
                }
            }
        }
    }
}
