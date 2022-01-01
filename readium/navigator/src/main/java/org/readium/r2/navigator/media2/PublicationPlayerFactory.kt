package org.readium.r2.navigator.media2

import android.content.Context
import androidx.media2.common.MediaItem
import androidx.media2.common.SessionPlayer
import com.google.common.util.concurrent.MoreExecutors
import org.readium.r2.navigator.ExperimentalAudiobook
import org.readium.r2.shared.publication.Locator
import org.readium.r2.shared.publication.Publication
import timber.log.Timber

@ExperimentalAudiobook
class PublicationPlayerFactory(
    engines: List<PublicationPlayer> = emptyList(),
    ignoreDefaultEngines: Boolean = false
) {
    private val defaultEngines: List<PublicationPlayer> by lazy {
        listOf(
           ExoPublicationPlayer()
        )
    }

    private val engines: List<PublicationPlayer> = engines +
            if (!ignoreDefaultEngines) defaultEngines else emptyList()

    fun open(context: Context, publication: Publication, initialLocator: Locator?): SessionPlayer? =
        engines.lazyMapFirstNotNullOrNull { it.open(context, publication) }
            ?.also { preparePlayer(publication, initialLocator, it) }

    @Suppress("UNCHECKED_CAST")
    private inline fun <T, R> List<T>.lazyMapFirstNotNullOrNull(transform: (T) -> R): R? {
        for (it in this) {
            return transform(it) ?: continue
        }
        return null
    }

    private fun preparePlayer(publication: Publication, initialLocator: Locator?, player: SessionPlayer) {
        val list = publication.readingOrder.mapIndexed { index, link ->
            MediaItem.Builder()
                .setMetadata(linkMetadata(index, link))
                .build()
        }
        val metadata = publicationMetadata(publication)
        val future = player.setPlaylist(list, metadata)
        future.addListener(
            { Timber.d("SessionPlayer setPlayList finished with code ${future.get().resultCode}") },
            MoreExecutors.directExecutor()
        )
    }
}
