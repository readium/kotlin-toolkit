package org.readium.r2.navigator.media2

import android.app.PendingIntent
import android.content.Context
import androidx.media2.common.MediaItem
import androidx.media2.common.MediaMetadata
import androidx.media2.common.SessionPlayer
import androidx.media2.session.MediaSession
import org.readium.r2.navigator.ExperimentalAudiobook
import org.readium.r2.shared.util.Try
import org.readium.r2.shared.util.flatMap
import timber.log.Timber
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.ExperimentalTime

/**
 * This class's purpose is two-fold:
 * - wrapping the MediaController inside a coroutine-based API
 * - adding compound commands
 *
 * All commands, either basic or compound, are executed in order of arrival on the same thread.
 * Compound commands have two limitations due to the framework weaknesses:
 * - in case of failure, the playback can be left in an intermediate state
 * - the behaviour is undefined if any external controller takes actions at the same time
 */

@ExperimentalAudiobook
@OptIn(ExperimentalTime::class)
internal class SessionPlayerFacade(
    private val sessionPlayer: SessionPlayer,
) {
    val playerState: SessionPlayerState
        get() = SessionPlayerState.from(sessionPlayer.playerState)

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
        sessionPlayer.close()
    }

    fun session(context: Context, id: String, activityIntent: PendingIntent): MediaSession {
        return MediaSession.Builder(context, sessionPlayer)
            .setId(id)
            .setSessionActivity(activityIntent)
            .build()
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
            val error = SessionPlayerError.fromSessionResultCode(resultCode)
            Try.failure(SessionPlayerException(error))
        }

    /*
     * Synchronous API
     */

    private fun prepareSync(): SessionPlayerResult {
        Timber.d("executing prepare")
        val result = sessionPlayer.prepare().get()
        Timber.d("prepare finished with result ${result.resultCode}")
        return result.toTry()
    }

    private fun setPlaylistSync(playlist: List<MediaItem>, metadata: MediaMetadata): SessionPlayerResult {
        Timber.d("executing setPlaylist")
        val result = sessionPlayer.setPlaylist(playlist, metadata).get()
        Timber.d("setPlaylist finished with result ${result.resultCode}")
        return result.toTry()
    }

    private fun playSync(): SessionPlayerResult {
        Timber.d("executing play")
        val result = sessionPlayer.play().get()
        Timber.d("play finished with result ${result.resultCode}")
        return result.toTry()
    }

    private fun pauseSync(): SessionPlayerResult {
        Timber.d("executing pause")
        val result = sessionPlayer.pause().get()
        Timber.d("pause finished with result ${result.resultCode}")
        return result.toTry()
    }

    private fun seekToSync(position: Duration): SessionPlayerResult {
        //FIXME: Behaviour in case of out of range position is unclear.
        Timber.d("executing seekTo $position")
        //FIXME: seekTo's future completes before the actual seeking has been done.
        val result = sessionPlayer.seekTo(position.inWholeMilliseconds).get()
        Timber.d("seekTo finished with result ${result.resultCode}")
        return result.toTry()
    }

    private fun skipToPlaylistItemSync(index: Int): SessionPlayerResult {
        Timber.d("executing skipToPlaylistItem $index")
        val result = sessionPlayer.skipToPlaylistItem(index).get()
        Timber.d("skipToPlaylistItem finished with result ${result.resultCode}")
        return result.toTry()
    }

    private fun setPlaybackSpeedSync(speed: Double): SessionPlayerResult {
        Timber.d("executing setPlaybackSpeed $speed")
        val result = sessionPlayer.setPlaybackSpeed(speed.toFloat()).get()
        Timber.d("setPlaybackSpeed finished with result ${result.resultCode}")
        return result.toTry()
    }
}
