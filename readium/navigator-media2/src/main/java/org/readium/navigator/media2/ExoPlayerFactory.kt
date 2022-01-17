/*
 * Copyright 2022 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.navigator.media2

import android.content.Context
import androidx.media2.common.SessionPlayer
import com.google.android.exoplayer2.C
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.audio.AudioAttributes
import com.google.android.exoplayer2.ext.media2.SessionPlayerConnector
import com.google.android.exoplayer2.source.DefaultMediaSourceFactory
import com.google.android.exoplayer2.upstream.DataSource
import com.google.android.exoplayer2.upstream.cache.Cache
import com.google.android.exoplayer2.upstream.cache.CacheDataSource
import org.readium.r2.shared.publication.Publication

@ExperimentalMedia2
class ExoPlayerFactory(private val cache: Cache? = null) {

    fun createPlayer(context: Context, publication: Publication): SessionPlayer {
        val dataSourceFactory = createDataSource(publication, cache)
        val player: ExoPlayer = ExoPlayer.Builder(context)
            .setMediaSourceFactory(DefaultMediaSourceFactory(dataSourceFactory))
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setContentType(C.CONTENT_TYPE_MUSIC)
                    .setUsage(C.USAGE_MEDIA)
                    .build(),
                true
            )
            .setHandleAudioBecomingNoisy(true)
            .build()

        return SessionPlayerConnector(player)
    }

    private fun createDataSource(publication: Publication, cache: Cache?): DataSource.Factory {
        var factory: DataSource.Factory = PublicationDataSource.Factory(publication)

        if (cache != null) {
            factory = CacheDataSource.Factory()
                .setCache(cache)
                .setUpstreamDataSourceFactory(factory)
                // Disable writing to the cache by the player. We'll handle downloads through the
                // service.
                .setCacheWriteDataSinkFactory(null)
        }

        return factory
    }
}