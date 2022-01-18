package org.readium.navigator.media2

import androidx.media2.common.MediaMetadata
import org.readium.r2.shared.publication.Publication
import org.readium.r2.shared.publication.services.cover

class DefaultMetadataFactory(private val publication: Publication): MediaMetadataFactory {

    private val authors: String?
        get() = publication.metadata.authors
            .joinToString(", ") { it.name }.takeIf { it.isNotBlank() }

    override val fillPublicationMetadata: suspend MediaMetadata.Builder.() -> Unit
        get() = {
            putString(MediaMetadata.METADATA_KEY_TITLE, publication.metadata.title)
            putString(MediaMetadata.METADATA_KEY_ALBUM, publication.metadata.title)
            putString(MediaMetadata.METADATA_KEY_DISPLAY_SUBTITLE, publication.metadata.title)
            putLong(MediaMetadata.METADATA_KEY_NUM_TRACKS, publication.readingOrder.size.toLong())

            authors
                ?.let { putString(MediaMetadata.METADATA_KEY_AUTHOR,  it) }

            publication.metadata.duration
                ?.let { putLong(MediaMetadata.METADATA_KEY_DURATION, it.toLong() * 1000) }

            publication.cover()
                ?.let { putBitmap(MediaMetadata.METADATA_KEY_ART, it)}
        }

    override val fillResourceMetadata: suspend MediaMetadata.Builder.(Int) -> Unit
        get() = { index ->
            val link = publication.readingOrder[index]
            putLong(MediaMetadata.METADATA_KEY_TRACK_NUMBER, index.toLong())
            putString(MediaMetadata.METADATA_KEY_MEDIA_URI, link.href)
            putString(MediaMetadata.METADATA_KEY_TITLE, link.title)
            putString(MediaMetadata.METADATA_KEY_ALBUM, publication.metadata.title)
            putString(MediaMetadata.METADATA_KEY_DISPLAY_SUBTITLE, publication.metadata.title)
            putLong(MediaMetadata.METADATA_KEY_DURATION, (link.duration?.toLong() ?: 0) * 1000)

            authors?.let { putString(MediaMetadata.METADATA_KEY_AUTHOR, it) }
            publication.cover()?.let { putBitmap(MediaMetadata.METADATA_KEY_ART, it)}
        }
}