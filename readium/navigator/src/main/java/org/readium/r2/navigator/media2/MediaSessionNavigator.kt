package org.readium.r2.navigator.media2

import android.app.PendingIntent
import android.content.Context
import androidx.media2.common.MediaMetadata
import androidx.media2.common.SessionPlayer
import androidx.media2.session.MediaSession
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import org.readium.r2.navigator.ExperimentalAudiobook
import org.readium.r2.navigator.Navigator
import org.readium.r2.navigator.extensions.sum
import org.readium.r2.navigator.extensions.time
import org.readium.r2.shared.publication.*
import org.readium.r2.shared.util.Try
import org.readium.r2.shared.util.flatMap
import timber.log.Timber
import java.util.concurrent.Executors
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlin.time.ExperimentalTime

/**
 * An audiobook navigator to connect to a MediaSession from Jetpack Media2.
 *
 * Use [create] to get an instance for a given publication, and build a session from it
 * with the [session] method. Apps are responsible for attaching this session to a service able to
 * expose it.
 *
 * You can build a [MediaSessionNavigator] upon any Media2 [SessionPlayer] implementation
 * providing [create] with it. If you don't, ExoPlayer will be used, without cache.
 * Use [ExoPlayerFactory] to build a [SessionPlayer] based on ExoPlayer with caching capabilities.
 */
@ExperimentalAudiobook
@OptIn(ExperimentalTime::class)
class MediaSessionNavigator private constructor(
    override val publication: Publication,
    private val playerFacade: SessionPlayerFacade,
    private val playerCallback: SessionPlayerCallback,
    private val configuration: Configuration
) : Navigator {

    private val coroutineScope: CoroutineScope = MainScope()

    private fun <T> Flow<T>.stateInFirst(coroutineScope: CoroutineScope): StateFlow<T> =
        stateIn(coroutineScope, SharingStarted.Lazily, runBlocking { first() })

    private val totalDuration: Duration? =
        this.playerFacade.playlist!!.durations?.sum()

    private val currentLocatorFlow: Flow<Locator> =
        combine(
            playerCallback.currentItem,
            playerCallback.currentPosition
        ) { currentItem, currentPosition ->
            locator(
                currentItem,
                currentPosition,
                this.playerFacade.playlist!!.map { it.metadata!! })
        }

    private fun locator(
        item: MediaMetadata,
        position: Duration,
        playlist: List<MediaMetadata>
    ): Locator {
        val link = publication.readingOrder[item.index]
        val itemStartPosition = playlist.slice(0 until item.index).map { it.duration }.sum()
        val totalProgression = totalDuration?.let { (itemStartPosition + position) / it }

        return link.toLocator().copyWithLocations(
            fragments = listOf("t=${position.inWholeSeconds}"),
            progression = item.duration?.let { position / it },
            position = item.index + 1,
            totalProgression = totalProgression
        )
    }

    override val currentLocator: StateFlow<Locator> =
        currentLocatorFlow.stateInFirst(coroutineScope)

    private val playbackStateFlow: Flow<Playback> =
        combine(
            playerCallback.playerState,
            playerCallback.playbackSpeed,
            playerCallback.currentItem,
            playerCallback.currentPosition,
            playerCallback.bufferedPosition
        ) { currentState, playbackSpeed, currentItem, currentPosition, bufferedPosition ->
            val state = when (currentState) {
                SessionPlayerState.Playing ->
                    Playback.State.Playing
                SessionPlayerState.Idle, SessionPlayerState.Error ->
                    Playback.State.Error
                SessionPlayerState.Paused ->
                    if (playerCallback.playbackCompleted) {
                        Playback.State.Finished
                    } else {
                        Playback.State.Paused
                    }
            }
            Playback(
                state = state,
                rate = playbackSpeed.toDouble(),
                currentIndex = currentItem.index,
                currentLink = publication.readingOrder[currentItem.index],
                currentPosition = currentPosition,
                bufferedPosition = bufferedPosition
            )
        }

    /**
     * Indicates the navigator current state.
     */
    val playback: StateFlow<Playback> =
        playbackStateFlow.stateInFirst(coroutineScope)

    /**
     * Sets the speed of the media playback.
     *
     * Normal speed is 1.0 and 0.0 is incorrect.
     */
    suspend fun setPlaybackRate(rate: Double): Try<Unit, Exception> =
        playerFacade.setPlaybackSpeed(rate).toNavigatorResult()

    /**
     * Resumes or start the playback at the current location.
     */
    suspend fun play(): Try<Unit, Exception> =
        playerFacade.play().toNavigatorResult()

    /**
     * Pauses the playback.
     */
    suspend fun pause(): Try<Unit, Exception> =
        playerFacade.pause().toNavigatorResult()

    /**
     * Seeks to the given time at the given resource.
     */
    suspend fun seek(index: Int, position: Duration): Try<Unit, Exception> =
        playerFacade.seekTo(index, position).toNavigatorResult()

    /**
     * Seeks to the given locator.
     */
    suspend fun go(locator: Locator): Try<Unit, Exception> {
        Timber.d("Go to locator $locator")
        val itemIndex = checkNotNull(publication.readingOrder.indexOfFirstWithHref(locator.href))
        val position = locator.locations.time ?: Duration.ZERO
        return seek(itemIndex, position)
    }

    /**
     * Seeks to the beginning of the given link.
     */
    suspend fun go(link: Link) =
        go(link.toLocator())

    /**
     * Skips to a little amount of time later.
     */
    suspend fun goForward(): Try<Unit, Exception> =
        seekBy(configuration.skipForwardInterval)

    /**
     * Skips to a little amount of time before.
     */
    suspend fun goBackward(): Try<Unit, Exception> =
        seekBy(-configuration.skipBackwardInterval)

    private suspend fun seekBy(offset: Duration): Try<Unit, Exception> =
        this.playerFacade.playlist!!.durations
            ?.let { smartSeekBy(offset, it) }
            ?: dummySeekBy(offset)

    private suspend fun smartSeekBy(
        offset: Duration,
        durations: List<Duration>
    ): Try<Unit, Exception> {
        val (newIndex, newPosition) =
            SmartSeeker.dispatchSeek(
                offset,
                playerFacade.currentPosition!!,
                playerFacade.currentIndex!!,
                durations
            )
        Timber.d("Smart seeking by $offset resolved to item $newIndex position $newPosition")
        return playerFacade.seekTo(newIndex, newPosition).toNavigatorResult()
    }

    private suspend fun dummySeekBy(offset: Duration): Try<Unit, Exception> {
        val newIndex = playerFacade.currentIndex!!
        val newPosition = playerFacade.currentPosition!! + offset
        return playerFacade.seekTo(newIndex, newPosition).toNavigatorResult()
    }

    /**
     * Stops the playback.
     *
     * Compared to [pause], the navigator may clear its state in whatever way is appropriate. For
     * example, recovering a player's resources.
     */
    fun close() {
        playerFacade.unregisterPlayerCallback(playerCallback)
        playerCallback.close()
        playerFacade.close()
        coroutineScope.cancel()
    }

    fun session(context: Context, id: String, activityIntent: PendingIntent): MediaSession =
        playerFacade.session(context, id, activityIntent)


    data class Configuration(
        val positionRefreshRate: Double = 2.0,  // Hz
        val skipForwardInterval: Duration = 30.seconds,
        val skipBackwardInterval: Duration = 30.seconds,
    )

    @ExperimentalTime
    data class Playback(
        val state: State,
        val rate: Double,
        val currentIndex: Int,
        val currentLink: Link,
        val currentPosition: Duration,
        val bufferedPosition: Duration,
    ) {

        enum class State {
            Playing,
            Paused,
            Finished,
            Error
        }
    }

    sealed class Exception : kotlin.Exception()

    /*
     * Compatibility
     */

    private fun launchAndRun(runnable: suspend () -> Unit, callback: () -> Unit) =
        coroutineScope.launch { runnable() }.invokeOnCompletion { callback() }

    override fun go(locator: Locator, animated: Boolean, completion: () -> Unit): Boolean {
        launchAndRun({ go(locator) }, completion)
        return true
    }

    override fun go(link: Link, animated: Boolean, completion: () -> Unit): Boolean {
        launchAndRun({ go(link) }, completion)
        return true
    }

    override fun goForward(animated: Boolean, completion: () -> Unit): Boolean {
        launchAndRun({ goForward() }, completion)
        return true
    }

    override fun goBackward(animated: Boolean, completion: () -> Unit): Boolean {
        launchAndRun({ goBackward() }, completion)
        return true
    }

    companion object {

        suspend fun create(
            context: Context,
            publication: Publication,
            initialLocator: Locator?,
            configuration: Configuration = Configuration(),
            player: SessionPlayer = ExoPlayerFactory().createPlayer(context, publication)
        ): Try<MediaSessionNavigator, Exception> {

            val positionRefreshDelay = (1.0 / configuration.positionRefreshRate).seconds
            val callback = SessionPlayerCallback(positionRefreshDelay)
            val callbackExecutor = Executors.newSingleThreadExecutor()
            player.registerPlayerCallback(callbackExecutor, callback)

            val facade = SessionPlayerFacade(player)
            return preparePlayer(publication, facade)
                // Ignoring failure to set initial locator
                .onSuccess { goInitialLocator(publication, initialLocator, facade) }
                // Player must be ready to play when MediaNavigator's constructor is called.
                .map { MediaSessionNavigator(publication, facade, callback, configuration) }
        }

        private suspend fun preparePlayer(
            publication: Publication, player:
            SessionPlayerFacade
        ): Try<Unit, Exception> {
            val playlist = publication.readingOrder.toPlayList()
            val metadata = publicationMetadata(publication)
            return player.setPlaylist(playlist, metadata)
                .flatMap { player.prepare() }
                .toNavigatorResult()
        }

        private suspend fun goInitialLocator(
            publication: Publication,
            initialLocator: Locator?,
            player: SessionPlayerFacade
        ) {
            initialLocator?.let { locator ->
                val itemIndex = publication.readingOrder.indexOfFirstWithHref(locator.href)
                    ?: run { Timber.e("Invalid initial locator."); return }
                val position = locator.locations.time
                    ?: Duration.ZERO
                player.seekTo(itemIndex, position)
                    .onFailure { Timber.d("Failed to seek to the provided initial locator.") }
            }
        }

        private class PlayerException(
            val error: SessionPlayerError,
            override val message: String = "${error.name} error occurred in SessionPlayer."
        ) : Exception()

        internal fun SessionPlayerResult.toNavigatorResult(): Try<Unit, Exception> =
            if (isSuccess)
                Try.success(Unit)
            else
                this.mapFailure { PlayerException(it.error) }
    }
}
