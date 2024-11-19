/*
 * Copyright (C) 2019 The Android Open Source Project
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
import android.os.Handler

internal class AudioBecomingNoisyManager(
    context: Context,
    eventHandler: Handler,
    listener: EventListener,
) {
    private val context: Context
    private val receiver: AudioBecomingNoisyReceiver
    private var receiverRegistered = false

    interface EventListener {
        fun onAudioBecomingNoisy()
    }

    init {
        this.context = context.applicationContext
        receiver = AudioBecomingNoisyReceiver(eventHandler, listener)
    }

    /**
     * Enables the [AudioBecomingNoisyManager] which calls [ ][EventListener.onAudioBecomingNoisy]
     * upon receiving an intent of [ ][AudioManager.ACTION_AUDIO_BECOMING_NOISY].
     *
     * @param enabled True if the listener should be notified when audio is becoming noisy.
     */
    fun setEnabled(enabled: Boolean) {
        if (enabled && !receiverRegistered) {
            context.registerReceiver(
                receiver,
                IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY)
            )
            receiverRegistered = true
        } else if (!enabled && receiverRegistered) {
            context.unregisterReceiver(receiver)
            receiverRegistered = false
        }
    }

    private inner class AudioBecomingNoisyReceiver(
        private val eventHandler: Handler,
        private val listener: EventListener,
    ) :
        BroadcastReceiver(), Runnable {
        override fun onReceive(context: Context, intent: Intent) {
            if (AudioManager.ACTION_AUDIO_BECOMING_NOISY == intent.action) {
                eventHandler.post(this)
            }
        }

        override fun run() {
            if (receiverRegistered) {
                listener.onAudioBecomingNoisy()
            }
        }
    }
}
