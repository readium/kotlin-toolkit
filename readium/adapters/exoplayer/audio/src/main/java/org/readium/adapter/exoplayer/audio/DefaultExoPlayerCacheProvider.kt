@file:kotlin.OptIn(InternalReadiumApi::class)

package org.readium.adapter.exoplayer.audio

import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.FileDataSource
import androidx.media3.datasource.cache.Cache
import androidx.media3.datasource.cache.CacheDataSink
import androidx.media3.datasource.cache.CacheDataSource
import org.readium.r2.shared.DelicateReadiumApi
import org.readium.r2.shared.InternalReadiumApi
import org.readium.r2.shared.publication.Publication
import org.readium.r2.shared.util.Url
import org.readium.r2.shared.util.toUrl

/**
 * Uses the given cache only for remote publications and URLs resolved against the
 * publication base URL as cache keys.
 */
@OptIn(UnstableApi::class)
public class DefaultExoPlayerCacheProvider(
    private val cache: Cache,
) : ExoPlayerCacheProvider {

    @kotlin.OptIn(DelicateReadiumApi::class)
    override fun getCache(publication: Publication): Cache? =
        cache.takeUnless { publication.baseUrl == null }

    override fun computeKey(publication: Publication, url: Url): String =
        (publication.baseUrl?.resolve(url) ?: url).normalize().toString()
}

@OptIn(UnstableApi::class)
public fun ExoPlayerCacheProvider.createCacheDataSourceFactory(
    publication: Publication,
): CacheDataSource.Factory? {
    val cache = getCache(publication) ?: return null
    val upstreamDataSourceFactory = ExoPlayerDataSource.Factory(publication)

    return CacheDataSource.Factory()
        .setCache(cache)
        .setCacheKeyFactory { dataSpec -> computeKey(publication, dataSpec.uri.toUrl()!!) }
        .setCacheWriteDataSinkFactory(CacheDataSink.Factory().setCache(cache))
        .setCacheReadDataSourceFactory(FileDataSource.Factory())
        .setUpstreamDataSourceFactory(upstreamDataSourceFactory)
        .setFlags(CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR)
}
