/*
 * Copyright 2022 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.navigator.media3.audio

import android.media.MediaDataSource
import android.media.MediaMetadataRetriever
import android.media.MediaMetadataRetriever.METADATA_KEY_DURATION
import android.os.Build
import androidx.annotation.RequiresApi
import java.io.IOException
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.runBlocking
import org.readium.r2.shared.error.getOrThrow
import org.readium.r2.shared.fetcher.Fetcher

@RequiresApi(Build.VERSION_CODES.M)
internal class MetadataRetriever(
    private val resource: Fetcher.Resource
) {

    private val retriever: MediaMetadataRetriever =
        MediaMetadataRetriever()
            .apply {
                setDataSource(ResourceMediaDataSource(resource))
            }

    fun duration(): Duration? =
        retriever.extractMetadata(METADATA_KEY_DURATION)
            ?.toIntOrNull()
            ?.takeUnless { it == 0 }
            ?.milliseconds

    fun close() {
        runBlocking { resource.close() }
    }

    class ResourceMediaDataSource(
        private val resource: Fetcher.Resource
    ) : MediaDataSource() {

        override fun readAt(position: Long, buffer: ByteArray, offset: Int, size: Int): Int {
            if (size == 0) {
                return 0
            }

            val data = runBlocking {
                resource.read(position until position + size)
                    .mapFailure { IOException("Resource error", it) }
                    .getOrThrow()
            }

            if (data.isEmpty()) {
                return -1
            }

            data.copyInto(buffer, offset)
            return data.size
        }

        override fun getSize(): Long {
            return runBlocking {
                resource.length()
                    .mapFailure { IOException("Resource error", it) }
                    .getOrThrow()
            }
        }

        override fun close() {
            runBlocking { resource.close() }
        }
    }
}
