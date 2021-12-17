package org.readium.r2.navigator.media2

import androidx.media2.common.MediaItem
import androidx.media2.session.MediaController
import androidx.media2.session.SessionResult
import org.readium.r2.navigator.ExperimentalAudiobook
import org.readium.r2.shared.util.Try
import org.readium.r2.shared.util.flatMap
import timber.log.Timber
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine
import kotlin.time.Duration
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
internal class MediaControllerFacade(
    private val mediaController: MediaController,
) {
    val mediaControllerState: MediaControllerState
        get() = MediaControllerState.from(mediaController.playerState)

    val currentPosition: Duration?
        get() = mediaController.currentPositionDuration

    val currentItem: MediaItem?
        get() = mediaController.currentMediaItem

    val playlist: List<MediaItem>?
        get() = mediaController.playlist

    val playbackSpeed: Double?
        get() = mediaController.playbackSpeedNullable

    fun close() {
        Timber.d("executing close")
        mediaController.close()
    }

    /*
     * Basic commands
     */

    suspend fun prepare(): MediaControllerResult =
        submitCommand(this::prepareSync)

    suspend fun play(): MediaControllerResult =
        submitCommand(this::playSync)

    suspend fun pause(): MediaControllerResult =
        submitCommand(this::pauseSync)

    suspend fun setPlaybackSpeed(speed: Double): MediaControllerResult =
        submitCommand { setPlaybackSpeedSync(speed) }

    /*
     * Compound commands
     */

    suspend fun seekTo(index: Int, position: Duration): MediaControllerResult =
        submitCommandOnPause {
            when {
                index == mediaController.currentMediaItemIndex -> {
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
     * Helpers
     */

    private val queueExecutor: ExecutorService =
        Executors.newSingleThreadExecutor()

    private suspend fun submitCommand(command: () -> MediaControllerResult): MediaControllerResult =
        suspendCoroutine { continuation ->
            queueExecutor.submit {
                val result = command()
                continuation.resume(result)
            }
        }

    private suspend fun submitCommandOnPause(command: () -> MediaControllerResult): MediaControllerResult =
        submitCommand {
            val isPlaying = mediaController.stateEnum == MediaControllerState.Playing
            pauseSync()
            val result = command()
            if (isPlaying) playSync()
            result
        }

    private fun SessionResult.toTry() =
        if (resultCode == 0) {
            Try.success(Unit)
        } else {
            val error = MediaControllerError.fromSessionResultCode(resultCode)
            Try.failure(MediaControllerException(error))
        }

    /*
     * Synchronous API
     */

    private fun prepareSync(): MediaControllerResult {
        Timber.d("executing prepare")
        val result = mediaController.prepare().get()
        Timber.d("prepare finished with result ${result.resultCode}")
        return result.toTry()
    }

    private fun playSync(): MediaControllerResult {
        Timber.d("executing play")
        val result = mediaController.play().get()
        Timber.d("play finished with result ${result.resultCode}")
        return result.toTry()
    }

    private fun pauseSync(): MediaControllerResult {
        Timber.d("executing pause")
        val result = mediaController.pause().get()
        Timber.d("pause finished with result ${result.resultCode}")
        return result.toTry()
    }

    private fun seekToSync(position: Duration): MediaControllerResult {
        Timber.d("executing seekTo $position")
        val result = mediaController.seekTo(position.inWholeMilliseconds).get()
        Timber.d("seekTo finished with result ${result.resultCode}")
        return result.toTry()
    }

    private fun skipToPlaylistItemSync(index: Int): MediaControllerResult {
        Timber.d("executing skipToPlaylistItem $index")
        val result = mediaController.skipToPlaylistItem(index).get()
        Timber.d("skipToPlaylistItem finished with result ${result.resultCode}")
        return result.toTry()
    }

    private fun setPlaybackSpeedSync(speed: Double): MediaControllerResult {
        Timber.d("executing setPlaybackSpeed $speed")
        val result = mediaController.setPlaybackSpeed(speed.toFloat()).get()
        Timber.d("setPlaybackSpeed finished with result ${result.resultCode}")
        return result.toTry()
    }
}
