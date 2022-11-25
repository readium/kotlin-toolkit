/*
 * Copyright 2021 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.navigator.media2

import android.app.PendingIntent
import android.content.Context
import androidx.media2.common.MediaItem
import androidx.media2.common.MediaMetadata
import androidx.media2.common.SessionPlayer
import androidx.media2.session.MediaSession
import com.google.common.util.concurrent.SettableFuture
import java.util.concurrent.*
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.ExperimentalTime
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.receiveAsFlow
import org.readium.r2.shared.util.Try
import org.readium.r2.shared.util.flatMap
import timber.log.Timber

/**
 * This class's purpose is two-fold:
 * - wrapping the [SessionPlayer] inside a coroutine-based API
 * - adding compound commands
 *
 * All commands, either basic or compound, are executed in order of arrival on the same thread.
 * Compound commands have two limitations due to the framework weaknesses:
 * - in case of failure, the playback can be left in an intermediate state
 * - the behaviour is undefined if any external controller takes actions at the same time
 */
@OptIn(ExperimentalTime::class)
internal class SessionPlayerFacade(
    private val sessionPlayer: SessionPlayer,
    private val seekCompletedReceiver: ReceiveChannel<Long>,
    playerStateFlow: Flow<SessionPlayerState>,
) {
    private val coroutineScope = MainScope()

    private var pendingSeek: SettableFuture<Long>? = null

    private var waitedSeek: Long? = null

    private var pendingState: SettableFuture<SessionPlayerState>? = null

    private var waitedState: SessionPlayerState? = null

    init {
        seekCompletedReceiver.receiveAsFlow()
            .onEach { if (it == waitedSeek) pendingSeek?.set(it) }
            .launchIn(coroutineScope)

        playerStateFlow
            .onEach { if (it == waitedState) pendingState?.set(it) }
            .launchIn(coroutineScope)
    }

    val playerState: SessionPlayerState
        get() = SessionPlayerState.fromCode(sessionPlayer.playerState)

    val currentPosition: Duration?
        get() = sessionPlayer.currentPositionDuration

    val currentIndex: Int?
        get() = sessionPlayer.currentMediaItemIndex
            .takeUnless { it == SessionPlayer.INVALID_ITEM_INDEX }

    val currentDuration: Duration?
        get() = sessionPlayer.duration
            .takeUnless { it == SessionPlayer.UNKNOWN_TIME }
            ?.milliseconds

    val playlist: List<MediaItem>?
        get() = sessionPlayer.playlist

    val playbackSpeed: Double?
        get() = sessionPlayer.playbackSpeedNullable

    fun unregisterPlayerCallback(callback: SessionPlayer.PlayerCallback) {
        sessionPlayer.unregisterPlayerCallback(callback)
    }

    fun close() {
        seekCompletedReceiver.cancel()
        sessionPlayer.close()
    }

    fun session(context: Context, id: String?, activityIntent: PendingIntent): MediaSession {
        val builder = MediaSession.Builder(context, sessionPlayer)
            .setSessionActivity(activityIntent)

        id?.let { builder.setId(id) }
        return builder.build()
    }

    /*
     * Basic commands
     */

    suspend fun prepare(): SessionPlayerResult =
        submitCommand(this::prepareSync)

    suspend fun setPlaylist(playlist: List<MediaItem>, metadata: MediaMetadata): SessionPlayerResult =
        submitCommand { setPlaylistSync(playlist, metadata) }

    suspend fun play(): SessionPlayerResult =
        submitCommand(this::playSync)

    suspend fun pause(): SessionPlayerResult =
        submitCommand(this::pauseSync)

    suspend fun setPlaybackSpeed(speed: Double): SessionPlayerResult =
        submitCommand { setPlaybackSpeedSync(speed) }

    /*
     * Compound commands
     */

    suspend fun seekTo(index: Int, position: Duration): SessionPlayerResult =
        submitCommandOnPause {
            when {
                index == sessionPlayer.currentMediaItemIndex -> {
                    seekToSync(position)
                }
                position == Duration.ZERO -> {
                    skipToPlaylistItemSync(index)
                }
                else -> {
                    skipToPlaylistItemSync(index)
                        // WORKAROUND: skipToPlayListItem terminates before Exoplayer is aware that
                        // the new item is seekable. Not to wait would result in the command being skipped.
                        .also { Thread.sleep(500) }
                        .flatMap { seekToSync(position) }
                }
            }
        }

    /*
     * Internal Helpers
     */

    private val queueExecutor: ExecutorService =
        Executors.newSingleThreadExecutor()

    private suspend fun submitCommand(command: () -> SessionPlayerResult): SessionPlayerResult =
        suspendCoroutine { continuation ->
            queueExecutor.submit {
                val result = command()
                continuation.resume(result)
            }
        }

    private suspend fun submitCommandOnPause(command: () -> SessionPlayerResult): SessionPlayerResult =
        submitCommand {
            val isPlaying = sessionPlayer.stateEnum == SessionPlayerState.Playing
            pauseSync()
            val result = command()
            if (isPlaying) playSync()
            result
        }

    private fun SessionPlayer.PlayerResult.toTry() =
        if (resultCode == 0) {
            Try.success(Unit)
        } else {
            val error =
                SessionPlayerError.fromCode(resultCode)
            Try.failure(SessionPlayerException(error))
        }

    /*
     * Synchronous API
     */

    private fun prepareSync(): SessionPlayerResult {
        Timber.v("executing prepare")
        val result = sessionPlayer.prepare().get()
        Timber.v("prepare finished with result ${result.resultCode}")
        return result.toTry()
    }

    private fun setPlaylistSync(playlist: List<MediaItem>, metadata: MediaMetadata): SessionPlayerResult {
        Timber.v("executing setPlaylist")
        val result = sessionPlayer.setPlaylist(playlist, metadata).get()
        Timber.v("setPlaylist finished with result ${result.resultCode}")
        return result.toTry()
    }

    private fun playSync(): SessionPlayerResult {
        Timber.v("executing play")
        val result = sessionPlayer.play().get()

        if (result.resultCode == 0) {
            try {
                waitForState(SessionPlayerState.Playing).get(1, TimeUnit.SECONDS)
            } catch (e: TimeoutException) {
                Timber.v("expected ${SessionPlayerState.Playing} state not received")
            }
        }

        Timber.v("play finished with result ${result.resultCode}")
        return result.toTry()
    }

    private fun pauseSync(): SessionPlayerResult {
        Timber.v("executing pause")
        val result = sessionPlayer.pause().get()

        if (result.resultCode == 0) {
            try {
                waitForState(SessionPlayerState.Paused).get(1, TimeUnit.SECONDS)
                pendingState = null
            } catch (e: TimeoutException) {
                Timber.v("expected ${SessionPlayerState.Paused} state not received")
            }
        }

        Timber.v("pause finished with result ${result.resultCode}")
        return result.toTry()
    }

    // This is useful because the play/pause commands complete before the new state is published.
    private fun waitForState(state: SessionPlayerState): Future<SessionPlayerState> {
        val future = SettableFuture.create<SessionPlayerState>()
        waitedState = state
        pendingState = future
        return future
    }

    private fun seekToSync(position: Duration): SessionPlayerResult {
        Timber.v("executing seekTo $position")
        val result = sessionPlayer.seekTo(position.inWholeMilliseconds).get()

        if (result.resultCode == 0) {
            try {
                waitForSeekCompleted(position.inWholeMilliseconds).get(1, TimeUnit.SECONDS)
            } catch (e: TimeoutCancellationException) {
                val exception = SessionPlayerException(SessionPlayerError.INFO_SKIPPED)
                Timber.v("seek callback was not called")
                return SessionPlayerResult.failure(exception)
            }
        }

        Timber.v("seekTo finished with result ${result.resultCode}")
        return result.toTry()
    }

    // This is useful because the seek command can return before the seeking has actually completed
    private fun waitForSeekCompleted(position: Long): Future<Long> {
        val future = SettableFuture.create<Long>()
        waitedSeek = position
        pendingSeek = future
        return future
    }

    private fun skipToPlaylistItemSync(index: Int): SessionPlayerResult {
        Timber.v("executing skipToPlaylistItem $index")
        val result = sessionPlayer.skipToPlaylistItem(index).get()
        Timber.v("skipToPlaylistItem finished with result ${result.resultCode}")
        return result.toTry()
    }

    private fun setPlaybackSpeedSync(speed: Double): SessionPlayerResult {
        Timber.v("executing setPlaybackSpeed $speed")
        val result = sessionPlayer.setPlaybackSpeed(speed.toFloat()).get()
        Timber.v("setPlaybackSpeed finished with result ${result.resultCode}")
        return result.toTry()
    }
}
