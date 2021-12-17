package org.readium.r2.navigator.media2

import android.content.Context
import android.os.Bundle
import androidx.media2.common.MediaMetadata
import androidx.media2.session.MediaController
import androidx.media2.session.SessionToken
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import org.readium.r2.navigator.ExperimentalAudiobook
import org.readium.r2.navigator.extensions.sum
import org.readium.r2.navigator.extensions.time
import org.readium.r2.shared.publication.Link
import org.readium.r2.shared.publication.Locator
import org.readium.r2.shared.publication.toLocator
import timber.log.Timber
import java.util.concurrent.Executors
import kotlin.time.Duration
import kotlin.time.ExperimentalTime

@ExperimentalAudiobook
@OptIn(ExperimentalTime::class)
class MediaSessionNavigator private constructor(
  private val controllerFacade: MediaControllerFacade,
  private val controllerCallback: MediaControllerCallback,
  private val configuration: Configuration,
) {

  val currentLocator: Flow<Locator> =
    combine(controllerCallback.currentItem, controllerCallback.currentPosition) { currentItem, currentPosition ->
      controllerFacade.locatorNow(currentItem, currentPosition)
    }

  val playbackState: Flow<MediaNavigatorPlayback> =
    combine(
      controllerCallback.mediaControllerState,
      controllerCallback.currentItem,
      controllerCallback.currentPosition,
      controllerCallback.bufferedPosition
    ) { currentState, currentItem, currentPosition, bufferedPosition ->
      when (currentState) {
        MediaControllerState.Paused, MediaControllerState.Playing ->
          MediaNavigatorPlayback.Playing(
            paused = currentState == MediaControllerState.Paused,
            currentItem = currentItem,
            currentPosition = currentPosition,
            bufferedPosition = bufferedPosition
          )
        MediaControllerState.Idle, MediaControllerState.Error ->
          MediaNavigatorPlayback.Error
      }
    }

  val playbackRate: Double
    get() = checkNotNull(controllerFacade.playbackSpeed)

  val playlist: List<MediaMetadata>
    get() = checkNotNull(controllerFacade.playlist).map { it.metadata!! }

  val totalDuration: Duration
    get() = playlist.map { it.duration }.sum()

  suspend fun prepare(): MediaNavigatorResult {
    return controllerFacade.prepare().toNavigatorResult()
  }

  suspend fun setPlaybackRate(rate: Double): MediaNavigatorResult =
    controllerFacade.setPlaybackSpeed(rate).toNavigatorResult()

  suspend fun play(): MediaNavigatorResult =
    controllerFacade.play().toNavigatorResult()

  suspend fun pause(): MediaNavigatorResult =
    controllerFacade.pause().toNavigatorResult()

  suspend fun seek(itemIndex: Int, position: Duration): MediaNavigatorResult=
    controllerFacade.seekTo(itemIndex, position).toNavigatorResult()

  suspend fun go(locator: Locator): MediaNavigatorResult {
    Timber.d("Go to locator $locator")
    val itemIndex = checkNotNull(controllerFacade.playlist).indexOfFirstWithHref(locator.href)
    val position = locator.locations.time ?: Duration.ZERO
    return seek(itemIndex, position)
  }

  suspend fun go(link: Link) =
    go(link.toLocator())

  suspend fun goForward(): MediaNavigatorResult =
    smartSeek(configuration.skipForwardInterval)

  suspend fun goBackward(): MediaNavigatorResult =
    smartSeek(-configuration.skipBackwardInterval)

  private suspend fun smartSeek(offset: Duration): MediaNavigatorResult {
    val(newIndex, newPosition) = SmartSeeker.dispatchSeek(
      offset,
      controllerFacade.currentPosition!!,
      controllerFacade.currentItem!!.metadata!!.index,
      playlist.map { it.duration }
    )
    Timber.d("Smart seeking by $offset resolved to item $newIndex position $newPosition")
    return controllerFacade.seekTo(newIndex, newPosition).toNavigatorResult()
  }

  fun close() {
    controllerFacade.close()
  }

  data class Configuration(
    val positionRefreshRate: Double = 2.0,  // Hz
    val skipForwardInterval: Duration = Duration.seconds(30),
    val skipBackwardInterval: Duration = Duration.seconds(30),
  )

  companion object {

    suspend fun create(
      context: Context,
      sessionToken: SessionToken,
      connectionHints: Bundle,
      configuration: Configuration = Configuration()
    ): MediaSessionNavigator {

      val positionRefreshDelay = Duration.seconds((1.0 / configuration.positionRefreshRate))
      val controllerCallback = MediaControllerCallback(positionRefreshDelay)
      val callbackExecutor = Executors.newSingleThreadExecutor()

      val mediaController = MediaController.Builder(context)
        .setConnectionHints(connectionHints)
        .setSessionToken(sessionToken)
        .setControllerCallback(callbackExecutor, controllerCallback)
        .build()

      val controllerFacade = MediaControllerFacade(mediaController)

      // Wait for the MediaController being connected and the playlist being ready.

      controllerCallback.connectedState.first { it }
      controllerCallback.mediaControllerState.first()
      controllerFacade.prepare()

      return MediaSessionNavigator(
        controllerFacade,
        controllerCallback,
        configuration,
      )
    }
  }
}
