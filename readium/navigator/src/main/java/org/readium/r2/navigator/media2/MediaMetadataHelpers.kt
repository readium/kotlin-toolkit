package org.readium.r2.navigator.media2

import androidx.core.os.bundleOf
import androidx.media2.common.MediaItem
import androidx.media2.common.MediaMetadata
import org.readium.r2.navigator.ExperimentalAudiobook
import org.readium.r2.shared.publication.Link
import org.readium.r2.shared.publication.Publication
import kotlin.time.Duration
import kotlin.time.ExperimentalTime

@ExperimentalAudiobook
internal fun List<MediaItem>.firstWithHref(href: String) = first { item ->
    item.metadata?.href == href.takeWhile { it !in "#?" }
}

@ExperimentalAudiobook
internal fun List<MediaItem>.indexOfFirstWithHref(href: String) = indexOfFirst { item ->
    item.metadata?.href == href.takeWhile { it !in "#?" }
}

@ExperimentalAudiobook
val MediaMetadata.href: String
    get() = getString(MediaMetadata.METADATA_KEY_MEDIA_URI)
        .let { checkNotNull(it) { "Missing href in item metadata."} }

@ExperimentalAudiobook
val MediaMetadata.index: Int
    get() = getLong(MediaMetadata.METADATA_KEY_TRACK_NUMBER).toInt()

@ExperimentalAudiobook
val MediaMetadata.title: String?
    get() = getString(MediaMetadata.METADATA_KEY_DISPLAY_TITLE)
        ?: getString(MediaMetadata.METADATA_KEY_TITLE)

private const val METADATA_KEY_MEDIA_TYPE = "readium.audio.metadata.MEDIA_TYPE"

@ExperimentalAudiobook
val MediaMetadata.type: String?
    get() = extras?.getString(METADATA_KEY_MEDIA_TYPE)

@ExperimentalAudiobook
@ExperimentalTime
val MediaMetadata.duration: Duration
    get() = getLong(MediaMetadata.METADATA_KEY_DURATION)
        .also { check(it != 0L) { "Missing duration in item metadata" } }
        .let { Duration.milliseconds(it) }


@ExperimentalAudiobook
fun linkMetadata(index: Int, link: Link): MediaMetadata {
    val extras = bundleOf(
        METADATA_KEY_MEDIA_TYPE to link.title
    )
    return MediaMetadata.Builder()
        .putLong(MediaMetadata.METADATA_KEY_TRACK_NUMBER, index.toLong())
        .putString(MediaMetadata.METADATA_KEY_MEDIA_URI, link.href)
        .putString(MediaMetadata.METADATA_KEY_DISPLAY_TITLE, link.title)
        .putLong(MediaMetadata.METADATA_KEY_DURATION, link.duration!!.toLong() * 1000)
        .setExtras(extras)
        .build()
}

@ExperimentalAudiobook
fun publicationMetadata(publication: Publication): MediaMetadata {
    val builder = MediaMetadata.Builder()
        .putString(MediaMetadata.METADATA_KEY_DISPLAY_TITLE, publication.metadata.title)

    publication.metadata.duration
        ?.let { builder.putLong(MediaMetadata.METADATA_KEY_DURATION, it.toLong() * 1000) }

    return builder.build()
}
