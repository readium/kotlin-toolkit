package org.readium.adapter.exoplayer.audio

import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.cache.Cache
import org.readium.r2.shared.publication.Publication
import org.readium.r2.shared.util.Url

/**
 *  To be implemented to provide ExoPlayer with caching ability.
 */
@OptIn(UnstableApi::class)
public interface ExoPlayerCacheProvider {

    /**
     * Returns the cache to use or null if caching is not necessary with the given publication.
     */
    public fun getCache(publication: Publication): Cache?

    /**
     * Computes a unique cache key for the resource of [publication] at [url] . It can be an
     * absolute URL or a mix of [url] with some publication identifier.
     */
    public fun computeKey(publication: Publication, url: Url): String
}
