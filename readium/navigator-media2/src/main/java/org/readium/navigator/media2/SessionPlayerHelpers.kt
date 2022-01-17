/*
 * Copyright 2022 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.navigator.media2

import androidx.core.os.bundleOf
import androidx.media2.common.MediaItem
import androidx.media2.common.MediaMetadata
import androidx.media2.common.SessionPlayer
import org.readium.r2.shared.publication.Link
import org.readium.r2.shared.publication.Publication
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.ExperimentalTime

internal val SessionPlayer.stateEnum: SessionPlayerState
    get() = SessionPlayerState.from(playerState)

internal val SessionPlayer.playbackSpeedNullable
    get() =  playbackSpeed.takeUnless { it == 0f  }?.toDouble()

@ExperimentalTime
internal val SessionPlayer.currentPositionDuration: Duration?
    get() = msToDuration(currentPosition)

@ExperimentalTime
internal val SessionPlayer.bufferedPositionDuration: Duration?
    get() = msToDuration(bufferedPosition)

@ExperimentalTime
private fun msToDuration(ms: Long): Duration? =
    if (ms == SessionPlayer.UNKNOWN_TIME)
        null
    else
        ms.milliseconds

/**
 * Metadata
 */

internal val MediaMetadata.href: String
    get() = getString(MediaMetadata.METADATA_KEY_MEDIA_URI)
        .let { checkNotNull(it) { "Missing href in item metadata."} }

internal val MediaMetadata.index: Int
    get() = getLong(MediaMetadata.METADATA_KEY_TRACK_NUMBER).toInt()

internal val MediaMetadata.title: String?
    get() = getString(MediaMetadata.METADATA_KEY_DISPLAY_TITLE)
        ?: getString(MediaMetadata.METADATA_KEY_TITLE)

private const val METADATA_KEY_MEDIA_TYPE = "readium.audio.metadata.MEDIA_TYPE"

internal val MediaMetadata.type: String?
    get() = extras?.getString(METADATA_KEY_MEDIA_TYPE)

@ExperimentalTime
internal val MediaMetadata.duration: Duration?
    get() = getLong(MediaMetadata.METADATA_KEY_DURATION)
        .takeUnless { it == 0L }
        ?.milliseconds

internal fun linkMetadata(index: Int, link: Link): MediaMetadata {
    val extras = bundleOf(
        METADATA_KEY_MEDIA_TYPE to link.title
    )
    return MediaMetadata.Builder()
        .putLong(MediaMetadata.METADATA_KEY_TRACK_NUMBER, index.toLong())
        .putString(MediaMetadata.METADATA_KEY_MEDIA_URI, link.href)
        .putString(MediaMetadata.METADATA_KEY_DISPLAY_TITLE, link.title)
        .putLong(MediaMetadata.METADATA_KEY_DURATION, (link.duration?.toLong() ?: 0) * 1000)
        .setExtras(extras)
        .build()
}

internal fun List<Link>.toPlayList(): List<MediaItem> =
    mapIndexed { index, link ->
        MediaItem.Builder()
            .setMetadata(linkMetadata(index, link))
            .build()
    }

internal fun publicationMetadata(publication: Publication): MediaMetadata {
    val builder = MediaMetadata.Builder()
        .putString(MediaMetadata.METADATA_KEY_DISPLAY_TITLE, publication.metadata.title)

    publication.metadata.duration
        ?.let { builder.putLong(MediaMetadata.METADATA_KEY_DURATION, it.toLong() * 1000) }

    return builder.build()
}

@ExperimentalTime
internal val List<MediaMetadata>.durations: List<Duration>?
    get() {
        val durations = mapNotNull { it.duration }
        return durations.takeIf { it.size == this.size }
    }

@ExperimentalTime
internal val List<MediaItem>.metadata: List<MediaMetadata>
    get() = map { it.metadata!! }

