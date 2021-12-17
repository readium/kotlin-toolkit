package org.readium.r2.navigator.media2

import android.content.Context
import androidx.media2.common.SessionPlayer
import com.google.android.exoplayer2.C
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.SimpleExoPlayer
import com.google.android.exoplayer2.audio.AudioAttributes
import com.google.android.exoplayer2.ext.media2.SessionPlayerConnector
import com.google.android.exoplayer2.source.DefaultMediaSourceFactory
import com.google.android.exoplayer2.upstream.DataSource
import com.google.android.exoplayer2.upstream.cache.Cache
import com.google.android.exoplayer2.upstream.cache.CacheDataSource
import org.readium.r2.navigator.ExperimentalAudiobook
import org.readium.r2.navigator.audio.PublicationDataSource
import org.readium.r2.shared.publication.Publication
import org.readium.r2.shared.publication.services.isRestricted
import kotlin.time.Duration
import kotlin.time.ExperimentalTime

@ExperimentalAudiobook
@OptIn(ExperimentalTime::class)
class ExoPublicationPlayer(
    private val cache: Cache? = null
) : PublicationPlayer {

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

    override fun open(context: Context, publication: Publication): SessionPlayer? {
        if (publication.isRestricted) {
            return null
        }

        val dataSourceFactory = createDataSource(publication, cache)
        val player: ExoPlayer = SimpleExoPlayer.Builder(context)
            .setSeekBackIncrementMs(Duration.seconds(30).inWholeMilliseconds)
            .setSeekForwardIncrementMs(Duration.seconds(30).inWholeMilliseconds)
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
}