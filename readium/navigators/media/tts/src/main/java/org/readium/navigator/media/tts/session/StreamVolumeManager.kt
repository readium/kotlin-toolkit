/*
 * Copyright 2020 The Android Open Source Project
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

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioManager
import android.os.Build
import android.os.Handler
import androidx.media3.common.C
import androidx.media3.common.util.Assertions
import androidx.media3.common.util.Log

@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
/** A manager that wraps [AudioManager] to control/listen audio stream volume.  */
internal class StreamVolumeManager(context: Context, eventHandler: Handler, listener: Listener) {
    /** A listener for changes in the manager.  */
    interface Listener {
        /** Called when the audio stream type is changed.  */
        fun onStreamTypeChanged(streamType: @C.StreamType Int)

        /** Called when the audio stream volume or mute state is changed.  */
        fun onStreamVolumeChanged(streamVolume: Int, streamMuted: Boolean)
    }

    private val applicationContext: Context
    private val eventHandler: Handler
    private val listener: Listener
    private val audioManager: AudioManager
    private var receiver: VolumeChangeReceiver? = null
    private var streamType: @C.StreamType Int
    private var volume: Int
    private var muted: Boolean

    /** Creates a manager.  */
    init {
        applicationContext = context.applicationContext
        this.eventHandler = eventHandler
        this.listener = listener
        audioManager = Assertions.checkStateNotNull(
            applicationContext.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        )
        streamType = C.STREAM_TYPE_DEFAULT
        volume = getVolumeFromManager(audioManager, streamType)
        muted = getMutedFromManager(audioManager, streamType)
        val receiver = VolumeChangeReceiver()
        val filter = IntentFilter(VOLUME_CHANGED_ACTION)
        try {
            applicationContext.registerReceiver(receiver, filter)
            this.receiver = receiver
        } catch (e: RuntimeException) {
            Log.w(TAG, "Error registering stream volume receiver", e)
        }
    }

    /** Sets the audio stream type.  */
    fun setStreamType(streamType: @C.StreamType Int) {
        if (this.streamType == streamType) {
            return
        }
        this.streamType = streamType
        updateVolumeAndNotifyIfChanged()
        listener.onStreamTypeChanged(streamType)
    }

    /**
     * Gets the minimum volume for the current audio stream. It can be changed if [ ][.setStreamType] is called.
     */
    val minVolume: Int
        get() = if (Build.VERSION.SDK_INT >= 28) audioManager.getStreamMinVolume(streamType) else 0

    /**
     * Gets the maximum volume for the current audio stream. It can be changed if [ ][.setStreamType] is called.
     */
    val maxVolume: Int
        get() = audioManager.getStreamMaxVolume(streamType)

    /** Gets the current volume for the current audio stream.  */
    fun getVolume(): Int {
        return volume
    }

    /** Gets whether the current audio stream is muted or not.  */
    fun isMuted(): Boolean {
        return muted
    }

    /**
     * Sets the volume with the given value for the current audio stream. The value should be between
     * [.getMinVolume] and [.getMaxVolume], otherwise it will be ignored.
     */
    fun setVolume(volume: Int) {
        if (volume < minVolume || volume > maxVolume) {
            return
        }
        audioManager.setStreamVolume(streamType, volume, VOLUME_FLAGS)
        updateVolumeAndNotifyIfChanged()
    }

    /**
     * Increases the volume by one for the current audio stream. It will be ignored if the current
     * volume is equal to [.getMaxVolume].
     */
    fun increaseVolume() {
        if (volume >= maxVolume) {
            return
        }
        audioManager.adjustStreamVolume(streamType, AudioManager.ADJUST_RAISE, VOLUME_FLAGS)
        updateVolumeAndNotifyIfChanged()
    }

    /**
     * Decreases the volume by one for the current audio stream. It will be ignored if the current
     * volume is equal to [.getMinVolume].
     */
    fun decreaseVolume() {
        if (volume <= minVolume) {
            return
        }
        audioManager.adjustStreamVolume(streamType, AudioManager.ADJUST_LOWER, VOLUME_FLAGS)
        updateVolumeAndNotifyIfChanged()
    }

    /** Sets the mute state of the current audio stream.  */
    fun setMuted(muted: Boolean) {
        audioManager.adjustStreamVolume(
            streamType,
            if (muted) AudioManager.ADJUST_MUTE else AudioManager.ADJUST_UNMUTE,
            VOLUME_FLAGS
        )
        updateVolumeAndNotifyIfChanged()
    }

    /** Releases the manager. It must be called when the manager is no longer required.  */
    fun release() {
        if (receiver != null) {
            try {
                applicationContext.unregisterReceiver(receiver)
            } catch (e: RuntimeException) {
                Log.w(TAG, "Error unregistering stream volume receiver", e)
            }
            receiver = null
        }
    }

    private fun updateVolumeAndNotifyIfChanged() {
        val newVolume = getVolumeFromManager(audioManager, streamType)
        val newMuted = getMutedFromManager(audioManager, streamType)
        if (volume != newVolume || muted != newMuted) {
            volume = newVolume
            muted = newMuted
            listener.onStreamVolumeChanged(newVolume, newMuted)
        }
    }

    private inner class VolumeChangeReceiver : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            eventHandler.post { updateVolumeAndNotifyIfChanged() }
        }
    }

    companion object {
        private const val TAG = "StreamVolumeManager"

        // TODO(b/151280453): Replace the hidden intent action with an official one.
        // Copied from AudioManager#VOLUME_CHANGED_ACTION
        private const val VOLUME_CHANGED_ACTION = "android.media.VOLUME_CHANGED_ACTION"

        // TODO(b/153317944): Allow users to override these flags.
        private const val VOLUME_FLAGS = AudioManager.FLAG_SHOW_UI
        private fun getVolumeFromManager(
            audioManager: AudioManager,
            streamType: @C.StreamType Int,
        ): Int {
            // AudioManager#getStreamVolume(int) throws an exception on some devices. See
            // https://github.com/google/ExoPlayer/issues/8191.
            return try {
                audioManager.getStreamVolume(streamType)
            } catch (e: RuntimeException) {
                Log.w(
                    TAG,
                    "Could not retrieve stream volume for stream type $streamType",
                    e
                )
                audioManager.getStreamMaxVolume(streamType)
            }
        }

        private fun getMutedFromManager(
            audioManager: AudioManager,
            streamType: @C.StreamType Int,
        ): Boolean {
            return audioManager.isStreamMute(streamType)
        }
    }
}
