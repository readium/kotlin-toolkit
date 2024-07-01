package org.readium.adapter.exoplayer.audio

import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.cache.Cache
import org.readium.r2.shared.DelicateReadiumApi
import org.readium.r2.shared.publication.Publication
import org.readium.r2.shared.util.Url

/**
 * Uses the given cache only for remote publications and URLs resolved against the
 * publication base URL as cache keys.
 */
@OptIn(UnstableApi::class)
public class DefaultExoPlayerCacheProvider(
    private val cache: Cache
) : ExoPlayerCacheProvider {

    @kotlin.OptIn(DelicateReadiumApi::class)
    override fun getCache(publication: Publication): Cache? =
        cache.takeUnless { publication.baseUrl == null }

    override fun computeKey(publication: Publication, url: Url): String =
        (publication.baseUrl?.resolve(url) ?: url).normalize().toString()
}
