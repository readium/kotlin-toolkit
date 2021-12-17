package org.readium.r2.navigator.media2

import androidx.media2.common.MediaMetadata
import androidx.media2.common.SessionPlayer
import org.readium.r2.navigator.ExperimentalAudiobook
import org.readium.r2.navigator.extensions.sum
import org.readium.r2.shared.publication.Locator
import kotlin.time.Duration
import kotlin.time.ExperimentalTime

@ExperimentalAudiobook
@ExperimentalTime
internal fun MediaControllerFacade.locatorNow(item: MediaMetadata, position: Duration): Locator {
    val playlist = checkNotNull(this.playlist).map { it.metadata!! }
    return locator(item, position, playlist)
}

@ExperimentalAudiobook
@ExperimentalTime
fun SessionPlayer?.locatorNow(): Locator? {
    if (this == null) return null

    val playlist = this.playlist
        ?.map { it.metadata!! }
        ?: return null

    val item = this.currentMediaItem?.metadata
        ?: return null

    val position = Duration.seconds(this.currentPosition / 1000)

    return locator(item, position, playlist)
}

@ExperimentalAudiobook
@ExperimentalTime
private fun locator(item: MediaMetadata, position: Duration, playlist: List<MediaMetadata>): Locator {

    fun itemStartPosition(index: Int) =
        playlist.slice(0 until index).map { it.duration }.sum()

    fun totalProgression(item: MediaMetadata, position: Duration) =
        (itemStartPosition(item.index) + position) / playlist.map { it.duration }.sum()

    return Locator(
        href = item.href,
        title = item.title,
        type = item.type ?: "",
        locations = Locator.Locations(
            fragments = listOf("t=${position.inWholeSeconds}"),
            progression = position / item.duration,
            position = item.index,
            totalProgression = totalProgression(item, position)
        )
    )
}
