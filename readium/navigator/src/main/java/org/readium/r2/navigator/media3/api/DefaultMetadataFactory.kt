/*
 * Copyright 2022 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.navigator.media3.api

import androidx.media3.common.MediaMetadata
import androidx.media3.common.MediaMetadata.PICTURE_TYPE_FRONT_COVER
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import org.readium.r2.shared.publication.Publication

internal class DefaultMetadataFactory(private val publication: Publication) : MediaMetadataFactory {

    private val coroutineScope =
        CoroutineScope(Dispatchers.Default)

    private val authors: String?
        get() = publication.metadata.authors
            .joinToString(", ") { it.name }.takeIf { it.isNotBlank() }

    private val cover: Deferred<ByteArray?> = coroutineScope.async {
        publication.linkWithRel("cover")
            ?.let { publication.get(it) }
            ?.read()
            ?.getOrNull()
    }

    @androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
    override suspend fun publicationMetadata(): MediaMetadata {
        val builder = MediaMetadata.Builder()
            .setTitle(publication.metadata.title)
            .setAlbumTitle(publication.metadata.title)
            .setTotalTrackCount(publication.readingOrder.size)

        authors
            ?.let {
                builder.setArtist(it)
                builder.setAlbumArtist(it)
            }

        cover.await()
            ?.let { builder.maybeSetArtworkData(it, PICTURE_TYPE_FRONT_COVER) }

        return builder.build()
    }

    override suspend fun resourceMetadata(index: Int): MediaMetadata {

        val builder = MediaMetadata.Builder()
        val link = publication.readingOrder[index]

        builder.setTrackNumber(index)
        // builder.setMediaUri(link.href)
        builder.setTitle(link.title)
        builder.setTitle(publication.metadata.title)
        builder.setAlbumTitle(publication.metadata.title)
        // builder.setDuration(MediaMetadata.METADATA_KEY_DURATION, (link.duration?.toLong() ?: 0) * 1000)

        authors
            ?.let {
                builder.setArtist(it)
                builder.setAlbumArtist(it)
            }

        cover.await()
            ?.let { builder.maybeSetArtworkData(it, PICTURE_TYPE_FRONT_COVER) }

        return builder.build()
    }
}
