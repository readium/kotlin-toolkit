/*
 * Copyright 2023 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.adapter.exoplayer.audio

import androidx.media3.common.ForwardingPlayer
import androidx.media3.exoplayer.ExoPlaybackException
import androidx.media3.exoplayer.ExoPlayer
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.ExperimentalTime
import timber.log.Timber

/**
 * A wrapper around ExoPlayer to customize some behaviours.
 */
@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
internal class ExoAudiobookPlayer(
    private val player: ExoPlayer,
    private val itemDurations: List<Duration>?,
    private val seekForwardIncrement: Duration,
    private val seekBackwardIncrement: Duration,
) : ForwardingPlayer(player) {

    fun seekBy(offset: Duration) {
        itemDurations
            ?.let { smartSeekBy(offset, it) }
            ?: dumbSeekBy(offset)
    }

    override fun seekForward() {
        seekBy(seekForwardIncrement)
    }

    override fun seekBack() {
        seekBy(-seekBackwardIncrement)
    }

    override fun getPlayerError(): ExoPlaybackException? {
        return player.playerError
    }

    @OptIn(ExperimentalTime::class)
    private fun smartSeekBy(
        offset: Duration,
        durations: List<Duration>,
    ) {
        val (newIndex, newPosition) =
            SmartSeeker.dispatchSeek(
                offset,
                player.currentPosition.milliseconds,
                player.currentMediaItemIndex,
                durations
            )
        Timber.v("Smart seeking by $offset resolved to item $newIndex position $newPosition")
        player.seekTo(newIndex, newPosition.inWholeMilliseconds)
    }

    private fun dumbSeekBy(offset: Duration) {
        val newIndex = player.currentMediaItemIndex
        val newPosition = player.currentPosition + offset.inWholeMilliseconds
        player.seekTo(newIndex, newPosition)
    }
}
