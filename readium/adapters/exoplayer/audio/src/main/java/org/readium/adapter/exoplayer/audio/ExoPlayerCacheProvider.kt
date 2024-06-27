package org.readium.adapter.exoplayer.audio

import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.cache.Cache
import org.readium.r2.shared.publication.Href
import org.readium.r2.shared.publication.Publication

@OptIn(UnstableApi::class)
public interface ExoPlayerCacheProvider {

    public fun getCache(publication: Publication): Cache?

    public fun computeKey(publication: Publication, href: Href): String
}
