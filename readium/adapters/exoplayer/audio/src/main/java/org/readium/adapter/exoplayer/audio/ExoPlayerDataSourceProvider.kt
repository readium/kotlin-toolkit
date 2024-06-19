package org.readium.adapter.exoplayer.audio

import androidx.media3.datasource.DataSource
import org.readium.r2.shared.publication.Publication

public fun interface ExoPlayerDataSourceProvider {

    public fun createDataSourceFactory(publication: Publication): DataSource.Factory
}
