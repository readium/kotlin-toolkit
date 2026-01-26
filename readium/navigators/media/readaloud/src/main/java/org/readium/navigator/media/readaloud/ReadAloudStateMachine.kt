/*
 * Copyright 2025 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

@file:OptIn(ExperimentalReadiumApi::class)

package org.readium.navigator.media.readaloud

import org.readium.navigator.media.readaloud.preferences.ReadAloudSettings
import org.readium.r2.shared.ExperimentalReadiumApi
import org.readium.r2.shared.util.Error

internal class ReadAloudStateMachine(
    private val dataLoader: ReadAloudDataLoader,
    private val navigationHelper: ReadAloudNavigationHelper,
) {

    sealed interface PlaybackState {

        data object Ready : PlaybackState

        data object Starved : PlaybackState

        data object Ended : PlaybackState

        data class Failure(val error: Error) : PlaybackState
    }

    data class State(
        val playbackState: PlaybackState,
        val playWhenReady: Boolean,
        val node: ReadAloudNode,
        val nodeIndex: Int,
        val playerPaused: Boolean,
        val segment: ReadAloudSegment,
        val settings: ReadAloudSettings,
    )

    fun play(
        segment: ReadAloudSegment,
        itemIndex: Int,
        playWhenReady: Boolean,
        settings: ReadAloudSettings,
    ): State {
        segment.player.stop()
        segment.player.itemToPlay = itemIndex
        segment.applySettings(settings)

        if (playWhenReady) {
            segment.player.start()
        }

        return State(
            playbackState = PlaybackState.Ready,
            playWhenReady = playWhenReady,
            node = segment.nodes[itemIndex],
            nodeIndex = itemIndex,
            playerPaused = false,
            segment = segment,
            settings = settings
        )
    }

    fun State.jump(node: ReadAloudNode): State {
        val firstContentNode = with(navigationHelper) { node.firstContentNode() }
            ?: return copy(playbackState = PlaybackState.Ended)

        val itemRef = dataLoader.getItemRef(firstContentNode)
            ?: return copy(playbackState = PlaybackState.Ended)

        val currentSegment = segment

        if (currentSegment != itemRef.segment) {
            currentSegment.player.release()
        }

        val index = itemRef.nodeIndex!! // This is a content node
        return play(itemRef.segment, index, playWhenReady, settings)
    }

    fun State.restart(): State {
        segment.player.release()

        val itemRef = dataLoader.getItemRef(node)
            ?: return copy(playbackState = PlaybackState.Ended)

        val nodeIndex = itemRef.nodeIndex!!

        return play(itemRef.segment, nodeIndex, playWhenReady, settings)
    }

    fun State.pause(): State {
        segment.player.pause()
        return copy(playWhenReady = false, playerPaused = true)
    }

    fun State.resume(): State {
        if (playerPaused) {
            segment.player.resume()
        } else {
            segment.player.start()
        }
        return copy(playWhenReady = true, playerPaused = false)
    }

    fun State.release(): State {
        segment.player.release()
        return this
    }

    fun State.onSettingsChanged(
        newSettings: ReadAloudSettings,
    ): State {
        navigationHelper.settings = newSettings
        dataLoader.settings = newSettings

        return copy(settings = newSettings).restart()
    }

    private fun ReadAloudSegment.applySettings(settings: ReadAloudSettings) {
        player.speed = settings.speed
        player.pitch = settings.pitch
    }

    fun State.onPlaybackEngineStateChanged(state: PlaybackEngine.PlaybackState): State {
        return when (state) {
            PlaybackEngine.PlaybackState.Playing ->
                copy(playbackState = PlaybackState.Ready)
            PlaybackEngine.PlaybackState.Starved ->
                copy(playbackState = PlaybackState.Starved)
        }
    }

    fun State.onPlaybackCompleted(): State {
        return if (nodeIndex + 1 < segment.nodes.size) {
            setSegmentItem(index = nodeIndex + 1, playWhenReady = playWhenReady && settings.readContinuously)
        } else {
            onSegmentEnded()
        }
    }

    private fun State.setSegmentItem(index: Int, playWhenReady: Boolean): State {
        segment.player.itemToPlay = index
        if (playWhenReady) {
            segment.player.start()
        }
        return copy(nodeIndex = index, node = segment.nodes[index], playWhenReady = playWhenReady)
    }

    private fun State.onSegmentEnded(): State {
        val nextNode = node.next()
            ?: return copy(playbackState = PlaybackState.Ended)

        val newSegment = dataLoader.getItemRef(nextNode)?.segment
            ?: return copy(playbackState = PlaybackState.Ended)

        segment.player.release()

        val playWhenReady = settings.readContinuously

        return play(newSegment, 0, playWhenReady, settings)
    }
}
