package org.readium.navigator.media2

import androidx.media2.common.MediaMetadata
import org.readium.r2.shared.publication.Publication
import org.readium.r2.shared.publication.services.cover

class DefaultMetadataFactory(private val publication: Publication): MediaMetadataFactory {

    private val authors: String?
        get() = publication.metadata.authors
            .joinToString(", ") { it.name }.takeIf { it.isNotBlank() }

    override suspend fun publicationMetadata(): MediaMetadata {
        val builder = MediaMetadata.Builder()
            .putString(MediaMetadata.METADATA_KEY_TITLE, publication.metadata.title)
            .putString(MediaMetadata.METADATA_KEY_ALBUM, publication.metadata.title)
            .putString(MediaMetadata.METADATA_KEY_DISPLAY_SUBTITLE, publication.metadata.title)
            .putLong(MediaMetadata.METADATA_KEY_NUM_TRACKS, publication.readingOrder.size.toLong())

        authors
            ?.let { builder.putString(MediaMetadata.METADATA_KEY_AUTHOR,  it) }

        publication.metadata.duration
            ?.let { builder.putLong(MediaMetadata.METADATA_KEY_DURATION, it.toLong() * 1000) }

        publication.cover()
            ?.let { builder.putBitmap(MediaMetadata.METADATA_KEY_ART, it)}

        return builder.build()
    }

    override suspend fun resourceMetadata(index: Int): MediaMetadata {
        val builder = MediaMetadata.Builder()
        val link = publication.readingOrder[index]
        builder.putLong(MediaMetadata.METADATA_KEY_TRACK_NUMBER, index.toLong())
        builder.putString(MediaMetadata.METADATA_KEY_MEDIA_URI, link.href)
        builder.putString(MediaMetadata.METADATA_KEY_TITLE, link.title)
        builder.putString(MediaMetadata.METADATA_KEY_ALBUM, publication.metadata.title)
        builder.putString(MediaMetadata.METADATA_KEY_DISPLAY_SUBTITLE, publication.metadata.title)
        builder.putLong(MediaMetadata.METADATA_KEY_DURATION, (link.duration?.toLong() ?: 0) * 1000)

        authors?.let { builder.putString(MediaMetadata.METADATA_KEY_AUTHOR, it) }
        publication.cover()?.let { builder.putBitmap(MediaMetadata.METADATA_KEY_ART, it)}
        return builder.build()
    }
}