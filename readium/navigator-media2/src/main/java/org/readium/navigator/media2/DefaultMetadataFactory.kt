package org.readium.navigator.media2

import android.graphics.Bitmap
import androidx.media2.common.MediaMetadata
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import org.readium.r2.shared.publication.Publication
import org.readium.r2.shared.publication.services.cover

@ExperimentalMedia2
internal class DefaultMetadataFactory(private val publication: Publication) : MediaMetadataFactory {

    private val coroutineScope =
        CoroutineScope(Dispatchers.Default)

    private val authors: String?
        get() = publication.metadata.authors
            .joinToString(", ") { it.name }.takeIf { it.isNotBlank() }

    private val cover: Deferred<Bitmap?> = coroutineScope.async {
        publication.cover()
    }

    override suspend fun publicationMetadata(): MediaMetadata {
        val builder = MediaMetadata.Builder()
            .putString(MediaMetadata.METADATA_KEY_TITLE, publication.metadata.title)
            .putString(MediaMetadata.METADATA_KEY_ALBUM, publication.metadata.title)
            .putLong(MediaMetadata.METADATA_KEY_NUM_TRACKS, publication.readingOrder.size.toLong())

        authors
            ?.let {
                builder.putString(MediaMetadata.METADATA_KEY_AUTHOR, it)
                builder.putString(MediaMetadata.METADATA_KEY_ARTIST, it)
            }

        publication.metadata.duration
            ?.let { builder.putLong(MediaMetadata.METADATA_KEY_DURATION, it.toLong() * 1000) }

        cover.await()
            ?.let { builder.putBitmap(MediaMetadata.METADATA_KEY_ART, it) }

        return builder.build()
    }

    override suspend fun resourceMetadata(index: Int): MediaMetadata {
        // See the implementation for how each metadata is used in media2:
        // https://cs.android.com/androidx/platform/frameworks/support/+/androidx-main:media2/media2-session/src/main/java/androidx/media2/session/MediaNotificationHandler.java;l=175?q=MediaNotificationHandler

        val builder = MediaMetadata.Builder()
        val link = publication.readingOrder[index]
        builder.putLong(MediaMetadata.METADATA_KEY_TRACK_NUMBER, index.toLong())
        builder.putString(MediaMetadata.METADATA_KEY_MEDIA_URI, link.href)
        builder.putString(MediaMetadata.METADATA_KEY_TITLE, link.title)
        builder.putString(MediaMetadata.METADATA_KEY_DISPLAY_SUBTITLE, publication.metadata.title)
        builder.putString(MediaMetadata.METADATA_KEY_ALBUM, publication.metadata.title)
        builder.putLong(MediaMetadata.METADATA_KEY_DURATION, (link.duration?.toLong() ?: 0) * 1000)

        authors?.let {
            builder.putString(MediaMetadata.METADATA_KEY_ARTIST, it)
            builder.putString(MediaMetadata.METADATA_KEY_AUTHOR, it)
        }
        cover.await()?.let { builder.putBitmap(MediaMetadata.METADATA_KEY_ART, it) }
        return builder.build()
    }
}
