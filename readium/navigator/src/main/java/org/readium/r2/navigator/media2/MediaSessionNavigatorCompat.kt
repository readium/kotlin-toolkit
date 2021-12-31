package org.readium.r2.navigator.media2

import android.content.Context
import android.os.Bundle
import androidx.media2.common.MediaMetadata
import androidx.media2.session.SessionToken
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collect
import org.readium.r2.navigator.ExperimentalAudiobook
import org.readium.r2.shared.publication.Link
import org.readium.r2.shared.publication.Locator
import org.readium.r2.shared.publication.Publication
import kotlin.time.Duration
import kotlin.time.ExperimentalTime

@ExperimentalAudiobook
@OptIn(ExperimentalTime::class, ExperimentalCoroutinesApi::class)
class MediaSessionNavigatorCompat(
    context: Context,
    override val publication: Publication,
    sessionToken: SessionToken,
    connectionHints: Bundle,
    private val configuration: MediaSessionNavigator.Configuration = MediaSessionNavigator.Configuration(),
) : MediaNavigator {

    /**
     * This must be a single thread executor in order for commands to be executed in the right order
     * once the navigator is initialized.
     */

    private val coroutineScope =
        CoroutineScope(Dispatchers.Default + SupervisorJob())

    private val navigator: Deferred<MediaSessionNavigator> =
        coroutineScope.async {
            MediaSessionNavigator.create(context, sessionToken, connectionHints, configuration,)
        }

    private val _currentLocator: MutableStateFlow<Locator> =
        MutableStateFlow(Locator(href = "#", type = "")).also { stateFlow ->
            coroutineScope.launch {
                navigator.await().currentLocator.collect { locator -> stateFlow.value = locator }
            }
        }

    private val _playback: MutableStateFlow<MediaNavigatorPlayback?> =
        MutableStateFlow<MediaNavigatorPlayback?>(null).also { stateFlow ->
            coroutineScope.launch {
                navigator.await().playbackState.collect { playback -> stateFlow.value = playback }
            }
        }

    override val currentLocator: StateFlow<Locator>
        get() = _currentLocator

    override val playback: StateFlow<MediaNavigatorPlayback?>
        get() = _playback

    override val playlist: List<MediaMetadata>?
        get() = navigator.takeIf { it.isCompleted }?.getCompleted()?.playlist

    override suspend fun play() =
        navigator.await().play()

    override suspend fun pause() =
        navigator.await().pause()

    override suspend fun setPlaybackRate(rate: Double) =
        navigator.await().setPlaybackRate(rate)

    override suspend fun seek(itemIndex: Int, position: Duration) =
        navigator.await().seek(itemIndex, position)

    override suspend fun goForward() =
        navigator.await().goForward()

    override suspend fun goBackward() =
        navigator.await().goForward()

    override suspend fun go(link: Link) =
        navigator.await().go(link)

    override suspend fun go(locator: Locator) =
        navigator.await().go(locator)

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

    override fun close() {
        coroutineScope.cancel()
    }

    private fun launch(runnable: suspend () -> Unit) =
        coroutineScope.launch{ runnable() }

    private fun launchAndRun(runnable: suspend () -> Unit, callback: () -> Unit) =
        coroutineScope.launch { runnable() }.invokeOnCompletion { callback() }
}
