/*
 * Copyright 2022 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

@file:OptIn(InternalReadiumApi::class)

package org.readium.navigator.media.common

import android.graphics.Bitmap
import android.net.Uri
import android.util.Size
import androidx.media3.common.MediaMetadata
import androidx.media3.common.MediaMetadata.PICTURE_TYPE_FRONT_COVER
import java.io.ByteArrayOutputStream
import kotlinx.coroutines.*
import org.readium.r2.shared.InternalReadiumApi
import org.readium.r2.shared.extensions.tryOrNull
import org.readium.r2.shared.publication.Publication
import org.readium.r2.shared.publication.services.coverFitting

/**
 * Builds media metadata using the given title, author and cover,
 * and fall back on what is in the publication.
 */
@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
internal class DefaultMediaMetadataFactory(
    private val publication: Publication,
    title: String? = null,
    author: String? = null,
    private val cover: Uri? = null,
) : MediaMetadataFactory {

    private val coroutineScope =
        CoroutineScope(Dispatchers.Default)

    private val title: String? =
        title ?: publication.metadata.title

    private val author: String? =
        author ?: publication.metadata.authors
            .firstOrNull { it.name.isNotBlank() }?.name

    private val coverBytes: Deferred<ByteArray?> = coroutineScope.async(start = CoroutineStart.LAZY) {
        tryOrNull {
            val byteStream = ByteArrayOutputStream(4096)
            // byte array will go cross processes and should be kept small
            publication.coverFitting(Size(400, 400))
                ?.compress(Bitmap.CompressFormat.PNG, 80, byteStream)
                ?.let { byteStream.toByteArray() }
        }
    }

    override suspend fun publicationMetadata(): MediaMetadata {
        val builder = MediaMetadata.Builder()
            .setTitle(title)
            .setTotalTrackCount(publication.readingOrder.size)

        author
            ?.let { builder.setArtist(it) }

        putCover(builder)

        return builder.build()
    }

    override suspend fun resourceMetadata(index: Int): MediaMetadata {
        val builder = MediaMetadata.Builder()
            .setTrackNumber(index)
            .setTitle(title)

        author
            ?.let { builder.setArtist(it) }

        putCover(builder)

        return builder.build()
    }

    private suspend fun putCover(builder: MediaMetadata.Builder) {
        cover
            ?.let { builder.setArtworkUri(it) }
            ?: run {
                coverBytes.await()
                    ?.let { builder.setArtworkData(it, PICTURE_TYPE_FRONT_COVER) }
            }
    }
}
