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

/**
 * Builds media metadata using the given title, author and cover,
 * and fall back on what is in the publication.
 */
@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
internal class DefaultMediaMetadataFactory(
    private val publication: Publication,
    title: String? = null,
    author: String? = null,
    cover: ByteArray? = null
) : MediaMetadataFactory {

    private val coroutineScope =
        CoroutineScope(Dispatchers.Default)

    private val title: String =
        title ?: publication.metadata.title

    private val authors: String? =
        author ?: publication.metadata.authors
            .firstOrNull { it.name.isNotBlank() }?.name

    private val cover: Deferred<ByteArray?> = coroutineScope.async {
        cover ?: publication.linkWithRel("cover")
            ?.let { publication.get(it) }
            ?.read()
            ?.getOrNull()
    }

    override suspend fun publicationMetadata(): MediaMetadata {
        val builder = MediaMetadata.Builder()
            .setTitle(title)
            .setTotalTrackCount(publication.readingOrder.size)

        authors
            ?.let { builder.setArtist(it) }

        cover.await()
            ?.let { builder.maybeSetArtworkData(it, PICTURE_TYPE_FRONT_COVER) }

        return builder.build()
    }

    override suspend fun resourceMetadata(index: Int): MediaMetadata {
        val builder = MediaMetadata.Builder()
            .setTrackNumber(index)
            .setTitle(title)

        authors
            ?.let { builder.setArtist(it) }

        cover.await()
            ?.let { builder.maybeSetArtworkData(it, PICTURE_TYPE_FRONT_COVER) }

        return builder.build()
    }
}
