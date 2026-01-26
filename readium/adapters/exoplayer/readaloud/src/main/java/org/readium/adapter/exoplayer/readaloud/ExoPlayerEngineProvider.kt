/*
 * Copyright 2025 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

@file:OptIn(InternalReadiumApi::class)

package org.readium.adapter.exoplayer.readaloud

import android.app.Application
import org.readium.adapter.exoplayer.audio.ExoPlayerDataSource
import org.readium.navigator.media.readaloud.AudioChunk
import org.readium.navigator.media.readaloud.AudioEngineFactory
import org.readium.navigator.media.readaloud.AudioEngineProgress
import org.readium.navigator.media.readaloud.AudioEngineProvider
import org.readium.navigator.media.readaloud.PlaybackEngine
import org.readium.r2.shared.ExperimentalReadiumApi
import org.readium.r2.shared.InternalReadiumApi
import org.readium.r2.shared.publication.Publication

@ExperimentalReadiumApi
public class ExoPlayerEngineProvider(
    private val application: Application,
) : AudioEngineProvider<ExoPlayerEngine.Error> {

    override fun createEngineFactory(publication: Publication): ExoPlayerEngineFactory {
        val dataSourceFactory = ExoPlayerDataSource.Factory(publication)
        return ExoPlayerEngineFactory(application, dataSourceFactory)
    }
}

@ExperimentalReadiumApi
public class ExoPlayerEngineFactory internal constructor(
    private val application: Application,
    private val dataSourceFactory: ExoPlayerDataSource.Factory,
) : AudioEngineFactory<ExoPlayerEngine.Error> {

    override fun createPlaybackEngine(
        chunks: List<AudioChunk>,
        listener: PlaybackEngine.Listener<AudioEngineProgress, ExoPlayerEngine.Error>,
    ): PlaybackEngine {
        return ExoPlayerEngine(application, dataSourceFactory, chunks, listener)
    }
}
